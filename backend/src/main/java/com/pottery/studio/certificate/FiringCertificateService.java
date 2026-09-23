package com.pottery.studio.certificate;

import com.pottery.studio.common.BizException;
import com.pottery.studio.course.Artwork;
import com.pottery.studio.course.ArtworkRepository;
import com.pottery.studio.course.Course;
import com.pottery.studio.course.CourseRepository;
import com.pottery.studio.firing.FiringBatch;
import com.pottery.studio.firing.FiringBatchRepository;
import com.pottery.studio.firing.Kiln;
import com.pottery.studio.firing.KilnRepository;
import com.pottery.studio.greenware.Greenware;
import com.pottery.studio.greenware.GreenwareRepository;
import com.pottery.studio.material.Material;
import com.pottery.studio.material.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 烧成履历凭证服务。
 *
 * 核心约束：
 * 1. 凭证行 append-only：首签 V1，更正只追加新版本并把旧版置 SUPERSEDED，绝不覆盖/删除旧版；
 * 2. 签发前必须逐段核对 作品 → 课程 / 坯体 → 泥料(釉料) / 烧成批次 → 窑炉 的来源链，
 *    缺来源或关联冲突时不能生成"看似完整"的凭证，并指出缺的是哪段；
 * 3. 凭证内容是签发时刻的快照，业务表之后被修正不影响旧凭证；
 * 4. 签发/更正带乐观版本号 expectedCurrentId，别人已更新版本时旧页面请求被拒，要求重新读取。
 */
@Service
public class FiringCertificateService {

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final FiringCertificateRepository certificateRepository;
    private final ArtworkRepository artworkRepository;
    private final CourseRepository courseRepository;
    private final GreenwareRepository greenwareRepository;
    private final MaterialRepository materialRepository;
    private final FiringBatchRepository batchRepository;
    private final KilnRepository kilnRepository;

    public FiringCertificateService(FiringCertificateRepository certificateRepository,
                                    ArtworkRepository artworkRepository,
                                    CourseRepository courseRepository,
                                    GreenwareRepository greenwareRepository,
                                    MaterialRepository materialRepository,
                                    FiringBatchRepository batchRepository,
                                    KilnRepository kilnRepository) {
        this.certificateRepository = certificateRepository;
        this.artworkRepository = artworkRepository;
        this.courseRepository = courseRepository;
        this.greenwareRepository = greenwareRepository;
        this.materialRepository = materialRepository;
        this.batchRepository = batchRepository;
        this.kilnRepository = kilnRepository;
    }

    // ================= 读 =================

    /** 作品详情页用的凭证聚合：版本 + 来源核对 + 当前资料差异 */
    public CertificateAggregate getAggregate(Long artworkId) {
        Artwork artwork = requireArtwork(artworkId);
        List<FiringCertificate> versions =
                certificateRepository.findByArtworkIdOrderByVersionNoAsc(artworkId);
        FiringCertificate current = versions.stream()
                .filter(v -> FiringCertificate.CURRENT.equals(v.getStatus()))
                .reduce((a, b) -> b)
                .orElse(null);

        CertificateAggregate agg = new CertificateAggregate();
        agg.setArtworkId(artworkId);
        agg.setIssued(current != null);
        agg.setVersions(versions);
        agg.setCurrent(current);
        agg.setCurrentId(current == null ? null : current.getId());

        ProvenanceView provenance = buildProvenance(artwork);
        agg.setProvenance(provenance);

        if (current != null) {
            List<FieldDiff> diffs = diffAgainst(current, artwork, provenance);
            agg.setDiffs(diffs);
            agg.setSourceChanged(!diffs.isEmpty());
        } else {
            agg.setDiffs(List.of());
            agg.setSourceChanged(false);
        }
        return agg;
    }

    /** 打印 / 查看单个版本；历史版本同样可读，内容就是当时快照 */
    public FiringCertificate getVersion(Long artworkId, Integer versionNo) {
        FiringCertificate cert = certificateRepository
                .findByArtworkIdAndVersionNo(artworkId, versionNo)
                .orElseThrow(() -> new BizException(
                        "作品 id=" + artworkId + " 的第 " + versionNo + " 版凭证不存在"));
        if (!artworkId.equals(cert.getArtworkId())) {
            throw new BizException("凭证与作品不匹配");
        }
        return cert;
    }

