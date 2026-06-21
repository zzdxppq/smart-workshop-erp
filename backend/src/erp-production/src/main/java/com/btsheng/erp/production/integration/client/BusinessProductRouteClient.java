package com.btsheng.erp.production.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** erp-production → erp-business · cnc_business 物料/工艺路线（替代双数据源） */
@FeignClient(
        name = "erp-business",
        url = "${app.business.url:}",
        path = "/internal/product-route",
        contextId = "businessProductRouteClient")
public interface BusinessProductRouteClient {

    @GetMapping("/materials/{productId}")
    Result<Map<String, Object>> resolveMaterial(@PathVariable("productId") String productId);

    @PutMapping("/materials/{materialId}/process-id")
    Result<Void> updateProcessId(@PathVariable("materialId") Long materialId,
                                 @RequestBody Map<String, Object> body);

    @GetMapping("/mdm-process/{processCode}")
    Result<Map<String, Object>> getMdmProcess(@PathVariable("processCode") String processCode);

    @GetMapping("/routes/{productCode}")
    Result<List<Map<String, Object>>> listRoutes(@PathVariable("productCode") String productCode);

    @PutMapping("/routes/{productCode}")
    Result<Void> replaceRoutes(@PathVariable("productCode") String productCode,
                               @RequestBody List<Map<String, Object>> routes);
}
