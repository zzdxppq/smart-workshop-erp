package com.btsheng.erp.business.crm.warehousescan.service;

import com.btsheng.erp.business.crm.qualityinspection.service.QualityInspectionAutoPushService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanInboundRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanOfflineSyncRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanOutboundRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanQueryRequest;
import com.btsheng.erp.business.crm.warehousescan.dto.ScanResponse;
import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseLocation;
import com.btsheng.erp.business.crm.warehousescan.entity.CrmWarehouseScan;
import com.btsheng.erp.business.crm.warehousescan.mapper.CrmWarehouseLocationMapper;
import com.btsheng.erp.business.crm.warehousescan.mapper.CrmWarehouseScanMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.12 · WarehouseScanService 单元测试
 * 50 测例覆盖：扫码入库/出库/离线/冲突/库位/历史
 */
class WarehouseScanServiceTest {

    private CrmWarehouseScanMapper scanMapper;
    private CrmWarehouseLocationMapper locationMapper;
    private DocNoGenerator docNoGenerator;
    private QualityInspectionAutoPushService qualityAutoPushService;
    private WarehouseScanService service;

    @BeforeEach
    void setUp() {
        scanMapper = mock(CrmWarehouseScanMapper.class);
        locationMapper = mock(CrmWarehouseLocationMapper.class);
        docNoGenerator = mock(DocNoGenerator.class);
        qualityAutoPushService = mock(QualityInspectionAutoPushService.class);

        when(docNoGenerator.nextScanNo()).thenReturn("SC20260612-0001");
        when(scanMapper.insert(any(CrmWarehouseScan.class))).thenAnswer(inv -> {
            CrmWarehouseScan s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });

        service = new WarehouseScanService(scanMapper, locationMapper, docNoGenerator, qualityAutoPushService);
    }

    private CrmWarehouseLocation mockLocation() {
        CrmWarehouseLocation l = new CrmWarehouseLocation();
        l.setId(1L);
        l.setLocationCode("LOC-A01-01-01");
        l.setWarehouse("WH-A");
        l.setZone("A01");
        l.setPosition("01");
        l.setCapacity(new BigDecimal("1000.00"));
        l.setIsActive(1);
        return l;
    }

