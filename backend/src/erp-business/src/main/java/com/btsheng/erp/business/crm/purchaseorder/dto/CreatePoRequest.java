package com.btsheng.erp.business.crm.purchaseorder.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreatePoRequest {
    private String vendorName;
    private String deliveryDate;
    private String note;
    private List<PoItemLine> items;
    private Long prId;
    private String prNo;
    private String workorderNo;
    private Long mrpRunId;
    private Long rfqId;
    private String sourceType;
    /** 仅内部/无订单采购服务使用 */
    private Boolean allowDirectCreate;

    @Data
    public static class PoItemLine {
        private Long materialId;
        private String materialCode;
        private String materialName;
        private Integer quantity;
        private BigDecimal unitPrice;
    }
}
