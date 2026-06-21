package com.btsheng.erp.platform.email.dto;

import lombok.Data;

@Data
public class EmailSendRequest {
    private String toAddress;
    private String subject;
    private String body;
    /** Base64 编码的附件内容（可选） */
    private String attachmentBase64;
    private String attachmentFilename;
    /** 测试/运维时可临时覆盖 SMTP 授权码（不落库） */
    private String authCode;
}
