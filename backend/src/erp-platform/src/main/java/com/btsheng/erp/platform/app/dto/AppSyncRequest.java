package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APP 增量同步请求")
public class AppSyncRequest {
    @Schema(description = "用户 ID", example = "10086")
    private Long userId;
    @Schema(description = "设备 ID", example = "DEV-12345")
    private String deviceId;
    @Schema(description = "上次同步时间（增量边界）")
    private LocalDateTime lastSyncTime;
    @Schema(description = "待上传的 pending_scan 列表")
    private List<PendingScan> pendingScans;

    @Data
    public static class PendingScan {
        @Schema(description = "扫码内容", example = "GD-20260610-0001")
        private String code;
        @Schema(description = "本地时间戳")
        private LocalDateTime localTs;
        @Schema(description = "设备 ID")
        private String deviceId;
    }
}
