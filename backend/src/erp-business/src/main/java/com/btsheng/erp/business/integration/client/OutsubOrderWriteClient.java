package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.Map;

/** 委外下单 · erp-business → erp-production（RFQ 定标转 WW-） */
@FeignClient(
        name = "erp-production",
        url = "${app.production.url:}",
        path = "/outsub/orders",
        contextId = "outsubOrderWriteClient")
public interface OutsubOrderWriteClient {

    @PostMapping
    Result<Object> createOrder(
            @RequestBody Map<String, Object> req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId);
}
