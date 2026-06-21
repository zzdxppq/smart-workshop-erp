package com.btsheng.erp.core.integration.dto;

import lombok.Data;

/** 委外单摘要（erp-production → erp-business Feign DTO） */
@Data
public class OutsourceOrderRef {
    private Long id;
    private String outsourceNo;
    private String status;
}
