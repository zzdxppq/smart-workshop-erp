package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 仓库/库存 · erp-business → erp-business（扫码服务）
 * V2.1 品质专项增强：报废扣减库存
 */
@FeignClient(
        name = "erp-business",
        path = "/app/scan",
        contextId = "warehouseClient")
public interface WarehouseClient {

    @PostMapping("/outbound")
    Result<Map<String, Object>> scanOutbound(
            @RequestBody Map<String, Object> req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId);
}
