package com.btsheng.erp.business.finance.profit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.39 · 财务·利润分析单（crm_profit_analysis · FR-9-4）
 *
 * <p>利润 = 订单收入 - 5 段成本
 * <p>跨模块：1.6 SETTLED 订单 + 1.37 5 段成本
 */
@Data
@Schema(description = "利润分析单（Story 1.39 FR-9-4）")
@TableName("crm_profit_analysis")
public class CrmProfitAnalysis implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("profit_no")      private String profitNo;
    @TableField("order_id")       private Long orderId;
    @TableField("order_no")       private String orderNo;
    @TableField("customer_id")    private Long customerId;
    @TableField("customer_name")  private String customerName;
    @TableField("product_id")     private Long productId;
    @TableField("product_code")   private String productCode;
    @TableField("product_name")   private String productName;
    @TableField("revenue")        private BigDecimal revenue;
    @TableField("cost_id")        private Long costId;
    @TableField("cost_no")        private String costNo;
    @TableField("total_cost")     private BigDecimal totalCost;
    @TableField("profit")         private BigDecimal profit;
    @TableField("profit_rate")    private BigDecimal profitRate;
    @TableField("alert_level")    private String alertLevel;
    @TableField("settled_date")   private LocalDate settledDate;
    @TableField("analysis_month") private String analysisMonth;
    @TableField("created_at")     private LocalDateTime createdAt;
    @TableField("updated_at")     private LocalDateTime updatedAt;
}
