package com.btsheng.erp.production.machine.service;

import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.machine.dto.MachineCreateRequest;
import com.btsheng.erp.production.machine.dto.MachineQueryRequest;
import com.btsheng.erp.production.machine.dto.MachineStatusChangeRequest;
import com.btsheng.erp.production.machine.dto.MachineUpdateRequest;
import com.btsheng.erp.production.machine.dto.MaintenanceCreateRequest;
import com.btsheng.erp.production.machine.entity.ProdMachine;
import com.btsheng.erp.production.machine.entity.ProdMachineLoad;
import com.btsheng.erp.production.machine.entity.ProdMachineMaintenance;
import com.btsheng.erp.production.machine.entity.ProdMachineStatusLog;
import com.btsheng.erp.production.machine.mapper.ProdMachineLoadMapper;
import com.btsheng.erp.production.machine.mapper.ProdMachineMapper;
import com.btsheng.erp.production.machine.mapper.ProdMachineMaintenanceMapper;
import com.btsheng.erp.production.machine.mapper.ProdMachineStatusLogMapper;
import com.btsheng.erp.production.workorder.entity.CrmProductionSchedule;
import com.btsheng.erp.production.workorder.mapper.CrmProductionScheduleMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.scan.mapper.CrmProductionReportMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** E5-S5 · 设备台账与机台负荷（FR-5-4�?*/
@Service
public class MachineService {

    private static final BigDecimal DEFAULT_AVAILABLE_HOURS = new BigDecimal("12");

