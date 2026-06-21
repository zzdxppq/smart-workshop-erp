package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

/** 产品工艺路线 · erp-business → erp-production */
@FeignClient(
        name = "erp-production",
        url = "${app.production.url:}",
        path = "/products",
        contextId = "productionProductRouteClient")
public interface ProductionProductRouteClient {

    @GetMapping("/{id}/routes")
    Result<Map<String, Object>> getProductRoute(@PathVariable("id") String id);
}
