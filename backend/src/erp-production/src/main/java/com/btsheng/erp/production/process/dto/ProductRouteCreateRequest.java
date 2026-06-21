package com.btsheng.erp.production.process.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Schema(description = "产品工艺路线创建（POST /products/{id}/routes）")
public class ProductRouteCreateRequest {

    @Schema(description = "变更原因 / 备注")
    private String changeReason;

    @Schema(description = "关联图纸 ID（工程转化后绑定工艺路线）")
    private Long drawingId;

    @Schema(description = "关联图号（冗余）")
    private String drawingNo;

    @Schema(description = "工序列表")
    private List<RouteProcessInput> processes;

    @Data
    public static class RouteProcessInput {
        private Integer processSeq;
        private String processCode;
        private BigDecimal stdTimeMin;
        private Boolean isOutsource;
    }
}
