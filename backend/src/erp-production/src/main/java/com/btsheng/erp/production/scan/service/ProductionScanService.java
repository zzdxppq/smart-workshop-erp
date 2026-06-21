package com.btsheng.erp.production.scan.service;

import com.btsheng.erp.production.integration.client.BusinessQualityInspectionClient;
import com.btsheng.erp.production.machine.service.MachineService;
import com.btsheng.erp.production.scan.dto.ScanPendingResponse;
import com.btsheng.erp.production.scan.dto.ScanReportRequest;
import com.btsheng.erp.production.scan.dto.ScanStartRequest;
import com.btsheng.erp.production.scan.dto.ScanStationRequest;
import com.btsheng.erp.production.scan.entity.CrmProductionReport;
import com.btsheng.erp.production.scan.entity.CrmProductionScan;
import com.btsheng.erp.production.scan.entity.CrmProductionStation;
import com.btsheng.erp.production.scan.mapper.CrmProductionReportMapper;
import com.btsheng.erp.production.scan.mapper.CrmProductionScanMapper;
import com.btsheng.erp.production.scan.mapper.CrmProductionStationMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderStepMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * V1.3.7 · Story 1.16 · APP 扫码开�?报工/过站 Service
 *
 * 4 业务方法：scanStart / scanReport / scanStationChange / listPending / getScanHistory
 * 5 类码：GD-工单 / LZ-流转 / SB-设备
 * 4 P1 修补：扫码开工唯一 / 报工数量 �?工序数量 / 过站顺序严格 / 设备占用冲突
 * 4 P2 修补：批量扫�?100 / 离线缓存 TTL 24h / 异常告警
 */
@Service
public class ProductionScanService {

    public static final String SCAN_TYPE_START = "START";
    public static final String SCAN_TYPE_REPORT = "REPORT";
    public static final String SCAN_TYPE_STATION = "STATION";

    public static final Pattern WORKORDER_BARCODE_PATTERN = com.btsheng.erp.production.scan.util.BarcodePrefixUtil.WORKORDER;
    public static final Pattern TRANSFER_BARCODE_PATTERN = com.btsheng.erp.production.scan.util.BarcodePrefixUtil.TRANSFER;
    public static final Pattern EQUIPMENT_BARCODE_PATTERN = com.btsheng.erp.production.scan.util.BarcodePrefixUtil.EQUIPMENT;

    /** 批量扫码上限（P2 修补 1�?*/
    public static final int BATCH_SCAN_LIMIT = 100;

    private final CrmProductionScanMapper scanMapper;
    private final CrmProductionReportMapper reportMapper;
    private final CrmProductionStationMapper stationMapper;
    private final CrmWorkorderMapper workorderMapper;
    private final CrmWorkorderStepMapper stepMapper;
    private final ErpDocNoGenerator docNoGenerator;
    private final MachineService machineService;
    private final BusinessQualityInspectionClient qualityInspectionClient;

    @Autowired
    public ProductionScanService(CrmProductionScanMapper scanMapper,
                                   CrmProductionReportMapper reportMapper,
                                   CrmProductionStationMapper stationMapper,
                                   CrmWorkorderMapper workorderMapper,
                                   CrmWorkorderStepMapper stepMapper,
                                   ErpDocNoGenerator docNoGenerator,
                                   MachineService machineService,
                                   BusinessQualityInspectionClient qualityInspectionClient) {
        this.scanMapper = scanMapper;
        this.reportMapper = reportMapper;
        this.stationMapper = stationMapper;
        this.workorderMapper = workorderMapper;
        this.stepMapper = stepMapper;
        this.docNoGenerator = docNoGenerator;
        this.machineService = machineService;
        this.qualityInspectionClient = qualityInspectionClient;
    }

