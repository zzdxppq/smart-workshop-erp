package com.btsheng.erp.production.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** erp-production → erp-business · 图纸-业务关联写入 */
@FeignClient(
        name = "erp-business",
        url = "${app.business.url:}",
        path = "/internal/drawing-links",
        contextId = "businessDrawingLinkClient")
public interface BusinessDrawingLinkClient {

    @PostMapping
    Result<Void> createLink(@RequestBody Map<String, Object> body);
}
