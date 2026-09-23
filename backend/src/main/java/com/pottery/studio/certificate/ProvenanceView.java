package com.pottery.studio.certificate;

import java.util.List;

/**
 * 从当前业务表实时汇总的来源链视图（不是快照）：
 * 凭证签发前核对、以及"当前资料 vs 当前凭证"差异对比都基于它。
 */
public class ProvenanceView {

    private boolean courseMissing;
    private boolean greenwareMissing;
    private boolean batchMissing;
    private boolean clayMissing;
    private boolean glazeMissing;
    private boolean kilnMissing;
    private boolean linkConflict;

    private List<SourceCheck> checks;
    private boolean issuable;
    /** 不能签发时给工作人员的总说明（含缺的是哪段来源） */
    private String blockMessage;

    // 课程
    private Long courseId;
    private String courseCode;
    private String courseTitle;
    private String teacher;
    // 坯体
    private Long greenwareId;
    private String greenwareCode;
    private String greenwareName;
    private String greenwareStage;
    private Long greenwareBatchId;
    // 泥料
    private Long clayId;
    private String clayCode;
    private String clayName;
    // 釉料（可空）
    private Long glazeId;
    private String glazeCode;
    private String glazeName;
    // 批次 + 窑炉
    private Long batchId;
    private String batchNo;
    private String batchStage;
    private String fireType;
    private Integer targetTemp;
    private Integer peakTemp;
    private java.time.LocalDateTime loadedAt;
    private java.time.LocalDateTime heatingAt;
    private java.time.LocalDateTime soakingAt;
    private java.time.LocalDateTime coolingAt;
    private java.time.LocalDateTime outAt;
    private Long kilnId;
    private String kilnName;

    public List<SourceCheck> getChecks() {
        return checks;
    }

    public void setChecks(List<SourceCheck> checks) {
        this.checks = checks;
    }

    public boolean isCourseMissing() {
        return courseMissing;
    }

    public void setCourseMissing(boolean courseMissing) {
        this.courseMissing = courseMissing;
    }

    public boolean isGreenwareMissing() {
        return greenwareMissing;
    }

    public void setGreenwareMissing(boolean greenwareMissing) {
        this.greenwareMissing = greenwareMissing;
    }

    public boolean isBatchMissing() {
        return batchMissing;
    }

    public void setBatchMissing(boolean batchMissing) {
        this.batchMissing = batchMissing;
    }

    public boolean isClayMissing() {
        return clayMissing;
    }

    public void setClayMissing(boolean clayMissing) {
        this.clayMissing = clayMissing;
    }

    public boolean isGlazeMissing() {
        return glazeMissing;
    }

    public void setGlazeMissing(boolean glazeMissing) {
        this.glazeMissing = glazeMissing;
    }

    public boolean isKilnMissing() {
        return kilnMissing;
    }

    public void setKilnMissing(boolean kilnMissing) {
        this.kilnMissing = kilnMissing;
    }

    public boolean isLinkConflict() {
        return linkConflict;
    }

    public void setLinkConflict(boolean linkConflict) {
        this.linkConflict = linkConflict;
    }

    public boolean isIssuable() {
        return issuable;
    }

    public void setIssuable(boolean issuable) {
        this.issuable = issuable;
    }

    public String getBlockMessage() {
        return blockMessage;
    }

    public void setBlockMessage(String blockMessage) {
        this.blockMessage = blockMessage;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getTeacher() {
        return teacher;
    }

    public void setTeacher(String teacher) {
        this.teacher = teacher;
    }

    public Long getGreenwareId() {
        return greenwareId;
    }

    public void setGreenwareId(Long greenwareId) {
        this.greenwareId = greenwareId;
    }

    public String getGreenwareCode() {
        return greenwareCode;
    }

    public void setGreenwareCode(String greenwareCode) {
        this.greenwareCode = greenwareCode;
    }

    public String getGreenwareName() {
        return greenwareName;
    }

    public void setGreenwareName(String greenwareName) {
        this.greenwareName = greenwareName;
    }

    public String getGreenwareStage() {
        return greenwareStage;
    }

    public void setGreenwareStage(String greenwareStage) {
        this.greenwareStage = greenwareStage;
    }

    public Long getGreenwareBatchId() {
        return greenwareBatchId;
    }

    public void setGreenwareBatchId(Long greenwareBatchId) {
        this.greenwareBatchId = greenwareBatchId;
    }

    public Long getClayId() {
        return clayId;
    }

    public void setClayId(Long clayId) {
        this.clayId = clayId;
    }

    public String getClayCode() {
        return clayCode;
    }

    public void setClayCode(String clayCode) {
        this.clayCode = clayCode;
    }

    public String getClayName() {
        return clayName;
    }

    public void setClayName(String clayName) {
        this.clayName = clayName;
    }

    public Long getGlazeId() {
        return glazeId;
    }

    public void setGlazeId(Long glazeId) {
        this.glazeId = glazeId;
    }

    public String getGlazeCode() {
        return glazeCode;
    }

    public void setGlazeCode(String glazeCode) {
        this.glazeCode = glazeCode;
    }

    public String getGlazeName() {
        return glazeName;
    }

    public void setGlazeName(String glazeName) {
        this.glazeName = glazeName;
    }

    public Long getBatchId() {
        return batchId;
    }

    public void setBatchId(Long batchId) {
        this.batchId = batchId;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public String getBatchStage() {
        return batchStage;
    }

    public void setBatchStage(String batchStage) {
        this.batchStage = batchStage;
    }

    public String getFireType() {
        return fireType;
    }

    public void setFireType(String fireType) {
        this.fireType = fireType;
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

    public java.time.LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(java.time.LocalDateTime loadedAt) {
        this.loadedAt = loadedAt;
    }

    public java.time.LocalDateTime getHeatingAt() {
        return heatingAt;
    }

    public void setHeatingAt(java.time.LocalDateTime heatingAt) {
        this.heatingAt = heatingAt;
    }

    public java.time.LocalDateTime getSoakingAt() {
        return soakingAt;
    }

    public void setSoakingAt(java.time.LocalDateTime soakingAt) {
        this.soakingAt = soakingAt;
    }

    public java.time.LocalDateTime getCoolingAt() {
        return coolingAt;
    }

    public void setCoolingAt(java.time.LocalDateTime coolingAt) {
        this.coolingAt = coolingAt;
    }

    public java.time.LocalDateTime getOutAt() {
        return outAt;
    }

    public void setOutAt(java.time.LocalDateTime outAt) {
        this.outAt = outAt;
    }

    public Long getKilnId() {
        return kilnId;
    }

    public void setKilnId(Long kilnId) {
        this.kilnId = kilnId;
    }

    public String getKilnName() {
        return kilnName;
    }

    public void setKilnName(String kilnName) {
        this.kilnName = kilnName;
    }
}
