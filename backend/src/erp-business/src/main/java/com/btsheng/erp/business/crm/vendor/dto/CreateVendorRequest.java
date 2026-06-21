package com.btsheng.erp.business.crm.vendor.dto;

import lombok.Data;

import java.util.List;

@Data
public class CreateVendorRequest {
    private String vendorName;
    private String contactName;
    private String contactPhone;
    private String contactEmail;
    private String defaultReconEmail;
    private List<String> capabilities;
    private String creditLevel;
}
