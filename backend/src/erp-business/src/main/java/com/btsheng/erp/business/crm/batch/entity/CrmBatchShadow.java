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
 * V1.3.8 · Story 3.1 · 批次影子表（与 crm_batch 结构一致，供双写对比 cron 使用）
 *
 * <p>对比逻辑：每 1h cron job 对比 crm_batch 与 crm_batch_shadow 聚合（batch_no + material_id），
 * 不一致率 > 0.1% 触发 stream:notify 告警。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "物料批次影子表（Story 3.1 灰度对比）")
@TableName("crm_batch_shadow")
public class CrmBatchShadow implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

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

    @TableField("quality_status")
    private String qualityStatus = "PENDING";

    @TableField("created_by")
    private Long createdBy;

    @TableField("created_at")
    private LocalDateTime createdAt;
}