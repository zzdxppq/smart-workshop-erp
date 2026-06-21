package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** 邮件发送 · erp-business → erp-platform */
@FeignClient(
        name = "erp-platform",
        url = "${app.platform.url:}",
        path = "/email",
        contextId = "emailClient")
public interface EmailClient {

    @PostMapping("/send")
    Result<Map<String, Object>> send(@RequestBody Map<String, Object> body);
}
