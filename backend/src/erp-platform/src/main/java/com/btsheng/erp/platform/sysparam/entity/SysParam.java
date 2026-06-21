package com.btsheng.erp.platform.sysparam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** V1.3.7 Story 1.3 · AC-1.3.2 · sys_param */
@Data
@Schema(description = "系统参数（sys_param）")
@TableName("sys_param")
public class SysParam implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("param_key")
    @Schema(description = "参数键", example = "app.cache-ttl")
    private String paramKey;

    @TableField("param_value")
    @Schema(description = "参数值", example = "PT24H")
    private String paramValue;

    @TableField("param_group")
    @Schema(description = "参数组", example = "APP_CACHE_TTL")
    private String paramGroup;

    @TableField("description")
    private String description;

    @TableField("updated_by")
    private Long updatedBy;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
