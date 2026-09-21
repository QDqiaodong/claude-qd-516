package com.pottery.studio.greenware;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pottery.studio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * 工位分区（成型区 / 干燥区 / 施釉区 ...）。
 * 数据访问路线：JPA 自关联品类树 —— parent 指向自身，children 为自身集合。
 */
@Entity
@Table(name = "work_area")
public class WorkArea extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id", insertable = false, updatable = false)
    @JsonIgnore
    private WorkArea parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY)
    @JsonIgnore
    private List<WorkArea> children = new ArrayList<>();

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    /** 可容纳坯体数，仅末级工位有值 */
    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Transient
    private Integer greenwareCount;

    @Transient
    private Integer depth;

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

    public WorkArea getParent() {
        return parent;
    }

    public void setParent(WorkArea parent) {
        this.parent = parent;
    }

    public List<WorkArea> getChildren() {
        return children;
    }

    public void setChildren(List<WorkArea> children) {
        this.children = children;
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

    public Integer getGreenwareCount() {
        return greenwareCount;
    }

    public void setGreenwareCount(Integer greenwareCount) {
        this.greenwareCount = greenwareCount;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }
}
