package com.btsheng.erp.production.process.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工艺更新请求（PUT /processes/{id}）")
public class ProcessUpdateRequest {
    private String processName;
    private String processType;
    private String description;
    private String comment;
    private Integer isActive;
    private Boolean isReusable;
}
