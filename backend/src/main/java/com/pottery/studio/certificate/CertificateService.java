package com.pottery.studio.certificate;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.ConflictException;
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
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * 烧成履历凭证服务。
 *
 * 设计要点：
 * 1) 签发前逐段核对来源链（作品→课程 / 作品→坯体→泥料釉料 / 作品→批次→窑炉 / 坯体与批次关联一致 / 批次已出窑），
 *    缺来源或关联冲突时只返回核对结论，拒绝落库，绝不拼出一张看似完整的凭证，并指出缺的是哪一段。
 * 2) 凭证内容全部写入 snap_* 快照列；之后课程 / 材料 / 作品被修正，旧凭证仍显示原内容。
 * 3) 更正只能追加新版本并把旧版置为 SUPERSEDED，不更新、不删除旧行；每作品仅一条 CURRENT。
 * 4) 更正带 expectedVersionNo：旧页面持有的版本已不是当前版时返回 409，要求重新读取，避免旧数据覆盖新版本。
 */
@Service
public class CertificateService {

    public static final String GROUP_ARTWORK = "作品与课程";
    public static final String GROUP_GREENWARE = "来源坯体与材料";
    public static final String GROUP_FIRING = "烧成批次与窑炉";

    private static final DateTimeFormatter DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final Map<String, String> OWNER_LABEL = Map.of(
            "TAKEN", "学员带走", "CONSIGN", "留馆寄售", "SOLD", "已售出");
    private static final Map<String, String> FIRE_LABEL = Map.of("BISQUE", "素烧", "GLAZE", "釉烧");
    private static final Map<String, String> STAGE_LABEL = Map.of(
            "SHAPED", "已成型", "DRYING", "晾坯中", "BISQUE_READY", "可素烧",
            "BISQUED", "已素烧", "GLAZED", "已施釉", "FIRING", "烧制中", "FINISHED", "已完成");

    private final FiringCertificateRepository certificateRepository;
    private final ArtworkRepository artworkRepository;
    private final CourseRepository courseRepository;
    private final GreenwareRepository greenwareRepository;
    private final MaterialRepository materialRepository;
    private final FiringBatchRepository batchRepository;
    private final KilnRepository kilnRepository;

    public CertificateService(FiringCertificateRepository certificateRepository,
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

    /** 作品详情凭证面板：当前版 + 历史版 + 核对 + 差异 */
    public CertificateView getView(Long artworkId) {
        Artwork artwork = requireArtwork(artworkId);

        CertificateView view = new CertificateView();
        view.setEverIssued(certificateRepository.existsByArtworkId(artworkId));

        List<FiringCertificate> all = certificateRepository.findByArtworkIdOrderByVersionNoDesc(artworkId);
        FiringCertificate current = null;
        List<FiringCertificate> history = new ArrayList<>();
        for (FiringCertificate c : all) {
            if (FiringCertificate.CURRENT.equals(c.getStatus()) && current == null) {
                current = c;
            } else {
                history.add(c);
            }
        }
        view.setCurrentVersion(current);
        view.setHistory(history);

        CertificateCheckView check = precheck(artwork);
        view.setCheck(check);

        if (current != null && check.getPendingSnapshot() != null) {
            List<FieldDrift> drift = buildDrift(current, check.getPendingSnapshot());
            view.setDrift(drift);
            int changed = 0;
            for (FieldDrift d : drift) {
                if (d.isChanged()) {
                    changed++;
                }
            }
            view.setDriftCount(changed);
        }
        return view;
    }

    /** 打印 / 查看单个版本：历史版本也按当时快照原样展示，不被当前版或当前数据替换 */
    public FiringCertificate getVersion(Long artworkId, Long certificateId) {
        FiringCertificate c = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new BizException("凭证版本不存在，id=" + certificateId));
        if (!c.getArtworkId().equals(artworkId)) {
            throw new BizException("该凭证版本不属于当前作品");
        }
        return c;
    }

    /** 只取当前版本（打印当前版） */
    public FiringCertificate getCurrent(Long artworkId) {
        requireArtwork(artworkId);
        return certificateRepository
                .findFirstByArtworkIdAndStatusOrderByVersionNoDesc(artworkId, FiringCertificate.CURRENT)
                .orElseThrow(() -> new BizException("作品还没有签发过烧成履历凭证"));
    }

    /** 签发前核对（不落库），缺失/冲突逐条指出 */
    public CertificateCheckView precheck(Long artworkId) {
        return precheck(requireArtwork(artworkId));
    }

