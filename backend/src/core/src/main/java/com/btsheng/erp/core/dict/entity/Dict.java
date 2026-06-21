package com.btsheng.erp.core.dict.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 字典项（V1.3.7 · Story 1.3 · AC-1.3.1 · FR-1-3-1）
 */
@Data
@Schema(description = "字典项（sys_dict）")
@TableName("sys_dict")
public class Dict implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键 ID", example = "1")
    private Long id;

    @TableField("dict_type")
    @Schema(description = "字典类型编码", example = "MATERIAL_CATEGORY")
    private String dictType;

    @TableField("dict_code")
    @Schema(description = "字典编码", example = "STEEL")
    private String dictCode;

    @TableField("dict_label")
    @Schema(description = "字典标签（显示名）", example = "钢材")
    private String dictLabel;

    @TableField("sort")
    @Schema(description = "排序", example = "1")
    private Integer sort;

    @TableField("status")
    @Schema(description = "状态：ACTIVE/INACTIVE/DELETED", example = "ACTIVE")
    private String status;
}
