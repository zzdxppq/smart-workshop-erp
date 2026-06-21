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
 * V1.3.7 · Story 1.7 · AC-3.1.1 · 图纸主表
 *
 * 4 状态机：DRAFT → RELEASED → ARCHIVED + OBSOLETE（被新版本替代）
 * 唯一索引：(drawing_no, version) + material_code 唯一
 */
@Data
@Schema(description = "图纸主表（crm_drawing）")
@TableName("crm_drawing")
public class CrmDrawing implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_no") private String drawingNo;             // DWG-YYYYMMDD-NNNN
    @TableField("customer_drawing_no") private String customerDrawingNo;  // V2.1 客户图号
            @TableField("version") private String version = "v1";           // v1 < v2 < v3 严格递增
    @TableField("title") private String title;
    @TableField("material_grade") private String materialGrade;
    @TableField("spec_size") private String specSize;
    @TableField("unit_weight") private java.math.BigDecimal unitWeight;   // V2.1 单件重量 kg
    @TableField("material_code") private String materialCode;       // 料号，转化前可空
            @TableField("process_route") private String processRoute;       // JSON 工艺路线（5 段）
    @TableField("status") private String status = "DRAFT";
    @TableField("pdf_path") private String pdfPath;
    @TableField("signature_scan_path") private String signatureScanPath;
    @TableField("is_encrypted") private Integer isEncrypted = 0;     // AES-256-GCM 加密标记
            @TableField("owner_user_id") private Long ownerUserId;
    @TableField("dept_id") private Long deptId;
    @TableField("is_fa") private Integer isFa = 0;                  // FA 件 → > 20万 二次密码
            @TableField("is_new") private Integer isNew = 0;
    @TableField("quote_approval_status") private String quoteApprovalStatus; // V1.3.9：PENDING（待审批）/ APPROVED（已审批，可建单）
    @TableField("comment") private String comment;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
