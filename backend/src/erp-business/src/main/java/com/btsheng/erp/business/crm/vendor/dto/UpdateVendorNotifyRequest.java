package com.btsheng.erp.business.crm.vendor.dto;

import lombok.Data;

@Data
public class UpdateVendorNotifyRequest {
    private String notifyEmail;
    private String defaultReconEmail;
}
