package com.btsheng.erp.business.crm.drawing.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("crm_drawing_attachment")
public class CrmDrawingAttachment implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id")
    private Long drawingId;
    @TableField("file_name")
    private String fileName;
    @TableField("file_type")
    private String fileType;
    @TableField("file_path")
    private String filePath;
    @TableField("file_size")
    private Long fileSize;
    @TableField("uploaded_by")
    private Long uploadedBy;
    @TableField("created_at")
    private LocalDateTime createdAt;
}
