package com.btsheng.erp.business.crm.batch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.8 · Story 3.1 · 物料批次表 (crm_batch · FR-8-3 升级)
 *
 * <p>V1.3.7 crm_incoming 是 PO 粒度（一条记录 = 一次到货汇总）。
 * V1.3.8 crm_batch 是物料粒度（一条记录 = 一个物料的一批到货），一个 PO 可能产生多条 batch。
 *
 * <p>迁移：V49__batch.sql
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "物料批次（Story 3.1 FR-8-3 升级）")
@TableName("crm_batch")
public class CrmBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "批次号 BATCH-YYYYMMDD-流水")
    @TableField("batch_no")
    private String batchNo;

    @TableField("material_id")
    private Long materialId;

    @TableField("po_id")
    private Long poId;

    @TableField("po_item_id")
    private Long poItemId;

    @TableField("quantity")
    private Integer quantity;

    @TableField("arrived_at")
    private LocalDateTime arrivedAt;

    @Schema(description = "PENDING / PASSED / REJECTED")
    @TableField("quality_status")
    private String qualityStatus = "PENDING";

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}