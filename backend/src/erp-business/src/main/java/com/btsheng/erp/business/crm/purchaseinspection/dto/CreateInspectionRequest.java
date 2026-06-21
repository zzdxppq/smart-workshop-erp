package com.btsheng.erp.business.crm.purchaseinspection.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

/**
 * V1.3.7 · Story 1.35 · 创建采购来料质检单请求
 */
@Data
@Schema(description = "创建采购来料质检单请求（Story 1.35 FR-8-4）")
public class CreateInspectionRequest {

    @Schema(description = "采购单 ID", example = "1", required = true)
    private Long poId;
    @Schema(description = "采购单号", example = "PO20260401-0001", required = true)
    private String poNo;
    @Schema(description = "到货 ID（1.34 关联）")
    private Long incomingId;
    @Schema(description = "厂商 ID")
    private Long vendorId;
    @Schema(description = "厂商名称")
    private String vendorName;
    @Schema(description = "物料 ID", example = "1001", required = true)
    private Long materialId;
    @Schema(description = "物料编码")
    private String materialCode;
    @Schema(description = "物料名称")
    private String materialName;
    @Schema(description = "扫码批次号（1.12 关联）")
    private String batchNo;
    @Schema(description = "质检员 ID", example = "601", required = true)
    private Long inspectorId;
    @Schema(description = "质检员姓名")
    private String inspectorName;
    @Schema(description = "抽样数", example = "50", required = true)
    private Integer sampleSize;
    @Schema(description = "AQL 等级 · P1 修补 2 I/II/III", example = "II")
    private String aqlLevel = "II";
    @Schema(description = "通知邮箱 · P1 修补 1 单一 163 邮箱 AD-3", example = "inspect@btsheng-163.com")
    private String notifyEmail;
    @Schema(description = "备注")
    private String remark;
}
