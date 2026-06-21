package com.btsheng.erp.business.crm.quote.cost.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工序-成本项自动匹配规则（V1.3.9 CNC工艺库报价联动）
 */
@Data
@TableName("crm_process_cost_mapping")
public class ProcessCostMapping implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("keyword")
    private String keyword;            // 工序名称关键词

    @TableField("cost_item_code")
    private String costItemCode;        // 匹配的成本项编码

    @TableField("sort_order")
    private Integer sortOrder;         // 匹配优先级

    @TableField("is_active")
    private Integer isActive = 1;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
