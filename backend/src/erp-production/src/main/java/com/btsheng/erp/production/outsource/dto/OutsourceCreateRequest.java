package com.btsheng.erp.production.outsource.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "委外单创建请求（AC-5.4.2 · WW{yyyyMMdd}{seq:4}）")
public class OutsourceCreateRequest {
    @Schema(description = "工单号", required = true)
    private String workorderNo;
    @Schema(description = "工序号", required = true)
    private Integer stepNo;
    @Schema(description = "供应商 ID", required = true)
    private Long supplierId;
    @Schema(description = "供应商名称", required = true)
    private String supplierName;
    @Schema(description = "工序名称")
    private String processName;
    @Schema(description = "委外物料编码", required = true)
    private String materialCode;
    @Schema(description = "数量", required = true)
    private Integer qty;
    @Schema(description = "单价", required = true)
    private BigDecimal unitPrice;
    @Schema(description = "交期", required = true)
    private LocalDate deliveryDate;
    @Schema(description = "加急")
    private Integer isUrgent = 0;
    @Schema(description = "委外明细列表")
    private List<OutsourceItemRequest> items;
    @Schema(description = "备注")
    private String remark;

    @Data
    public static class OutsourceItemRequest {
        private String materialCode;
        private String materialName;
        private String spec;
        private Integer qty;
        private String unit;
        private BigDecimal unitPrice;
    }
}
