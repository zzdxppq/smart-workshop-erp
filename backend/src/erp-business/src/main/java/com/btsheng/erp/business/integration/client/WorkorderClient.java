package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

/** 工单 · erp-business → erp-production（AC-5.1.1 订单转工单） */
@FeignClient(
        name = "erp-production",
        url = "${app.production.url:}",
        path = "/workorders",
        contextId = "workorderClient")
public interface WorkorderClient {

    @PostMapping
    Result<Map<String, Object>> createWorkorder(
            @RequestBody Map<String, Object> req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId);

    @PostMapping("/from-order")
    Result<Map<String, Object>> createFromOrder(
            @RequestBody Map<String, Object> req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId);

    @GetMapping("/visitor-search")
    Result<Map<String, Object>> visitorSearch(@RequestParam("keyword") String keyword);

    /**
     * 默认活跃工单（脱敏），按状态过滤：PRODUCING/PARTIAL_SHIPPED/SCHEDULED/IN_PROGRESS
     * 用于 /visitor/progress/list 默认视图
     */
    @GetMapping("/visitor-active")
    Result<Map<String, Object>> visitorActive(@RequestParam(value = "limit", defaultValue = "23") Integer limit);

    /**
     * 单个工单详情 + 工序时间线（脱敏），用于 /visitor/progress/detail
     */
    @GetMapping("/visitor-detail/{workorderNo}")
    Result<Map<String, Object>> visitorDetail(@PathVariable("workorderNo") String workorderNo);

    @GetMapping("/by-no/{workorderNo}")
    Result<Map<String, Object>> getByNo(@PathVariable("workorderNo") String workorderNo);

    @GetMapping("/by-sales-order/{orderId}")
    Result<Map<String, Object>> getBySalesOrderId(@PathVariable("orderId") Long orderId);
}
