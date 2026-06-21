package com.btsheng.erp.platform.label.dto;

import lombok.Data;

@Data
public class UpdateLabelTemplateRequest {
    private String colorStrip;
    private String factoryName;
    private Integer dpi;
    private Integer enabled;
}
