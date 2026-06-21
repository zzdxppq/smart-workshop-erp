package com.btsheng.erp.production.mrp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "MRP 运行响应（AC-5.3.1 · 缺料清单 + 建议采购量）")
public class MrpRunResponse {
    private Long runId;
    private String runNo;
    private String status;
    private String triggerType;
    private Integer totalShortage;
    private Integer totalPurchaseSuggestion;
    private String startedAt;
    private String completedAt;
    private List<ShortageItem> shortages;

    @Data
    public static class ShortageItem {
        private String materialCode;
        private String materialName;
        private Integer requiredQty;
        private Integer currentStock;
        private Integer onOrderQty;
        private Integer shortageQty;
        private Integer purchaseSuggestion;
    }
}
