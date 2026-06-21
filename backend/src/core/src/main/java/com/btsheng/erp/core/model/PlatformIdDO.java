package com.btsheng.erp.core.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * Platform 库仅主键基类（V1.3.7）
 *
 * <p>对应 init.sql 中无审计字段的表（sys_dept、sys_position 等）。
 */
@Schema(description = "Platform 主键实体")
public abstract class PlatformIdDO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID", example = "10086")
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
}
