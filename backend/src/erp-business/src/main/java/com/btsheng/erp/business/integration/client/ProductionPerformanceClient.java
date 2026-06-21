package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(
        name = "erp-production",
        url = "${app.production.url:}",
        path = "/dashboard/performance",
        contextId = "productionPerformanceClient")
public interface ProductionPerformanceClient {

    @GetMapping("/piece-wages")
    Result<List<Map<String, Object>>> pieceWages(@RequestParam("year") int year,
                                                  @RequestParam("month") int month);
}
