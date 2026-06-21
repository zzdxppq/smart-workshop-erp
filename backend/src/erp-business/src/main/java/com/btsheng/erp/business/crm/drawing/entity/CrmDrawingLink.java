package com.btsheng.erp.business.crm.drawing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 图纸-业务单据关联表
 *
 * <p>biz_type 枚举（评审建议）：ORDER / PO / INCOMING / INSPECTION / WORKORDER_PROCESS
 * <p>索引：uk_biz_ref (biz_type, biz_id, drawing_id) UNIQUE · idx_drawing_link · idx_biz_lookup
 * <p>外键：fk_draw_link_drawing ON DELETE RESTRICT（评审建议）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
@Data
@Schema(description = "图纸-业务单据关联表（crm_drawing_link）")
@TableName("crm_drawing_link")
public class CrmDrawingLink implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("biz_type")   private String bizType;     // ORDER/PO/INCOMING/INSPECTION/WORKORDER_PROCESS
            @TableField("biz_id")     private Long bizId;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("created_by") private Long createdBy;

    /** 5 类 biz_type 枚举（评审建议） */
    public static final String BIZ_TYPE_ORDER = "ORDER";
    public static final String BIZ_TYPE_PO = "PO";
    public static final String BIZ_TYPE_INCOMING = "INCOMING";
    public static final String BIZ_TYPE_INSPECTION = "INSPECTION";
    public static final String BIZ_TYPE_WORKORDER_PROCESS = "WORKORDER_PROCESS";
    public static final String BIZ_TYPE_OUTSOURCE = "OUTSOURCE";

    /** 7 角色 scope 枚举 */
    public static final String SCOPE_ALL = "ALL";
    public static final String SCOPE_ORDER = "ORDER";
    public static final String SCOPE_PO = "PO";
    public static final String SCOPE_INCOMING = "INCOMING";
    public static final String SCOPE_INSPECTION = "INSPECTION";
    public static final String SCOPE_PROCESS = "WORKORDER_PROCESS";
    public static final String SCOPE_NONE = "NONE";
}