package com.btsheng.erp.business.crm.materialbarcode.dto;

import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialBarcode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "物料条码响应")
public class BarcodeResponse {
    @Schema(description = "条码号 BC{yyyyMMdd}{seq:4}")
    private String barcodeNo;
    @Schema(description = "物料编码")
    private String materialCode;
    @Schema(description = "物料名称")
    private String materialName;
    @Schema(description = "规格")
    private String spec;
    @Schema(description = "单位")
    private String unit;
    @Schema(description = "工艺 ID")
    private Long processId;
    @Schema(description = "工艺路线（JSON）")
    private String processRoute;
    @Schema(description = "5 段成本")
    private CostBreakdown costBreakdown;
    @Schema(description = "批次号")
    private String batchNo;
    @Schema(description = "数量")
    private Integer qty;
    @Schema(description = "二维码 base64（P2 修补）")
    private String qrCodeUrl;
    @Schema(description = "状态")
    private String status;
    @Schema(description = "生成时间")
    private String generatedAt;
    @Schema(description = "扫码历史（最近 10 条）")
    private List<ScanHistoryItem> history;

    @Data
    public static class CostBreakdown {
        private BigDecimal material;
        private BigDecimal labor;
        private BigDecimal machine;
        private BigDecimal overhead;
        private BigDecimal outsource;
        private BigDecimal total;
    }

    @Data
    public static class ScanHistoryItem {
        private String scanType;
        private Long scanUserId;
        private String scanAt;
        private String scanLocation;
    }

    public static BarcodeResponse from(CrmMaterialBarcode entity) {
        BarcodeResponse r = new BarcodeResponse();
        r.setBarcodeNo(entity.getBarcodeNo());
        r.setMaterialCode(entity.getMaterialCode());
        r.setSpec(entity.getSpec());
        r.setProcessId(entity.getProcessId());
        r.setBatchNo(entity.getBatchNo());
        r.setQty(entity.getQty());
        r.setQrCodeUrl(entity.getQrCodeUrl());
        r.setStatus(entity.getStatus());
        r.setGeneratedAt(entity.getGeneratedAt() == null ? null : entity.getGeneratedAt().toString());
        return r;
    }
}
