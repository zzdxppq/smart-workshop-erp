package com.btsheng.erp.business.crm.quote.template.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V2.1 · 报价与订单协同设计 · 报价范本
 */
@Data
@Schema(description = "报价范本（crm_quote_template）")
@TableName("crm_quote_template")
public class CrmQuoteTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("template_no") private String templateNo;       // MB-YYYYMMDD-NNNN
    @TableField("template_name") private String templateName;  // 范本名称
    @TableField("category") private String category;           // 分类：机加工/钣金/组装等
    @TableField("process_type") private String processType;    // 工艺类型
    @TableField("cost_material") private BigDecimal costMaterial = BigDecimal.ZERO;  // 材料成本
    @TableField("cost_labor") private BigDecimal costLabor = BigDecimal.ZERO;      // 人工成本
    @TableField("cost_machine") private BigDecimal costMachine = BigDecimal.ZERO;   // 机台成本
    @TableField("cost_overhead") private BigDecimal costOverhead = BigDecimal.ZERO; // 管理费率
    @TableField("cost_outsource") private BigDecimal costOutsource = BigDecimal.ZERO; // 委外费率
    @TableField("profit_margin") private BigDecimal profitMargin = new BigDecimal("0.20"); // 利润率
    @TableField("billing_method") private String billingMethod = "BY_QUANTITY";  // 计费方式
    @TableField("unit") private String unit = "件";            // 计费单位
    @TableField("remark") private String remark;              // 备注
    @TableField("is_active") private Integer isActive = 1;    // 是否启用
    @TableField("created_by") private Long createdBy;         // 创建人
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
