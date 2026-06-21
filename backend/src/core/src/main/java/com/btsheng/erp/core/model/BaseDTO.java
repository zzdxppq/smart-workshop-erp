package com.btsheng.erp.core.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 基础 DTO（V1.3.7）
 *
 * <p>提供 createTime / updateTime 序列化格式，链路追踪字段。所有 API DTO 继承此基类。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "基础 DTO")
public abstract class BaseDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "创建时间", example = "2026-06-10 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    protected LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-06-10 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Shanghai")
    protected LocalDateTime updateTime;

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
