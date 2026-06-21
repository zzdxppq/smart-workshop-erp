package com.btsheng.erp.business.crm.warehousescan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "离线扫码批量同步请求（AC-4.2.3）")
public class ScanOfflineSyncRequest {
    @Schema(description = "客户端 ID", required = true)
    private String clientId;
    @Schema(description = "扫码记录列表（离线缓存）", required = true)
    private List<ScanOfflineItem> items;

    @Data
    public static class ScanOfflineItem {
        private String clientScanId;     // 客户端本地 ID
            private String scanType;
        private String barcodeNo;
        private String materialCode;
        private String locationCode;
        private Integer qty;
        private String workorderNo;
        private String batchNo;
        private Long clientScannedAt;
    }
}
