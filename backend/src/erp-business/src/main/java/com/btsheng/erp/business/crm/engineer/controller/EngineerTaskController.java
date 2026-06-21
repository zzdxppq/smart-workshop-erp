package com.btsheng.erp.business.crm.engineer.controller;

import com.btsheng.erp.business.crm.engineer.service.EngineerTaskService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V2.1 · 工程师待办任务聚合 API
 */
@Tag(name = "E3-Engineer-Task", description = "工程师待办任务（V2.1）")
@RestController
@RequestMapping("/engineering")
public class EngineerTaskController {

    private final EngineerTaskService taskService;

    @Autowired
    public EngineerTaskController(EngineerTaskService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "工程师待办任务汇总（报价 + 订单工作台，V2.1）")
    @GetMapping("/tasks")
    public Result<List<Map<String, Object>>> listTasks(
            @RequestParam(value = "phase", required = false) String phase) {
        return taskService.listUnifiedTasks(phase);
    }

    @Operation(summary = "报价工艺定义队列（含 engineerPhase，V2.1）")
    @GetMapping("/quote-queue")
    public Result<Map<String, Object>> quoteQueue(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "phase", required = false) String phase,
            @RequestParam(value = "customerId", required = false) Long customerId,
            @RequestParam(value = "dateFrom", required = false) String dateFrom,
            @RequestParam(value = "dateTo", required = false) String dateTo) {
        return taskService.listQuoteEngineerQueue(pageNum, pageSize, phase, customerId, dateFrom, dateTo);
    }

    @Operation(summary = "订单工程转化队列（含 engineerPhase，V2.1）")
    @GetMapping("/order-queue")
    public Result<Map<String, Object>> orderQueue(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
            @RequestParam(value = "phase", required = false) String phase) {
        return taskService.listOrderEngineerQueue(pageNum, pageSize, phase);
    }
}
