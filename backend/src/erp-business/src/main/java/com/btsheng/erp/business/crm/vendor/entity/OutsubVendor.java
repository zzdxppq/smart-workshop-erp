package com.btsheng.erp.business.crm.vendor.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@TableName("outsub_vendor")
public class OutsubVendor implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("vendor_code")
    private String vendorCode;

    @TableField("vendor_name")
    private String vendorName;

    @TableField("capabilities_json")
    private String capabilitiesJson;

    @TableField("credit_level")
    private String creditLevel;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_name")
    private String contactName;

    @TableField("default_recon_email")
    private String defaultReconEmail;

    @TableField("business_license_url")
    private String businessLicenseUrl;

    @TableField("business_license_expire_date")
    private java.time.LocalDate businessLicenseExpireDate;

    @TableField("notify_channel")
    private String notifyChannel;

    @TableField("avg_delivery_days")
    private Integer avgDeliveryDays;

    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