    // ================= 签发 / 更正 =================

    /** 首次签发：从当前可核实数据生成 V1；升级前已登记的老作品同样走这里补签，不重建历史。 */
    @Transactional
    public CertificateAggregate issue(Long artworkId, String issuedBy, Long expectedCurrentId) {
        Artwork artwork = artworkRepository.findLockById(artworkId)
                .orElseThrow(() -> new BizException("作品不存在，id=" + artworkId));
        FiringCertificate existing = findCurrent(artworkId);
        if (existing != null) {
            throw new BizException("作品【" + artwork.getTitle()
                    + "】已存在第 " + existing.getVersionNo() + " 版凭证，资料有变化请走「发起更正」，不能重复首签");
        }
        // 乐观锁：页面读取时没有凭证（null），签发瞬间也必须仍没有
        if (expectedCurrentId != null) {
            throw conflictOrReject(artwork);
        }
        ProvenanceView view = requireIssuable(artwork);
        FiringCertificate v1 = snapshot(artwork, view, 1, FiringCertificate.CURRENT, null,
                normalizeIssuer(issuedBy));
        certificateRepository.save(v1);
        return getAggregate(artworkId);
    }

    /**
     * 发起更正：对照差异后追加新版本，不覆盖旧版。
     * expectedCurrentId 必须等于页面读取时看到的当前凭证 id；
     * 若另一名工作人员已更新过版本，这里拒绝并要求重新读取，避免旧数据覆盖后续记录。
     */
    @Transactional
    public CertificateAggregate correct(Long artworkId, String issuedBy, String reason,
                                        Long expectedCurrentId) {
        Artwork artwork = artworkRepository.findLockById(artworkId)
                .orElseThrow(() -> new BizException("作品不存在，id=" + artworkId));
        FiringCertificate current = findCurrent(artworkId);
        if (current == null) {
            throw new BizException("作品【" + artwork.getTitle() + "】还没有签发过凭证，请先首签");
        }
        if (expectedCurrentId == null || !expectedCurrentId.equals(current.getId())) {
            throw conflictOrReject(artwork);
        }
        if (reason == null || reason.isBlank()) {
            throw new BizException("发起更正必须填写更正原因，以便和旧版本对照留痕");
        }
        ProvenanceView view = requireIssuable(artwork);
        List<FieldDiff> diffs = diffAgainst(current, artwork, view);
        if (diffs.isEmpty()) {
            throw new BizException("当前资料与第 " + current.getVersionNo()
                    + " 版凭证完全一致，没有可更正的差异，无需追加新版本");
        }
        // 旧版保留为历史，绝不覆盖
        current.setStatus(FiringCertificate.SUPERSEDED);
        certificateRepository.save(current);

        FiringCertificate next = snapshot(artwork, view, current.getVersionNo() + 1,
                FiringCertificate.CURRENT, reason.trim(), normalizeIssuer(issuedBy));
        certificateRepository.save(next);
        return getAggregate(artworkId);
    }

    // ================= 来源链核对 =================

