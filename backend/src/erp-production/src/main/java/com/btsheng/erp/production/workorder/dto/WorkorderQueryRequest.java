package com.btsheng.erp.production.workorder.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "工单查询")
public class WorkorderQueryRequest {
    @Schema(description = "关键字")
    private String keyword;
    @Schema(description = "状态 DRAFT/SCHEDULED/IN_PROGRESS/COMPLETED/CANCELLED")
    private String status;
    @Schema(description = "物料编码")
    private String materialCode;
    @Schema(description = "页码（0 起）")
    private Integer page = 0;
    @Schema(description = "页大小")
    private Integer size = 20;

    /** OpenAPI / Web 前端使用 pageNum（1 起） */
    public void setPageNum(Integer pageNum) {
        if (pageNum != null && pageNum > 0) {
            this.page = pageNum - 1;
        }
    }

    /** OpenAPI / Web 前端使用 pageSize */
    public void setPageSize(Integer pageSize) {
        if (pageSize != null && pageSize > 0) {
            this.size = pageSize;
        }
    }
}
