package com.btsheng.erp.business.crm.warehouselocation.dto;

import com.btsheng.erp.business.crm.warehouselocation.entity.CrmBatch;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批次追溯响应（AC-4.3.2 · 扫码 → 批次 → 入库时间 + 操作人）")
public class BatchTraceResponse {
    private String batchNo;
    private String materialCode;
    private Long supplierId;
    private String supplierName;
    private Integer qty;
    private String qualityStatus;
    private String locationCode;
    private String receivedAt;
    private String expiredAt;
    private List<TraceStep> traceSteps;

    @Data
    public static class TraceStep {
        private String stepName;     // INBOUND / TRANSFER / OUTBOUND
            private String operator;
        private String operatedAt;
        private String location;
        private Integer qty;
    }

    public static BatchTraceResponse from(CrmBatch b) {
        BatchTraceResponse r = new BatchTraceResponse();
        r.setBatchNo(b.getBatchNo());
        r.setMaterialCode(b.getMaterialCode());
        r.setSupplierId(b.getSupplierId());
        r.setSupplierName(b.getSupplierName());
        r.setQty(b.getQty());
        r.setQualityStatus(b.getQualityStatus());
        r.setLocationCode(b.getLocationCode());
        r.setReceivedAt(b.getArrivedAt() == null ? null : b.getArrivedAt().toString());
        r.setExpiredAt(null);
        return r;
    }
}
