package com.pottery.studio.greenware;

import java.util.ArrayList;
import java.util.List;

/** 工位分区树节点 */
public class WorkAreaNode {

    private Long id;
    private Long parentId;
    private String code;
    private String name;
    private Integer capacity;
    private Integer sortNo;
    private Integer depth;
    private Integer greenwareCount;
    private List<WorkAreaNode> children = new ArrayList<>();

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

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
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

    public Integer getGreenwareCount() {
        return greenwareCount;
    }

    public void setGreenwareCount(Integer greenwareCount) {
        this.greenwareCount = greenwareCount;
    }

    public List<WorkAreaNode> getChildren() {
        return children;
    }

    public void setChildren(List<WorkAreaNode> children) {
        this.children = children;
    }
}
