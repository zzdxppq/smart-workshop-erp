package com.btsheng.erp.platform.print.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 模式一 ZPL 打印成功返回（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "ZPL 发送成功响应")
public class ZplPrintResult implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "sys_print_log.id", example = "1")
    private Long printLogId;

    @Schema(description = "logNo · PR-{yyyyMMdd}-{seq:4}", example = "PR-20260614-001")
    private String logNo;

    @Schema(description = "发送字节数", example = "256")
    private Integer bytesSent;

    @Schema(description = "发送延迟 ms（含 200ms 缓冲）", example = "350")
    private Integer latencyMs;

    @Schema(description = "协议", example = "ZPL")
    private String protocol;
}
