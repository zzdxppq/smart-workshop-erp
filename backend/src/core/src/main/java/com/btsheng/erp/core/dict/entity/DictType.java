package com.btsheng.erp.core.dict.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.btsheng.erp.core.model.PlatformAuditDO;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 字典类型（V1.3.7 · Story 1.3 · AC-1.3.1 · sys_dict_type）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "字典类型（sys_dict_type）")
@TableName("sys_dict_type")
public class DictType extends PlatformAuditDO {
    private static final long serialVersionUID = 1L;

    @TableField("type_code")
    @Schema(description = "字典类型编码", example = "MATERIAL_CATEGORY")
    private String typeCode;

    @TableField("type_name")
    @Schema(description = "字典类型名称", example = "物料分类")
    private String typeName;

    @TableField("description")
    private String description;

    @TableField("is_builtin")
    @Schema(description = "是否内置（1=不可删 0=可删）", example = "1")
    private Integer isBuiltin;

    @TableField("status")
    private String status;
}
