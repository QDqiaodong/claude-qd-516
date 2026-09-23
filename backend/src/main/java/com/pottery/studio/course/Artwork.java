package com.pottery.studio.course;

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

/** 学员作品 */
@Entity
@Table(name = "artwork")
public class Artwork extends BaseEntity {

    /** TAKEN 学员带走 / CONSIGN 留馆寄售 / SOLD 已售出 */
    public static final String TAKEN = "TAKEN";
    public static final String CONSIGN = "CONSIGN";
    public static final String SOLD = "SOLD";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "title")
    private String title;

    @Column(name = "student_name")
    private String studentName;

    @Column(name = "course_id")
    private Long courseId;

    @Column(name = "greenware_id")
    private Long greenwareId;

    @Column(name = "firing_batch_id")
    private Long firingBatchId;

    @Column(name = "owner_status")
    private String ownerStatus;

    @Column(name = "consign_price")
    private BigDecimal consignPrice;

    @Column(name = "shelf_no")
    private String shelfNo;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Transient
    private String courseTitle;

    @Transient
    private String greenwareCode;

    @Transient
    private String batchNo;

    /** 当前凭证版本号（列表展示用）；未签发为 null，历史版本不计入 */
    @Transient
    private Integer certVersionNo;

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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public Long getCourseId() {
        return courseId;
    }

    public void setCourseId(Long courseId) {
        this.courseId = courseId;
    }

    public Long getGreenwareId() {
        return greenwareId;
    }

    public void setGreenwareId(Long greenwareId) {
        this.greenwareId = greenwareId;
    }

    public Long getFiringBatchId() {
        return firingBatchId;
    }

    public void setFiringBatchId(Long firingBatchId) {
        this.firingBatchId = firingBatchId;
    }

    public String getOwnerStatus() {
        return ownerStatus;
    }

    public void setOwnerStatus(String ownerStatus) {
        this.ownerStatus = ownerStatus;
    }

    public BigDecimal getConsignPrice() {
        return consignPrice;
    }

    public void setConsignPrice(BigDecimal consignPrice) {
        this.consignPrice = consignPrice;
    }

    public String getShelfNo() {
        return shelfNo;
    }

    public void setShelfNo(String shelfNo) {
        this.shelfNo = shelfNo;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getGreenwareCode() {
        return greenwareCode;
    }

    public void setGreenwareCode(String greenwareCode) {
        this.greenwareCode = greenwareCode;
    }

    public String getBatchNo() {
        return batchNo;
    }

    public void setBatchNo(String batchNo) {
        this.batchNo = batchNo;
    }

    public Integer getCertVersionNo() {
        return certVersionNo;
    }

    public void setCertVersionNo(Integer certVersionNo) {
        this.certVersionNo = certVersionNo;
    }
}
