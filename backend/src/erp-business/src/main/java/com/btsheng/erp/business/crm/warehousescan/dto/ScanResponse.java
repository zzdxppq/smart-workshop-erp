package com.btsheng.erp.business.crm.warehousescan.dto;

import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseScan;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "扫码响应")
public class ScanResponse {
    private String scanNo;
    private String scanType;
    private String barcodeNo;
    private String materialCode;
    private String locationCode;
    private Integer qty;
    private String workorderNo;
    private String syncStatus;
    private Long scannedBy;
    private LocalDateTime scannedAt;
    private String conflictType;
    private String conflictResolution;
    private String remark;

    public static ScanResponse from(CrmWarehouseScan e) {
        ScanResponse r = new ScanResponse();
        r.setScanNo(e.getScanNo());
        r.setScanType(e.getScanType());
        r.setBarcodeNo(e.getBarcodeNo());
        r.setMaterialCode(e.getMaterialCode());
        r.setLocationCode(e.getLocationCode());
        r.setQty(e.getQty());
        r.setWorkorderNo(e.getWorkorderNo());
        r.setSyncStatus(e.getSyncStatus());
        r.setScannedBy(e.getScannedBy());
        r.setScannedAt(e.getScannedAt());
        r.setConflictType(e.getConflictType());
        r.setConflictResolution(e.getConflictResolution());
        r.setRemark(e.getRemark());
        return r;
    }
}