    // ================= 写：签发 / 更正 =================

    /** 首次签发（补签）：从当前可核实数据生成 V1。升级前的老作品无凭证也允许从此入口补签。 */
    @Transactional
    public FiringCertificate issue(Long artworkId, String issuedBy) {
        String staff = requireStaff(issuedBy);
        // 先锁作品行，串行化同一作品的签发/更正，避免并发产生双 CURRENT
        Artwork artwork = artworkRepository.lockById(artworkId)
                .orElseThrow(() -> new BizException("作品不存在，id=" + artworkId));

        if (!certificateRepository.lockCurrent(artworkId).isEmpty()) {
            throw new BizException("作品已存在当前版本凭证，资料有变化请使用“发起更正”追加新版本，不能重复签发");
        }
        CertificateCheckView check = precheck(artwork);
        if (!check.isReady()) {
            throw new BizException("来源核对未通过，不能签发凭证：" + joinBlocking(check));
        }

        FiringCertificate cert = check.getPendingSnapshot();
        int versionNo = certificateRepository.findFirstByArtworkIdOrderByVersionNoDesc(artworkId)
                .map(c -> c.getVersionNo() + 1).orElse(1);
        cert.setVersionNo(versionNo);
        cert.setStatus(FiringCertificate.CURRENT);
        cert.setCertificateNo(artwork.getCode() + "-V" + versionNo);
        cert.setIssuedBy(staff);
        cert.setIssuedAt(LocalDateTime.now());
        cert.setChangeReason(null);
        return certificateRepository.save(cert);
    }

    /**
     * 发起更正：以当前可核实数据追加新版本，旧版置为 SUPERSEDED。
     * expectedVersionNo 为页面打开时看到的当前版本号；已被他人更新则 409，要求重新读取。
     */
    @Transactional
    public FiringCertificate correct(Long artworkId, Integer expectedVersionNo,
                                     String issuedBy, String changeReason) {
        String staff = requireStaff(issuedBy);
        if (expectedVersionNo == null) {
            throw new BizException("发起更正必须携带页面所依据的版本号，以防旧页面覆盖新版本");
        }
        if (changeReason == null || changeReason.isBlank()) {
            throw new BizException("发起更正必须填写更正原因");
        }

        // 锁住作品行：与其他签发/更正串行
        Artwork artwork = artworkRepository.lockById(artworkId)
                .orElseThrow(() -> new BizException("作品不存在，id=" + artworkId));

        // 行锁内取当前版本；并发的另一个更正会等在这里，待对方提交后读到新版本
        List<FiringCertificate> currents = certificateRepository.lockCurrent(artworkId);
        FiringCertificate current = currents.isEmpty() ? null : currents.get(0);
        if (current == null) {
            throw new BizException("作品还没有凭证，不能更正，请先签发首版凭证");
        }

        // 并发保护：另一工作人员已更新过版本，旧页面必须重新读取后处理
        if (!current.getVersionNo().equals(expectedVersionNo)) {
            throw new ConflictException("凭证版本已变化：您页面上的是 V" + expectedVersionNo
                    + "，当前已为 V" + current.getVersionNo() + "，请重新读取最新版本后再发起更正");
        }

        CertificateCheckView check = precheck(artwork);
        if (!check.isReady()) {
            throw new BizException("当前来源核对未通过，不能据此生成新凭证：" + joinBlocking(check));
        }

        // 旧版只改状态位，快照内容原样保留
        current.setStatus(FiringCertificate.SUPERSEDED);
        certificateRepository.save(current);

        int newVersionNo = certificateRepository.findFirstByArtworkIdOrderByVersionNoDesc(artworkId)
                .map(c -> c.getVersionNo() + 1).orElse(1);
        FiringCertificate cert = check.getPendingSnapshot();
        cert.setVersionNo(newVersionNo);
        cert.setStatus(FiringCertificate.CURRENT);
        cert.setCertificateNo(artwork.getCode() + "-V" + newVersionNo);
        cert.setIssuedBy(staff);
        cert.setIssuedAt(LocalDateTime.now());
        cert.setChangeReason(changeReason.trim());
        return certificateRepository.save(cert);
    }

    // ================= 来源核对 + 快照组装 =================

