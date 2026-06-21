package com.btsheng.erp.business.crm.materialbarcodebatch.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.8 · Story 3.2 · 物料码批次映射表（crm_material_barcode_batch）
 *
 * <p>V1.3.7 1.11 老表 crm_material_barcode 字段 barcode_no 格式 BC{yyyyMMdd}{seq:4}，
 * 仅标识物料，不能定位批次。V1.3.8 新表 barcode_no 格式 WL-{material_no}-BATCH-{YYYYMMDD}-{seq:4}，
 * 复合物料+批次，扫码一次获得两层信息。
 *
 * <p>迁移：V50__material_barcode_batch.sql
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "物料码批次映射（Story 3.2 复合物料码）")
@TableName("crm_material_barcode_batch")
public class CrmMaterialBarcodeBatch implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("material_id")
    private Long materialId;

    @TableField("batch_id")
    private Long batchId;

    @Schema(description = "WL-{material_no}-BATCH-{YYYYMMDD}-{seq:4}")
    @TableField("barcode_no")
    private String barcodeNo;

    @TableField("is_active")
    private Integer isActive = 1;

    @TableField("created_at")
    private LocalDateTime createdAt;
}