    /**
     * AC-5.2.1：扫码开�?     */
    @Transactional
    @AuditLog(module = "production", action = "production.scan_start")
    public Result<CrmProductionScan> scanStart(ScanStartRequest req, Long operatorUserId) {
        if (req.getWorkorderNo() == null || req.getWorkorderNo().isEmpty()) {
            return Result.fail(40001, "WORKORDER_NO_REQUIRED");
        }
        if (req.getStepNo() == null) {
            return Result.fail(40001, "STEP_NO_REQUIRED");
        }

        // P1 修补 1：扫码开工唯一
            String workorderNo = com.btsheng.erp.production.scan.util.BarcodePrefixUtil.normalizeWorkorderNo(req.getWorkorderNo());
        req.setWorkorderNo(workorderNo);
        CrmWorkorder wo = workorderMapper.selectByNo(workorderNo);
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }
        if (!"SCHEDULED".equals(wo.getStatus())) {
            return Result.fail(40903, "WORKORDER_NOT_SCHEDULED");
        }

        // SB- 设备码解�?�?equipment_id（FR-5-2-1�?
            if (req.getEquipmentId() == null && req.getMachineBarcode() != null && !req.getMachineBarcode().isBlank()) {
            var machine = machineService.resolveByBarcode(req.getMachineBarcode());
            if (machine == null) {
                return Result.fail(40404, "MACHINE_NOT_FOUND");
            }
            if (!"IDLE".equals(machine.getStatus()) && !"RUNNING".equals(machine.getStatus())) {
                return Result.fail(40903, "MACHINE_NOT_AVAILABLE");
            }
            req.setEquipmentId(machine.getId());
        }

        // 设备占用冲突检测（P1 修补 4�?
            if (req.getEquipmentId() != null) {
            List<CrmProductionScan> recent = scanMapper.selectList(null);
            boolean occupied = recent.stream()
                .anyMatch(s -> req.getEquipmentId().equals(s.getEquipmentId())
                            && "START".equals(s.getScanType())
                            && s.getScannedAt() != null
                            && s.getScannedAt().isAfter(LocalDateTime.now().minusHours(8)));
            if (occupied) {
                return Result.fail(40903, "EQUIPMENT_OCCUPIED");
            }
        }

        // 工单状�?�?IN_PROGRESS
            wo.setStatus("IN_PROGRESS");
        wo.setActualStart(LocalDateTime.now());
        wo.setUpdatedAt(LocalDateTime.now());
        workorderMapper.updateById(wo);

        // 写扫码记�?
            CrmProductionScan scan = new CrmProductionScan();
        scan.setScanNo(docNoGenerator.nextProductionScanNo());
        scan.setWorkorderNo(req.getWorkorderNo());
        scan.setScanType(SCAN_TYPE_START);
        scan.setOperatorUserId(operatorUserId);
        scan.setEquipmentId(req.getEquipmentId());
        scan.setStepNo(req.getStepNo());
        scan.setScannedAt(LocalDateTime.now());
        scan.setClientId(req.getClientId());
        scan.setSyncStatus("SYNCED");
        scanMapper.insert(scan);

