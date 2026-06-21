package com.btsheng.erp.business.finance.signedscan.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SignedScanArchiveVo {
    private Long id;
    private Long reconcileId;
    private String reconcileNo;
    private String vendorName;
    private Integer periodYear;
    private Integer periodMonth;
    private String signerName;
    private LocalDateTime signedAt;
    private String signatureImagePath;
}
