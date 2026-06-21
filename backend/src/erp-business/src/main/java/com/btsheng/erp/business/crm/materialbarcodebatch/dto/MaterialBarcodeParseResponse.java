package com.btsheng.erp.business.crm.materialbarcodebatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * V1.3.8 · Story 3.2 · 物料码解析响应 DTO（扫码用）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "物料码解析响应（Story 3.2 AC-3.2.2）")
public class MaterialBarcodeParseResponse {

    private Long materialId;

    @Schema(description = "料号 WL-XXXX（统一术语：原『物料号』『物料编码』均称料号）")
    private String materialNo;

    private Long batchId;

    @Schema(description = "批次号 BATCH-YYYYMMDD-流水")
    private String batchNo;

    private LocalDateTime arrivedAt;

    @Schema(description = "PENDING / PASSED / REJECTED")
    private String qualityStatus;
}