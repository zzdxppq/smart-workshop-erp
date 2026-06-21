package com.btsheng.erp.business.crm.qualityinspection.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class InspectionTemplateResponse {
    private Long id;
    private String templateNo;
    private String templateName;
    private String drawingNoPattern;
    private String materialCode;
    private String inspectionType;
    private BigDecimal sampleRatio;
    private String status;
    private Integer version;
    private String remark;
    private Long publishedBy;
    private LocalDateTime publishedAt;
    private Long archivedBy;
    private LocalDateTime archivedAt;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer itemCount;
    private List<InspectionTemplateItemDto> items;
}
