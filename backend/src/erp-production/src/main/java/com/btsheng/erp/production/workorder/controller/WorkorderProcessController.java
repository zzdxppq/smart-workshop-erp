package com.btsheng.erp.production.workorder.controller;

import com.btsheng.erp.production.workorder.entity.CrmWorkorderProcess;
import com.btsheng.erp.production.workorder.service.WorkorderProcessService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** E5-S6 · 工单工序 / 锁机台 / 自动接续 */
@Tag(name = "E5-Workorder-Process", description = "工单工序树（Story 5.6）")
@RestController
@RequestMapping("/workorders/{workorderId}/processes")
public class WorkorderProcessController {

    private final WorkorderProcessService processService;

    @Autowired
    public WorkorderProcessController(WorkorderProcessService processService) {
        this.processService = processService;
    }

    @GetMapping
    @Operation(summary = "工单工序树")
    public Result<List<CrmWorkorderProcess>> list(@PathVariable Long workorderId) {
        return processService.listProcesses(workorderId);
    }

    @PostMapping("/{seq}/lock-next-machine")
    @Operation(summary = "锁定下一工序机台")
    public Result<CrmWorkorderProcess> lockNextMachine(
            @PathVariable Long workorderId,
            @PathVariable Integer seq,
            @RequestBody(required = false) Map<String, Object> body) {
        return processService.lockNextMachine(workorderId, seq, body);
    }

    @GetMapping("/{seq}/handoff")
    @Operation(summary = "工序交接信息")
    public Result<Map<String, Object>> handoff(
            @PathVariable Long workorderId,
            @PathVariable Integer seq) {
        return processService.getHandoff(workorderId, seq);
    }

    @PostMapping("/{seq}/auto-continue")
    @Operation(summary = "自动接续下一工序")
    public Result<Map<String, Object>> autoContinue(
            @PathVariable Long workorderId,
            @PathVariable Integer seq) {
        return processService.autoContinue(workorderId, seq);
    }
}
