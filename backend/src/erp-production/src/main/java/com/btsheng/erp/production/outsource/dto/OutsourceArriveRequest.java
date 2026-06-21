package com.btsheng.erp.production.outsource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * V1.3.5 · E12-S2 · 仓管扫 WW- 委外到货请求
 */
@Data
@Schema(description = "委外到货扫码请求")
public class OutsourceArriveRequest {

    @Schema(description = "委外单号（与扫码一致）", example = "WW20260612-0001")
    private String outsourceNo;

    @Schema(description = "实收数量", example = "50")
    private Integer actualQty;

    @Schema(description = "实收重量 kg", example = "12.5")
    private BigDecimal actualWeight;

    @Schema(description = "外观照片 URL")
    private List<String> photoUrls;

    @Schema(description = "备注")
    private String remark;
}
