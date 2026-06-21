package com.btsheng.erp.business.crm.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.8 · Story 3.1 · 批次创建响应 DTO
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "批次创建响应（Story 3.1 AC-3.1.1）")
public class BatchCreateResponse {

    @Schema(description = "生成的批次列表（每个物料一行）")
    private List<BatchInfo> batches;

    @Schema(description = "操作后 PO 状态（PENDING_SHIP / PARTIAL_ARRIVED / ALL_ARRIVED）")
    private String poStatusAfter;

    @Schema(description = "触发的来料检单号列表（按物料粒度）")
    private List<String> qualityOrders;

    @Data
    public static class BatchInfo {
        private String batchNo;
        private Long materialId;
        private Integer quantity;
    }
}