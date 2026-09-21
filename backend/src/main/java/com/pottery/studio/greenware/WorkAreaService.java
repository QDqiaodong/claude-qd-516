package com.pottery.studio.greenware;

import com.pottery.studio.common.BizException;
import com.pottery.studio.common.PartialCopy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkAreaService {

    private final WorkAreaRepository areaRepository;
    private final GreenwareRepository greenwareRepository;

    public WorkAreaService(WorkAreaRepository areaRepository, GreenwareRepository greenwareRepository) {
        this.areaRepository = areaRepository;
        this.greenwareRepository = greenwareRepository;
    }

    public List<WorkArea> list() {
        return decorate(areaRepository.findAll());
    }

    /** 返回整棵自关联分区树 */
    public List<WorkAreaNode> tree() {
        return buildTree(areaRepository.findAll());
    }

    /** 按父节点查子节点；parentId 为空时返回顶层分区 */
    public List<WorkAreaNode> children(Long parentId) {
        List<WorkAreaNode> roots = tree();
        if (parentId == null) {
            return roots;
        }
        WorkAreaNode hit = findNode(roots, parentId);
        if (hit == null) {
            throw new BizException("工位分区不存在，id=" + parentId);
        }
        return hit.getChildren();
    }

    @Transactional
    public WorkArea create(WorkArea request) {
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BizException("分区名称不能为空");
        }
        if (request.getCode() == null || request.getCode().isBlank()) {
            throw new BizException("分区编码不能为空");
        }
        if (areaRepository.existsByCode(request.getCode())) {
            throw new BizException("分区编码【" + request.getCode() + "】已存在");
        }
        if (request.getParentId() != null) {
            WorkArea parent = requireExists(request.getParentId());
            if (parent.getCapacity() != null) {
                throw new BizException("末级工位【" + parent.getName() + "】下面不能再建子分区");
            }
        }
        // 默认值放在 service 的 create 里
        if (request.getSortNo() == null) {
            request.setSortNo(0);
        }
        return areaRepository.save(request);
    }

    @Transactional
    public WorkArea update(Long id, WorkArea request) {
        WorkArea exist = requireExists(id);
        if (request.getParentId() != null) {
            WorkArea parent = requireExists(request.getParentId());
            if (parent.getId().equals(id) || isDescendant(parent, id)) {
                throw new BizException("不能把分区【" + exist.getName() + "】挂到自己的下级分区【" + parent.getName() + "】下面");
            }
        }
        if (request.getCapacity() != null && !areaRepository.findByParentIdOrderBySortNoAscIdAsc(id).isEmpty()) {
            throw new BizException("分区【" + exist.getName() + "】下还有子分区，只有末级工位才能设置容量");
        }
        PartialCopy.apply(request, exist, "code", "depth", "greenwareCount", "parent", "children");
        return areaRepository.save(exist);
    }

    @Transactional
    public void delete(Long id) {
        WorkArea exist = requireExists(id);
        List<WorkArea> children = areaRepository.findByParentIdOrderBySortNoAscIdAsc(id);
        if (!children.isEmpty()) {
            throw new BizException("分区【" + exist.getName() + "】下还有 " + children.size() + " 个子分区，不能删除");
        }
        long used = greenwareRepository.findByAreaIdOrderByIdAsc(id).size();
        if (used > 0) {
            throw new BizException("分区【" + exist.getName() + "】里还放着 " + used + " 件坯体，不能删除");
        }
        areaRepository.delete(exist);
    }

    public WorkArea requireExists(Long id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new BizException("工位分区不存在，id=" + id));
    }

    public boolean isLeaf(Long id) {
        return areaRepository.findByParentIdOrderBySortNoAscIdAsc(id).isEmpty();
    }

    private List<WorkArea> decorate(List<WorkArea> rows) {
        for (WorkArea a : rows) {
            a.setGreenwareCount(greenwareRepository.findByAreaIdOrderByIdAsc(a.getId()).size());
            a.setDepth(depthOf(a.getId()));
        }
        return rows;
    }

    private List<WorkAreaNode> buildTree(List<WorkArea> all) {
        Map<Long, WorkAreaNode> nodeMap = new HashMap<>();
        for (WorkArea a : all) {
            WorkAreaNode node = new WorkAreaNode();
            node.setId(a.getId());
            node.setParentId(a.getParentId());
            node.setCode(a.getCode());
            node.setName(a.getName());
            node.setCapacity(a.getCapacity());
            node.setSortNo(a.getSortNo());
            node.setGreenwareCount(greenwareRepository.findByAreaIdOrderByIdAsc(a.getId()).size());
            nodeMap.put(a.getId(), node);
        }
        List<WorkAreaNode> roots = new ArrayList<>();
        for (WorkArea a : all) {
            WorkAreaNode node = nodeMap.get(a.getId());
            if (a.getParentId() == null) {
                roots.add(node);
            } else {
                WorkAreaNode parent = nodeMap.get(a.getParentId());
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

    private void assignDepth(List<WorkAreaNode> nodes, int depth) {
        for (WorkAreaNode n : nodes) {
            n.setDepth(depth);
            assignDepth(n.getChildren(), depth + 1);
        }
    }

    private void sortTree(List<WorkAreaNode> nodes) {
        nodes.sort((a, b) -> {
            int s = Integer.compare(a.getSortNo() == null ? 0 : a.getSortNo(), b.getSortNo() == null ? 0 : b.getSortNo());
            return s != 0 ? s : Long.compare(a.getId(), b.getId());
        });
        for (WorkAreaNode n : nodes) {
            sortTree(n.getChildren());
        }
    }

    private WorkAreaNode findNode(List<WorkAreaNode> nodes, Long id) {
        for (WorkAreaNode n : nodes) {
            if (n.getId().equals(id)) {
                return n;
            }
            WorkAreaNode hit = findNode(n.getChildren(), id);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    private int depthOf(Long id) {
        int depth = 1;
        Long cursor = id;
        int guard = 0;
        while (cursor != null && guard++ < 32) {
            WorkArea a = areaRepository.findById(cursor).orElse(null);
            if (a == null || a.getParentId() == null) {
                break;
            }
            depth++;
            cursor = a.getParentId();
        }
        return depth;
    }

    private boolean isDescendant(WorkArea candidate, Long ancestorId) {
        Long cursor = candidate.getParentId();
        int guard = 0;
        while (cursor != null && guard++ < 32) {
            if (cursor.equals(ancestorId)) {
                return true;
            }
            WorkArea a = areaRepository.findById(cursor).orElse(null);
            if (a == null) {
                break;
            }
            cursor = a.getParentId();
        }
        return false;
    }
}
