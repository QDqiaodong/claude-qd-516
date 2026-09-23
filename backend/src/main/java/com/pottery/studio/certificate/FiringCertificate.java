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
 * 烧成履历凭证：一行 = 一次签发/更正后不可变的快照版本。
 * 只能追加新版本、把旧版本置为 SUPERSEDED，绝不 UPDATE 旧行的快照内容。
 * 每个作品同时只有一条 status = CURRENT。
 */
@Entity
@Table(name = "firing_certificate")
public class FiringCertificate extends BaseEntity {

    /** 当前版本：页面、打印默认展示的版本 */
    public static final String CURRENT = "CURRENT";
    /** 历史版本：已被新版本取代，仅供追溯查看，不能被当作当前版 */
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

    @Column(name = "certificate_no")
    private String certificateNo;

    @Column(name = "issued_by")
    private String issuedBy;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    /** 更正原因，V1 首签时为空 */
    @Column(name = "change_reason")
    private String changeReason;

    // ---------- 作品快照 ----------

    @Column(name = "snap_artwork_code")
    private String snapArtworkCode;

    @Column(name = "snap_title")
    private String snapTitle;

    @Column(name = "snap_student_name")
    private String snapStudentName;

    @Column(name = "snap_owner_status")
    private String snapOwnerStatus;

    // ---------- 课程快照 ----------

    @Column(name = "snap_course_id")
    private Long snapCourseId;

    @Column(name = "snap_course_code")
    private String snapCourseCode;

    @Column(name = "snap_course_title")
    private String snapCourseTitle;

    @Column(name = "snap_teacher")
    private String snapTeacher;

    // ---------- 来源坯体快照 ----------

    @Column(name = "snap_greenware_id")
    private Long snapGreenwareId;

    @Column(name = "snap_greenware_code")
    private String snapGreenwareCode;

    @Column(name = "snap_greenware_name")
    private String snapGreenwareName;

    @Column(name = "snap_greenware_stage")
    private String snapGreenwareStage;

    @Column(name = "snap_shaped_at")
    private LocalDateTime snapShapedAt;

    // ---------- 泥料 / 釉料快照 ----------

    @Column(name = "snap_clay_id")
    private Long snapClayId;

    @Column(name = "snap_clay_code")
    private String snapClayCode;

    @Column(name = "snap_clay_name")
    private String snapClayName;

    @Column(name = "snap_clay_temp")
    private Integer snapClayTemp;

    @Column(name = "snap_glaze_id")
    private Long snapGlazeId;

    @Column(name = "snap_glaze_code")
    private String snapGlazeCode;

    @Column(name = "snap_glaze_name")
    private String snapGlazeName;

    @Column(name = "snap_glaze_temp")
    private Integer snapGlazeTemp;

    // ---------- 烧成批次 / 窑炉快照 ----------

    @Column(name = "snap_batch_id")
    private Long snapBatchId;

    @Column(name = "snap_batch_no")
    private String snapBatchNo;

    @Column(name = "snap_fire_type")
    private String snapFireType;

    @Column(name = "snap_target_temp")
    private Integer snapTargetTemp;

    @Column(name = "snap_peak_temp")
    private Integer snapPeakTemp;

    @Column(name = "snap_kiln_id")
    private Long snapKilnId;

    @Column(name = "snap_kiln_code")
    private String snapKilnCode;

    @Column(name = "snap_kiln_name")
    private String snapKilnName;

    @Column(name = "snap_loaded_at")
    private LocalDateTime snapLoadedAt;

    @Column(name = "snap_heating_at")
    private LocalDateTime snapHeatingAt;

    @Column(name = "snap_soaking_at")
    private LocalDateTime snapSoakingAt;

    @Column(name = "snap_cooling_at")
    private LocalDateTime snapCoolingAt;

    @Column(name = "snap_out_at")
    private LocalDateTime snapOutAt;

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

    public String getCertificateNo() {
        return certificateNo;
    }

    public void setCertificateNo(String certificateNo) {
        this.certificateNo = certificateNo;
    }

    public String getIssuedBy() {
        return issuedBy;
    }

