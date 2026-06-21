package com.btsheng.erp.platform.label.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 标签模板元数据（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.1）
 *
 * <p>label_template 表 10 字段：PK + 业务 7 + tenant 1 + 审计 2
 * <p>SB- 不入库 · 由 {@code LabelTemplateService.getTemplate("SB")} 取 GD 模板 + 覆盖 color_strip=#6B7280
 * <p>4 行 seed（GD/LZ/WW/WL）+ 3 索引（PRIMARY + uk_label_type + idx_tenant）+ 2 CHECK 约束
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Data
@Schema(description = "标签模板元数据（label_template）")
@TableName("label_template")
public class LabelTemplate implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID", example = "1")
    private Long id;

    /**
     * 模板类型。注意：MySQL 保留字，使用反引号映射。
     */
    @TableField("`type`")
    @Schema(description = "模板类型：GD / LZ / WW / WL · SB 由代码层 fallback 到 GD", example = "GD")
    private String type;

    @TableField("color_strip")
    @Schema(description = "色条 HEX · #1E40AF / #16A34A / #EA580C / #000000", example = "#1E40AF")
    private String colorStrip;

    @TableField("factory_name")
    @Schema(description = "厂名 · 默认昆山佰泰胜精密加工 · 可由 sys_dict dict_type=COMPANY_NAME 覆盖",
            example = "昆山佰泰胜精密加工")
    private String factoryName;

    @TableField("layout_json")
    @Schema(description = "三区坐标 + 字体 + DPI · JSON 字符串",
            example = "{\"topBarH\":5,\"qrAreaH\":18,\"textAreaH\":7,\"fontSize\":8,\"qrSizePx\":300,\"qrSizeMm\":12}")
    private String layoutJson;

    @TableField("dpi")
    @Schema(description = "DPI · 203 / 300", example = "300")
    private Integer dpi;

    @TableField("enabled")
    @Schema(description = "启停：1=启用 / 0=停用", example = "1")
    private Integer enabled;

    @TableField("tenant_id")
    @Schema(description = "租户 ID", example = "1")
    private Long tenantId;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public static final String TYPE_GD = "GD";
    public static final String TYPE_LZ = "LZ";
    public static final String TYPE_SB = "SB";
    public static final String TYPE_WW = "WW";
    public static final String TYPE_WL = "WL";

    /** SB- fallback 灰条色（architect §2.2） */
    public static final String SB_FALLBACK_COLOR = "#6B7280";

    /** 默认厂名 */
    public static final String DEFAULT_FACTORY_NAME = "昆山佰泰胜精密加工";
}