    /** 组装实时来源链视图并逐段核对（不抛异常，返回明细给页面展示来源缺失/冲突状态） */
    public ProvenanceView buildProvenance(Artwork artwork) {
        ProvenanceView v = new ProvenanceView();
        List<SourceCheck> checks = new ArrayList<>();

        // --- 课程段 ---
        Course course = artwork.getCourseId() == null ? null
                : courseRepository.findById(artwork.getCourseId()).orElse(null);
        if (course == null) {
            v.setCourseMissing(true);
            checks.add(SourceCheck.missing("COURSE", "来源课程",
                    "作品登记的课程（id=" + artwork.getCourseId() + "）查不到，缺【课程】这段来源"));
        } else {
            v.setCourseId(course.getId());
            v.setCourseCode(course.getCode());
            v.setCourseTitle(course.getTitle());
            v.setTeacher(course.getTeacher());
            checks.add(SourceCheck.ok("COURSE", "来源课程"));
        }

        // --- 坯体段 ---
        Greenware greenware = artwork.getGreenwareId() == null ? null
                : greenwareRepository.findById(artwork.getGreenwareId()).orElse(null);
        if (greenware == null) {
            v.setGreenwareMissing(true);
            checks.add(SourceCheck.missing("GREENWARE", "来源坯体",
                    "作品登记的来源坯体（id=" + artwork.getGreenwareId()
                            + "）查不到，缺【坯体】这段来源，泥料/釉料也无从核对"));
        } else {
            v.setGreenwareId(greenware.getId());
            v.setGreenwareCode(greenware.getCode());
            v.setGreenwareName(greenware.getName());
            v.setGreenwareStage(greenware.getStage());
            v.setGreenwareBatchId(greenware.getFiringBatchId());
            checks.add(SourceCheck.ok("GREENWARE", "来源坯体"));
        }

        // --- 泥料段（坯体的泥料必填） ---
        Material clay = null;
        if (greenware != null) {
            clay = greenware.getClayId() == null ? null
                    : materialRepository.findById(greenware.getClayId()).orElse(null);
            if (clay == null) {
                v.setClayMissing(true);
                checks.add(SourceCheck.missing("CLAY", "泥料",
                        "坯体【" + greenware.getCode() + "】登记的泥料（id="
                                + greenware.getClayId() + "）查不到，缺【泥料】这段来源"));
            } else if (!"CLAY".equals(clay.getKind())) {
                v.setLinkConflict(true);
                checks.add(SourceCheck.conflict("CLAY", "泥料",
                        "坯体【" + greenware.getCode() + "】引用的材料【" + clay.getCode()
                                + "】不是泥料（实际为釉料），泥料关联冲突"));
            } else {
                v.setClayId(clay.getId());
                v.setClayCode(clay.getCode());
                v.setClayName(clay.getName());
                checks.add(SourceCheck.ok("CLAY", "泥料"));
            }

            // --- 釉料段（可空；一旦登记就必须查得到且确实是釉料） ---
            if (greenware.getGlazeId() != null) {
                Material glaze = materialRepository.findById(greenware.getGlazeId()).orElse(null);
                if (glaze == null) {
                    v.setGlazeMissing(true);
                    checks.add(SourceCheck.missing("GLAZE", "釉料",
                            "坯体【" + greenware.getCode() + "】登记的釉料（id="
                                    + greenware.getGlazeId() + "）查不到，缺【釉料】这段来源"));
                } else if (!"GLAZE".equals(glaze.getKind())) {
                    v.setLinkConflict(true);
                    checks.add(SourceCheck.conflict("GLAZE", "釉料",
                            "坯体【" + greenware.getCode() + "】引用的材料【" + glaze.getCode()
                                    + "】不是釉料（实际为泥料），釉料关联冲突"));
                } else {
                    v.setGlazeId(glaze.getId());
                    v.setGlazeCode(glaze.getCode());
                    v.setGlazeName(glaze.getName());
                    checks.add(SourceCheck.ok("GLAZE", "釉料"));
                }
            } else {
                checks.add(SourceCheck.ok("GLAZE", "釉料（本坯未施釉）"));
            }
        } else {
            checks.add(SourceCheck.missing("CLAY", "泥料", "来源坯体缺失，无法核对【泥料】这段来源"));
            checks.add(SourceCheck.missing("GLAZE", "釉料", "来源坯体缺失，无法核对【釉料】这段来源"));
        }

        // --- 烧成批次段 ---
        FiringBatch batch = artwork.getFiringBatchId() == null ? null
                : batchRepository.findById(artwork.getFiringBatchId()).orElse(null);
        if (batch == null) {
            v.setBatchMissing(true);
            checks.add(SourceCheck.missing("BATCH", "烧成批次",
                    "作品登记的烧成批次（id=" + artwork.getFiringBatchId()
                            + "）查不到，缺【烧成批次】这段来源，温度与时间无从核对"));
        } else {
            v.setBatchId(batch.getId());
            v.setBatchNo(batch.getBatchNo());
            v.setBatchStage(batch.getStage());
            v.setFireType(batch.getFireType());
            v.setTargetTemp(batch.getTargetTemp());
            v.setPeakTemp(batch.getPeakTemp());
            v.setLoadedAt(batch.getLoadedAt());
            v.setHeatingAt(batch.getHeatingAt());
            v.setSoakingAt(batch.getSoakingAt());
            v.setCoolingAt(batch.getCoolingAt());
            v.setOutAt(batch.getOutAt());
            if (!FiringBatch.OUT.equals(batch.getStage())) {
                v.setLinkConflict(true);
                checks.add(SourceCheck.conflict("BATCH", "烧成批次",
                        "批次【" + batch.getBatchNo() + "】还没出窑（当前阶段 "
                                + batch.getStage() + "），烧成未完成，不能签发烧成履历凭证"));
            } else if (batch.getPeakTemp() == null) {
                v.setLinkConflict(true);
                checks.add(SourceCheck.conflict("BATCH", "烧成批次",
                        "批次【" + batch.getBatchNo() + "】已出窑但没有实际峰值温度，烧成记录不完整"));
            } else {
                checks.add(SourceCheck.ok("BATCH", "烧成批次"));
            }

            // --- 窑炉段 ---
            Kiln kiln = batch.getKilnId() == null ? null
                    : kilnRepository.findById(batch.getKilnId()).orElse(null);
            if (kiln == null) {
                v.setKilnMissing(true);
                checks.add(SourceCheck.missing("KILN", "窑炉",
                        "批次【" + batch.getBatchNo() + "】登记的窑炉（id="
                                + batch.getKilnId() + "）查不到，缺【窑炉】这段来源"));
            } else {
                v.setKilnId(kiln.getId());
                v.setKilnName(kiln.getName());
                checks.add(SourceCheck.ok("KILN", "窑炉"));
            }
        }

        // --- 关联一致性段：作品、坯体、批次三者必须互相对得上 ---
        List<String> linkProblems = new ArrayList<>();
        if (batch != null && greenware != null) {
            // 出窑后 greenware.firing_batch_id 会被烧成流程清空、坯体阶段继续推进，
            // 因此不拿"坯体当前批次/阶段"与作品批次硬比（那会把正常历史误判成冲突）。
            // 这里核对仍可核实的材料一致性：
            // 1) 釉烧作品的来源坯体必须有施釉记录（素烧之后再施釉是正常流程，不反向限制）；
            if ("GLAZE".equals(batch.getFireType()) && greenware.getGlazeId() == null) {
                linkProblems.add("作品登记为釉烧批次【" + batch.getBatchNo()
                        + "】，但来源坯体【" + greenware.getCode() + "】没有施釉记录");
            }
            // 2) 批次目标温度与泥料建议烧成温度差距过大（超过 400℃）视为资料冲突。
            if (clay != null && clay.getFiringTemp() != null
                    && Math.abs(clay.getFiringTemp() - batch.getTargetTemp()) > 400) {
                linkProblems.add("泥料【" + clay.getCode() + "】建议烧成温度 " + clay.getFiringTemp()
                        + "℃，与批次【" + batch.getBatchNo() + "】目标温度 "
                        + batch.getTargetTemp() + "℃ 差距过大");
            }
        }
        // 同一件坯体只能对应一件作品（来源唯一）
        for (Artwork other : artworkRepository.findAll()) {
            if (!other.getId().equals(artwork.getId())
                    && artwork.getGreenwareId() != null
                    && artwork.getGreenwareId().equals(other.getGreenwareId())) {
                linkProblems.add("来源坯体同时被作品【" + other.getCode() + " " + other.getTitle()
                        + "】登记，一件坯体不能出具两份作品凭证");
                break;
            }
        }
        if (linkProblems.isEmpty()) {
            checks.add(SourceCheck.ok("LINK", "关联一致性"));
        } else {
            v.setLinkConflict(true);
            checks.add(SourceCheck.conflict("LINK", "关联一致性", String.join("；", linkProblems)));
        }

        boolean issuable = checks.stream().allMatch(c -> SourceCheck.OK.equals(c.getStatus()));
        v.setChecks(checks);
        v.setIssuable(issuable);
        if (!issuable) {
            List<String> blocked = new ArrayList<>();
            for (SourceCheck c : checks) {
                if (!SourceCheck.OK.equals(c.getStatus())) {
                    blocked.add("【" + c.getSegmentName() + "】"
                            + (SourceCheck.MISSING.equals(c.getStatus()) ? "来源缺失：" : "关联冲突：")
                            + c.getMessage());
                }
            }
            v.setBlockMessage("来源核对未通过，不能生成凭证：" + String.join("；", blocked)
                    + "。请先在对应业务模块补齐/修正后再签发。");
        }
        return v;
    }