    private CertificateCheckView precheck(Artwork artwork) {
        List<SegmentCheck> segments = new ArrayList<>();

        // 课程
        Course course = artwork.getCourseId() == null ? null
                : courseRepository.findById(artwork.getCourseId()).orElse(null);
        if (artwork.getCourseId() == null || course == null) {
            segments.add(SegmentCheck.missing("COURSE", "所属课程",
                    "缺少来源【课程】：作品未关联课程或课程已被删除，无法记录课程名称与授课老师"));
        } else {
            segments.add(SegmentCheck.ok("COURSE", "所属课程",
                    course.getCode() + " " + course.getTitle() + "（" + course.getTeacher() + "）"));
        }

        // 来源坯体
        Greenware greenware = artwork.getGreenwareId() == null ? null
                : greenwareRepository.findById(artwork.getGreenwareId()).orElse(null);
        if (artwork.getGreenwareId() == null || greenware == null) {
            segments.add(SegmentCheck.missing("GREENWARE", "来源坯体",
                    "缺少来源【坯体】：作品未关联来源坯体或坯体已被删除，泥料釉料来源无从核对"));
        } else {
            segments.add(SegmentCheck.ok("GREENWARE", "来源坯体",
                    greenware.getCode() + " " + greenware.getName()
                            + "（" + stageLabel(greenware.getStage()) + "）"));
        }

        // 泥料（依赖坯体存在）
        Material clay = null;
        if (greenware == null) {
            segments.add(SegmentCheck.missing("CLAY", "泥料",
                    "缺少来源【泥料】：来源坯体缺失，泥料段无法核对（请先补齐来源坯体）"));
        } else if (greenware.getClayId() == null) {
            segments.add(SegmentCheck.missing("CLAY", "泥料",
                    "缺少来源【泥料】：坯体【" + greenware.getCode() + "】未登记所用泥料"));
        } else {
            clay = materialRepository.findById(greenware.getClayId()).orElse(null);
            if (clay == null) {
                segments.add(SegmentCheck.missing("CLAY", "泥料",
                        "缺少来源【泥料】：坯体登记的泥料 id=" + greenware.getClayId() + " 已不存在"));
            } else if (!"CLAY".equals(clay.getKind())) {
                segments.add(SegmentCheck.conflict("CLAY", "泥料",
                        clay.getCode() + " " + clay.getName(),
                        "材料冲突：坯体登记的材料【" + clay.getName() + "】不是泥料，来源对不上"));
            } else {
                segments.add(SegmentCheck.ok("CLAY", "泥料",
                        clay.getCode() + " " + clay.getName()
                                + (clay.getFiringTemp() != null ? "（建议 " + clay.getFiringTemp() + "℃）" : "")));
            }
        }

        // 釉料：未施釉属正常；登记了却查不到 / 种类不对才算问题
        Material glaze = null;
        if (greenware != null && greenware.getGlazeId() != null) {
            glaze = materialRepository.findById(greenware.getGlazeId()).orElse(null);
            if (glaze == null) {
                segments.add(SegmentCheck.missing("GLAZE", "釉料",
                        "缺少来源【釉料】：坯体登记的釉料 id=" + greenware.getGlazeId() + " 已不存在"));
            } else if (!"GLAZE".equals(glaze.getKind())) {
                segments.add(SegmentCheck.conflict("GLAZE", "釉料",
                        glaze.getCode() + " " + glaze.getName(),
                        "材料冲突：坯体登记的材料【" + glaze.getName() + "】不是釉料，来源对不上"));
            } else {
                segments.add(SegmentCheck.ok("GLAZE", "釉料",
                        glaze.getCode() + " " + glaze.getName()
                                + (glaze.getFiringTemp() != null ? "（建议 " + glaze.getFiringTemp() + "℃）" : "")));
            }
        } else {
            segments.add(SegmentCheck.ok("GLAZE", "釉料", "未施釉（素烧坯体）"));
        }

        // 烧成批次
        FiringBatch batch = artwork.getFiringBatchId() == null ? null
                : batchRepository.findById(artwork.getFiringBatchId()).orElse(null);
        if (artwork.getFiringBatchId() == null || batch == null) {
            segments.add(SegmentCheck.missing("BATCH", "烧成批次",
                    "缺少来源【烧成批次】：作品未关联烧成批次或批次已被删除，烧成温度与时间无来源"));
        } else {
            segments.add(SegmentCheck.ok("BATCH", "烧成批次",
                    batch.getBatchNo() + "（" + fireLabel(batch.getFireType()) + "，目标 "
                            + batch.getTargetTemp() + "℃）"));
        }

        // 窑炉
        Kiln kiln = (batch == null || batch.getKilnId() == null) ? null
                : kilnRepository.findById(batch.getKilnId()).orElse(null);
        if (batch == null) {
            segments.add(SegmentCheck.missing("KILN", "窑炉",
                    "缺少来源【窑炉】：烧成批次缺失，窑炉段无法核对（请先补齐烧成批次）"));
        } else if (kiln == null) {
            segments.add(SegmentCheck.missing("KILN", "窑炉",
                    "缺少来源【窑炉】：批次【" + batch.getBatchNo() + "】所用窑炉已不存在"));
        } else {
            segments.add(SegmentCheck.ok("KILN", "窑炉",
                    kiln.getCode() + " " + kiln.getName()));
        }

        // 关联一致：坯体当前挂在某批次时，必须与作品所记批次一致
        if (greenware != null && batch != null && greenware.getFiringBatchId() != null
                && !greenware.getFiringBatchId().equals(batch.getId())) {
            FiringBatch other = batchRepository.findById(greenware.getFiringBatchId()).orElse(null);
            segments.add(SegmentCheck.conflict("LINK_GW_BATCH", "坯体↔批次关联",
                            "作品记批次【" + batch.getBatchNo() + "】，坯体当前在【"
                                    + (other == null ? greenware.getFiringBatchId() : other.getBatchNo()) + "】",
                            "关联冲突：坯体【" + greenware.getCode() + "】当前关联的烧成批次与作品登记的批次【"
                                    + batch.getBatchNo() + "】不一致，不能据此签发"));
        } else {
            segments.add(SegmentCheck.ok("LINK_GW_BATCH", "坯体↔批次关联",
                    greenware == null || batch == null ? "—"
                            : "坯体【" + greenware.getCode() + "】与作品批次【" + batch.getBatchNo() + "】关联一致"));
        }

        // 关联一致：批次必须已出窑，才有实际峰值温度与完整烧成时间
        if (batch != null && FiringBatch.OUT.equals(batch.getStage())) {
            segments.add(SegmentCheck.ok("LINK_BATCH_OUT", "批次烧成状态",
                    "批次【" + batch.getBatchNo() + "】已出窑，实际峰值 "
                            + (batch.getPeakTemp() == null ? "未记录" : batch.getPeakTemp() + "℃")));
        } else if (batch != null) {
            segments.add(SegmentCheck.conflict("LINK_BATCH_OUT", "批次烧成状态",
                    "批次【" + batch.getBatchNo() + "】当前阶段："
                            + FiringBatchServiceLabel.stage(batch.getStage()),
                    "关联冲突：烧成批次【" + batch.getBatchNo() + "】尚未出窑，烧成履历不完整，不能签发凭证"));
        } else {
            segments.add(SegmentCheck.missing("LINK_BATCH_OUT", "批次烧成状态",
                    "缺少来源【烧成批次】：无法确认批次是否已完成烧成"));
        }

        List<SegmentCheck> blocking = new ArrayList<>();
        for (SegmentCheck s : segments) {
            if (!"OK".equals(s.getState())) {
                blocking.add(s);
            }
        }

        CertificateCheckView view = new CertificateCheckView();
        view.setArtworkId(artwork.getId());
        view.setArtworkCode(artwork.getCode());
        view.setArtworkTitle(artwork.getTitle());
        view.setSegments(segments);
        view.setBlockingSegments(blocking);
        view.setReady(blocking.isEmpty());
        // 即便不通过也拼一份预览：缺失段对应的快照列为空，页面只作预览，绝不作为正式凭证
        view.setPendingSnapshot(buildSnapshot(artwork, course, greenware, clay, glaze, batch, kiln));
        return view;
    }

