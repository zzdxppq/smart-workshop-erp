package com.btsheng.erp.production.machine.controller;

import com.btsheng.erp.production.machine.dto.MachineCreateRequest;
import com.btsheng.erp.production.machine.dto.MachineQueryRequest;
import com.btsheng.erp.production.machine.dto.MachineStatusChangeRequest;
import com.btsheng.erp.production.machine.dto.MachineUpdateRequest;
import com.btsheng.erp.production.machine.dto.MaintenanceCreateRequest;
import com.btsheng.erp.production.machine.entity.ProdMachine;
import com.btsheng.erp.production.machine.entity.ProdMachineMaintenance;
import com.btsheng.erp.production.machine.service.MachineService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

/** E5-S5 · 设备机台台账与负荷 API */
@Tag(name = "E5-Machine", description = "设备机台（Story 5.5 · prod_machine）")
@RestController
@RequestMapping("/machines")
public class MachineController {

    private final MachineService machineService;

    @Autowired
    public MachineController(MachineService machineService) {
        this.machineService = machineService;
    }

    @PostMapping
    @Operation(summary = "创建设备机台")
    public Result<ProdMachine> create(@RequestBody MachineCreateRequest req) {
        return machineService.createMachine(req);
    }

    @GetMapping
    @Operation(summary = "设备机台列表")
    public Result<Map<String, Object>> list(MachineQueryRequest query) {
        return machineService.listMachines(query);
    }

    @GetMapping("/{id}/detail")
    @Operation(summary = "设备详情（关联排产 + 维保记录 + 负荷/OEE）")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return machineService.getMachineDetail(id);
    }

    @GetMapping("/{id}")
    @Operation(summary = "设备详情")
    public Result<ProdMachine> get(@PathVariable Long id) {
        return machineService.getMachine(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新设备机台")
    public Result<ProdMachine> update(@PathVariable Long id, @RequestBody MachineUpdateRequest req) {
        return machineService.updateMachine(id, req);
    }

    @GetMapping("/{id}/load")
    @Operation(summary = "机台日负荷（FR-5-4-2 · 超载 >12h 预警）")
    public Result<Map<String, Object>> load(
            @PathVariable Long id,
            @RequestParam(value = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return machineService.getLoad(id, date);
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "变更设备状态")
    public Result<ProdMachine> changeStatus(@PathVariable Long id, @RequestBody MachineStatusChangeRequest req) {
        return machineService.changeStatus(id, req);
    }

    @PostMapping("/{id}/maintenance")
    @Operation(summary = "添加维保记录")
    public Result<ProdMachineMaintenance> addMaintenance(@PathVariable Long id, @RequestBody MaintenanceCreateRequest req) {
        return machineService.addMaintenance(id, req);
    }

    @DeleteMapping("/{id}/maintenance/{maintenanceId}")
    @Operation(summary = "删除维保记录")
    public Result<Void> deleteMaintenance(@PathVariable Long id, @PathVariable Long maintenanceId) {
        return machineService.deleteMaintenance(id, maintenanceId);
    }
}
