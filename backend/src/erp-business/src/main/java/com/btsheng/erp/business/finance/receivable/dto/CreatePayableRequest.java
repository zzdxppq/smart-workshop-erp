package com.btsheng.erp.business.finance.receivable.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * V1.3.7 · Story 1.36 · 创建应付请求
 */
@Data
@Schema(description = "创建应付请求（Story 1.36 FR-9-1）")
public class CreatePayableRequest {
    @Schema(description = "供应商 ID", required = true) private Long vendorId;
    @Schema(description = "供应商名称") private String vendorName;
    @Schema(description = "PO ID", required = true) private Long poId;
    @Schema(description = "PO 编号", required = true) private String poNo;
    @Schema(description = "PO 金额 · P1 修补 1 非负", required = true) private BigDecimal totalAmount;
    @Schema(description = "到期日", required = true) private LocalDate dueDate;
}
