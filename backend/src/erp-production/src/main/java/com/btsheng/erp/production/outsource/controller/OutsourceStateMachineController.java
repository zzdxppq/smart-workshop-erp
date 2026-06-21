package com.btsheng.erp.production.outsource.controller;

import com.btsheng.erp.production.outsource.dto.OutsourceStateAdvanceRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceStateRollbackRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory;
import com.btsheng.erp.production.outsource.service.OutsourceStateMachineService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.22 · 委外 7 状态机 Controller (FR-6-2)
 *
 * <p>5 端点：
 * <ul>
 *   <li>POST /outsource-states/advance              状态机推进</li>
 *   <li>POST /outsource-states/rollback             状态机回退（含 REJECTED）</li>
 *   <li>GET  /outsource-states/{outsourceId}/history 状态历史</li>
 *   <li>GET  /outsource-states/{outsourceId}        当前状态</li>
 *   <li>GET  /outsource-states/matrix               状态转换矩阵</li>
 * </ul>
 */
@RestController
@RequestMapping("/outsource-states")
@Tag(name = "E6-Outsource-State-Machine", description = "委外 7 状态机（Story 1.22 FR-6-2）")
public class OutsourceStateMachineController {

    private final OutsourceStateMachineService service;

    @Autowired
    public OutsourceStateMachineController(OutsourceStateMachineService service) {
        this.service = service;
    }

    @PostMapping("/advance")
    @Operation(summary = "委外状态机推进（AC-6.2.1/AC-6.2.2 · 40904 状态守卫）")
    public Result<CrmOutsourceOrder> advance(
            @RequestBody OutsourceStateAdvanceRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.advanceState(req.getOutsourceId(), req.getTargetState(), req.getOperatorRole(), userId, req.getReason());
    }

    @PostMapping("/rollback")
    @Operation(summary = "委外状态机回退（REJECTED 拒收路径）")
    public Result<CrmOutsourceOrder> rollback(
            @RequestBody OutsourceStateRollbackRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return service.rollbackState(req.getOutsourceId(), req.getReason(), req.getOperatorRole(), userId);
    }

    @GetMapping("/{outsourceId}/history")
    @Operation(summary = "委外状态机历史（AC-6.2.3 · 100% 留痕）")
    public Result<List<CrmOutsourceStateHistory>> history(@PathVariable Long outsourceId) {
        return service.getStateHistory(outsourceId);
    }

    @GetMapping("/{outsourceId}")
    @Operation(summary = "获取委外单当前状态")
    public Result<CrmOutsourceOrder> getState(@PathVariable Long outsourceId) {
        return service.getOutsourceState(outsourceId);
    }

    @GetMapping("/matrix")
    @Operation(summary = "8 状态转换矩阵（OpenAPI 元数据）")
    public Result<Map<String, Object>> matrix() {
        return service.getTransitionMatrix();
    }
}
