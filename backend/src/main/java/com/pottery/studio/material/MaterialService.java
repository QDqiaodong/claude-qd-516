package com.pottery.studio.material;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialService {

    private static final BigDecimal ZERO = new BigDecimal("0");
    private static final BigDecimal MAX_SHRINK = new BigDecimal("30");
    private static final int MAX_TEMP_GAP = 80;

    private final MaterialRepository materialRepository;
    private final MaterialCategoryService categoryService;

    public MaterialService(MaterialRepository materialRepository, MaterialCategoryService categoryService) {
        this.materialRepository = materialRepository;
        this.categoryService = categoryService;
    }

    public List<Material> list(Long categoryId, String kind) {
        List<Material> rows;
        if (categoryId != null) {
            rows = materialRepository.findByCategoryIdOrderByIdAsc(categoryId);
        } else if (kind != null && !kind.isBlank()) {
            rows = materialRepository.findByKindOrderByIdAsc(kind);
        } else {
            rows = materialRepository.findAll();
        }
        return decorate(rows);
    }

    /** 取某分类及其所有后代分类下的材料 */
    public List<Material> listByCategoryTree(Long categoryId) {
        if (categoryId == null) {
            return decorate(materialRepository.findAll());
        }
        List<Long> ids = collectCategoryIds(categoryId);
        List<Material> rows = new ArrayList<>();
        for (Long cid : ids) {
            rows.addAll(materialRepository.findByCategoryIdOrderByIdAsc(cid));
        }
        return decorate(rows);
    }

    public Material get(Long id) {
        return decorateOne(requireExists(id));
    }

    @Transactional
    public Material create(Material request) {
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("材料编号不能为空");
        }
        if (materialRepository.existsByCode(request.getCode())) {
            throw new BizException("材料编号【" + request.getCode() + "】已存在");
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException("材料名称不能为空");
        }
        if (request.getCategoryId() == null) {
            throw new BizException("材料必须指定所属分类，请先选择分类");
        }
        // 默认值放在 service 的 create 里
        if (request.getStockKg() == null) {
            request.setStockKg(ZERO);
        }
        if (request.getUnitPrice() == null) {
            request.setUnitPrice(ZERO);
        }
        if (request.getSafetyStock() == null) {
            request.setSafetyStock(ZERO);
        }
        request.setStatus(calcStatus(request.getStockKg(), request.getSafetyStock()));
        validate(request);
        return decorateOne(materialRepository.save(request));
    }

    @Transactional
    public Material update(Long id, Material request) {
        Material exist = requireExists(id);
        if (request.getCategoryId() != null) {
            categoryService.requireExists(request.getCategoryId());
        }
        PartialCopy.apply(request, exist, "code", "categoryName", "pairClayName");
        validate(exist);
        exist.setStatus(calcStatus(exist.getStockKg(), exist.getSafetyStock()));
        return decorateOne(materialRepository.save(exist));
    }

    /** 领用出库 */
    @Transactional
    public Material consume(Long id, BigDecimal amount) {
        Material exist = requireExists(id);
        if (amount == null || amount.compareTo(ZERO) <= 0) {
            throw new BizException("领用数量必须大于 0");
        }
        if (exist.getStockKg() == null || exist.getStockKg().compareTo(amount) < 0) {
            throw new BizException("材料【" + exist.getName() + "】当前库存 "
                    + fmt(exist.getStockKg()) + "kg，不足本次领用 " + fmt(amount) + "kg");
        }
        exist.setStockKg(exist.getStockKg().subtract(amount));
        exist.setStatus(calcStatus(exist.getStockKg(), exist.getSafetyStock()));
        return decorateOne(materialRepository.save(exist));
    }

    @Transactional
    public void delete(Long id) {
        Material exist = requireExists(id);
        materialRepository.delete(exist);
    }

    public Material requireExists(Long id) {
        return materialRepository.findById(id)
                .orElseThrow(() -> new BizException("材料不存在，id=" + id));
    }

    // ---------- 校验规则 ----------

    private void validate(Material m) {
        String kind = m.getKind();
        if (kind == null || (!"CLAY".equals(kind) && !"GLAZE".equals(kind))) {
            throw new BizException("材料类型只能是 CLAY（泥料）或 GLAZE（釉料），当前传入【"
                    + (kind == null ? "空" : kind) + "】");
        }
        MaterialCategory category = categoryService.requireExists(m.getCategoryId());
        if (category == null) {
            throw new BizException("材料分类不存在，id=" + m.getCategoryId());
        }

        if ("CLAY".equals(kind)) {
            if (m.getShrinkRate() == null) {
                throw new BizException("泥料【" + m.getName() + "】必须填写收缩率");
            }
            if (m.getShrinkRate().compareTo(ZERO) < 0 || m.getShrinkRate().compareTo(MAX_SHRINK) > 0) {
                throw new BizException("泥料收缩率必须在 0%~30% 之间，当前为 "
                        + fmt(m.getShrinkRate()) + "%");
            }
        } else {
            if (m.getPairClayId() == null) {
                throw new BizException("釉料【" + m.getName() + "】必须指定配套泥料");
            }
            Material clay = requireExists(m.getPairClayId());
            if (!"CLAY".equals(clay.getKind())) {
                throw new BizException("釉料【" + m.getName() + "】的配套材料【" + clay.getName() + "】不是泥料");
            }
            if (m.getFiringTemp() == null || clay.getFiringTemp() == null) {
                throw new BizException("釉料【" + m.getName() + "】与配套泥料都必须填写烧成温度");
            }
            int gap = Math.abs(m.getFiringTemp() - clay.getFiringTemp());
            if (gap > MAX_TEMP_GAP) {
                throw new BizException("釉料【" + m.getName() + "】烧成温度 " + m.getFiringTemp()
                        + "℃ 与配套泥料【" + clay.getName() + "】" + clay.getFiringTemp()
                        + "℃ 相差 " + gap + "℃，超出允许温差 " + MAX_TEMP_GAP + "℃");
            }
        }
    }

    private String calcStatus(BigDecimal stock, BigDecimal safety) {
        BigDecimal s = stock == null ? ZERO : stock;
        BigDecimal f = safety == null ? ZERO : safety;
        if (s.compareTo(ZERO) <= 0) {
            return "DEPLETED";
        }
        return s.compareTo(f) <= 0 ? "LOW" : "NORMAL";
    }

    private List<Long> collectCategoryIds(Long categoryId) {
        List<Long> ids = new ArrayList<>();
        collect(categoryId, ids);
        return ids;
    }

    private void collect(Long categoryId, List<Long> sink) {
        sink.add(categoryId);
        for (MaterialCategoryNode child : categoryService.children(categoryId)) {
            collect(child.getId(), sink);
        }
    }

    private List<Material> decorate(List<Material> rows) {
        if (rows.isEmpty()) {
            return rows;
        }
        Map<Long, String> names = new HashMap<>();
        for (MaterialCategory c : categoryService.list()) {
            names.put(c.getId(), c.getName());
        }
        for (Material m : rows) {
            m.setCategoryName(names.get(m.getCategoryId()));
        }
        Map<Long, Material> clayCache = new HashMap<>();
        for (Material m : rows) {
            if (m.getPairClayId() != null) {
                Material clay = clayCache.computeIfAbsent(m.getPairClayId(), k -> materialRepository.findById(k).orElse(null));
                m.setPairClayName(clay == null ? null : clay.getName());
            }
        }
        return rows;
    }

    private Material decorateOne(Material m) {
        return decorate(new ArrayList<>(List.of(m))).get(0);
    }

    private String fmt(BigDecimal v) {
        return v == null ? "0.00" : v.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
