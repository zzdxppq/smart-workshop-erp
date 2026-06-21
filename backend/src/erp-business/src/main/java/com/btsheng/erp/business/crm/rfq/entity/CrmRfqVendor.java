package com.btsheng.erp.business.crm.rfq.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.7 · Story 1.32 · 询价-厂商关联（crm_rfq_vendor · FR-8-1）
 */
@Data
@Schema(description = "询价-厂商关联（Story 1.32 FR-8-1）")
@TableName("crm_rfq_vendor")
public class CrmRfqVendor implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;
    @TableField("rfq_id")         private Long rfqId;
    @TableField("vendor_id")      private Long vendorId;
    @TableField("vendor_name")    private String vendorName;
    @TableField("vendor_code")    private String vendorCode;
    @TableField("contact_name")   private String contactName;
    @TableField("contact_phone")  private String contactPhone;
    @TableField("invited_at")     private LocalDateTime invitedAt;
    @TableField("quote_status")   private String quoteStatus = "PENDING";
    @TableField("created_at")     private LocalDateTime createdAt;
}
