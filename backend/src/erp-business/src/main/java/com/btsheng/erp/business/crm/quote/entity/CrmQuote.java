package com.btsheng.erp.business.crm.quote.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** V1.3.7 · Story 1.5 · AC-2.2.1 · 报价单 (sys_dict?type=CUSTOMER_STATUS 查黑名单) */
@Data
@Schema(description = "报价单（crm_quote）")
@TableName("crm_quote")
public class CrmQuote implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("quote_no") private String quoteNo;          // BJ+YYYYMMDD+NNNN
            @TableField("customer_id") private Long customerId;
    @TableField("customer_name") private String customerName;
    @TableField("owner_user_id") private Long ownerUserId;
    @TableField("dept_id") private Long deptId;
    @TableField("currency") private String currency = "CNY";
    @TableField("total_amount")  // 写操作由 Service 计算后 set totalAmount,字段本身可写
            private BigDecimal totalAmount;
    @TableField("delivery_date") private LocalDate deliveryDate;
    @TableField("is_fa") private Integer isFa = 0;
    @TableField("is_new") private Integer isNew = 0;
    @TableField("status") private String status = "DRAFT";
    @TableField("current_node") private Integer currentNode = 1;
    @TableField("engineer_completed") private Integer engineerCompleted = 0;
    @TableField("comment") private String comment;
    @TableField("is_deleted") private Integer isDeleted = 0;
    @TableField("created_at") private LocalDateTime createdAt;
    @TableField("updated_at") private LocalDateTime updatedAt;
}
