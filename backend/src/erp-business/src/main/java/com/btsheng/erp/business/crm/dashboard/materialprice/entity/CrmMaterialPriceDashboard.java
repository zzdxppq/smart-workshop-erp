package com.btsheng.erp.business.crm.dashboard.materialprice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Schema(description = "料号价格看板")
@TableName("crm_material_price_dashboard")
public class CrmMaterialPriceDashboard implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("dashboard_no")   private String dashboardNo;
    @TableField("material_code")  private String materialCode;
    @TableField("material_name")  private String materialName;
    @TableField("vendor_name")    private String vendorName;
    @TableField("price")          private BigDecimal price = BigDecimal.ZERO;
    @TableField("price_period")   private String pricePeriod;
    @TableField("price_type")     private String priceType;
    @TableField("cost_total")     private BigDecimal costTotal;
    @TableField("price_trend")    private BigDecimal priceTrend;
    @TableField("snapshot_at")    private LocalDateTime snapshotAt;
    @TableField("created_at")     private LocalDateTime createdAt;
}