        return Result.ok(scan);
    }

    /**
     * AC-5.2.2：扫码报�?     * P1 修补 2：报工数�?�?工序数量
     */
    @Transactional
    @AuditLog(module = "production", action = "production.scan_report")
    public Result<CrmProductionReport> scanReport(ScanReportRequest req, Long operatorUserId) {
        if (req.getWorkorderNo() == null) {
            return Result.fail(40001, "WORKORDER_NO_REQUIRED");
        }
        if (req.getReportedQty() == null || req.getReportedQty() <= 0) {
            return Result.fail(40001, "REPORTED_QTY_INVALID");
        }
        if (req.getStepNo() == null) {
            return Result.fail(40001, "STEP_NO_REQUIRED");
        }

        req.setWorkorderNo(com.btsheng.erp.production.scan.util.BarcodePrefixUtil.normalizeWorkorderNo(req.getWorkorderNo()));
        CrmWorkorder wo = workorderMapper.selectByNo(req.getWorkorderNo());
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }

        // P1 修补 2：报工数�?�?工序数量
            List<CrmWorkorderStep> steps = stepMapper.selectByWorkorderId(wo.getId());
        CrmWorkorderStep step = steps.stream()
            .filter(s -> req.getStepNo().equals(s.getStepNo()))
            .findFirst().orElse(null);
        if (step == null) {
            return Result.fail(40404, "STEP_NOT_FOUND");
        }
        int alreadyReported = reportMapper.sumReportedQty(req.getWorkorderNo(), req.getStepNo());
        if (alreadyReported + req.getReportedQty() > wo.getQty()) {
            return Result.fail(40001, "REPORTED_QTY_EXCEED_ORDER_QTY");
        }

        // 写报�?
            CrmProductionReport report = new CrmProductionReport();
        report.setReportNo(docNoGenerator.nextReportNo());
        report.setWorkorderNo(req.getWorkorderNo());
        report.setStepNo(req.getStepNo());
        report.setReportedQty(req.getReportedQty());
        report.setActualMinutes(req.getActualMinutes() != null ? req.getActualMinutes() : 0);
        report.setIsAbnormal(req.getIsAbnormal() != null ? req.getIsAbnormal() : 0);
        report.setAbnormalType(req.getAbnormalType());
        report.setAbnormalNote(req.getAbnormalNote());
        report.setReportedBy(operatorUserId);
        report.setReportedAt(LocalDateTime.now());
        reportMapper.insert(report);

        // 写扫码记�?
            CrmProductionScan scan = new CrmProductionScan();
        scan.setScanNo(docNoGenerator.nextProductionScanNo());
        scan.setWorkorderNo(req.getWorkorderNo());
        scan.setScanType(SCAN_TYPE_REPORT);
        scan.setOperatorUserId(operatorUserId);
        scan.setQty(req.getReportedQty());
        scan.setStepNo(req.getStepNo());
        scan.setScannedAt(LocalDateTime.now());
        scan.setClientId(req.getClientId());
        scan.setSyncStatus("SYNCED");
        scanMapper.insert(scan);

        pushIpqcPending(report, wo, step, operatorUserId);

        return Result.ok(report);
    }

    private void pushIpqcPending(CrmProductionReport report, CrmWorkorder wo,
                                 CrmWorkorderStep step, Long operatorUserId) {
        if (qualityInspectionClient == null || report == null) {
            return;
        }
        try {
            Map<String, Object> body = new HashMap<>();
            body.put("inspectType", "IPQC");
            body.put("workOrderNo", report.getWorkorderNo());
            body.put("workOrderId", wo != null ? wo.getId() : null);
            body.put("processName", step != null ? step.getStepName() : "工序" + report.getStepNo());
            body.put("materialCode", wo != null ? wo.getMaterialCode() : null);
            body.put("qty", report.getReportedQty());
            body.put("sourceRef", "REPORT:" + report.getReportNo());
            body.put("remark", "报工完成自动生成过程检 · " + report.getWorkorderNo());
            qualityInspectionClient.createPending(body);
        } catch (Exception ignored) {
            // 跨服务推送失败不阻塞报工
        }
    }

    /**
     * AC-5.2.3：扫码过�?     * P1 修补 3：过站顺序严�?     */
    @Transactional
    @AuditLog(module = "production", action = "production.scan_station")
    public Result<CrmProductionStation> scanStationChange(ScanStationRequest req, Long operatorUserId) {
        if (req.getWorkorderNo() == null) {
            return Result.fail(40001, "WORKORDER_NO_REQUIRED");
        }
        if (req.getFromStepNo() == null || req.getToStepNo() == null) {
            return Result.fail(40001, "STEP_NO_REQUIRED");
        }
        req.setWorkorderNo(com.btsheng.erp.production.scan.util.BarcodePrefixUtil.normalizeWorkorderNo(req.getWorkorderNo()));
        if (req.getToStepNo() <= req.getFromStepNo()) {
            return Result.fail(40903, "STATION_ORDER_INVALID");
        }

        CrmWorkorder wo = workorderMapper.selectByNo(req.getWorkorderNo());
        if (wo == null) {
            return Result.fail(40404, "WORKORDER_NOT_FOUND");
        }

        // 工序必须存在
            List<CrmWorkorderStep> steps = stepMapper.selectByWorkorderId(wo.getId());
        boolean toStepExists = steps.stream()
            .anyMatch(s -> req.getToStepNo().equals(s.getStepNo()));
        if (!toStepExists) {
            return Result.fail(40404, "TARGET_STEP_NOT_FOUND");
        }

        // 写工序流�?
            CrmProductionStation station = new CrmProductionStation();
        station.setTransferNo(docNoGenerator.nextTransferNo());
        station.setWorkorderNo(req.getWorkorderNo());
        station.setFromStepNo(req.getFromStepNo());
        station.setToStepNo(req.getToStepNo());
        station.setFromEquipmentId(req.getFromEquipmentId());
        station.setToEquipmentId(req.getToEquipmentId());
        station.setTransferredBy(operatorUserId);
        station.setTransferredAt(LocalDateTime.now());
        stationMapper.insert(station);

        // 写扫码记�?
            CrmProductionScan scan = new CrmProductionScan();
        scan.setScanNo(docNoGenerator.nextProductionScanNo());
        scan.setWorkorderNo(req.getWorkorderNo());
        scan.setScanType(SCAN_TYPE_STATION);
        scan.setOperatorUserId(operatorUserId);
        scan.setEquipmentId(req.getToEquipmentId());
        scan.setStepNo(req.getToStepNo());
        scan.setScannedAt(LocalDateTime.now());
        scan.setClientId(req.getClientId());
        scan.setSyncStatus("SYNCED");
        scanMapper.insert(scan);

        return Result.ok(station);
    }

    /**
     * AC-5.2.4：扫码待办列�?     */
    public Result<ScanPendingResponse> listPending(Long operatorUserId) {
        ScanPendingResponse resp = new ScanPendingResponse();
        // 待开工：SCHEDULED 状�?+ 当前用户所属部�?        // 简化：取所�?SCHEDULED 工单
            List<CrmWorkorder> scheduled = workorderMapper.selectList(null);
        List<ScanPendingResponse.PendingItem> pendingStart = new ArrayList<>();
        for (CrmWorkorder wo : scheduled) {
            if ("SCHEDULED".equals(wo.getStatus())) {
                ScanPendingResponse.PendingItem item = new ScanPendingResponse.PendingItem();
                item.setWorkorderNo(wo.getWorkorderNo());
                item.setProductName(wo.getProductName());
                item.setStatus(wo.getStatus());
                item.setPriority(wo.getPriority() == null ? "5" : wo.getPriority().toString());
                item.setScheduledStart(wo.getScheduledStart() == null ? null : wo.getScheduledStart().toString());
                pendingStart.add(item);
            }
        }
        resp.setPendingStart(pendingStart);
        resp.setPendingReport(new ArrayList<>());
        resp.setPendingStation(new ArrayList<>());
        return Result.ok(resp);
    }

    /**
     * 扫码历史
     */
    public Result<List<CrmProductionScan>> getScanHistory(String workorderNo) {
        return Result.ok(scanMapper.selectByWorkorder(workorderNo, 50));
    }

    /**
     * 报工历史
     */
    public Result<List<CrmProductionReport>> getReportHistory(String workorderNo) {
        return Result.ok(reportMapper.selectByWorkorder(workorderNo));
    }

    /**
     * 过站历史
     */
    public Result<List<CrmProductionStation>> getStationHistory(String workorderNo) {
        return Result.ok(stationMapper.selectByWorkorder(workorderNo));
    }

    /**
     * 分页查询扫码记录
     */
    public Result<Map<String, Object>> listScans(String workorderNo, String scanType, int page, int size) {
        int limit = size > 0 ? size : 20;
        int offset = Math.max(page, 0) * limit;
        List<Map<String, Object>> list = scanMapper.selectScans(workorderNo, scanType, limit, offset);
        Map<String, Object> p = new HashMap<>();
        p.put("list", list);
        p.put("page", page);
        p.put("size", limit);
        return Result.ok(p);
    }
}
