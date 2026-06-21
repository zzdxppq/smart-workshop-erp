package com.btsheng.erp.business.crm.bom.entity;

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
 * V1.3.7 · Story 1.9 · BOM 多级树（5 级递归上限）
 *
 * P1 修补 1：item_level 0-4 + parent_item_id 自引用
 * P2 修补：物料替代（substitute_materials + is_substitute）
 */
@Data
@Schema(description = "BOM 多级树（crm_bom_item）")
@TableName("crm_bom_item")
public class CrmBomItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("bom_id") private Long bomId;
    @TableField("parent_item_id") private Long parentItemId;            // 自引用
            @TableField("item_level") private Integer itemLevel = 0;            // 0-4
    @TableField("item_no") private Integer itemNo = 1;                  // 同级排序
            @TableField("material_code") private String materialCode;
    @TableField("material_name") private String materialName;
    @TableField("spec") private String spec;
    @TableField("qty") private BigDecimal qty = BigDecimal.ONE;         // P1 修补 3：正数
            @TableField("unit") private String unit = "PCS";
    @TableField("unit_cost") private BigDecimal unitCost = BigDecimal.ZERO;
    @TableField("total_cost") private BigDecimal totalCost = BigDecimal.ZERO;
    @TableField("segment") private String segment = "原材料";           // 5 段
            @TableField("substitute_materials") private String substituteMaterials; // 替代物料
    @TableField("is_substitute") private Integer isSubstitute = 0;
    @TableField("process_step_id") private Long processStepId;          // Story 1.10
            @TableField("created_at") private LocalDateTime createdAt;
}
