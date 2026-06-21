package com.btsheng.erp.business.crm.materialbarcodebatch.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.8 · Story 3.2 · 物料码生成响应 DTO
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "物料码生成响应（Story 3.2 AC-3.2.1）")
public class MaterialBarcodeGenerateResponse {

    @Schema(description = "复合物料码 WL-{material_no}-BATCH-{YYYYMMDD}-{seq:4}")
    private String barcodeNo;

    @Schema(description = "是否首次生成（false 表示已存在）")
    private Boolean isNew;

    @Schema(description = "老物料码（仅当 toActiveOld=false 时返回，V1.3.7 1.11 的 WL-XXXX）")
    private String oldBarcode;
}