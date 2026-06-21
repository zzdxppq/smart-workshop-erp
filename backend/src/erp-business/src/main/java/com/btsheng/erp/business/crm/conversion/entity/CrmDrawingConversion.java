package com.btsheng.erp.business.crm.conversion.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.8 · AC-3.2.2 工程转化主表
 *
 * 3 P1 修补：转化时锁定原版本 / 标注不可修改（只追加）/ 转化结果 PDF 签字扫描件复用 1.7
 * 状态机扩展：CONVERTED（V1.3.7 第 5 状态）
 * 5 段成本聚合：原材料 / 粗加工 / 精加工 / 表面处理 / 检验
 */
@Data
@Schema(description = "工程转化记录（crm_drawing_conversion）")
@TableName("crm_drawing_conversion")
public class CrmDrawingConversion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("drawing_no") private String drawingNo;
    @TableField("locked_version") private String lockedVersion;       // P1 修补：锁定原版本
            @TableField("bom_no") private String bomNo;                       // BOM{yyyyMMdd}{seq:4}
    @TableField("bom_type") private String bomType = "STANDARD";      // STANDARD/FA/PROTOTYPE
            @TableField("target_qty") private Integer targetQty = 1;          // P1 修补：正整数
    @TableField("total_cost") private BigDecimal totalCost = BigDecimal.ZERO;
    @TableField("engineer_user_id") private Long engineerUserId;
    @TableField("engineer_name") private String engineerName;         // PDF 水印用
            @TableField("status") private String status = "CONVERTED";        // CONVERTED/FAILED
    @TableField("error_message") private String errorMessage;
    @TableField("process_route_snapshot") private String processRouteSnapshot;
    @TableField("cost_breakdown") private String costBreakdown;       // 5 段成本明细 JSON
            @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;

    /** 转化后物料编码（非持久化 · 供前端跳转 BOM 维护） */
    @TableField(exist = false) private String materialCode;
    /** 转化后 BOM 主键（非持久化） */
    @TableField(exist = false) private Long bomId;
    /** 厂内图号（非持久化 · 如 WL-1001-V1.0） */
    @TableField(exist = false) private String factoryDrawingNo;
}
