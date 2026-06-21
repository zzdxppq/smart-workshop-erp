package com.btsheng.erp.production.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** erp-production → erp-business · MRP 缺料转采购单 */
@FeignClient(
        name = "erp-business",
        url = "${app.business.url:}",
        path = "/internal/mrp-purchase",
        contextId = "businessMrpPurchaseClient",
        configuration = com.btsheng.erp.production.integration.config.InternalFeignConfig.class)
public interface BusinessMrpPurchaseClient {

    @PostMapping("/create-from-shortages")
    Result<Map<String, Object>> createFromShortages(@RequestBody Map<String, Object> body);
}
