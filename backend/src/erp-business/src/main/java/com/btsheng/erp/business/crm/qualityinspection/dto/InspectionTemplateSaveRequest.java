package com.btsheng.erp.business.crm.qualityinspection.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InspectionTemplateSaveRequest {
    private String templateName;
    private String drawingNoPattern;
    private String materialCode;
    private String inspectionType;
    private BigDecimal sampleRatio;
    private String remark;
    private List<InspectionTemplateItemDto> items;
}