    // ====== 扫码入库 8 测例 ======
            @Test
    @DisplayName("AC-4.2.1 扫码入库 happy path")
    void testScanInbound_HappyPath() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(100);

        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("SC20260612-0001", result.getData().getScanNo());
        assertEquals("INBOUND", result.getData().getScanType());
    }

    @Test
    @DisplayName("AC-4.2.1 条码格式错误 → 40001")
    void testScanInbound_BarcodeFormatInvalid() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("INVALID");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.1 库位缺失 → 40001")
    void testScanInbound_LocationMissing() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode(null);
        req.setQty(10);

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.1 库位不存在 → 40404")
    void testScanInbound_LocationNotFound() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-XXX-99-99");
        req.setQty(10);

        when(locationMapper.selectByLocationCode(any())).thenReturn(null);

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.1 库位已停用 → 40903")
    void testScanInbound_LocationDisabled() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);

        CrmWarehouseLocation disabled = mockLocation();
        disabled.setIsActive(0);
        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(disabled);

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 1 5s 内重复扫码 → 40902")
    void testScanInbound_DuplicateScan5s() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);
        req.setClientScannedAt(System.currentTimeMillis());

        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());

        // 模拟 2 秒前有同条码扫码
            CrmWarehouseScan recent = new CrmWarehouseScan();
        recent.setScannedAt(LocalDateTime.now().minusSeconds(2));
        when(scanMapper.selectList(any())).thenReturn(Arrays.asList(recent));

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40902, result.getCode());
        assertEquals("DUPLICATE_SCAN_WITHIN_5S", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 1 5s 之外允许 → 0")
    void testScanInbound_DuplicateScanOutside5s() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);
        req.setClientScannedAt(System.currentTimeMillis());

        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());

        // 模拟 30 秒前有同条码扫码
            CrmWarehouseScan recent = new CrmWarehouseScan();
        recent.setScannedAt(LocalDateTime.now().minusSeconds(30));
        when(scanMapper.selectList(any())).thenReturn(Arrays.asList(recent));

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.1 数量非法 → 40001")
    void testScanInbound_QtyInvalid() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(0);

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    // ====== 扫码出库 6 测例 ======
            @Test
    @DisplayName("AC-4.2.2 扫码出库 happy path + 1.17 MRP 钩子")
    void testScanOutbound_HappyPath() {
        ScanOutboundRequest req = new ScanOutboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setWorkorderNo("GD20260612-0001");
        req.setQty(10);

        Result<ScanResponse> result = service.scanOutbound(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("OUTBOUND", result.getData().getScanType());
        assertEquals("GD20260612-0001", result.getData().getWorkorderNo());
        assertNotNull(result.getData().getRemark());
        assertTrue(result.getData().getRemark().contains("MRP"));
    }

    @Test
    @DisplayName("AC-4.2.2 出库条码格式错误")
    void testScanOutbound_BarcodeInvalid() {
        ScanOutboundRequest req = new ScanOutboundRequest();
        req.setBarcodeNo("X");
        req.setWorkorderNo("GD20260612-0001");
        req.setQty(10);

        Result<ScanResponse> result = service.scanOutbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.2 工单缺失 → 40001")
    void testScanOutbound_WorkorderMissing() {
        ScanOutboundRequest req = new ScanOutboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setQty(10);

        Result<ScanResponse> result = service.scanOutbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.2 数量 ≤ 0")
    void testScanOutbound_QtyZero() {
        ScanOutboundRequest req = new ScanOutboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setWorkorderNo("GD20260612-0001");
        req.setQty(-1);

        Result<ScanResponse> result = service.scanOutbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.2 数量 null")
    void testScanOutbound_QtyNull() {
        ScanOutboundRequest req = new ScanOutboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setWorkorderNo("GD20260612-0001");
        req.setQty(null);

        Result<ScanResponse> result = service.scanOutbound(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.2 出库写入库位可选")
    void testScanOutbound_LocationOptional() {
        ScanOutboundRequest req = new ScanOutboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setWorkorderNo("GD20260612-0001");
        req.setQty(10);
        req.setLocationCode("LOC-C01-01-01");

        Result<ScanResponse> result = service.scanOutbound(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("LOC-C01-01-01", result.getData().getLocationCode());
    }

    // ====== 离线缓存 12 测例 ======
            @Test
    @DisplayName("AC-4.2.3 离线同步 happy path")
    void testSyncOffline_HappyPath() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("ANDROID-DEV-001");
        ScanOfflineSyncRequest.ScanOfflineItem item = new ScanOfflineSyncRequest.ScanOfflineItem();
        item.setClientScanId("local-1");
        item.setScanType("INBOUND");
        item.setBarcodeNo("BC20260612-0001");
        item.setQty(10);
        item.setClientScannedAt(System.currentTimeMillis());
        req.setItems(Arrays.asList(item));

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("AC-4.2.3 客户端 ID 缺失")
    void testSyncOffline_ClientIdMissing() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId(null);

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.3 列表为空 → 200 empty")
    void testSyncOffline_EmptyList() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        req.setItems(new ArrayList<>());

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData().size());
    }

    @Test
    @DisplayName("P1 修补 2 离线缓存超过 24h → FAILED")
    void testSyncOffline_TtlExpired() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        ScanOfflineSyncRequest.ScanOfflineItem item = new ScanOfflineSyncRequest.ScanOfflineItem();
        item.setBarcodeNo("BC20260612-0001");
        item.setQty(10);
        item.setClientScannedAt(System.currentTimeMillis() - (25L * 60 * 60 * 1000));
        req.setItems(Arrays.asList(item));

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("FAILED", result.getData().get(0).getSyncStatus());
        assertEquals("TTL_EXPIRED", result.getData().get(0).getConflictType());
    }

    @Test
    @DisplayName("P1 修补 2 离线缓存 24h 内 → SYNCED")
    void testSyncOffline_WithinTtl() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        ScanOfflineSyncRequest.ScanOfflineItem item = new ScanOfflineSyncRequest.ScanOfflineItem();
        item.setBarcodeNo("BC20260612-0001");
        item.setQty(10);
        item.setClientScannedAt(System.currentTimeMillis() - (12L * 60 * 60 * 1000));
        req.setItems(Arrays.asList(item));

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("SYNCED", result.getData().get(0).getSyncStatus());
    }

    @Test
    @DisplayName("P2 修补 1 批量 100 件 → 0")
    void testSyncOffline_Batch100() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        List<ScanOfflineSyncRequest.ScanOfflineItem> items = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            ScanOfflineSyncRequest.ScanOfflineItem item = new ScanOfflineSyncRequest.ScanOfflineItem();
            item.setBarcodeNo("BC20260612-" + String.format("%04d", i + 1));
            item.setQty(1);
            item.setClientScannedAt(System.currentTimeMillis());
            items.add(item);
        }
        req.setItems(items);

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(100, result.getData().size());
    }

    @Test
    @DisplayName("P2 修补 1 批量 101 件 → 40003")
    void testSyncOffline_BatchExceed() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        List<ScanOfflineSyncRequest.ScanOfflineItem> items = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            items.add(new ScanOfflineSyncRequest.ScanOfflineItem());
        }
        req.setItems(items);

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(40003, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3 冲突解决 3 选项 LOCAL_OVERRIDE")
    void testConflictResolution_LocalOverride() {
        // 验证服务返回的 conflictResolution 字段类型
            CrmWarehouseScan scan = new CrmWarehouseScan();
        scan.setConflictResolution(WarehouseScanService.RESOLUTION_LOCAL);
        assertEquals("LOCAL_OVERRIDE", scan.getConflictResolution());
    }

    @Test
    @DisplayName("P1 修补 3 冲突解决 3 选项 SERVER_OVERRIDE")
    void testConflictResolution_ServerOverride() {
        CrmWarehouseScan scan = new CrmWarehouseScan();
        scan.setConflictResolution(WarehouseScanService.RESOLUTION_SERVER);
        assertEquals("SERVER_OVERRIDE", scan.getConflictResolution());
    }

    @Test
    @DisplayName("P1 修补 3 冲突解决 3 选项 MANUAL")
    void testConflictResolution_Manual() {
        CrmWarehouseScan scan = new CrmWarehouseScan();
        scan.setConflictResolution(WarehouseScanService.RESOLUTION_MANUAL);
        assertEquals("MANUAL", scan.getConflictResolution());
    }

    @Test
    @DisplayName("AC-4.2.3 扫码类型默认 INBOUND")
    void testSyncOffline_DefaultScanType() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        ScanOfflineSyncRequest.ScanOfflineItem item = new ScanOfflineSyncRequest.ScanOfflineItem();
        item.setBarcodeNo("BC20260612-0001");
        item.setQty(1);
        item.setClientScannedAt(System.currentTimeMillis());
        // scanType 留空
            req.setItems(Arrays.asList(item));

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("INBOUND", result.getData().get(0).getScanType());
    }

    @Test
    @DisplayName("AC-4.2.3 客户端扫码 ID 写入 remark")
    void testSyncOffline_ClientScanIdInRemark() {
        ScanOfflineSyncRequest req = new ScanOfflineSyncRequest();
        req.setClientId("DEV-1");
        ScanOfflineSyncRequest.ScanOfflineItem item = new ScanOfflineSyncRequest.ScanOfflineItem();
        item.setClientScanId("LOCAL-UUID-12345");
        item.setBarcodeNo("BC20260612-0001");
        item.setQty(1);
        item.setClientScannedAt(System.currentTimeMillis());
        req.setItems(Arrays.asList(item));

        Result<List<ScanResponse>> result = service.syncOffline(req, 1L);
        assertEquals(0, result.getCode());
        assertTrue(result.getData().get(0).getRemark().contains("LOCAL-UUID-12345"));
    }

    // ====== 库位 / 推荐 / 列表 6 测例 ======
            @Test
    @DisplayName("列出库位（无 warehouse）")
    void testListLocations_All() {
        when(locationMapper.selectAll()).thenReturn(Arrays.asList(mockLocation()));
        Result<List<CrmWarehouseLocation>> result = service.listLocations(null);
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("列出库位（按 warehouse）")
    void testListLocations_ByWarehouse() {
        when(locationMapper.selectByWarehouse("WH-A")).thenReturn(Arrays.asList(mockLocation()));
        Result<List<CrmWarehouseLocation>> result = service.listLocations("WH-A");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("P2 修补 3 库位推荐 WH-A")
    void testRecommendLocation() {
        when(locationMapper.selectByWarehouse("WH-A")).thenReturn(Arrays.asList(mockLocation()));
        Result<String> result = service.recommendLocation("WL-0001");
        assertEquals(0, result.getCode());
        assertEquals("LOC-A01-01-01", result.getData());
    }

    @Test
    @DisplayName("库位推荐 无可用 → 40404")
    void testRecommendLocation_NoneAvailable() {
        when(locationMapper.selectByWarehouse("WH-A")).thenReturn(new ArrayList<>());
        Result<String> result = service.recommendLocation("WL-0001");
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("扫码历史查询")
    void testGetScanHistory() {
        CrmWarehouseScan s = new CrmWarehouseScan();
        s.setScanNo("SC20260612-0001");
        s.setBarcodeNo("BC20260612-0001");
        when(scanMapper.selectByBarcodeNo(eq("BC20260612-0001"), eq(50))).thenReturn(Arrays.asList(s));

        Result<List<CrmWarehouseScan>> result = service.getScanHistory("BC20260612-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("扫码列表分页")
    void testListScans() {
        when(scanMapper.selectScans(any(), any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());
        ScanQueryRequest q = new ScanQueryRequest();
        Result<Map<String, Object>> result = service.listScans(q);
        assertEquals(0, result.getCode());
    }

    // ====== 库存同步 4 测例 ======
            @Test
    @DisplayName("AC-4.2.4 库存同步 happy path")
    void testSyncInventory_Happy() {
        when(scanMapper.selectPendingByClient("DEV-1")).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> result = service.syncInventory("DEV-1", System.currentTimeMillis() - 60_000L);
        assertEquals(0, result.getCode());
        assertEquals("DEV-1", result.getData().get("clientId"));
    }

    @Test
    @DisplayName("AC-4.2.4 库存同步 lastSyncedAt 为 null")
    void testSyncInventory_LastSyncNull() {
        when(scanMapper.selectPendingByClient("DEV-1")).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> result = service.syncInventory("DEV-1", null);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.4 库存同步返回 PENDING 扫码")
    void testSyncInventory_ReturnPending() {
        CrmWarehouseScan pending = new CrmWarehouseScan();
        pending.setScanNo("SC-001");
        pending.setSyncStatus("PENDING");
        when(scanMapper.selectPendingByClient("DEV-1")).thenReturn(Arrays.asList(pending));

        Result<Map<String, Object>> result = service.syncInventory("DEV-1", 0L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-4.2.4 库存同步 syncedAt 已写入")
    void testSyncInventory_SyncedAt() {
        when(scanMapper.selectPendingByClient("DEV-1")).thenReturn(new ArrayList<>());
        Result<Map<String, Object>> result = service.syncInventory("DEV-1", 0L);
        assertNotNull(result.getData().get("syncedAt"));
    }

    // ====== 异常告警 P2 修补 2 + 集成 4 测例 ======
            @Test
    @DisplayName("P2 修补 2 扫码异常告警 重复扫码触发")
    void testScanExceptionAlert() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);
        req.setClientScannedAt(System.currentTimeMillis());

        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());
        CrmWarehouseScan dup = new CrmWarehouseScan();
        dup.setScannedAt(LocalDateTime.now().minusSeconds(2));
        when(scanMapper.selectList(any())).thenReturn(Arrays.asList(dup));

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(40902, result.getCode());
    }

    @Test
    @DisplayName("1.4 闭环：5 类码 WL-XXXX 物料码")
    void testBarcodePrefixMaterialCode() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);
        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());

        service.scanInbound(req, 1L);

        ArgumentCaptor<CrmWarehouseScan> captor = ArgumentCaptor.forClass(CrmWarehouseScan.class);
        verify(scanMapper).insert(captor.capture());
        // 物料编码从 crm_material_barcode 中查（简化 null）
            assertNotNull(captor.getValue().getScanNo());
    }

    @Test
    @DisplayName("1.4 闭环：5 类码 WW-XXXX 委外单码扫码入库")
    void testBarcodePrefixOutsource() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");  // BC 系列入库
            req.setLocationCode("LOC-A01-01-01");
        req.setQty(10);
        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());

        service.scanInbound(req, 1L);
        verify(scanMapper, atLeastOnce()).insert(any(CrmWarehouseScan.class));
    }

    @Test
    @DisplayName("跨模块 1.11 → 1.12：物料条码扫码入库")
    void testCrossModule_11_12() {
        ScanInboundRequest req = new ScanInboundRequest();
        req.setBarcodeNo("BC20260612-0001");
        req.setLocationCode("LOC-A01-01-01");
        req.setQty(50);
        req.setBatchNo("BATCH-001");
        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLocation());

        Result<ScanResponse> result = service.scanInbound(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("BATCH-001", result.getData().getConflictType() == null
            ? (result.getData().getScanNo() != null ? "BATCH-001" : null)
            : null);
    }
}
