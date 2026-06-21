package com.btsheng.erp.platform.admin.dto;

import lombok.Data;

@Data
public class EmailTemplateDto {
    private String key;
    private String name;
    private String subject;
    private String body;
    private String description;
}
