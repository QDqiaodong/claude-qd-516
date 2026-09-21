package com.pottery.studio.greenware;

import com.pottery.studio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 坯体 */
@Entity
@Table(name = "greenware")
public class Greenware extends BaseEntity {

    public static final String SHAPED = "SHAPED";
    public static final String DRYING = "DRYING";
    public static final String BISQUE_READY = "BISQUE_READY";
    public static final String BISQUED = "BISQUED";
    public static final String GLAZED = "GLAZED";
    public static final String FIRING = "FIRING";
    public static final String FINISHED = "FINISHED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "clay_id")
    private Long clayId;

    @Column(name = "glaze_id")
    private Long glazeId;

    @Column(name = "area_id")
    private Long areaId;

    @Column(name = "stage")
    private String stage;

    /** 含水率 % */
    @Column(name = "moisture")
    private BigDecimal moisture;

    @Column(name = "height_cm")
    private BigDecimal heightCm;

    @Column(name = "firing_batch_id")
    private Long firingBatchId;

    @Column(name = "shaped_at")
    private LocalDateTime shapedAt;

    @Transient
    private String clayName;

    @Transient
    private String glazeName;

    @Transient
    private String areaName;

    @Transient
    private String batchNo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getClayId() {
        return clayId;
    }

    public void setClayId(Long clayId) {
        this.clayId = clayId;
    }

    public Long getGlazeId() {
        return glazeId;
    }

    public void setGlazeId(Long glazeId) {
        this.glazeId = glazeId;
    }

    public Long getAreaId() {
        return areaId;
    }

    public void setAreaId(Long areaId) {
        this.areaId = areaId;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public BigDecimal getMoisture() {
        return moisture;
    }

    public void setMoisture(BigDecimal moisture) {
        this.moisture = moisture;
    }

    public BigDecimal getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(BigDecimal heightCm) {
        this.heightCm = heightCm;
    }

    public Long getFiringBatchId() {
        return firingBatchId;
    }

    public void setFiringBatchId(Long firingBatchId) {
        this.firingBatchId = firingBatchId;
    }

    public LocalDateTime getShapedAt() {
        return shapedAt;
    }

    public void setShapedAt(LocalDateTime shapedAt) {
        this.shapedAt = shapedAt;
    }

    public String getClayName() {
        return clayName;
    }

    public void setClayName(String clayName) {
        this.clayName = clayName;
    }

    public String getGlazeName() {
        return glazeName;
    }

    public void setGlazeName(String glazeName) {
        this.glazeName = glazeName;
    }

    public String getAreaName() {
        return areaName;
    }

    public void setAreaName(String areaName) {
        this.areaName = areaName;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }
}
