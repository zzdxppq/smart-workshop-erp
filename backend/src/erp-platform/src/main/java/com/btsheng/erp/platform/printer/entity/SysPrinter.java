package com.btsheng.erp.platform.printer.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 打印机配置（V1.3.9 Sprint 12 · Story 12.2 · AC-12.2.1）
 *
 * <p>sys_printer 表 16 字段：PK + 业务 11 + 审计 2 + tenant 1
 * <p>心跳 fail_count ≥ 2 才标 OFFLINE（防瞬断误标）
 * <p>NORMAL 类型保持 status=UNKNOWN（OS 打印队列无 IP · 不探活）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "打印机配置（sys_printer）")
@TableName("sys_printer")
public class SysPrinter implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID", example = "1")
    private Long id;

    @TableField("name")
    @Schema(description = "打印机名称 · UNIQUE", example = "Zebra-1")
    private String name;

    @TableField("type")
    @Schema(description = "类型：NORMAL / LABEL", example = "LABEL")
    private String type;

    @TableField("ip")
    @Schema(description = "IPv4/IPv6 · LABEL 必填 · NORMAL 可空", example = "192.168.1.100")
    private String ip;

    @TableField("port")
    @Schema(description = "端口 · 1-65535 · 默认 9100", example = "9100")
    private Integer port;

    @TableField("protocol")
    @Schema(description = "协议：ZPL / TSPL / PDF_BROWSER", example = "ZPL")
    private String protocol;

    @TableField("model_suggestion")
    @Schema(description = "型号建议：DELI_DL888B / ZEBRA_ZD420 / TSC_TTP244PRO / OTHER", example = "ZEBRA_ZD420")
    private String modelSuggestion;

    @TableField("enabled")
    @Schema(description = "启停：1=启用 / 0=停用", example = "1")
    private Integer enabled;

    @TableField("status")
    @Schema(description = "心跳状态：ONLINE / OFFLINE / UNKNOWN", example = "UNKNOWN")
    private String status;

    @TableField("fail_count")
    @Schema(description = "心跳连续失败计数 · 达 2 标 OFFLINE", example = "0")
    private Integer failCount;

    @TableField("last_heartbeat_at")
    @Schema(description = "最后心跳成功时间")
    private LocalDateTime lastHeartbeatAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("created_by")
    private Long createdBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("tenant_id")
    @Schema(description = "租户 ID", example = "1")
    private Long tenantId;

    public static final String TYPE_NORMAL = "NORMAL";
    public static final String TYPE_LABEL = "LABEL";

    public static final String STATUS_ONLINE = "ONLINE";
    public static final String STATUS_OFFLINE = "OFFLINE";
    public static final String STATUS_UNKNOWN = "UNKNOWN";

    public static final String PROTOCOL_ZPL = "ZPL";
    public static final String PROTOCOL_TSPL = "TSPL";
    public static final String PROTOCOL_PDF_BROWSER = "PDF_BROWSER";

    public static final String MODEL_DELI = "DELI_DL888B";
    public static final String MODEL_ZEBRA = "ZEBRA_ZD420";
    public static final String MODEL_TSC = "TSC_TTP244PRO";
    public static final String MODEL_OTHER = "OTHER";
}
