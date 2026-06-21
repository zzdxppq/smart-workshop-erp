package com.btsheng.erp.business.crm.purchaserequest.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ConvertPrToPoRequest {
    private String vendorName;
    private BigDecimal unitPrice;
    private Integer qty;
    private String deliveryDate;
    private String note;
}