    private ProvenanceView requireIssuable(Artwork artwork) {
        ProvenanceView view = buildProvenance(artwork);
        if (!view.isIssuable()) {
            throw new BizException(view.getBlockMessage());
        }
        return view;
    }

    // ================= 快照与差异 =================

    private FiringCertificate snapshot(Artwork artwork, ProvenanceView v, int versionNo,
                                       String status, String reason, String issuer) {
        FiringCertificate c = new FiringCertificate();
        c.setArtworkId(artwork.getId());
        c.setVersionNo(versionNo);
        c.setStatus(status);
        c.setChangeReason(reason);
        c.setIssuedBy(issuer);
        c.setIssuedAt(LocalDateTime.now());

        c.setArtworkCode(artwork.getCode());
        c.setArtworkTitle(artwork.getTitle());
        c.setStudentName(artwork.getStudentName());
        c.setOwnerStatus(artwork.getOwnerStatus());
        c.setFinishedAt(artwork.getFinishedAt());

        c.setCourseId(v.getCourseId());
        c.setCourseCode(v.getCourseCode());
        c.setCourseTitle(v.getCourseTitle());
        c.setTeacher(v.getTeacher());

        c.setGreenwareId(v.getGreenwareId());
        c.setGreenwareCode(v.getGreenwareCode());
        c.setGreenwareName(v.getGreenwareName());
        c.setClayId(v.getClayId());
        c.setClayCode(v.getClayCode());
        c.setClayName(v.getClayName());
        c.setGlazeId(v.getGlazeId());
        c.setGlazeCode(v.getGlazeCode());
        c.setGlazeName(v.getGlazeName());

        c.setFiringBatchId(v.getBatchId());
        c.setBatchNo(v.getBatchNo());
        c.setKilnId(v.getKilnId());
        c.setKilnName(v.getKilnName());
        c.setFireType(v.getFireType());
        c.setTargetTemp(v.getTargetTemp());
        c.setPeakTemp(v.getPeakTemp());
        c.setLoadedAt(v.getLoadedAt());
        c.setHeatingAt(v.getHeatingAt());
        c.setSoakingAt(v.getSoakingAt());
        c.setCoolingAt(v.getCoolingAt());
        c.setOutAt(v.getOutAt());
        return c;
    }

