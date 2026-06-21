package com.btsheng.erp.platform.app.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "APP 消息标记已读请求")
public class AppMessageReadRequest {
    @Schema(description = "用户 ID", example = "10086")
    private Long userId;
}
