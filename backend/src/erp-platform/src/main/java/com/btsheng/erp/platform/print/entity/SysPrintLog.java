package com.btsheng.erp.platform.print.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印留痕（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.3）
 *
 * <p>sys_print_log 表 17 字段：PK + 业务 13 + 审计 1 + tenant 1 + reference 1
 * <p>与 8.3 sys_workflow_event 同 sys_ 命名范式
 * <p>12.1 图纸打印（code_type=DRAWING）与 12.4 标签打印共表
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "打印留痕（sys_print_log）")
@TableName("sys_print_log")
public class SysPrintLog implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID", example = "1")
    private Long id;

    @TableField("log_no")
    @Schema(description = "业务编号 · PR-{yyyyMMdd}-{seq:4}", example = "PR-20260614-001")
    private String logNo;

    @TableField("operator_user_id")
    @Schema(description = "操作人 sys_user.id", example = "1001")
    private Long operatorUserId;

    @TableField("operator_name")
    @Schema(description = "操作人姓名（冗余 · 防改名）", example = "张师傅")
    private String operatorName;

    @TableField("printed_at")
    @Schema(description = "打印时间", example = "2026-06-14T10:30:00")
    private LocalDateTime printedAt;

    @TableField("code_type")
    @Schema(description = "GD / LZ / SB / WW / WL / DRAWING", example = "GD")
    private String codeType;

    @TableField("code_value")
    @Schema(description = "业务编码", example = "GD-260614-001")
    private String codeValue;

    @TableField("copies")
    @Schema(description = "份数 · 1-100", example = "1")
    private Integer copies;

    @TableField("printer_id")
    @Schema(description = "sys_printer.id · 模式二为 NULL", example = "5")
    private Long printerId;

    @TableField("printer_name_snapshot")
    @Schema(description = "模式一 sys_printer.name · 模式二'普通浏览器'", example = "Zebra-1")
    private String printerNameSnapshot;

    @TableField("printer_ip_snapshot")
    @Schema(description = "模式一 IP · 模式二 NULL", example = "192.168.1.100")
    private String printerIpSnapshot;

    @TableField("print_mode")
    @Schema(description = "ZPL_DIRECT / PDF_BROWSER", example = "ZPL_DIRECT")
    private String printMode;

    @TableField("status")
    @Schema(description = "SUCCESS / FAILED / PENDING", example = "SUCCESS")
    private String status;

    @TableField("error_msg")
    @Schema(description = "FAILED 时记录", example = "SocketTimeoutException: connect timed out")
    private String errorMsg;

    @TableField("reference_log_id")
    @Schema(description = "补打时指向原始 sys_print_log.id", example = "99")
    private Long referenceLogId;

    @TableField("remark")
    @Schema(description = "备注", example = "工序首件打印")
    private String remark;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("tenant_id")
    @Schema(description = "租户 ID", example = "1")
    private Long tenantId;

    // ===== 静态常量（与 sys_dict PRINTER_PROTOCOL 对齐） =====
            public static final String MODE_ZPL_DIRECT = "ZPL_DIRECT";
    public static final String MODE_PDF_BROWSER = "PDF_BROWSER";

    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_PENDING = "PENDING";

    public static final String CODE_TYPE_GD = "GD";
    public static final String CODE_TYPE_LZ = "LZ";
    public static final String CODE_TYPE_SB = "SB";
    public static final String CODE_TYPE_WW = "WW";
    public static final String CODE_TYPE_WL = "WL";
    public static final String CODE_TYPE_DRAWING = "DRAWING";

    public static final String BROWSER_PRINTER_NAME = "普通浏览器";

    // ===== 错误码常量（PM prompt 体系优先 · 与 architect 决策一致） =====
            public static final int ERR_PRINTER_OFFLINE = 50201;
    public static final int ERR_PROTOCOL_UNSUPPORTED = 50202;
    public static final int ERR_ZPL_SEND_FAILED = 50203;
    public static final int ERR_REPLAY_FORBIDDEN = 40954;
}
