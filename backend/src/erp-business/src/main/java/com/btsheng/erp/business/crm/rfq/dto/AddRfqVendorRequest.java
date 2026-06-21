package com.btsheng.erp.business.crm.rfq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * V1.3.7 · Story 1.32 · 添加询价厂商请求
 */
@Data
@Schema(description = "添加询价厂商请求（Story 1.32 FR-8-1）")
public class AddRfqVendorRequest {

    @Schema(description = "厂商 ID", example = "901", required = true)
    private Long vendorId;

    @Schema(description = "厂商名称", example = "上海铝业", required = true)
    private String vendorName;

    @Schema(description = "厂商编码")
    private String vendorCode;

    @Schema(description = "联系人")
    private String contactName;

    @Schema(description = "联系电话")
    private String contactPhone;
}
