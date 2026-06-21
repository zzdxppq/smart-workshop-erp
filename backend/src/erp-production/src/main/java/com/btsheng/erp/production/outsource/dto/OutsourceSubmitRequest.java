package com.btsheng.erp.production.outsource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "委外提交请求（AC-5.4.3 · 触发 1.2 审批流）")
public class OutsourceSubmitRequest {
    @Schema(description = "提交备注")
    private String note;
}
