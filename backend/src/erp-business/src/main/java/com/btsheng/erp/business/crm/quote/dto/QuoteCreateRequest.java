package com.btsheng.erp.business.crm.quote.dto;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "报价创建请求")
public class QuoteCreateRequest {
    @Schema(description = "报价主单")
    private CrmQuote quote;
    @Schema(description = "报价明细（≥ 1）")
    private List<CrmQuoteItem> items;
}
