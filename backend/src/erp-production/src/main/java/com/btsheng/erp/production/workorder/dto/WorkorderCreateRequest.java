package com.btsheng.erp.production.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "工单创建请求（AC-5.1.1）")
public class WorkorderCreateRequest {
    @Schema(description = "图纸 ID", required = true)
    private Long drawingId;
    @Schema(description = "BOM ID", required = true)
    private Long bomId;
    @Schema(description = "工艺路线 ID", required = true)
    private Long processRouteId;
    @Schema(description = "物料编码（成品 CP-XXXX）", required = true)
    private String materialCode;
    @Schema(description = "产品名称", required = true)
    private String productName;
    @Schema(description = "数量", required = true)
    private Integer qty;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "优先级 1=紧急 ~ 10=低")
    private Integer priority = 5;
    @Schema(description = "机台 ID")
    private Long equipmentId;
    @Schema(description = "机台类型")
    private String equipmentType;
    @Schema(description = "预计工时")
    private BigDecimal estimatedHours;
    @Schema(description = "FA 件")
    private Integer isFa = 0;
    @Schema(description = "备注")
    private String remark;
    @Schema(description = "销售订单 ID")
    private Long salesOrderId;
    @Schema(description = "销售订单号 XS")
    private String salesOrderNo;
    @Schema(description = "预设工单号（订单转工单时使用）")
    private String workorderNo;
    @Schema(description = "计划交期")
    private String deliveryDate;
}
