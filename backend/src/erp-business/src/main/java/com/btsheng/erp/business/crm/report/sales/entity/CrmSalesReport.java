package com.btsheng.erp.business.crm.report.sales.entity;

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
@Schema(description = "销售报表")
@TableName("crm_sales_report")
public class CrmSalesReport implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("report_no")     private String reportNo;
    @TableField("customer_name") private String customerName;
    @TableField("sales_user")    private String salesUser;
    @TableField("amount")        private BigDecimal amount = BigDecimal.ZERO;
    @TableField("order_count")   private Integer orderCount = 0;
    @TableField("rank_no")       private Integer rankNo;
    @TableField("report_period") private String reportPeriod;
    @TableField("snapshot_at")   private LocalDateTime snapshotAt;
    @TableField("created_at")    private LocalDateTime createdAt;
}
