package com.btsheng.erp.production.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** erp-production → erp-business · 报工后自动推送 IPQC 待检 */
@FeignClient(
        name = "erp-business",
        url = "${app.business.url:}",
        path = "/internal/quality-inspections",
        contextId = "businessQualityInspectionClient",
        configuration = com.btsheng.erp.production.integration.config.InternalFeignConfig.class)
public interface BusinessQualityInspectionClient {

    @PostMapping("/pending")
    Result<Map<String, Object>> createPending(@RequestBody Map<String, Object> body);
}
