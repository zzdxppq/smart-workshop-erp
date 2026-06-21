package com.btsheng.erp.business.crm.qualityinspection.dto;

import lombok.Data;

@Data
public class InspectionTemplateItemDto {
    private Long id;
    private Integer sortOrder;
    private String itemName;
    private String standard;
    private String toleranceUpper;
    private String toleranceLower;
    private String severity;
}
