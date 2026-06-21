package com.btsheng.erp.production.workorder.service;

import com.btsheng.erp.production.machine.entity.ProdMachine;
import com.btsheng.erp.production.machine.mapper.ProdMachineMapper;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderProcess;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderProcessMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderStepMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** E5-S6 · 工单工序�?/ 锁机�?/ 自动接续 */
@Service
public class WorkorderProcessService {

    private final CrmWorkorderMapper workorderMapper;
    private final CrmWorkorderProcessMapper processMapper;
    private final CrmWorkorderStepMapper stepMapper;
    private final ProdMachineMapper machineMapper;

    @Autowired
    public WorkorderProcessService(CrmWorkorderMapper workorderMapper,
                                   CrmWorkorderProcessMapper processMapper,
                                   CrmWorkorderStepMapper stepMapper,
                                   ProdMachineMapper machineMapper) {
        this.workorderMapper = workorderMapper;
        this.processMapper = processMapper;
        this.stepMapper = stepMapper;
        this.machineMapper = machineMapper;
    }

    public Result<List<CrmWorkorderProcess>> listProcesses(Long workorderId) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        return Result.ok(processMapper.selectByWorkorderId(workorderId));
    }

    @Transactional
    @AuditLog(module = "production", action = "production.lock_next_machine")
    public Result<CrmWorkorderProcess> lockNextMachine(Long workorderId, Integer seq, Map<String, Object> body) {
        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        CrmWorkorderProcess proc = processMapper.selectByWorkorderAndSeq(workorderId, seq);
        if (proc == null) {
            return Result.fail(40404, "PROCESS_NOT_FOUND");
        }
        Long machineId = body != null && body.get("machineId") != null
                ? Long.valueOf(body.get("machineId").toString()) : proc.getMachineId();
        if (machineId == null) {
            return Result.fail(40001, "MACHINE_ID_REQUIRED");
        }
        ProdMachine machine = machineMapper.selectById(machineId);
        if (machine == null || machine.getIsActive() == null || machine.getIsActive() == 0) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        proc.setLockedMachineId(machineId);
        proc.setUpdatedAt(LocalDateTime.now());
        processMapper.updateById(proc);

        CrmWorkorderStep step = stepMapper.selectByWorkorderId(workorderId).stream()
                .filter(s -> seq.equals(s.getStepNo())).findFirst().orElse(null);
        if (step != null) {
            step.setLockedMachineId(machineId);
            stepMapper.updateById(step);
        }
        CrmWorkorderProcess next = processMapper.selectByWorkorderAndSeq(workorderId, seq + 1);
        if (next != null) {
            next.setMachineId(machineId);
            next.setUpdatedAt(LocalDateTime.now());
            processMapper.updateById(next);
        }
        return Result.ok(proc);
    }

    public Result<Map<String, Object>> getHandoff(Long workorderId, Integer seq) {
        CrmWorkorderProcess current = processMapper.selectByWorkorderAndSeq(workorderId, seq);
        if (current == null) {
            return Result.fail(40404, "PROCESS_NOT_FOUND");
        }
        CrmWorkorderProcess next = processMapper.selectByWorkorderAndSeq(workorderId, seq + 1);
        Map<String, Object> data = new HashMap<>();
        data.put("current", current);
        data.put("next", next);
        data.put("lockedMachineId", current.getLockedMachineId());
        if (current.getLockedMachineId() != null) {
            data.put("lockedMachine", machineMapper.selectById(current.getLockedMachineId()));
        }
        return Result.ok(data);
    }

    @Transactional
    @AuditLog(module = "production", action = "production.auto_continue")
    public Result<Map<String, Object>> autoContinue(Long workorderId, Integer seq) {
        Result<Map<String, Object>> handoff = getHandoff(workorderId, seq);
        if (!handoff.isSuccess()) {
            return Result.fail(handoff.getCode(), handoff.getMessage());
        }
        CrmWorkorderProcess current = processMapper.selectByWorkorderAndSeq(workorderId, seq);
        CrmWorkorderProcess next = processMapper.selectByWorkorderAndSeq(workorderId, seq + 1);
        if (next == null) {
            return Result.fail(40903, "NO_NEXT_PROCESS");
        }
        Long targetMachine = current.getLockedMachineId() != null ? current.getLockedMachineId() : next.getMachineId();
        if (targetMachine == null) {
            return Result.fail(40001, "NO_MACHINE_FOR_CONTINUE");
        }
        current.setStatus("COMPLETED");
        current.setUpdatedAt(LocalDateTime.now());
        processMapper.updateById(current);
        next.setMachineId(targetMachine);
        next.setStatus("IN_PROGRESS");
        next.setUpdatedAt(LocalDateTime.now());
        processMapper.updateById(next);

        CrmWorkorder wo = workorderMapper.selectById(workorderId);
        if (wo != null) {
            wo.setEquipmentId(targetMachine);
            wo.setUpdatedAt(LocalDateTime.now());
            workorderMapper.updateById(wo);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("continuedToSeq", next.getProcessSeq());
        resp.put("machineId", targetMachine);
        resp.put("nextProcess", next);
        return Result.ok(resp);
    }
}
