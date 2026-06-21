package com.btsheng.erp.platform.admin.dto;

import lombok.Data;

import java.util.List;

@Data
public class FieldEncryptionConfigDto {
    private String algorithm = "AES-256-GCM";
    private String dekPath = "/etc/erp/dek.key";
    private List<String> whitelistFields;
    private String note;
}
