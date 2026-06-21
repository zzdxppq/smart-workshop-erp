package com.btsheng.erp.business.crm.materialbarcodebatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * V1.3.8 · Story 3.2 · 物料码生成请求 DTO
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "物料码生成请求（Story 3.2 AC-3.2.1 · 内部接口由 3.1 调用）")
public class MaterialBarcodeGenerateRequest {

    @Schema(description = "物料 ID", example = "5001", required = true)
    @NotNull
    private Long materialId;

    @Schema(description = "批次 ID（来自 crm_batch，由 Story 3.1 生成）", example = "8001", required = true)
    @NotNull
    private Long batchId;

    @Schema(description = "料号 WL-XXXX（用于拼接复合物料条码）", example = "WL-A001", required = true)
    @NotNull
    private String materialNo;
}