    private final ProdMachineMapper machineMapper;
    private final ProdMachineLoadMapper loadMapper;
    private final CrmProductionScheduleMapper scheduleMapper;
    private final ProdMachineMaintenanceMapper maintenanceMapper;
    private final ProdMachineStatusLogMapper statusLogMapper;
    private final CrmProductionReportMapper reportMapper;
    private final CrmWorkorderMapper workorderMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public MachineService(ProdMachineMapper machineMapper,
                          ProdMachineLoadMapper loadMapper,
                          CrmProductionScheduleMapper scheduleMapper,
                          ProdMachineMaintenanceMapper maintenanceMapper,
                          ProdMachineStatusLogMapper statusLogMapper,
                          CrmProductionReportMapper reportMapper,
                          CrmWorkorderMapper workorderMapper,
                          ErpDocNoGenerator docNoGenerator) {
        this.machineMapper = machineMapper;
        this.loadMapper = loadMapper;
        this.scheduleMapper = scheduleMapper;
        this.maintenanceMapper = maintenanceMapper;
        this.statusLogMapper = statusLogMapper;
        this.reportMapper = reportMapper;
        this.workorderMapper = workorderMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Transactional
    @AuditLog(module = "machine", action = "machine.create")
    public Result<ProdMachine> createMachine(MachineCreateRequest req) {
        if (req == null || req.getMachineName() == null || req.getMachineName().isBlank()) {
            return Result.fail(40001, "MACHINE_NAME_REQUIRED");
        }
        if (req.getMachineType() == null || req.getMachineType().isBlank()) {
            return Result.fail(40001, "MACHINE_TYPE_REQUIRED");
        }
        ProdMachine m = new ProdMachine();
        m.setMachineCode(docNoGenerator.nextMachineCode(req.getMachineType()));
        m.setMachineName(req.getMachineName());
        m.setMachineType(req.getMachineType());
        m.setMachineNo(req.getMachineNo());
        m.setStatus(req.getStatus() != null ? req.getStatus() : "IDLE");
        m.setMaintenanceCycleDays(req.getMaintenanceCycleDays() != null ? req.getMaintenanceCycleDays() : 90);
        m.setRemark(req.getRemark());
        m.setIsActive(1);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        machineMapper.insert(m);
        return Result.ok(m);
    }

    public Result<Map<String, Object>> listMachines(MachineQueryRequest query) {
        int limit = query.getSize() > 0 ? query.getSize() : 20;
        int offset = Math.max(query.getPage(), 0) * limit;
        List<ProdMachine> all = machineMapper.selectActive();
        List<Map<String, Object>> rows = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (ProdMachine m : all) {
            if (query.getMachineType() != null && !query.getMachineType().isEmpty()
                    && !query.getMachineType().equalsIgnoreCase(m.getMachineType())) {
                continue;
            }
            if (query.getStatus() != null && !query.getStatus().isEmpty()
                    && !query.getStatus().equalsIgnoreCase(m.getStatus())) {
                continue;
            }
            if (query.getKeyword() != null && !query.getKeyword().isEmpty()) {
                String kw = query.getKeyword().toLowerCase();
                if (!contains(m.getMachineCode(), kw) && !contains(m.getMachineName(), kw)
                        && !contains(m.getMachineNo(), kw)) {
                    continue;
                }
            }
            rows.add(toListRow(m, today));
        }
        int from = Math.min(offset, rows.size());
        int to = Math.min(offset + limit, rows.size());
        Map<String, Object> result = new HashMap<>();
        result.put("list", rows.subList(from, to));
        result.put("total", rows.size());
        result.put("page", query.getPage());
        result.put("size", limit);
        return Result.ok(result);
    }

    /**
     * V1.3.9 P0 · 返回设备类型下拉列表（去重，排序）
     * 用于工艺库新建工艺时设备类型下拉
     */
    public Result<List<String>> listTypes() {
        List<ProdMachine> all = machineMapper.selectActive();
        Map<String, String> unique = new java.util.LinkedHashMap<>();
        for (ProdMachine m : all) {
            if (m.getMachineType() != null && !m.getMachineType().isBlank()) {
                unique.putIfAbsent(m.getMachineType(), m.getMachineType());
            }
        }
        return Result.ok(new java.util.ArrayList<>(unique.values()));
    }

    public Result<Map<String, Object>> getMachineDetail(Long id) {
        ProdMachine m = machineMapper.selectById(id);
        if (m == null || m.getIsActive() == null || m.getIsActive() == 0) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        LocalDate today = LocalDate.now();
        Map<String, Object> detail = new HashMap<>(toListRow(m, today));
        detail.put("maintenanceRecords", maintenanceMapper.selectByMachineId(id, 20));
        List<Map<String, Object>> activeSchedules = new ArrayList<>();
        for (CrmProductionSchedule s : scheduleMapper.selectList(null)) {
            if (id.equals(s.getEquipmentId())) {
                Map<String, Object> row = new HashMap<>();
                row.put("scheduleNo", s.getScheduleNo());
                row.put("workorderId", s.getWorkorderId());
                row.put("planStart", s.getPlanStart());
                row.put("planEnd", s.getPlanEnd());
                row.put("status", s.getStatus());
                activeSchedules.add(row);
            }
        }
        detail.put("activeSchedules", activeSchedules);
        return Result.ok(detail);
    }

    public Result<ProdMachine> getMachine(Long id) {
        ProdMachine m = machineMapper.selectById(id);
        if (m == null || m.getIsActive() == null || m.getIsActive() == 0) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        return Result.ok(m);
    }

    public ProdMachine resolveByBarcode(String machineBarcode) {
        if (machineBarcode == null || machineBarcode.isBlank()) {
            return null;
        }
        return machineMapper.selectByCode(machineBarcode.trim());
    }

    @Transactional
    @AuditLog(module = "machine", action = "machine.update")
    public Result<ProdMachine> updateMachine(Long id, MachineUpdateRequest req) {
        ProdMachine m = machineMapper.selectById(id);
        if (m == null) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        if (req.getMachineName() != null) m.setMachineName(req.getMachineName());
        if (req.getMachineType() != null) m.setMachineType(req.getMachineType());
        if (req.getMachineNo() != null) m.setMachineNo(req.getMachineNo());
        if (req.getStatus() != null) m.setStatus(req.getStatus());
        if (req.getMaintenanceCycleDays() != null) m.setMaintenanceCycleDays(req.getMaintenanceCycleDays());
        if (req.getRemark() != null) m.setRemark(req.getRemark());
        if (req.getActive() != null) m.setIsActive(req.getActive() ? 1 : 0);
        m.setUpdatedAt(LocalDateTime.now());
        machineMapper.updateById(m);
        return Result.ok(m);
    }

    public Result<Map<String, Object>> getLoad(Long machineId, LocalDate date) {
        ProdMachine m = machineMapper.selectById(machineId);
        if (m == null) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        LocalDate loadDate = date != null ? date : LocalDate.now();
        ProdMachineLoad stored = loadMapper.selectByMachineAndDate(machineId, loadDate);
        BigDecimal planned = stored != null ? stored.getPlannedHours() : computePlannedHours(machineId, loadDate);
        BigDecimal available = stored != null ? stored.getAvailableHours() : DEFAULT_AVAILABLE_HOURS;
        int loadPercent = available.signum() == 0 ? 0
                : planned.multiply(new BigDecimal("100")).divide(available, 0, RoundingMode.HALF_UP).intValue();
        Map<String, Object> data = new HashMap<>();
        data.put("machineId", machineId);
        data.put("machineCode", m.getMachineCode());
        data.put("date", loadDate.toString());
        data.put("plannedHours", planned);
        data.put("availableHours", available);
        data.put("loadPercent", loadPercent);
        data.put("overload", planned.compareTo(available) > 0);
        return Result.ok(data);
    }

    public List<Map<String, Object>> listGanttMachines(LocalDate date) {
        LocalDate loadDate = date != null ? date : LocalDate.now();
        List<Map<String, Object>> machines = new ArrayList<>();
        for (ProdMachine m : machineMapper.selectActive()) {
            machines.add(toListRow(m, loadDate));
        }
        return machines;
    }

    private Map<String, Object> toListRow(ProdMachine m, LocalDate date) {
        Result<Map<String, Object>> load = getLoad(m.getId(), date);
        int loadPercent = load.getData() != null ? (Integer) load.getData().get("loadPercent") : 0;
        int oee = computeOee(m.getId(), date, loadPercent);
        Map<String, Object> row = new HashMap<>();
        row.put("id", m.getId());
        row.put("code", m.getMachineCode());
        row.put("name", m.getMachineName());
        row.put("machineCode", m.getMachineCode());
        row.put("machineName", m.getMachineName());
        row.put("machineType", m.getMachineType());
        row.put("machineNo", m.getMachineNo());
        row.put("status", m.getStatus());
        row.put("loadPercent", loadPercent);
        row.put("oee", oee);
        row.put("load", loadPercent);
        return row;
    }

    /** OEE：当日报工实际工时 / 可用工时（无报工时回退负荷率） */
    private int computeOee(Long machineId, LocalDate date, int loadPercentFallback) {
        BigDecimal actualHours = BigDecimal.ZERO;
        for (CrmProductionSchedule s : scheduleMapper.selectList(null)) {
            if (!machineId.equals(s.getEquipmentId()) || s.getPlanStart() == null) continue;
            if (!s.getPlanStart().toLocalDate().equals(date)) continue;
            if (s.getWorkorderId() == null) continue;
            var wo = workorderMapper.selectById(s.getWorkorderId());
            if (wo == null || wo.getWorkorderNo() == null) continue;
            var reports = reportMapper.selectByWorkorder(wo.getWorkorderNo());
            if (reports == null) continue;
            for (var r : reports) {
                if (r.getActualMinutes() != null) {
                    actualHours = actualHours.add(new BigDecimal(r.getActualMinutes())
                            .divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP));
                }
            }
        }
        if (actualHours.signum() == 0) {
            return loadPercentFallback;
        }
        return actualHours.multiply(new BigDecimal("100"))
                .divide(DEFAULT_AVAILABLE_HOURS, 0, RoundingMode.HALF_UP).intValue();
    }

