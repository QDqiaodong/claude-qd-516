package com.pottery.studio.material;

import java.util.ArrayList;
import java.util.List;

/** 分类树节点（接口返回的层级结构） */
public class MaterialCategoryNode {

    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer sortNo;
    private Integer depth;
    private Integer materialCount;
    private List<MaterialCategoryNode> children = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getSortNo() {
        return sortNo;
    }

    public void setSortNo(Integer sortNo) {
        this.sortNo = sortNo;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public Integer getMaterialCount() {
        return materialCount;
    }

    public void setMaterialCount(Integer materialCount) {
        this.materialCount = materialCount;
    }

    public List<MaterialCategoryNode> getChildren() {
        return children;
    }

    public void setChildren(List<MaterialCategoryNode> children) {
        this.children = children;
    }
}
