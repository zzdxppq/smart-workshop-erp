package com.btsheng.erp.business.crm.quote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/** V1.3.7 · Story 1.5 · AC-2.2.1 · 报价变更历史 (V1.3.7 红线 5) */
@Data
@Schema(description = "报价变更历史（crm_quote_history）")
@TableName("crm_quote_history")
public class CrmQuoteHistory implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("quote_id") private Long quoteId;
    @TableField("operation") private String operation;       // CREATE/UPDATE/SUBMIT/APPROVE/REJECT/CONVERT/PDF_DOWNLOAD
            @TableField("before_json") private String beforeJson;
    @TableField("after_json") private String afterJson;
    @TableField("changed_by") private Long changedBy;
    @TableField("changed_at") private LocalDateTime changedAt;
}
