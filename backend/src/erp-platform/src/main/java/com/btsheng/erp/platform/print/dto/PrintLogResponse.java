package com.btsheng.erp.platform.print.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印日志响应 DTO（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.3/12.4.4）
 *
 * <p>对应 sys_print_log 完整字段（含冗余快照）
 * <p>三仓统一 web-impl / android-impl / API
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "打印日志响应")
public class PrintLogResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID", example = "1")
    private Long id;

    @Schema(description = "业务编号", example = "PR-20260614-001")
    private String logNo;

    @Schema(description = "操作人 user_id", example = "1001")
    private Long operatorUserId;

    @Schema(description = "操作人姓名", example = "张师傅")
    private String operatorName;

    @Schema(description = "打印时间", example = "2026-06-14T10:30:00")
    private LocalDateTime printedAt;

    @Schema(description = "GD / LZ / SB / WW / WL / DRAWING", example = "GD")
    private String codeType;

    @Schema(description = "业务编码", example = "GD-260614-001")
    private String codeValue;

    @Schema(description = "份数", example = "1")
    private Integer copies;

    @Schema(description = "sys_printer.id · 模式二为 NULL", example = "5")
    private Long printerId;

    @Schema(description = "模式一 sys_printer.name · 模式二'普通浏览器'", example = "Zebra-1")
    private String printerNameSnapshot;

    @Schema(description = "模式一 IP · 模式二 NULL", example = "192.168.1.100")
    private String printerIpSnapshot;

    @Schema(description = "ZPL_DIRECT / PDF_BROWSER", example = "ZPL_DIRECT")
    private String printMode;

    @Schema(description = "SUCCESS / FAILED / PENDING", example = "SUCCESS")
    private String status;

    @Schema(description = "FAILED 时记录", example = "SocketTimeoutException: connect timed out")
    private String errorMsg;

    @Schema(description = "补打时指向原始 sys_print_log.id", example = "99")
    private Long referenceLogId;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "租户 ID", example = "1")
    private Long tenantId;
}
