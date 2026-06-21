package com.btsheng.erp.business.crm.batch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * V1.3.8 · Story 3.1 · PO 状态查询响应 DTO（按物料粒度）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "PO 状态查询响应（按物料粒度 · Story 3.1 AC-3.1.2）")
public class PoStatusResponse {

    private Long poId;

    @Schema(description = "PO 总状态：PENDING_SHIP / PARTIAL_ARRIVED / ALL_ARRIVED / CANCELLED")
    private String poStatus;

    private List<ItemStatus> items;

    @Data
    public static class ItemStatus {
        private Long materialId;
        private Integer ordered;
        private Integer arrived;
        private Integer batchCount;
        private String qualityStatus;
    }
}