    public void setIssuedBy(String issuedBy) {
        this.issuedBy = issuedBy;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getSnapArtworkCode() {
        return snapArtworkCode;
    }

    public void setSnapArtworkCode(String snapArtworkCode) {
        this.snapArtworkCode = snapArtworkCode;
    }

    public String getSnapTitle() {
        return snapTitle;
    }

    public void setSnapTitle(String snapTitle) {
        this.snapTitle = snapTitle;
    }

    public String getSnapStudentName() {
        return snapStudentName;
    }

    public void setSnapStudentName(String snapStudentName) {
        this.snapStudentName = snapStudentName;
    }

    public String getSnapOwnerStatus() {
        return snapOwnerStatus;
    }

    public void setSnapOwnerStatus(String snapOwnerStatus) {
        this.snapOwnerStatus = snapOwnerStatus;
    }

    public Long getSnapCourseId() {
        return snapCourseId;
    }

    public void setSnapCourseId(Long snapCourseId) {
        this.snapCourseId = snapCourseId;
    }

    public String getSnapCourseCode() {
        return snapCourseCode;
    }

    public void setSnapCourseCode(String snapCourseCode) {
        this.snapCourseCode = snapCourseCode;
    }

    public String getSnapCourseTitle() {
        return snapCourseTitle;
    }

    public void setSnapCourseTitle(String snapCourseTitle) {
        this.snapCourseTitle = snapCourseTitle;
    }

    public String getSnapTeacher() {
        return snapTeacher;
    }

    public void setSnapTeacher(String snapTeacher) {
        this.snapTeacher = snapTeacher;
    }

    public Long getSnapGreenwareId() {
        return snapGreenwareId;
    }

    public void setSnapGreenwareId(Long snapGreenwareId) {
        this.snapGreenwareId = snapGreenwareId;
    }

    public String getSnapGreenwareCode() {
        return snapGreenwareCode;
    }

    public void setSnapGreenwareCode(String snapGreenwareCode) {
        this.snapGreenwareCode = snapGreenwareCode;
    }

    public String getSnapGreenwareName() {
        return snapGreenwareName;
    }

    public void setSnapGreenwareName(String snapGreenwareName) {
        this.snapGreenwareName = snapGreenwareName;
    }

    public String getSnapGreenwareStage() {
        return snapGreenwareStage;
    }

    public void setSnapGreenwareStage(String snapGreenwareStage) {
        this.snapGreenwareStage = snapGreenwareStage;
    }

    public LocalDateTime getSnapShapedAt() {
        return snapShapedAt;
    }

    public void setSnapShapedAt(LocalDateTime snapShapedAt) {
        this.snapShapedAt = snapShapedAt;
    }

    public Long getSnapClayId() {
        return snapClayId;
    }

    public void setSnapClayId(Long snapClayId) {
        this.snapClayId = snapClayId;
    }

    public String getSnapClayCode() {
        return snapClayCode;
    }

    public void setSnapClayCode(String snapClayCode) {
        this.snapClayCode = snapClayCode;
    }

    public String getSnapClayName() {
        return snapClayName;
    }

    public void setSnapClayName(String snapClayName) {
        this.snapClayName = snapClayName;
    }

    public Integer getSnapClayTemp() {
        return snapClayTemp;
    }

    public void setSnapClayTemp(Integer snapClayTemp) {
        this.snapClayTemp = snapClayTemp;
    }

    public Long getSnapGlazeId() {
        return snapGlazeId;
    }

    public void setSnapGlazeId(Long snapGlazeId) {
        this.snapGlazeId = snapGlazeId;
    }

    public String getSnapGlazeCode() {
        return snapGlazeCode;
    }

    public void setSnapGlazeCode(String snapGlazeCode) {
        this.snapGlazeCode = snapGlazeCode;
    }

    public String getSnapGlazeName() {
        return snapGlazeName;
    }

    public void setSnapGlazeName(String snapGlazeName) {
        this.snapGlazeName = snapGlazeName;
    }

    public Integer getSnapGlazeTemp() {
        return snapGlazeTemp;
    }

    public void setSnapGlazeTemp(Integer snapGlazeTemp) {
        this.snapGlazeTemp = snapGlazeTemp;
    }

    public Long getSnapBatchId() {
        return snapBatchId;
    }

    public void setSnapBatchId(Long snapBatchId) {
        this.snapBatchId = snapBatchId;
    }

    public String getSnapBatchNo() {
        return snapBatchNo;
    }

    public void setSnapBatchNo(String snapBatchNo) {
        this.snapBatchNo = snapBatchNo;
    }

    public String getSnapFireType() {
        return snapFireType;
    }

    public void setSnapFireType(String snapFireType) {
        this.snapFireType = snapFireType;
    }

    public Integer getSnapTargetTemp() {
        return snapTargetTemp;
    }

    public void setSnapTargetTemp(Integer snapTargetTemp) {
        this.snapTargetTemp = snapTargetTemp;
    }

    public Integer getSnapPeakTemp() {
        return snapPeakTemp;
    }

    public void setSnapPeakTemp(Integer snapPeakTemp) {
        this.snapPeakTemp = snapPeakTemp;
    }

    public Long getSnapKilnId() {
        return snapKilnId;
    }

    public void setSnapKilnId(Long snapKilnId) {
        this.snapKilnId = snapKilnId;
    }

    public String getSnapKilnCode() {
        return snapKilnCode;
    }

    public void setSnapKilnCode(String snapKilnCode) {
        this.snapKilnCode = snapKilnCode;
    }

    public String getSnapKilnName() {
        return snapKilnName;
    }

    public void setSnapKilnName(String snapKilnName) {
        this.snapKilnName = snapKilnName;
    }

    public LocalDateTime getSnapLoadedAt() {
        return snapLoadedAt;
    }

    public void setSnapLoadedAt(LocalDateTime snapLoadedAt) {
        this.snapLoadedAt = snapLoadedAt;
    }

    public LocalDateTime getSnapHeatingAt() {
        return snapHeatingAt;
    }

    public void setSnapHeatingAt(LocalDateTime snapHeatingAt) {
        this.snapHeatingAt = snapHeatingAt;
    }

    public LocalDateTime getSnapSoakingAt() {
        return snapSoakingAt;
    }

    public void setSnapSoakingAt(LocalDateTime snapSoakingAt) {
        this.snapSoakingAt = snapSoakingAt;
    }

    public LocalDateTime getSnapCoolingAt() {
        return snapCoolingAt;
    }

    public void setSnapCoolingAt(LocalDateTime snapCoolingAt) {
        this.snapCoolingAt = snapCoolingAt;
    }

    public LocalDateTime getSnapOutAt() {
        return snapOutAt;
    }

    public void setSnapOutAt(LocalDateTime snapOutAt) {
        this.snapOutAt = snapOutAt;
    }
}
