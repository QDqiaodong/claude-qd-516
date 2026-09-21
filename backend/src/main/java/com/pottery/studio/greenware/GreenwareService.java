package com.pottery.studio.greenware;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import com.pottery.studio.material.Material;
import com.pottery.studio.material.MaterialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GreenwareService {

    /** 干燥阶段的推进顺序 */
    private static final List<String> STAGE_ORDER = List.of(
            Greenware.SHAPED, Greenware.DRYING, Greenware.BISQUE_READY,
            Greenware.BISQUED, Greenware.GLAZED, Greenware.FIRING, Greenware.FINISHED);

    private static final Map<String, String> STAGE_LABEL = Map.of(
            Greenware.SHAPED, "已成型",
            Greenware.DRYING, "晾坯中",
            Greenware.BISQUE_READY, "可素烧",
            Greenware.BISQUED, "已素烧",
            Greenware.GLAZED, "已施釉",
            Greenware.FIRING, "烧制中",
            Greenware.FINISHED, "已完成");

    /** 进入可素烧允许的最高含水率 */
    private static final BigDecimal MAX_MOISTURE_FOR_BISQUE = new BigDecimal("12.00");
    private static final BigDecimal DEFAULT_MOISTURE = new BigDecimal("20.00");

    private final GreenwareRepository greenwareRepository;
    private final WorkAreaService workAreaService;
    private final MaterialRepository materialRepository;

    public GreenwareService(GreenwareRepository greenwareRepository,
                            WorkAreaService workAreaService,
                            MaterialRepository materialRepository) {
        this.greenwareRepository = greenwareRepository;
        this.workAreaService = workAreaService;
        this.materialRepository = materialRepository;
    }

    public static String stageLabel(String stage) {
        return STAGE_LABEL.getOrDefault(stage, stage);
    }

    public List<Greenware> list(Long areaId, String stage) {
        List<Greenware> rows;
        if (areaId != null) {
            rows = greenwareRepository.findByAreaIdOrderByIdAsc(areaId);
        } else if (stage != null && !stage.isBlank()) {
            rows = greenwareRepository.findByStageOrderByIdAsc(stage);
        } else {
            rows = greenwareRepository.findAll();
        }
        return decorate(rows);
    }

    /** 取某分区及其所有后代分区下的坯体 */
    public List<Greenware> listByAreaTree(Long areaId) {
        if (areaId == null) {
            return decorate(greenwareRepository.findAll());
        }
        List<Long> ids = new ArrayList<>();
        collectAreaIds(areaId, ids);
        List<Greenware> rows = new ArrayList<>();
        for (Long aid : ids) {
            rows.addAll(greenwareRepository.findByAreaIdOrderByIdAsc(aid));
        }
        return decorate(rows);
    }

    public List<Greenware> listByBatch(Long batchId) {
        return decorate(greenwareRepository.findByFiringBatchIdOrderByIdAsc(batchId));
    }

    public Greenware get(Long id) {
        return decorateOne(requireExists(id));
    }

    @Transactional
    public Greenware create(Greenware request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("坯体编号不能为空");
        }
        if (greenwareRepository.existsByCode(request.getCode())) {
            throw new BizException("坯体编号【" + request.getCode() + "】已存在");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException("坯体名称不能为空");
        }
        // 可空外键先判 null
        if (request.getAreaId() == null) {
            throw new BizException("坯体必须指定存放工位，请先选择工位分区");
        }
        if (request.getClayId() == null) {
            throw new BizException("坯体必须指定所用泥料");
        }
        // 默认值一律在 service 的 create 里给
        if (request.getStage() == null) {
            request.setStage(Greenware.SHAPED);
        }
        if (request.getMoisture() == null) {
            request.setMoisture(DEFAULT_MOISTURE);
        }
        if (request.getShapedAt() == null) {
            request.setShapedAt(java.time.LocalDateTime.now());
        }
        validateArea(request.getAreaId(), null);
        validateMaterial(request);
        validateStageEnter(request.getStage(), request.getStage(), request);
        return decorateOne(greenwareRepository.save(request));
    }

    @Transactional
    public Greenware update(Long id, Greenware request) {
        Greenware exist = requireExists(id);
        String oldStage = exist.getStage();
        Long oldAreaId = exist.getAreaId();
        PartialCopy.apply(request, exist, "code", "clayName", "glazeName", "areaName", "batchNo");
        if (request.getAreaId() != null && !request.getAreaId().equals(oldAreaId)) {
            validateArea(request.getAreaId(), id);
        }
        if (request.getClayId() != null || request.getGlazeId() != null) {
            validateMaterial(exist);
        }
        validateStageEnter(oldStage, exist.getStage(), exist);
        return decorateOne(greenwareRepository.save(exist));
    }

    /** 推进干燥阶段：只能一级一级往前推，不能跳级也不能回退 */
    @Transactional
    public Greenware advanceStage(Long id, String targetStage) {
        Greenware exist = requireExists(id);
        if (targetStage == null || !STAGE_LABEL.containsKey(targetStage)) {
            throw new BizException("干燥阶段取值不合法，可选：" + String.join("、", STAGE_LABEL.values()));
        }
        validateStageEnter(exist.getStage(), targetStage, exist);
        exist.setStage(targetStage);
        return decorateOne(greenwareRepository.save(exist));
    }

    /** 转区：换到另一个末级工位 */
    @Transactional
    public Greenware move(Long id, Long targetAreaId) {
        Greenware exist = requireExists(id);
        if (targetAreaId == null) {
            throw new BizException("目标工位不能为空");
        }
        validateArea(targetAreaId, id);
        exist.setAreaId(targetAreaId);
        return decorateOne(greenwareRepository.save(exist));
    }

    @Transactional
    public void delete(Long id) {
        Greenware exist = requireExists(id);
        if (Greenware.FIRING.equals(exist.getStage())) {
            throw new BizException("坯体【" + exist.getCode() + "】正在窑内烧制，不能删除");
        }
        greenwareRepository.delete(exist);
    }

    public Greenware requireExists(Long id) {
        return greenwareRepository.findById(id)
                .orElseThrow(() -> new BizException("坯体不存在，id=" + id));
    }

    // ---------- 校验规则 ----------

    private void validateArea(Long areaId, Long excludeGreenwareId) {
        WorkArea area = workAreaService.requireExists(areaId);
        if (!workAreaService.isLeaf(areaId)) {
            throw new BizException("工位分区【" + area.getName() + "】下还有子分区，坯体只能存放在末级工位");
        }
        if (area.getCapacity() == null) {
            throw new BizException("工位分区【" + area.getName() + "】未设置容量，不能存放坯体");
        }
        int used = 0;
        for (Greenware g : greenwareRepository.findByAreaIdOrderByIdAsc(areaId)) {
            if (excludeGreenwareId == null || !excludeGreenwareId.equals(g.getId())) {
                used++;
            }
        }
        if (used >= area.getCapacity()) {
            throw new BizException("工位【" + area.getName() + "】容量 " + area.getCapacity()
                    + " 件，已占用 " + used + " 件，放不下更多坯体了");
        }
    }

    private void validateMaterial(Greenware g) {
        Material clay = materialRepository.findById(g.getClayId())
                .orElseThrow(() -> new BizException("泥料不存在，id=" + g.getClayId()));
        if (!"CLAY".equals(clay.getKind())) {
            throw new BizException("材料【" + clay.getName() + "】不是泥料，不能用来成型坯体");
        }
        if (g.getGlazeId() != null) {
            Material glaze = materialRepository.findById(g.getGlazeId())
                    .orElseThrow(() -> new BizException("釉料不存在，id=" + g.getGlazeId()));
            if (!"GLAZE".equals(glaze.getKind())) {
                throw new BizException("材料【" + glaze.getName() + "】不是釉料，不能施在坯体上");
            }
        }
    }

    private void validateStageEnter(String from, String to, Greenware g) {
        if (from == null) {
            from = Greenware.SHAPED;
        }
        if (from.equals(to)) {
            return;
        }
        int fromIdx = STAGE_ORDER.indexOf(from);
        int toIdx = STAGE_ORDER.indexOf(to);
        if (fromIdx < 0 || toIdx < 0) {
            throw new BizException("干燥阶段取值不合法，可选：" + String.join("、", STAGE_LABEL.values()));
        }
        if (toIdx < fromIdx) {
            throw new BizException("坯体干燥阶段不能回退，当前【" + stageLabel(from) + "】无法回到【" + stageLabel(to) + "】");
        }
        if (toIdx > fromIdx + 1) {
            throw new BizException("坯体干燥阶段不能跳级，从【" + stageLabel(from) + "】只能推进到【"
                    + stageLabel(STAGE_ORDER.get(fromIdx + 1)) + "】");
        }
        if (Greenware.BISQUE_READY.equals(to) && g.getMoisture() != null
                && g.getMoisture().compareTo(MAX_MOISTURE_FOR_BISQUE) > 0) {
            throw new BizException("坯体【" + g.getCode() + "】含水率 " + fmt(g.getMoisture())
                    + "% 高于 " + fmt(MAX_MOISTURE_FOR_BISQUE) + "%，还没干透不能进入可素烧");
        }
        if (Greenware.GLAZED.equals(to) && g.getGlazeId() == null) {
            throw new BizException("坯体【" + g.getCode() + "】还没指定釉料，不能标记为已施釉");
        }
    }

    private void collectAreaIds(Long areaId, List<Long> sink) {
        sink.add(areaId);
        for (WorkAreaNode child : workAreaService.children(areaId)) {
            collectAreaIds(child.getId(), sink);
        }
    }

    private List<Greenware> decorate(List<Greenware> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<Long, String> materialNames = new HashMap<>();
        for (Material m : materialRepository.findAll()) {
            materialNames.put(m.getId(), m.getName());
        }
        Map<Long, String> areaNames = new HashMap<>();
        for (WorkArea a : workAreaService.list()) {
            areaNames.put(a.getId(), a.getName());
        }
        for (Greenware g : rows) {
            g.setClayName(materialNames.get(g.getClayId()));
            g.setGlazeName(g.getGlazeId() == null ? null : materialNames.get(g.getGlazeId()));
            g.setAreaName(areaNames.get(g.getAreaId()));
        }
        return rows;
    }

    private Greenware decorateOne(Greenware g) {
        return decorate(new ArrayList<>(List.of(g))).get(0);
    }

    private String fmt(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
