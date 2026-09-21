package com.pottery.studio.firing;

import com.pottery.studio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

/** 烧成批次：装窑 → 升温 → 保温 → 冷却 → 出窑，串成一条时间轴 */
@Entity
@Table(name = "firing_batch")
public class FiringBatch extends BaseEntity {

    public static final String LOADING = "LOADING";
    public static final String HEATING = "HEATING";
    public static final String SOAKING = "SOAKING";
    public static final String COOLING = "COOLING";
    public static final String OUT = "OUT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "kiln_id")
    private Long kilnId;

    /** BISQUE 素烧 / GLAZE 釉烧 */
    @Column(name = "fire_type")
    private String fireType;

    @Column(name = "stage")
    private String stage;

    @Column(name = "target_temp")
    private Integer targetTemp;

    @Column(name = "peak_temp")
    private Integer peakTemp;

    @Column(name = "greenware_count")
    private Integer greenwareCount;

    @Column(name = "loaded_at")
    private LocalDateTime loadedAt;

    @Column(name = "heating_at")
    private LocalDateTime heatingAt;

    @Column(name = "soaking_at")
    private LocalDateTime soakingAt;

    @Column(name = "cooling_at")
    private LocalDateTime coolingAt;

    @Column(name = "out_at")
    private LocalDateTime outAt;

    @Column(name = "remark")
    private String remark;

    @Transient
    private String kilnName;

    @Transient
    private Integer kilnMaxTemp;

    @Transient
    private Integer kilnCapacity;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Long getKilnId() {
        return kilnId;
    }

    public void setKilnId(Long kilnId) {
        this.kilnId = kilnId;
    }

    public String getFireType() {
        return fireType;
    }

    public void setFireType(String fireType) {
        this.fireType = fireType;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public Integer getTargetTemp() {
        return targetTemp;
    }

    public void setTargetTemp(Integer targetTemp) {
        this.targetTemp = targetTemp;
    }

    public Integer getPeakTemp() {
        return peakTemp;
    }

    public void setPeakTemp(Integer peakTemp) {
        this.peakTemp = peakTemp;
    }

    public Integer getGreenwareCount() {
        return greenwareCount;
    }

    public void setGreenwareCount(Integer greenwareCount) {
        this.greenwareCount = greenwareCount;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(LocalDateTime loadedAt) {
        this.loadedAt = loadedAt;
    }

    public LocalDateTime getHeatingAt() {
        return heatingAt;
    }

    public void setHeatingAt(LocalDateTime heatingAt) {
        this.heatingAt = heatingAt;
    }

    public LocalDateTime getSoakingAt() {
        return soakingAt;
    }

    public void setSoakingAt(LocalDateTime soakingAt) {
        this.soakingAt = soakingAt;
    }

    public LocalDateTime getCoolingAt() {
        return coolingAt;
    }

    public void setCoolingAt(LocalDateTime coolingAt) {
        this.coolingAt = coolingAt;
    }

    public LocalDateTime getOutAt() {
        return outAt;
    }

    public void setOutAt(LocalDateTime outAt) {
        this.outAt = outAt;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getKilnName() {
        return kilnName;
    }

    public void setKilnName(String kilnName) {
        this.kilnName = kilnName;
    }

    public Integer getKilnMaxTemp() {
        return kilnMaxTemp;
    }

    public void setKilnMaxTemp(Integer kilnMaxTemp) {
        this.kilnMaxTemp = kilnMaxTemp;
    }

    public Integer getKilnCapacity() {
        return kilnCapacity;
    }

    public void setKilnCapacity(Integer kilnCapacity) {
        this.kilnCapacity = kilnCapacity;
    }
}
