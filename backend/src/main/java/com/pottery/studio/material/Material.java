package com.pottery.studio.material;

import com.pottery.studio.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;

/** 泥料 / 釉料台账 */
@Entity
@Table(name = "material")
public class Material extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "code")
    private String code;

    @Column(name = "name")
    private String name;

    /** CLAY 泥料 / GLAZE 釉料 */
    @Column(name = "kind")
    private String kind;

    @Column(name = "stock_kg")
    private BigDecimal stockKg;

    /** 收缩率 %（仅泥料） */
    @Column(name = "shrink_rate")
    private BigDecimal shrinkRate;

    @Column(name = "firing_temp")
    private Integer firingTemp;

    /** 配套泥料（仅釉料） */
    @Column(name = "pair_clay_id")
    private Long pairClayId;

    @Column(name = "unit_price")
    private BigDecimal unitPrice;

    @Column(name = "safety_stock")
    private BigDecimal safetyStock;

    /** NORMAL 正常 / LOW 低库存 / DEPLETED 耗尽 */
    @Column(name = "status")
    private String status;

    @Transient
    private String categoryName;

    @Transient
    private String pairClayName;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public BigDecimal getStockKg() {
        return stockKg;
    }

    public void setStockKg(BigDecimal stockKg) {
        this.stockKg = stockKg;
    }

    public BigDecimal getShrinkRate() {
        return shrinkRate;
    }

    public void setShrinkRate(BigDecimal shrinkRate) {
        this.shrinkRate = shrinkRate;
    }

    public Integer getFiringTemp() {
        return firingTemp;
    }

    public void setFiringTemp(Integer firingTemp) {
        this.firingTemp = firingTemp;
    }

    public Long getPairClayId() {
        return pairClayId;
    }

    public void setPairClayId(Long pairClayId) {
        this.pairClayId = pairClayId;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getSafetyStock() {
        return safetyStock;
    }

    public void setSafetyStock(BigDecimal safetyStock) {
        this.safetyStock = safetyStock;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getPairClayName() {
        return pairClayName;
    }

    public void setPairClayName(String pairClayName) {
        this.pairClayName = pairClayName;
    }
}
