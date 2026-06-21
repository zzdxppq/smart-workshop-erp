package com.btsheng.erp.core.integration.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** FR-6-3 委外历史价查询结果 */
@Data
public class OutsourceHistoryPriceResult {
    private Long vendorId;
    private String processName;
    private List<BigDecimal> historyPrices = new ArrayList<>();
    private BigDecimal suggestedPrice;
    private Integer sampleCount;
    private boolean empty;
    private String message;
    /** 建议价与录入价偏差超过该百分比时提示（默认 10） */
    private BigDecimal deviationWarnPct = new BigDecimal("10.00");
    private BigDecimal avgPrice;
    private String supplierName;
    private String materialCode;

    public static OutsourceHistoryPriceResult empty(Long vendorId, String processName) {
        OutsourceHistoryPriceResult r = new OutsourceHistoryPriceResult();
        r.setVendorId(vendorId);
        r.setProcessName(processName);
        r.setEmpty(true);
        r.setMessage("暂无历史价，请询价");
        r.setSampleCount(0);
        return r;
    }
}
