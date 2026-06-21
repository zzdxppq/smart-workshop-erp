package com.btsheng.erp.business.crm.materialbarcode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "物料条码查询")
public class BarcodeQueryRequest {
    @Schema(description = "关键字（条码号/物料编码）")
    private String keyword;
    @Schema(description = "物料编码")
    private String materialCode;
    @Schema(description = "状态 ACTIVE/USED/DISCARDED")
    private String status;
    @Schema(description = "页码")
    private Integer page = 0;
    @Schema(description = "页大小")
    private Integer size = 20;
}