    private BigDecimal computePlannedHours(Long machineId, LocalDate date) {
        List<CrmProductionSchedule> schedules = scheduleMapper.selectList(null);
        BigDecimal total = BigDecimal.ZERO;
        for (CrmProductionSchedule s : schedules) {
            if (!machineId.equals(s.getEquipmentId()) || s.getPlanStart() == null) {
                continue;
            }
            if (!s.getPlanStart().toLocalDate().equals(date)) {
                continue;
            }
            if (s.getPlanStart() != null && s.getPlanEnd() != null) {
                long minutes = java.time.Duration.between(s.getPlanStart(), s.getPlanEnd()).toMinutes();
                total = total.add(new BigDecimal(minutes).divide(new BigDecimal("60"), 2, RoundingMode.HALF_UP));
            }
        }
        return total;
    }

    private static boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }

    @Transactional
    @AuditLog(module = "machine", action = "machine.status_change")
    public Result<ProdMachine> changeStatus(Long id, MachineStatusChangeRequest req) {
        ProdMachine m = machineMapper.selectById(id);
        if (m == null || m.getIsActive() == null || m.getIsActive() == 0) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        if (req.getStatus() == null || req.getStatus().isBlank()) {
            return Result.fail(40001, "STATUS_REQUIRED");
        }
        String fromStatus = m.getStatus();
        m.setStatus(req.getStatus());
        m.setUpdatedAt(LocalDateTime.now());
        machineMapper.updateById(m);

        ProdMachineStatusLog log = new ProdMachineStatusLog();
        log.setMachineId(id);
        log.setFromStatus(fromStatus);
        log.setToStatus(req.getStatus());
        log.setReason(req.getReason());
        log.setEstimatedRecoveryDate(req.getEstimatedRecoveryDate() != null ? req.getEstimatedRecoveryDate().toString() : null);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.setChangedBy(auth != null && auth.getName() != null ? auth.getName() : "system");
        log.setChangedAt(LocalDateTime.now());
        statusLogMapper.insert(log);

        return Result.ok(m);
    }

    @Transactional
    @AuditLog(module = "machine", action = "machine.maintenance_add")
    public Result<ProdMachineMaintenance> addMaintenance(Long machineId, MaintenanceCreateRequest req) {
        if (machineMapper.selectById(machineId) == null) {
            return Result.fail(40404, "MACHINE_NOT_FOUND");
        }
        ProdMachineMaintenance record = new ProdMachineMaintenance();
        record.setMachineId(machineId);
        record.setMaintenanceType(req.getMaintenanceType());
        record.setPerformedAt(req.getPerformedAt() != null ? req.getPerformedAt() : LocalDateTime.now());
        record.setNextDue(req.getNextDue());
        record.setExecutor(req.getExecutor());
        record.setRemark(req.getRemark());
        record.setCreatedAt(LocalDateTime.now());
        maintenanceMapper.insert(record);
        return Result.ok(record);
    }

    @Transactional
    @AuditLog(module = "machine", action = "machine.maintenance_delete")
    public Result<Void> deleteMaintenance(Long machineId, Long maintenanceId) {
        ProdMachineMaintenance record = maintenanceMapper.selectById(maintenanceId);
        if (record == null) {
            return Result.fail(40404, "MAINTENANCE_RECORD_NOT_FOUND");
        }
        if (!machineId.equals(record.getMachineId())) {
            return Result.fail(40303, "MACHINE_MISMATCH");
        }
        maintenanceMapper.deleteById(maintenanceId);
        return Result.ok();
    }
}
