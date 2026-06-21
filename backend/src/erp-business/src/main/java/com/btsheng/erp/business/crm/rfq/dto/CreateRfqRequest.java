package com.btsheng.erp.business.crm.rfq.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.32 · 创建询价单请求
 */
@Data
@Schema(description = "创建询价单请求（Story 1.32 FR-8-1）")
public class CreateRfqRequest {

    @Schema(description = "询价标题", example = "6061 铝板 100 套采购询比价", required = true)
    private String title;

    @Schema(description = "物料 ID", example = "1001", required = true)
    private Long materialId;

    @Schema(description = "物料编码")
    private String materialCode;

    @Schema(description = "物料名称")
    private String materialName;

    @Schema(description = "需求数量", example = "100", required = true)
    private BigDecimal qty;

    @Schema(description = "单位", example = "PCS")
    private String unit;

    @Schema(description = "预算金额 · P1 修补 3", example = "50000.00", required = true)
    private BigDecimal budgetAmount;

    @Schema(description = "需求到货日")
    private LocalDate requiredDate;

    @Schema(description = "中标模式 LOWEST/WEIGHTED", example = "LOWEST")
    private String winnerMode = "LOWEST";

    @Schema(description = "询价来源 MATERIAL/OUTSOURCE/NO_ORDER")
    private String inquirySourceType;

    @Schema(description = "绑定采购申请 ID")
    private Long prId;

    @Schema(description = "绑定采购申请单号")
    private String prNo;

    @Schema(description = "关联工单号")
    private String workorderNo;

    @Schema(description = "委外工序号")
    private Integer processStepNo;

    @Schema(description = "待委外工序分配 ID")
    private Long allocationId;
}
