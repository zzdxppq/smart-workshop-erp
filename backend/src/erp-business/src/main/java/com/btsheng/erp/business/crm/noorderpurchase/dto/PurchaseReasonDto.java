package com.btsheng.erp.business.crm.noorderpurchase.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * V1.3.8 · Story 4.1 · 采购理由 DTO（前端下拉用）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@AllArgsConstructor
@Schema(description = "采购理由字典项（Story 4.1 AC-4.1.2）")
public class PurchaseReasonDto {
    @Schema(description = "理由编码")
    private String code;
    @Schema(description = "理由名称")
    private String name;
    @Schema(description = "前端标签颜色")
    private String color;
}