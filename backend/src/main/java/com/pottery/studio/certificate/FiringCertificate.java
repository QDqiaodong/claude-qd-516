package com.pottery.studio.certificate;

import com.pottery.studio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

/**
 * 烧成履历凭证：版本化的不可变快照行（append-only）。
 * 首签生成 V1（CURRENT）；更正时旧版置 SUPERSEDED 保留、新版本成为 CURRENT。
 * 每一列都是签发当时的资料副本，课程/材料/作品之后被修正不影响旧凭证。
 */
@Entity
@Table(name = "firing_certificate")
public class FiringCertificate extends BaseEntity {

    /** 当前版本：页面只允许把这个状态的版本当作"当前凭证" */
    public static final String CURRENT = "CURRENT";
    /** 历史版本：已被更正的新版本取代，保留只读，不得覆盖 */
    public static final String SUPERSEDED = "SUPERSEDED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "artwork_id")
    private Long artworkId;

    @Column(name = "version_no")
    private Integer versionNo;

    @Column(name = "status")
    private String status;

    /** 更正原因；V1 首签为 null */
    @Column(name = "change_reason")
    private String changeReason;

    @Column(name = "issued_by")
    private String issuedBy;

    // ---------- 作品快照 ----------

    @Column(name = "artwork_code")
    private String artworkCode;

    @Column(name = "artwork_title")
    private String artworkTitle;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "owner_status")
    private String ownerStatus;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    // ---------- 课程快照 ----------

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "course_code")
    private String courseCode;

    @Column(name = "course_title")
    private String courseTitle;

    @Column(name = "teacher")
    private String teacher;

    // ---------- 坯体 + 泥料/釉料快照 ----------

    @Column(name = "greenware_id")
    private Long greenwareId;

    @Column(name = "greenware_code")
    private String greenwareCode;

    @Column(name = "greenware_name")
    private String greenwareName;

    @Column(name = "clay_id")
    private Long clayId;

    @Column(name = "clay_code")
    private String clayCode;

    @Column(name = "clay_name")
    private String clayName;

    @Column(name = "glaze_id")
    private Long glazeId;

    @Column(name = "glaze_code")
    private String glazeCode;

    @Column(name = "glaze_name")
    private String glazeName;

    // ---------- 烧成批次 + 窑炉快照 ----------

    @Column(name = "firing_batch_id")
    private Long firingBatchId;

    @Column(name = "batch_no")
    private String batchNo;

    @Column(name = "kiln_id")
    private Long kilnId;

    @Column(name = "kiln_name")
    private String kilnName;

    @Column(name = "fire_type")
    private String fireType;

    @Column(name = "target_temp")
    private Integer targetTemp;

    @Column(name = "peak_temp")
    private Integer peakTemp;

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

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getArtworkId() {
        return artworkId;
    }

    public void setArtworkId(Long artworkId) {
        this.artworkId = artworkId;
    }

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public String getArtworkCode() {
        return artworkCode;
    }

    public void setArtworkCode(String artworkCode) {
        this.artworkCode = artworkCode;
    }

    public String getArtworkTitle() {
        return artworkTitle;
    }

    public void setArtworkTitle(String artworkTitle) {
        this.artworkTitle = artworkTitle;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getOwnerStatus() {
        return ownerStatus;
    }

    public void setOwnerStatus(String ownerStatus) {
        this.ownerStatus = ownerStatus;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
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

    public Long getFiringBatchId() {
        return firingBatchId;
    }

    public void setFiringBatchId(Long firingBatchId) {
        this.firingBatchId = firingBatchId;
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

    public String getKilnName() {
        return kilnName;
    }

    public void setKilnName(String kilnName) {
        this.kilnName = kilnName;
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

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
}