    private FiringCertificate buildSnapshot(Artwork artwork, Course course, Greenware greenware,
                                            Material clay, Material glaze, FiringBatch batch, Kiln kiln) {
        FiringCertificate c = new FiringCertificate();
        c.setArtworkId(artwork.getId());
        c.setSnapArtworkCode(artwork.getCode());
        c.setSnapTitle(artwork.getTitle());
        c.setSnapStudentName(artwork.getStudentName());
        c.setSnapOwnerStatus(artwork.getOwnerStatus());

        if (course != null) {
            c.setSnapCourseId(course.getId());
            c.setSnapCourseCode(course.getCode());
            c.setSnapCourseTitle(course.getTitle());
            c.setSnapTeacher(course.getTeacher());
        }
        if (greenware != null) {
            c.setSnapGreenwareId(greenware.getId());
            c.setSnapGreenwareCode(greenware.getCode());
            c.setSnapGreenwareName(greenware.getName());
            c.setSnapGreenwareStage(greenware.getStage());
            c.setSnapShapedAt(greenware.getShapedAt());
        }
        if (clay != null) {
            c.setSnapClayId(clay.getId());
            c.setSnapClayCode(clay.getCode());
            c.setSnapClayName(clay.getName());
            c.setSnapClayTemp(clay.getFiringTemp());
        }
        if (glaze != null) {
            c.setSnapGlazeId(glaze.getId());
            c.setSnapGlazeCode(glaze.getCode());
            c.setSnapGlazeName(glaze.getName());
            c.setSnapGlazeTemp(glaze.getFiringTemp());
        }
        if (batch != null) {
            c.setSnapBatchId(batch.getId());
            c.setSnapBatchNo(batch.getBatchNo());
            c.setSnapFireType(batch.getFireType());
            c.setSnapTargetTemp(batch.getTargetTemp());
            c.setSnapPeakTemp(batch.getPeakTemp());
            c.setSnapLoadedAt(batch.getLoadedAt());
            c.setSnapHeatingAt(batch.getHeatingAt());
            c.setSnapSoakingAt(batch.getSoakingAt());
            c.setSnapCoolingAt(batch.getCoolingAt());
            c.setSnapOutAt(batch.getOutAt());
        }
        if (kiln != null) {
            c.setSnapKilnId(kiln.getId());
            c.setSnapKilnCode(kiln.getCode());
            c.setSnapKilnName(kiln.getName());
        }
        return c;
    }

