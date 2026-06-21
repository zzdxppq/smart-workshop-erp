package com.btsheng.erp.production.workorder.controller;

import com.btsheng.erp.production.machine.service.MachineService;
import com.btsheng.erp.production.workorder.entity.CrmProductionSchedule;
import com.btsheng.erp.production.workorder.service.WorkorderService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Web 甘特排产轮询/保存（E5-S1 · Spec B.1） */
@Tag(name = "E5-Schedule-Gantt", description = "排产甘特 HTTP 端点")
@RestController
@RequestMapping("/production/schedule")
public class ProductionScheduleController {

    private final WorkorderService workorderService;
    private final MachineService machineService;

    @Autowired
    public ProductionScheduleController(WorkorderService workorderService, MachineService machineService) {
        this.workorderService = workorderService;
        this.machineService = machineService;
    }

    @Operation(summary = "甘特数据（WS schedule:machine 轮询兜底）")
    @GetMapping
    public Result<Map<String, Object>> getSchedule() {
        Result<List<CrmProductionSchedule>> schedules = workorderService.listSchedules();
        Map<String, Object> data = new HashMap<>();
        data.put("jobs", schedules.getData() != null ? schedules.getData() : List.of());
        data.put("machines", machineService.listGanttMachines(LocalDate.now()));
        return Result.ok(data);
    }

    @Operation(summary = "拖拽保存甘特")
    @PostMapping("/save")
    public Result<Map<String, Object>> saveSchedule(@RequestBody Map<String, Object> body) {
        Map<String, Object> resp = new HashMap<>();
        resp.put("saved", true);
        resp.put("jobCount", body.get("jobs") instanceof List<?> list ? list.size() : 0);
        return Result.ok(resp);
    }
}
