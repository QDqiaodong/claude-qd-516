package com.pottery.studio.material;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class MaterialCategoryService {

    private final MaterialCategoryRepository categoryRepository;
    private final MaterialRepository materialRepository;

    public MaterialCategoryService(MaterialCategoryRepository categoryRepository,
                                   MaterialRepository materialRepository) {
        this.categoryRepository = categoryRepository;
        this.materialRepository = materialRepository;
    }

    public List<MaterialCategory> list() {
        List<MaterialCategory> all = categoryRepository.findAll();
        fillMaterialCount(all);
        return all;
    }

    /** 返回整棵自关联品类树 */
    public List<MaterialCategoryNode> tree() {
        List<MaterialCategory> all = categoryRepository.findAll();
        return buildTree(all);
    }

    /** 按父节点查子节点；parentId 为空时返回顶层分类 */
    public List<MaterialCategoryNode> children(Long parentId) {
        if (parentId != null) {
            requireExists(parentId);
        }
        List<MaterialCategory> all = categoryRepository.findAll();
        List<MaterialCategoryNode> roots = buildTree(all);
        if (parentId == null) {
            return roots;
        }
        MaterialCategoryNode hit = findNode(roots, parentId);
        if (hit == null) {
            throw new BizException("分类不存在，id=" + parentId);
        }
        return hit.getChildren();
    }

    @Transactional
    public MaterialCategory create(MaterialCategory request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException("分类名称不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("分类编码不能为空");
        }
        if (categoryRepository.existsByCode(request.getCode())) {
            throw new BizException("分类编码【" + request.getCode() + "】已存在");
        }
        Long parentId = request.getParentId();
        if (parentId != null) {
            requireExists(parentId);
        }
        // 默认值一律在 service 的 create 里给
        if (request.getSortNo() == null) {
            request.setSortNo(0);
        }
        request.setDepth(parentId == null ? 1 : depthOf(parentId) + 1);
        return categoryRepository.save(request);
    }

    @Transactional
    public MaterialCategory update(Long id, MaterialCategory request) {
        MaterialCategory exist = requireExists(id);
        if (request.getParentId() != null && !request.getParentId().equals(exist.getParentId())) {
            MaterialCategory newParent = requireExists(request.getParentId());
            if (isDescendant(newParent, id)) {
                throw new BizException("不能把分类【" + exist.getName() + "】挂到自己的下级分类【" + newParent.getName() + "】下面");
            }
        }
        PartialCopy.apply(request, exist, "code", "depth", "materialCount", "parent", "children");
        exist.setDepth(exist.getParentId() == null ? 1 : depthOf(exist.getParentId()) + 1);
        return categoryRepository.save(exist);
    }

    @Transactional
    public void delete(Long id) {
        MaterialCategory exist = requireExists(id);
        List<MaterialCategory> children = categoryRepository.findByParentIdOrderBySortNoAscIdAsc(id);
        if (!children.isEmpty()) {
            throw new BizException("分类【" + exist.getName() + "】下还有 " + children.size() + " 个子分类，不能删除");
        }
        long used = materialRepository.countByCategoryId(id);
        if (used > 0) {
            throw new BizException("分类【" + exist.getName() + "】下还挂着 " + used + " 种材料，不能删除");
        }
        categoryRepository.delete(exist);
    }

    public MaterialCategory requireExists(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BizException("材料分类不存在，id=" + id));
    }

    // ---------- 内部工具 ----------

    private void fillMaterialCount(List<MaterialCategory> all) {
        Map<Long, Long> counter = new HashMap<>();
        for (Material m : materialRepository.findAll()) {
            counter.merge(m.getCategoryId(), 1L, Long::sum);
        }
        for (MaterialCategory c : all) {
            c.setMaterialCount(counter.getOrDefault(c.getId(), 0L).intValue());
            c.setDepth(depthOf(c.getId()));
        }
    }

    private List<MaterialCategoryNode> buildTree(List<MaterialCategory> all) {
        Map<Long, Long> counter = new HashMap<>();
        for (Material m : materialRepository.findAll()) {
            counter.merge(m.getCategoryId(), 1L, Long::sum);
        }
        Map<Long, MaterialCategoryNode> nodeMap = new HashMap<>();
        for (MaterialCategory c : all) {
            MaterialCategoryNode node = new MaterialCategoryNode();
            node.setId(c.getId());
            node.setParentId(c.getParentId());
            node.setCode(c.getCode());
            node.setName(c.getName());
            node.setSortNo(c.getSortNo());
            node.setMaterialCount(counter.getOrDefault(c.getId(), 0L).intValue());
            nodeMap.put(c.getId(), node);
        }
        List<MaterialCategoryNode> roots = new ArrayList<>();
        for (MaterialCategory c : all) {
            MaterialCategoryNode node = nodeMap.get(c.getId());
            if (c.getParentId() == null) {
                roots.add(node);
            } else {
                MaterialCategoryNode parent = nodeMap.get(c.getParentId());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        assignDepth(roots, 1);
        sortTree(roots);
        return roots;
    }

    private void assignDepth(List<MaterialCategoryNode> nodes, int depth) {
        for (MaterialCategoryNode n : nodes) {
            n.setDepth(depth);
            assignDepth(n.getChildren(), depth + 1);
        }
    }

    private void sortTree(List<MaterialCategoryNode> nodes) {
        nodes.sort((a, b) -> {
            int s = Integer.compare(a.getSortNo() == null ? 0 : a.getSortNo(), b.getSortNo() == null ? 0 : b.getSortNo());
            return s != 0 ? s : Long.compare(a.getId(), b.getId());
        });
        for (MaterialCategoryNode n : nodes) {
            sortTree(n.getChildren());
        }
    }

    private MaterialCategoryNode findNode(List<MaterialCategoryNode> nodes, Long id) {
        for (MaterialCategoryNode n : nodes) {
            if (n.getId().equals(id)) {
                return n;
            }
            MaterialCategoryNode hit = findNode(n.getChildren(), id);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private int depthOf(Long id) {
        int depth = 1;
        Long cursor = id;
        while (cursor != null) {
            MaterialCategory c = categoryRepository.findById(cursor).orElse(null);
            if (c == null || c.getParentId() == null) {
                break;
            }
            depth++;
            cursor = c.getParentId();
            if (depth > 32) {
                break;
            }
        }
        return depth;
    }

    private boolean isDescendant(MaterialCategory candidate, Long ancestorId) {
        Long cursor = candidate.getParentId();
        int guard = 0;
        while (cursor != null && guard++ < 32) {
            if (cursor.equals(ancestorId)) {
                return true;
            }
            MaterialCategory c = categoryRepository.findById(cursor).orElse(null);
            if (c == null) {
                break;
            }
            cursor = c.getParentId();
        }
        return false;
    }
}
