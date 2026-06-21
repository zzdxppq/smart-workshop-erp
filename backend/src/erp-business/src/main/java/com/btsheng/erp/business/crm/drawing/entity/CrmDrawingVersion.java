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
 * V1.3.7 · Story 1.7 · AC-3.1.2 · 图纸版本历史
 */
@Data
@Schema(description = "图纸版本历史（crm_drawing_version）")
@TableName("crm_drawing_version")
public class CrmDrawingVersion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("version") private String version;                  // v1 < v2 < v3
            @TableField("pdf_path") private String pdfPath;
    @TableField("signature_scan_path") private String signatureScanPath;
    @TableField("is_encrypted") private Integer isEncrypted = 0;
    @TableField("change_reason") private String changeReason;
    @TableField("changed_by") private Long changedBy;
    @TableField("changed_at") private LocalDateTime changedAt;
}