    // ================= 差异对照 =================

    private static final class SnapshotField {
        final String field;
        final String label;
        final String group;
        final Function<FiringCertificate, Object> getter;

        SnapshotField(String field, String label, String group, Function<FiringCertificate, Object> getter) {
            this.field = field;
            this.label = label;
            this.group = group;
            this.getter = getter;
        }
    }

    /** 凭证字段清单：同时驱动“差异对照”和打印排版，保证两边口径一致 */
    static final List<SnapshotField> SNAPSHOT_FIELDS = List.of(
            new SnapshotField("artworkCode", "作品编号", GROUP_ARTWORK, FiringCertificate::getSnapArtworkCode),
            new SnapshotField("title", "作品名称", GROUP_ARTWORK, FiringCertificate::getSnapTitle),
            new SnapshotField("studentName", "学员", GROUP_ARTWORK, FiringCertificate::getSnapStudentName),
            new SnapshotField("ownerStatus", "归属", GROUP_ARTWORK, FiringCertificate::getSnapOwnerStatus),
            new SnapshotField("courseCode", "课程编号", GROUP_ARTWORK, FiringCertificate::getSnapCourseCode),
            new SnapshotField("courseTitle", "课程名称", GROUP_ARTWORK, FiringCertificate::getSnapCourseTitle),
            new SnapshotField("teacher", "授课老师", GROUP_ARTWORK, FiringCertificate::getSnapTeacher),
            new SnapshotField("greenwareCode", "坯体编号", GROUP_GREENWARE, FiringCertificate::getSnapGreenwareCode),
            new SnapshotField("greenwareName", "坯体名称", GROUP_GREENWARE, FiringCertificate::getSnapGreenwareName),
            new SnapshotField("greenwareStage", "坯体阶段", GROUP_GREENWARE, FiringCertificate::getSnapGreenwareStage),
            new SnapshotField("shapedAt", "成型时间", GROUP_GREENWARE, FiringCertificate::getSnapShapedAt),
            new SnapshotField("clayCode", "泥料编号", GROUP_GREENWARE, FiringCertificate::getSnapClayCode),
            new SnapshotField("clayName", "泥料", GROUP_GREENWARE, FiringCertificate::getSnapClayName),
            new SnapshotField("clayTemp", "泥料建议温度", GROUP_GREENWARE, FiringCertificate::getSnapClayTemp),
            new SnapshotField("glazeCode", "釉料编号", GROUP_GREENWARE, FiringCertificate::getSnapGlazeCode),
            new SnapshotField("glazeName", "釉料", GROUP_GREENWARE, FiringCertificate::getSnapGlazeName),
            new SnapshotField("glazeTemp", "釉料建议温度", GROUP_GREENWARE, FiringCertificate::getSnapGlazeTemp),
            new SnapshotField("batchNo", "烧成批次", GROUP_FIRING, FiringCertificate::getSnapBatchNo),
            new SnapshotField("fireType", "烧成类型", GROUP_FIRING, FiringCertificate::getSnapFireType),
            new SnapshotField("kilnName", "窑炉", GROUP_FIRING, FiringCertificate::getSnapKilnName),
            new SnapshotField("targetTemp", "目标温度", GROUP_FIRING, FiringCertificate::getSnapTargetTemp),
            new SnapshotField("peakTemp", "实际峰值温度", GROUP_FIRING, FiringCertificate::getSnapPeakTemp),
            new SnapshotField("loadedAt", "装窑时间", GROUP_FIRING, FiringCertificate::getSnapLoadedAt),
            new SnapshotField("heatingAt", "升温时间", GROUP_FIRING, FiringCertificate::getSnapHeatingAt),
            new SnapshotField("soakingAt", "保温开始时间", GROUP_FIRING, FiringCertificate::getSnapSoakingAt),
            new SnapshotField("coolingAt", "冷却开始时间", GROUP_FIRING, FiringCertificate::getSnapCoolingAt),
            new SnapshotField("outAt", "出窑时间", GROUP_FIRING, FiringCertificate::getSnapOutAt));

