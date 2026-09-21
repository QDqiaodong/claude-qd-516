package com.pottery.studio.firing;

import com.pottery.studio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;

/** 窑炉 */
@Entity
@Table(name = "kiln")
public class Kiln extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    /** ELECTRIC 电窑 / GAS 燃气窑 */
    @Column(name = "kiln_type")
    private String kilnType;

    @Column(name = "max_temp")
    private Integer maxTemp;

    @Column(name = "volume_l")
    private Integer volumeL;

    /** IDLE 空闲 / FIRING 烧制中 / MAINTAIN 检修 */
    @Column(name = "status")
    private String status;

    @Transient
    private Integer runningBatchCount;

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

    public String getKilnType() {
        return kilnType;
    }

    public void setKilnType(String kilnType) {
        this.kilnType = kilnType;
    }

    public Integer getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(Integer maxTemp) {
        this.maxTemp = maxTemp;
    }

    public Integer getVolumeL() {
        return volumeL;
    }

    public void setVolumeL(Integer volumeL) {
        this.volumeL = volumeL;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getRunningBatchCount() {
        return runningBatchCount;
    }

    public void setRunningBatchCount(Integer runningBatchCount) {
        this.runningBatchCount = runningBatchCount;
    }
}
