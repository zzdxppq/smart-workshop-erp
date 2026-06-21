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
 * V1.3.7 · Story 1.8 · AC-3.2.1 图纸标注
 *
 * 4 类型：DIMENSION(尺寸) / TOLERANCE(公差) / PROCESS_REQ(工艺要求) / TECH_NOTE(技术注释)
 * 4 颜色：RED / YELLOW / BLUE / GREEN
 * P1 修补：标注必须挂载版本（防 v1→v2 标注丢失） + 不可修改只追加
 * P2 修补：SVG 嵌入（部署阶段）
 */
@Data
@Schema(description = "图纸标注（crm_drawing_annotation）")
@TableName("crm_drawing_annotation")
public class CrmDrawingAnnotation implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("drawing_id") private Long drawingId;
    @TableField("drawing_no") private String drawingNo;
    @TableField("version") private String version;                    // P1 修补：挂载版本
            @TableField("type") private String type;                          // DIMENSION/TOLERANCE/PROCESS_REQ/TECH_NOTE
    @TableField("content") private String content;                    // 至少 1 字符
            @TableField("color") private String color = "RED";               // RED/YELLOW/BLUE/GREEN
    @TableField("x") private BigDecimal x = BigDecimal.ZERO;
    @TableField("y") private BigDecimal y = BigDecimal.ZERO;
    @TableField("width") private BigDecimal width = BigDecimal.ZERO;
    @TableField("height") private BigDecimal height = BigDecimal.ZERO;
    @TableField("priority") private Integer priority = 5;            // 1-10
            @TableField("is_archived") private Integer isArchived = 0;        // 新增 v2 时 v1 自动归档
    @TableField("svg_data") private String svgData;                  // P2 修补：SVG 嵌入
            @TableField("created_by") private Long createdBy;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
