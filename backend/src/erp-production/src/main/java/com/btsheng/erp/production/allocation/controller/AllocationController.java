package com.btsheng.erp.production.allocation.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.allocation.dto.AllocationBoardResponse;
import com.btsheng.erp.production.allocation.dto.BatchAllocationRequest;
import com.btsheng.erp.production.allocation.dto.CreateAllocationRequest;
import com.btsheng.erp.production.allocation.dto.PendingAllocationResponse;
import com.btsheng.erp.production.allocation.entity.OutsubAllocation;
import com.btsheng.erp.production.allocation.service.OutsubAllocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · E5-Allocation · 工序分配职责分离
 */
@RestController
@RequestMapping("/production/allocations")
@Tag(name = "E5-Allocation", description = "工序分配（生管/采购职责分离 · V1.3.7）")
public class AllocationController {

    private final OutsubAllocationService allocationService;

    @Autowired
    public AllocationController(OutsubAllocationService allocationService) {
        this.allocationService = allocationService;
    }

    @PostMapping
    @Operation(summary = "【生管】分配工序归属（自制/委外）")
    public Result<OutsubAllocation> create(
            @RequestBody CreateAllocationRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return allocationService.createAllocation(req, userId);
    }

    @GetMapping("/pending")
    @Operation(summary = "【采购】取待委外工序清单")
    public Result<List<PendingAllocationResponse>> pending() {
        return allocationService.listPendingAllocations();
    }

    @GetMapping("/board")
    @Operation(summary = "【生管】工单工序划分看板（待分配/已分配分区 · E5-S4）")
    public Result<AllocationBoardResponse> board(@RequestParam Long workorderId) {
        return allocationService.getAllocationBoard(workorderId);
    }

    @PostMapping("/batch")
    @Operation(summary = "【生管】批量提交工序归属")
    public Result<Map<String, Object>> batch(
            @RequestBody BatchAllocationRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return allocationService.batchAllocate(req, userId);
    }

    @GetMapping
    @Operation(summary = "查询工单工序分配")
    public Result<List<OutsubAllocation>> listByWorkorder(
            @RequestParam Long workorderId) {
        return allocationService.listByWorkorder(workorderId);
    }
}
