package com.btsheng.erp.business.crm.drawing.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentDownloadPayload {
    private byte[] data;
    private String fileName;
    private String contentType;
}