    /** 对比当前凭证快照与业务表现值；只要引用 id 或快照文案发生变化就给出差异行 */
    private List<FieldDiff> diffAgainst(FiringCertificate c, Artwork artwork, ProvenanceView v) {
        List<FieldDiff> diffs = new ArrayList<>();

        // id 漂移（引用对象被换过）也算资料变化，同时呈现新旧文案
        addIfChanged(diffs, "artworkTitle", "作品名称", c.getArtworkTitle(), artwork.getTitle());
        addIfChanged(diffs, "studentName", "学员", c.getStudentName(), artwork.getStudentName());
        addIfChanged(diffs, "ownerStatus", "作品归属",
                ownerLabel(c.getOwnerStatus()), ownerLabel(artwork.getOwnerStatus()));
        addIfChanged(diffs, "finishedAt", "完成时间", dt(c.getFinishedAt()), dt(artwork.getFinishedAt()));

        addIfChanged(diffs, "course", "来源课程",
                c.getCourseCode() + " " + c.getCourseTitle(),
                v.isCourseMissing() ? "来源缺失" : v.getCourseCode() + " " + v.getCourseTitle());
        addIfChanged(diffs, "teacher", "授课老师", c.getTeacher(),
                v.isCourseMissing() ? "来源缺失" : v.getTeacher());

        addIfChanged(diffs, "greenware", "来源坯体",
                c.getGreenwareCode() + " " + c.getGreenwareName(),
                v.isGreenwareMissing() ? "来源缺失"
                        : v.getGreenwareCode() + " " + v.getGreenwareName());
        addIfChanged(diffs, "clay", "泥料",
                c.getClayCode() + " " + c.getClayName(),
                v.isClayMissing() ? "来源缺失" : v.getClayCode() + " " + v.getClayName());
        String oldGlaze = c.getGlazeId() == null ? "未施釉"
                : c.getGlazeCode() + " " + c.getGlazeName();
        String newGlaze = v.getGlazeId() == null
                ? (v.isGreenwareMissing() ? "来源缺失" : "未施釉")
                : v.getGlazeCode() + " " + v.getGlazeName();
        addIfChanged(diffs, "glaze", "釉料", oldGlaze, newGlaze);

        addIfChanged(diffs, "batch", "烧成批次", c.getBatchNo(),
                v.isBatchMissing() ? "来源缺失" : v.getBatchNo());
        addIfChanged(diffs, "kiln", "窑炉", c.getKilnName(),
                v.isKilnMissing() ? "来源缺失" : v.getKilnName());
        addIfChanged(diffs, "fireType", "烧成类型",
                fireTypeLabel(c.getFireType()),
                v.isBatchMissing() ? "来源缺失" : fireTypeLabel(v.getFireType()));
        addIfChanged(diffs, "targetTemp", "目标温度",
                c.getTargetTemp() == null ? "—" : c.getTargetTemp() + "℃",
                v.getTargetTemp() == null ? "—" : v.getTargetTemp() + "℃");
        addIfChanged(diffs, "peakTemp", "实际峰值温度",
                c.getPeakTemp() == null ? "—" : c.getPeakTemp() + "℃",
                v.getPeakTemp() == null ? "—" : v.getPeakTemp() + "℃");
        addIfChanged(diffs, "loadedAt", "装窑时间", dt(c.getLoadedAt()), dt(v.getLoadedAt()));
        addIfChanged(diffs, "heatingAt", "升温时间", dt(c.getHeatingAt()), dt(v.getHeatingAt()));
        addIfChanged(diffs, "soakingAt", "保温时间", dt(c.getSoakingAt()), dt(v.getSoakingAt()));
        addIfChanged(diffs, "coolingAt", "冷却时间", dt(c.getCoolingAt()), dt(v.getCoolingAt()));
        addIfChanged(diffs, "outAt", "出窑时间", dt(c.getOutAt()), dt(v.getOutAt()));
        return diffs;
    }

