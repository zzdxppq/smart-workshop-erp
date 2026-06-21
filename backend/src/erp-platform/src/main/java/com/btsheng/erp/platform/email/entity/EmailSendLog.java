package com.btsheng.erp.platform.email.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("email_send_log")
public class EmailSendLog implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("to_address") private String toAddress;
    @TableField("subject") private String subject;
    @TableField("attachment_hash") private String attachmentHash;
    @TableField("smtp_response") private String smtpResponse;
    @TableField("status") private String status;
    @TableField("retry_count") private Integer retryCount;
    @TableField("sent_at") private LocalDateTime sentAt;
    @TableField("created_at") private LocalDateTime createdAt;
}
