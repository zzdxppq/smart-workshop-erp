package com.btsheng.erp.production.outsource.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.outsource.service.OutsourceVendorSwitchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/outsource-switches")
@Tag(name = "E6-Outsource-Switch", description = "委外供应商切换")
public class OutsourceSupplierSwitchController {

    private final OutsourceVendorSwitchService switchService;

    public OutsourceSupplierSwitchController(OutsourceVendorSwitchService switchService) {
        this.switchService = switchService;
    }

    @GetMapping
    @Operation(summary = "供应商切换单列表")
    public Result<PageResponse<Map<String, Object>>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        return switchService.list(keyword, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "供应商切换单详情")
    public Result<Map<String, Object>> get(@PathVariable Long id) {
        return switchService.getById(id);
    }

    @PostMapping("/{id}/confirm-prod")
    @Operation(summary = "生管确认供应商切换")
    public Result<Map<String, Object>> confirmProd(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return switchService.confirmProd(id, userId);
    }

    @PostMapping("/{id}/confirm-purch")
    @Operation(summary = "采购确认供应商切换")
    public Result<Map<String, Object>> confirmPurch(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return switchService.confirmPurch(id, userId);
    }
}
