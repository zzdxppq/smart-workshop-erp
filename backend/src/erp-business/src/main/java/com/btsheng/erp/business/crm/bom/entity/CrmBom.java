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
 * V1.3.7 · Story 1.9 · AC-3.3 BOM 主表
 *
 * 3 状态机：DRAFT → RELEASED → ARCHIVED
 * 4 P1 修补：5 级递归上限 / 物料编码唯一 / 数量正整数 / 发布后只读
 * 4 P2 修补：5 段成本聚合 / 物料替代 / 多 BOM 版本 / BOM 对比
 */
@Data
@Schema(description = "BOM 主表（crm_bom）")
@TableName("crm_bom")
public class CrmBom implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("bom_no") private String bomNo;                       // BOM{yyyyMMdd}{seq:4}
            @TableField("bom_version") private String bomVersion = "v1";      // 多版本（P2 修补）
    @TableField("drawing_id") private Long drawingId;                  // 源图纸 ID
            @TableField("drawing_no") private String drawingNo;
    @TableField("bom_type") private String bomType = "STANDARD";       // STANDARD/FA/PROTOTYPE
            @TableField("target_qty") private Integer targetQty = 1;           // P1 修补 3：正整数
    @TableField("material_code") private String materialCode;          // P1 修补 2：唯一
            @TableField("total_cost") private BigDecimal totalCost = BigDecimal.ZERO;
    @TableField("cost_breakdown") private String costBreakdown;        // 5 段成本明细 JSON
            @TableField("process_route_id") private Long processRouteId;       // 关联工艺（Story 1.10）
    @TableField("parent_bom_id") private Long parentBomId;             // 多级树
            @TableField("bom_level") private Integer bomLevel = 0;             // 0-4（P1 修补 1：5 级上限）
    @TableField("status") private String status = "DRAFT";             // DRAFT/RELEASED/ARCHIVED
            @TableField("owner_user_id") private Long ownerUserId;
    @TableField("released_by") private Long releasedBy;
    @TableField("released_at") private LocalDateTime releasedAt;
    @TableField("is_substitutable") private Integer isSubstitutable = 0; // P2 修补：物料替代
            @TableField("comment") private String comment;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
