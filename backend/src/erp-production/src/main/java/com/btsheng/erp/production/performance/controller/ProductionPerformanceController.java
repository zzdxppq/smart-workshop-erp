package com.btsheng.erp.production.performance.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.performance.service.ProductionPerformanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** V1.4.0 · E11-S6 · 生产绩效看板 */
@RestController
@RequestMapping("/dashboard/performance")
@Tag(name = "E11-Performance-Board", description = "生产绩效看板")
public class ProductionPerformanceController {

    private final ProductionPerformanceService service;

    @Autowired
    public ProductionPerformanceController(ProductionPerformanceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "绩效排行（操作工/机台）")
    public Result<Map<String, Object>> board(
            @RequestParam(defaultValue = "day") String period,
            @RequestParam(defaultValue = "operator") String groupBy,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return service.getBoard(period, groupBy, userId, roles);
    }

    @GetMapping("/trend")
    @Operation(summary = "近 N 天产量/合格率趋势")
    public Result<List<Map<String, Object>>> trend(
            @RequestParam(defaultValue = "30") int days,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return service.getTrend(days, userId, roles);
    }

    @GetMapping("/operator/{operatorId}/detail")
    @Operation(summary = "操作工生产明细（每日/工序/不良品）")
    public Result<Map<String, Object>> operatorDetail(
            @PathVariable Long operatorId,
            @RequestParam(defaultValue = "month") String period,
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @RequestHeader(value = "X-User-Roles", required = false) String roles) {
        return service.getOperatorDetail(operatorId, period, userId, roles);
    }

    @GetMapping("/piece-wages")
    @Operation(summary = "月度计件产量汇总（薪酬核算内部调用）")
    public Result<List<Map<String, Object>>> pieceWages(
            @RequestParam int year,
            @RequestParam int month) {
        return service.getPieceWages(year, month);
    }
}
