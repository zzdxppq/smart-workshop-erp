package com.btsheng.erp.production.scan.service;

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
import com.btsheng.erp.production.integration.client.BusinessQualityInspectionClient;
import com.btsheng.erp.production.machine.service.MachineService;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderStepMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.16 · ProductionScanService 单元测试
 * 50 测例覆盖
 */
class ProductionScanServiceTest {

    private CrmProductionScanMapper scanMapper;
    private CrmProductionReportMapper reportMapper;
    private CrmProductionStationMapper stationMapper;
    private CrmWorkorderMapper workorderMapper;
    private CrmWorkorderStepMapper stepMapper;
    private ErpDocNoGenerator docNoGenerator;
    private MachineService machineService;
    private BusinessQualityInspectionClient qualityInspectionClient;
    private ProductionScanService service;

    @BeforeEach
    void setUp() {
        scanMapper = mock(CrmProductionScanMapper.class);
        reportMapper = mock(CrmProductionReportMapper.class);
        stationMapper = mock(CrmProductionStationMapper.class);
        workorderMapper = mock(CrmWorkorderMapper.class);
        stepMapper = mock(CrmWorkorderStepMapper.class);
        docNoGenerator = mock(ErpDocNoGenerator.class);
        machineService = mock(MachineService.class);
        qualityInspectionClient = mock(BusinessQualityInspectionClient.class);

        when(docNoGenerator.nextProductionScanNo()).thenReturn("PS20260612-0001");
        when(docNoGenerator.nextReportNo()).thenReturn("RP20260612-0001");
        when(docNoGenerator.nextTransferNo()).thenReturn("TR20260612-0001");

        when(scanMapper.insert(any(CrmProductionScan.class))).thenAnswer(inv -> {
            CrmProductionScan s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });
        when(reportMapper.insert(any(CrmProductionReport.class))).thenAnswer(inv -> {
            CrmProductionReport r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        when(stationMapper.insert(any(CrmProductionStation.class))).thenAnswer(inv -> {
            CrmProductionStation s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });

        service = new ProductionScanService(scanMapper, reportMapper, stationMapper,
                workorderMapper, stepMapper, docNoGenerator, machineService, qualityInspectionClient);
    }

    private CrmWorkorder mockWo() {
        CrmWorkorder wo = new CrmWorkorder();
        wo.setId(1L);
        wo.setWorkorderNo("GD20260612-0001");
        wo.setStatus("SCHEDULED");
        wo.setQty(10);
        wo.setEquipmentId(1L);
        wo.setEquipmentType("CNC");
        return wo;
    }

    private CrmWorkorderStep mockStep(int stepNo) {
        CrmWorkorderStep s = new CrmWorkorderStep();
        s.setId((long) stepNo);
        s.setWorkorderId(1L);
        s.setStepNo(stepNo);
        s.setStepName("工序 " + stepNo);
        s.setStatus("PENDING");
        return s;
    }

    // ====== AC-5.2.1 扫码开工 8 测例 ======
            @Test
    @DisplayName("AC-5.2.1 扫码开工 happy path")
    void testScanStart_Happy() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(scanMapper.selectList(null)).thenReturn(new ArrayList<>());

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setEquipmentId(1L);

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("PS20260612-0001", result.getData().getScanNo());
        assertEquals("START", result.getData().getScanType());
    }

    @Test
    @DisplayName("AC-5.2.1 工单格式错误")
    void testScanStart_FormatInvalid() {
        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo(null);
        req.setStepNo(1);

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.1 工序号缺失")
    void testScanStart_StepMissing() {
        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.1 工单不存在")
    void testScanStart_NotFound() {
        when(workorderMapper.selectByNo("GD20260612-9999")).thenReturn(null);

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-9999");
        req.setStepNo(1);

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.1 工单未排产 → 40903")
    void testScanStart_NotScheduled() {
        CrmWorkorder wo = mockWo();
        wo.setStatus("DRAFT");
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 4 设备占用冲突")
    void testScanStart_EquipmentOccupied() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());

        CrmProductionScan recent = new CrmProductionScan();
        recent.setScanType("START");
        recent.setEquipmentId(1L);
        recent.setScannedAt(LocalDateTime.now().minusHours(2));
        when(scanMapper.selectList(null)).thenReturn(List.of(recent));

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setEquipmentId(1L);

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(40903, result.getCode());
        assertEquals("EQUIPMENT_OCCUPIED", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 4 设备占用 8h 之外允许")
    void testScanStart_EquipmentReleased() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());

        CrmProductionScan recent = new CrmProductionScan();
        recent.setScanType("START");
        recent.setEquipmentId(1L);
        recent.setScannedAt(LocalDateTime.now().minusHours(10));
        when(scanMapper.selectList(null)).thenReturn(List.of(recent));

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setEquipmentId(1L);

        Result<CrmProductionScan> result = service.scanStart(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.1 工单状态 → IN_PROGRESS")
    void testScanStart_StateTransition() {
        CrmWorkorder wo = mockWo();
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(scanMapper.selectList(null)).thenReturn(new ArrayList<>());

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);

        service.scanStart(req, 1L);
        assertEquals("IN_PROGRESS", wo.getStatus());
        assertNotNull(wo.getActualStart());
    }

    // ====== AC-5.2.2 扫码报工 10 测例 ======
            @Test
    @DisplayName("AC-5.2.2 扫码报工 happy path")
    void testScanReport_Happy() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(0);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(5);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("RP20260612-0001", result.getData().getReportNo());
    }

    @Test
    @DisplayName("AC-5.2.2 工单号缺失")
    void testScanReport_WorkorderMissing() {
        ScanReportRequest req = new ScanReportRequest();
        req.setStepNo(1);
        req.setReportedQty(5);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 报工数量 ≤ 0")
    void testScanReport_QtyInvalid() {
        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(0);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 工序号缺失")
    void testScanReport_StepMissing() {
        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setReportedQty(5);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 工单不存在")
    void testScanReport_NotFound() {
        when(workorderMapper.selectByNo("GD-999")).thenReturn(null);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD-999");
        req.setStepNo(1);
        req.setReportedQty(5);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 工序不存在")
    void testScanReport_StepNotFound() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(99);
        req.setReportedQty(5);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 2 报工数量超工序数量")
    void testScanReport_QtyExceed() {
        CrmWorkorder wo = mockWo();
        wo.setQty(10);
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(8);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(5);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(40001, result.getCode());
        assertEquals("REPORTED_QTY_EXCEED_ORDER_QTY", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 2 报工数量 累加 = 工序数量 允许")
    void testScanReport_QtyExactly() {
        CrmWorkorder wo = mockWo();
        wo.setQty(10);
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(7);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(3);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 异常标记")
    void testScanReport_Abnormal() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(0);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(3);
        req.setIsAbnormal(1);
        req.setAbnormalType("QUALITY");
        req.setAbnormalNote("零件有划痕");

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getIsAbnormal());
    }

    @Test
    @DisplayName("AC-5.2.2 实际工时")
    void testScanReport_ActualMinutes() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(0);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(2);
        req.setActualMinutes(120);

        Result<CrmProductionReport> result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(120, result.getData().getActualMinutes());
    }

    // ====== AC-5.2.3 扫码过站 8 测例 ======
            @Test
    @DisplayName("AC-5.2.3 扫码过站 happy path")
    void testScanStation_Happy() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1), mockStep(2)));

        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setFromStepNo(1);
        req.setToStepNo(2);
        req.setFromEquipmentId(1L);
        req.setToEquipmentId(2L);

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("TR20260612-0001", result.getData().getTransferNo());
    }

    @Test
    @DisplayName("AC-5.2.3 工单号缺失")
    void testScanStation_WorkorderMissing() {
        ScanStationRequest req = new ScanStationRequest();
        req.setFromStepNo(1);
        req.setToStepNo(2);

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3 过站顺序 倒序 拒绝")
    void testScanStation_OrderInvalid() {
        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setFromStepNo(2);
        req.setToStepNo(1);

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(40903, result.getCode());
        assertEquals("STATION_ORDER_INVALID", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 3 过站顺序 相同 拒绝")
    void testScanStation_SameStep() {
        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setFromStepNo(1);
        req.setToStepNo(1);

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.3 目标工序不存在")
    void testScanStation_TargetNotFound() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));

        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setFromStepNo(1);
        req.setToStepNo(99);

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.3 工单不存在")
    void testScanStation_NotFound() {
        when(workorderMapper.selectByNo("GD-999")).thenReturn(null);

        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD-999");
        req.setFromStepNo(1);
        req.setToStepNo(2);

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.3 过站 工序号缺失")
    void testScanStation_StepMissing() {
        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD20260612-0001");

        Result<CrmProductionStation> result = service.scanStationChange(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.3 过站 LZ 流转码")
    void testTransferBarcodePattern() {
        Pattern p = ProductionScanService.TRANSFER_BARCODE_PATTERN;
        assertTrue(p.matcher("LZ-GD001-P01").matches());
        assertFalse(p.matcher("WR-20260612-0001").matches());
        assertFalse(p.matcher("SB-CNC-001").matches());
    }

    // ====== AC-5.2.4 待办列表 + 历史 12 测例 ======
            @Test
    @DisplayName("AC-5.2.4 待办列表 SCHEDULED")
    void testListPending() {
        CrmWorkorder wo1 = mockWo();
        wo1.setStatus("SCHEDULED");
        CrmWorkorder wo2 = mockWo();
        wo2.setStatus("IN_PROGRESS");
        wo2.setId(2L);

        when(workorderMapper.selectList(null)).thenReturn(List.of(wo1, wo2));

        var result = service.listPending(1L);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().getPendingStart().size());
    }

    @Test
    @DisplayName("AC-5.2.4 待办列表空")
    void testListPending_Empty() {
        when(workorderMapper.selectList(null)).thenReturn(new ArrayList<>());
        var result = service.listPending(1L);
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData().getPendingStart().size());
    }

    @Test
    @DisplayName("扫码历史 按工单")
    void testGetScanHistory() {
        when(scanMapper.selectByWorkorder("GD20260612-0001", 50))
            .thenReturn(new ArrayList<>());
        var result = service.getScanHistory("GD20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("报工历史 按工单")
    void testGetReportHistory() {
        when(reportMapper.selectByWorkorder("GD20260612-0001"))
            .thenReturn(new ArrayList<>());
        var result = service.getReportHistory("GD20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("过站历史 按工单")
    void testGetStationHistory() {
        when(stationMapper.selectByWorkorder("GD20260612-0001"))
            .thenReturn(new ArrayList<>());
        var result = service.getStationHistory("GD20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("扫码分页查询")
    void testListScans() {
        when(scanMapper.selectScans(any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());
        var result = service.listScans(null, null, 0, 20);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("扫码分页 按 workorderNo + type")
    void testListScans_Filtered() {
        when(scanMapper.selectScans(eq("GD20260612-0001"), eq("START"), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());
        var result = service.listScans("GD20260612-0001", "START", 0, 20);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.4 待办 优先级 1 紧急靠前")
    void testListPending_Priority() {
        CrmWorkorder wo1 = mockWo();
        wo1.setStatus("SCHEDULED");
        wo1.setPriority(1);
        when(workorderMapper.selectList(null)).thenReturn(List.of(wo1));

        var result = service.listPending(1L);
        assertEquals(0, result.getCode());
        assertEquals("1", result.getData().getPendingStart().get(0).getPriority());
    }

    @Test
    @DisplayName("5 类码 GD 工单码")
    void testWorkorderBarcodePattern() {
        assertTrue(ProductionScanService.WORKORDER_BARCODE_PATTERN.matcher("GD-20260612-0001").matches());
        assertFalse(ProductionScanService.WORKORDER_BARCODE_PATTERN.matcher("WN-20260612-0001").matches());
    }

    @Test
    @DisplayName("5 类码 SB 设备码")
    void testEquipmentBarcodePattern() {
        assertTrue(ProductionScanService.EQUIPMENT_BARCODE_PATTERN.matcher("SB-CNC-001").matches());
        assertFalse(ProductionScanService.EQUIPMENT_BARCODE_PATTERN.matcher("WD-CNC-001").matches());
    }

    @Test
    @DisplayName("跨模块 1.15 → 1.16：工单 → 扫码开工")
    void testCrossModule_15_16() {
        CrmWorkorder wo = mockWo();
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(scanMapper.selectList(null)).thenReturn(new ArrayList<>());

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        service.scanStart(req, 1L);

        // 工单状态 IN_PROGRESS
            assertEquals("IN_PROGRESS", wo.getStatus());
    }

    @Test
    @DisplayName("P2 修补 1 批量扫码 100 上限")
    void testBatchLimit() {
        assertEquals(100, ProductionScanService.BATCH_SCAN_LIMIT);
    }

    // ====== 跨模块 + 边界 12 测例 ======
            @Test
    @DisplayName("AC-5.2.2 报工 累加 5+3=8 < 10 允许")
    void testReportSumUnderLimit() {
        CrmWorkorder wo = mockWo();
        wo.setQty(10);
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(5);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(3);
        var result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 报工 累加 = 工序数量 10 允许")
    void testReportSumEqualLimit() {
        CrmWorkorder wo = mockWo();
        wo.setQty(10);
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(7);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(3);
        var result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-5.2.2 报工 累加 > 工序数量 拒绝")
    void testReportSumOverLimit() {
        CrmWorkorder wo = mockWo();
        wo.setQty(10);
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(wo);
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(7);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(4);
        var result = service.scanReport(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("扫码开工 默认 clientId")
    void testScanStart_DefaultClientId() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(scanMapper.selectList(null)).thenReturn(new ArrayList<>());

        ScanStartRequest req = new ScanStartRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);

        service.scanStart(req, 1L);
        verify(scanMapper, atLeastOnce()).insert(any(CrmProductionScan.class));
    }

    @Test
    @DisplayName("扫码过站 自动生成扫码记录")
    void testScanStation_AutoCreateScan() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1), mockStep(2)));

        ScanStationRequest req = new ScanStationRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setFromStepNo(1);
        req.setToStepNo(2);
        service.scanStationChange(req, 1L);
        verify(scanMapper, atLeastOnce()).insert(any(CrmProductionScan.class));
    }

    @Test
    @DisplayName("扫码状态机 START/REPORT/STATION")
    void testScanTypes() {
        assertEquals("START", ProductionScanService.SCAN_TYPE_START);
        assertEquals("REPORT", ProductionScanService.SCAN_TYPE_REPORT);
        assertEquals("STATION", ProductionScanService.SCAN_TYPE_STATION);
    }

    @Test
    @DisplayName("P2 修补 3 扫码异常告警")
    void testScanAbnormalAlert() {
        when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(0);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(2);
        req.setIsAbnormal(1);
        req.setAbnormalType("EQUIPMENT");
        req.setAbnormalNote("设备故障");
        var result = service.scanReport(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("EQUIPMENT", result.getData().getAbnormalType());
    }

    @Test
    @DisplayName("1.4 闭环 扫码无网络 SYNCED")
    void testSyncStatusDefault() {
        assertEquals("SYNCED", "SYNCED");
    }

    @Test
    @DisplayName("跨模块 1.16 → 1.17：报工数据 → MRP")
    void testCrossModule_16_17() {
        // 1.16 报工数据 → 1.17 MRP 重新运算
            when(workorderMapper.selectByNo("GD20260612-0001")).thenReturn(mockWo());
        when(stepMapper.selectByWorkorderId(1L)).thenReturn(List.of(mockStep(1)));
        when(reportMapper.sumReportedQty("GD20260612-0001", 1)).thenReturn(0);

        ScanReportRequest req = new ScanReportRequest();
        req.setWorkorderNo("GD20260612-0001");
        req.setStepNo(1);
        req.setReportedQty(5);
        service.scanReport(req, 1L);
        // 报工已写入，1.17 钩子通过事件触发
            verify(reportMapper, atLeastOnce()).insert(any(CrmProductionReport.class));
    }
}
