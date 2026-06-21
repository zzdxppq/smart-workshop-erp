package com.btsheng.erp.business.internal.productroute.dto;

import lombok.Data;

import java.math.BigDecimal;

/** erp-production Feign 读取 cnc_business 物料摘要 */
@Data
public class ProductRouteMaterialDto {
    private Long id;
    private String materialCode;
    private String materialName;
    private Long processId;
    private Integer isActive;
    private BigDecimal costTotal;
}
