package com.btsheng.erp.platform.email.dto;

import lombok.Data;

@Data
public class EmailTestRequest {
    private String toAddress;
    private String subject;
    private String body;
    /** 测试时可临时覆盖 SMTP 授权码（不落库） */
    private String authCode;
}
