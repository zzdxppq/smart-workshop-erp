package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/** 用户 · erp-business → erp-platform（HR 建档自动开通登录） */
@FeignClient(
        name = "erp-platform",
        url = "${app.platform.url:}",
        path = "/users",
        contextId = "platformUserClient")
public interface PlatformUserClient {

    @PostMapping
    Result<Map<String, Object>> createUser(@RequestBody Map<String, Object> body);
}
