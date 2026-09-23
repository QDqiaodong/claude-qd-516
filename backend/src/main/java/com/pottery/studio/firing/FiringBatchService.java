package com.pottery.studio.firing;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import com.pottery.studio.course.ArtworkRepository;
import com.pottery.studio.greenware.Greenware;
import com.pottery.studio.greenware.GreenwareRepository;
import com.pottery.studio.greenware.GreenwareService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class FiringBatchService {

    private static final List<String> STAGE_ORDER = List.of(
            FiringBatch.LOADING, FiringBatch.HEATING, FiringBatch.SOAKING, FiringBatch.COOLING, FiringBatch.OUT);

    private static final Map<String, String> STAGE_LABEL = Map.of(
            FiringBatch.LOADING, "装窑",
            FiringBatch.HEATING, "升温",
            FiringBatch.SOAKING, "保温",
            FiringBatch.COOLING, "冷却",
            FiringBatch.OUT, "出窑");

    /** 每 15 升容积可放 1 件坯体 */
    private static final int LITRE_PER_PIECE = 15;

    private final FiringBatchRepository batchRepository;
    private final KilnService kilnService;
    private final GreenwareRepository greenwareRepository;
    private final ArtworkRepository artworkRepository;

    public FiringBatchService(FiringBatchRepository batchRepository,
                              KilnService kilnService,
                              GreenwareRepository greenwareRepository,
                              ArtworkRepository artworkRepository) {
        this.batchRepository = batchRepository;
        this.kilnService = kilnService;
        this.greenwareRepository = greenwareRepository;
        this.artworkRepository = artworkRepository;
    }

    public static String stageLabel(String stage) {
        return STAGE_LABEL.getOrDefault(stage, stage);
    }

    public List<FiringBatch> list(Long kilnId, String stage) {
        List<FiringBatch> rows;
        if (kilnId != null) {
            rows = batchRepository.findByKilnIdOrderByIdAsc(kilnId);
        } else if (stage != null && !stage.isBlank()) {
            rows = batchRepository.findByStageOrderByIdAsc(stage);
        } else {
            rows = batchRepository.findAll();
        }
        return decorate(rows);
    }

    public FiringBatch get(Long id) {
        return decorateOne(requireExists(id));
    }

    @Transactional
    public FiringBatch create(FiringBatch request) {
        if (request.getBatchNo() == null || request.getBatchNo().isBlank()) {
            throw new BizException("批次号不能为空");
        }
        if (batchRepository.existsByBatchNo(request.getBatchNo())) {
            throw new BizException("批次号【" + request.getBatchNo() + "】已存在");
        }
        if (request.getKilnId() == null) {
            throw new BizException("烧成批次必须指定窑炉，请先选择窑炉");
        }
        Kiln kiln = kilnService.requireExists(request.getKilnId());
        if ("MAINTAIN".equals(kiln.getStatus())) {
            throw new BizException("窑炉【" + kiln.getName() + "】正在检修，不能新建烧成批次");
        }
        FiringBatch running = findRunningBatch(kiln.getId());
        if (running != null) {
            throw new BizException("窑炉【" + kiln.getName() + "】里还有未出窑的批次【" + running.getBatchNo()
                    + "】（当前" + stageLabel(running.getStage()) + "），一窑同时只能烧一个批次");
        }
        // 默认值放在 service 的 create 里
        if (request.getFireType() == null) {
            request.setFireType("BISQUE");
        }
        if (request.getStage() == null) {
            request.setStage(FiringBatch.LOADING);
        }
        if (request.getGreenwareCount() == null) {
            request.setGreenwareCount(0);
        }
        if (request.getLoadedAt() == null) {
            request.setLoadedAt(LocalDateTime.now());
        }
        validateFireType(request.getFireType());
        validateTemp(request, kiln);
        if (!FiringBatch.LOADING.equals(request.getStage())) {
            throw new BizException("新建的烧成批次必须从【装窑】阶段开始");
        }
        kiln.setStatus("FIRING");
        FiringBatch saved = batchRepository.save(request);
        return decorateOne(saved);
    }

    @Transactional
    public FiringBatch update(Long id, FiringBatch request) {
        FiringBatch exist = requireExists(id);
        Kiln kiln = kilnService.requireExists(exist.getKilnId());
        if (request.getKilnId() != null && !request.getKilnId().equals(exist.getKilnId())) {
            Kiln target = kilnService.requireExists(request.getKilnId());
            if ("MAINTAIN".equals(target.getStatus())) {
                throw new BizException("窑炉【" + target.getName() + "】正在检修，不能转窑");
            }
            FiringBatch running = findRunningBatch(target.getId());
            if (running != null) {
                throw new BizException("窑炉【" + target.getName() + "】里还有未出窑的批次【" + running.getBatchNo() + "】，不能转窑");
            }
            kiln = target;
        }
        if (request.getFireType() != null) {
            validateFireType(request.getFireType());
        }
        String oldStage = exist.getStage();
        PartialCopy.apply(request, exist, "batchNo", "kilnName", "kilnMaxTemp", "kilnCapacity");
        validateTemp(exist, kiln);
        validateStageEnter(oldStage, exist.getStage(), exist);
        return decorateOne(batchRepository.save(exist));
    }

    /** 推进烧成阶段：装窑 → 升温 → 保温 → 冷却 → 出窑，只能一级一级往前推 */
    @Transactional
    public FiringBatch advanceStage(Long id, String targetStage, Integer peakTemp) {
        FiringBatch exist = requireExists(id);
        if (targetStage == null || !STAGE_LABEL.containsKey(targetStage)) {
            throw new BizException("烧成阶段取值不合法，可选：装窑、升温、保温、冷却、出窑");
        }
        if (peakTemp != null) {
            exist.setPeakTemp(peakTemp);
        }
        validateStageEnter(exist.getStage(), targetStage, exist);
        LocalDateTime now = LocalDateTime.now();
        switch (targetStage) {
            case FiringBatch.HEATING -> exist.setHeatingAt(now);
            case FiringBatch.SOAKING -> exist.setSoakingAt(now);
            case FiringBatch.COOLING -> exist.setCoolingAt(now);
            case FiringBatch.OUT -> exist.setOutAt(now);
            default -> { }
        }
        exist.setStage(targetStage);
        FiringBatch saved = batchRepository.save(exist);
        if (FiringBatch.OUT.equals(targetStage)) {
            finishGreenwares(saved);
            Kiln kiln = kilnService.requireExists(saved.getKilnId());
            kiln.setStatus("IDLE");
        }
        return decorateOne(saved);
    }

    /** 装窑：把一件坯体放进批次 */
    @Transactional
    public FiringBatch loadGreenware(Long id, Long greenwareId) {
        FiringBatch exist = requireExists(id);
        if (!FiringBatch.LOADING.equals(exist.getStage())) {
            throw new BizException("批次【" + exist.getBatchNo() + "】已推进到【" + stageLabel(exist.getStage())
                    + "】，只能在装窑阶段往里放坯体");
        }
        if (greenwareId == null) {
            throw new BizException("待装窑的坯体不能为空");
        }
        Greenware g = greenwareRepository.findById(greenwareId)
                .orElseThrow(() -> new BizException("坯体不存在，id=" + greenwareId));
        if (g.getFiringBatchId() != null) {
            throw new BizException("坯体【" + g.getCode() + "】已经装在其他批次里了，不能重复装窑");
        }
        Kiln kiln = kilnService.requireExists(exist.getKilnId());
        String required = "BISQUE".equals(exist.getFireType()) ? Greenware.BISQUE_READY : Greenware.GLAZED;
        if (!required.equals(g.getStage())) {
            throw new BizException("坯体【" + g.getCode() + "】当前阶段为【" + GreenwareService.stageLabel(g.getStage())
                    + "】，不能装入" + ("BISQUE".equals(exist.getFireType()) ? "素烧" : "釉烧")
                    + "批次，需先推进到【" + GreenwareService.stageLabel(required) + "】");
        }
        int capacity = kiln.getVolumeL() / LITRE_PER_PIECE;
        if (exist.getGreenwareCount() + 1 > capacity) {
            throw new BizException("窑炉【" + kiln.getName() + "】容积 " + kiln.getVolumeL()
                    + "L，最多装 " + capacity + " 件，本批次已装 " + exist.getGreenwareCount() + " 件，装不下了");
        }
        g.setFiringBatchId(id);
        g.setStage(Greenware.FIRING);
        greenwareRepository.save(g);
        exist.setGreenwareCount(exist.getGreenwareCount() + 1);
        return decorateOne(batchRepository.save(exist));
    }

    @Transactional
    public void delete(Long id) {
        FiringBatch exist = requireExists(id);
        if (!FiringBatch.OUT.equals(exist.getStage())) {
            throw new BizException("批次【" + exist.getBatchNo() + "】还没出窑（当前" + stageLabel(exist.getStage())
                    + "），不能删除");
        }
        boolean referenced = artworkRepository.existsByFiringBatchId(id);
        if (referenced) {
            throw new BizException("烧成批次【" + exist.getBatchNo()
                    + "】已被学员作品引用，删除会造成作品烧成来源缺失，不能删除");
        }
        batchRepository.delete(exist);
    }

    public FiringBatch requireExists(Long id) {
        return batchRepository.findById(id)
                .orElseThrow(() -> new BizException("烧成批次不存在，id=" + id));
    }

    // ---------- 校验规则 ----------

    private void validateFireType(String fireType) {
        if (!"BISQUE".equals(fireType) && !"GLAZE".equals(fireType)) {
            throw new BizException("烧成类型只能是 BISQUE（素烧）或 GLAZE（釉烧），当前传入【" + fireType + "】");
        }
    }

    private void validateTemp(FiringBatch batch, Kiln kiln) {
        if (batch.getTargetTemp() == null) {
            throw new BizException("烧成批次必须填写目标温度");
        }
        if (batch.getTargetTemp() > kiln.getMaxTemp()) {
            throw new BizException("目标温度 " + batch.getTargetTemp() + "℃ 超出窑炉【" + kiln.getName()
                    + "】最高温度 " + kiln.getMaxTemp() + "℃");
        }
        if ("BISQUE".equals(batch.getFireType()) && (batch.getTargetTemp() < 700 || batch.getTargetTemp() > 900)) {
            throw new BizException("素烧批次目标温度应在 700℃~900℃ 之间，当前为 " + batch.getTargetTemp() + "℃");
        }
        if ("GLAZE".equals(batch.getFireType()) && (batch.getTargetTemp() < 1100 || batch.getTargetTemp() > 1320)) {
            throw new BizException("釉烧批次目标温度应在 1100℃~1320℃ 之间，当前为 " + batch.getTargetTemp() + "℃");
        }
    }

    private void validateStageEnter(String from, String to, FiringBatch batch) {
        if (from == null) {
            from = FiringBatch.LOADING;
        }
        if (from.equals(to)) {
            return;
        }
        int fromIdx = STAGE_ORDER.indexOf(from);
        int toIdx = STAGE_ORDER.indexOf(to);
        if (fromIdx < 0 || toIdx < 0) {
            throw new BizException("烧成阶段取值不合法，可选：装窑、升温、保温、冷却、出窑");
        }
        if (toIdx < fromIdx) {
            throw new BizException("烧成阶段不能回退，批次【" + batch.getBatchNo() + "】当前处于【"
                    + stageLabel(from) + "】，无法回到【" + stageLabel(to) + "】");
        }
        if (toIdx > fromIdx + 1) {
            throw new BizException("烧成阶段不能跳级，批次【" + batch.getBatchNo() + "】从【" + stageLabel(from)
                    + "】只能推进到【" + stageLabel(STAGE_ORDER.get(fromIdx + 1)) + "】");
        }
        if (FiringBatch.OUT.equals(to) && batch.getPeakTemp() == null) {
            throw new BizException("批次【" + batch.getBatchNo() + "】还没记录峰值温度，不能出窑");
        }
    }

    private void finishGreenwares(FiringBatch batch) {
        for (Greenware g : greenwareRepository.findByFiringBatchIdOrderByIdAsc(batch.getId())) {
            g.setFiringBatchId(null);
            g.setStage("BISQUE".equals(batch.getFireType()) ? Greenware.BISQUED : Greenware.FINISHED);
            greenwareRepository.save(g);
        }
    }

    private FiringBatch findRunningBatch(Long kilnId) {
        for (FiringBatch b : batchRepository.findByKilnIdOrderByIdAsc(kilnId)) {
            if (!FiringBatch.OUT.equals(b.getStage())) {
                return b;
            }
        }
        return null;
    }

    private List<FiringBatch> decorate(List<FiringBatch> rows) {
        for (FiringBatch b : rows) {
            Kiln kiln = kilnService.requireExists(b.getKilnId());
            b.setKilnName(kiln.getName());
            b.setKilnMaxTemp(kiln.getMaxTemp());
            b.setKilnCapacity(kiln.getVolumeL() / LITRE_PER_PIECE);
        }
        return rows;
    }

    private FiringBatch decorateOne(FiringBatch b) {
        return decorate(new java.util.ArrayList<>(List.of(b))).get(0);
    }
}