    private List<FieldDrift> buildDrift(FiringCertificate saved, FiringCertificate current) {
        List<FieldDrift> rows = new ArrayList<>();
        for (SnapshotField f : SNAPSHOT_FIELDS) {
            Object oldVal = f.getter.apply(saved);
            Object newVal = f.getter.apply(current);
            boolean changed = !Objects.equals(oldVal, newVal);
            rows.add(new FieldDrift(f.field, f.label, f.group,
                    display(f.field, oldVal), display(f.field, newVal), changed));
        }
        return rows;
    }

    /** 字段值展示口径：温度带 ℃，时间格式化，归属/烧成类型/阶段转中文，釉料空值明确写“未施釉” */
    public static String display(String field, Object v) {
        if (v == null) {
            return switch (field) {
                case "glazeCode", "glazeName", "glazeTemp" -> "未施釉";
                default -> "—";
            };
        }
        return switch (field) {
            case "ownerStatus" -> OWNER_LABEL.getOrDefault(v.toString(), v.toString());
            case "fireType" -> FIRE_LABEL.getOrDefault(v.toString(), v.toString());
            case "greenwareStage" -> STAGE_LABEL.getOrDefault(v.toString(), v.toString());
            case "targetTemp", "peakTemp", "clayTemp", "glazeTemp" -> v + " ℃";
            case "shapedAt", "loadedAt", "heatingAt", "soakingAt", "coolingAt", "outAt" ->
                    ((LocalDateTime) v).format(DT);
            default -> v.toString();
        };
    }

    // ================= 辅助 =================

    private Artwork requireArtwork(Long id) {
        return artworkRepository.findById(id)
                .orElseThrow(() -> new BizException("作品不存在，id=" + id));
    }

    private String requireStaff(String issuedBy) {
        if (issuedBy == null || issuedBy.isBlank()) {
            throw new BizException("必须填写签发工作人员，凭证需记录是谁签发/更正的");
        }
        return issuedBy.trim();
    }

    private String joinBlocking(CertificateCheckView check) {
        List<String> msgs = new ArrayList<>();
        for (SegmentCheck s : check.getBlockingSegments()) {
            msgs.add("【" + s.getLabel() + "】" + s.getMessage());
        }
        return String.join("；", msgs);
    }

    private static String ownerLabel(String s) {
        return s == null ? "—" : OWNER_LABEL.getOrDefault(s, s);
    }

    private static String fireLabel(String s) {
        return s == null ? "—" : FIRE_LABEL.getOrDefault(s, s);
    }

    private static String stageLabel(String s) {
        return s == null ? "—" : STAGE_LABEL.getOrDefault(s, s);
    }

    /** 供 buildDrift 之外复用归属文案 */
    public static String ownerStatusLabel(String s) {
        return ownerLabel(s);
    }

    public static String fireTypeLabel(String s) {
        return fireLabel(s);
    }

    public static String stageStatusLabel(String s) {
        return stageLabel(s);
    }

    /** 借用 firing 模块已有的阶段文案，避免在这里维护两份 */
    private static final class FiringBatchServiceLabel {
        static String stage(String stage) {
            return com.pottery.studio.firing.FiringBatchService.stageLabel(stage);
        }
    }
}