    private void addIfChanged(List<FieldDiff> diffs, String field, String label,
                              String oldVal, String newVal) {
        if (!Objects.equals(nullToEmpty(oldVal), nullToEmpty(newVal))) {
            diffs.add(new FieldDiff(field, label, oldVal, newVal));
        }
    }

    // ================= 杂项 =================

    private FiringCertificate findCurrent(Long artworkId) {
        return certificateRepository
                .findFirstByArtworkIdAndStatusOrderByVersionNoDesc(artworkId, FiringCertificate.CURRENT)
                .orElse(null);
    }

    private StaleVersionException conflictOrReject(Artwork artwork) {
        FiringCertificate serverCurrent = findCurrent(artwork.getId());
        Integer ver = serverCurrent == null ? null : serverCurrent.getVersionNo();
        Long id = serverCurrent == null ? null : serverCurrent.getId();
        return new StaleVersionException(
                "凭证版本已变化：另一名工作人员已更新作品【" + artwork.getTitle()
                        + "】的凭证（服务器当前为第 " + ver + " 版）。请关闭当前页面、重新读取作品后再处理，"
                        + "不能用旧页面的过时内容覆盖后续记录。", id, ver);
    }

    private Artwork requireArtwork(Long artworkId) {
        return artworkRepository.findById(artworkId)
                .orElseThrow(() -> new BizException("作品不存在，id=" + artworkId));
    }

    private String normalizeIssuer(String issuedBy) {
        if (issuedBy == null || issuedBy.isBlank()) {
            throw new BizException("签发工作人员不能为空");
        }
        return issuedBy.trim();
    }

    private static String ownerLabel(String s) {
        if (s == null) {
            return "—";
        }
        return switch (s) {
            case "TAKEN" -> "学员带走";
            case "CONSIGN" -> "留馆寄售";
            case "SOLD" -> "已售出";
            default -> s;
        };
    }

    private static String fireTypeLabel(String s) {
        if (s == null) {
            return "—";
        }
        return "BISQUE".equals(s) ? "素烧" : "GLAZE".equals(s) ? "釉烧" : s;
    }

    private static String dt(LocalDateTime t) {
        return t == null ? "—" : t.format(DT);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    /** 供删除作品等场景判断是否已有凭证 */
    public boolean hasCertificate(Long artworkId) {
        return certificateRepository.countByArtworkId(artworkId) > 0;
    }

    /** 供列表批量展示版本号：artworkId -> 当前版本号（无凭证返回空） */
    public Optional<Integer> currentVersionNo(Long artworkId) {
        FiringCertificate c = findCurrent(artworkId);
        return c == null ? Optional.empty() : Optional.of(c.getVersionNo());
    }
}
