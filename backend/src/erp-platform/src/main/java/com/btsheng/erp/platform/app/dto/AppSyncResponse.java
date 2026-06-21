package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "APP 增量同步响应")
public class AppSyncResponse {
    @Schema(description = "本次同步时间（下次同步用 lastSyncTime）")
    private LocalDateTime syncTime;
    @Schema(description = "服务端待同步消息列表（增量）")
    private List<AppMessage> messages;
    @Schema(description = "pending_scan 上传结果")
    private List<PendingScanResult> pendingScanResults;

    @Data
    public static class AppMessage {
        @Schema(description = "消息 ID")
        private Long id;
        @Schema(description = "消息类型：APPROVAL_NOTIFY/OVERDUE_NOTIFY/EXCEPTION_REPORT/SCAN_RECEIPT")
        private String type;
        @Schema(description = "标题")
        private String title;
        @Schema(description = "内容")
        private String content;
        @Schema(description = "业务 ID")
        private Long bizId;
        @Schema(description = "业务 URL")
        private String routeUrl;
        @Schema(description = "是否已读")
        private Boolean read;
        @Schema(description = "创建时间")
        private LocalDateTime createdAt;
    }

    @Data
    public static class PendingScanResult {
        @Schema(description = "扫码内容")
        private String code;
        @Schema(description = "冲突类型：NONE/OVERWRITE/MERGE")
        private String conflictType;
        @Schema(description = "解决方式：AUTO_OVERWRITE/USER_OVERWRITE/USER_MERGE/USER_CANCEL")
        private String resolution;
    }
}
