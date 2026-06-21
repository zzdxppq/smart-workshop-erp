package com.btsheng.erp.business.crm.inventoryalert.service;

import com.btsheng.erp.business.crm.inventoryalert.dto.AlertResolveRequest;
import com.btsheng.erp.business.crm.inventoryalert.dto.InventoryAlertQueryRequest;
import com.btsheng.erp.business.crm.inventoryalert.dto.SafetyStockRequest;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventoryAlert;
import com.btsheng.erp.business.crm.inventoryalert.entity.CrmInventorySafety;
import com.btsheng.erp.business.crm.inventoryalert.mapper.CrmInventoryAlertMapper;
import com.btsheng.erp.business.crm.inventoryalert.mapper.CrmInventorySafetyMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.14 · InventoryAlertService 单元测试
 * 18 测例覆盖
 */
class InventoryAlertServiceTest {

    private CrmInventorySafetyMapper safetyMapper;
    private CrmInventoryAlertMapper alertMapper;
    private InventoryAlertService service;

    @BeforeEach
    void setUp() {
        safetyMapper = mock(CrmInventorySafetyMapper.class);
        alertMapper = mock(CrmInventoryAlertMapper.class);

        when(safetyMapper.insert(any(CrmInventorySafety.class))).thenAnswer(inv -> {
            CrmInventorySafety s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });
        when(alertMapper.insert(any(CrmInventoryAlert.class))).thenAnswer(inv -> {
            CrmInventoryAlert a = inv.getArgument(0);
            a.setId(1L);
            return 1;
        });

        service = new InventoryAlertService(safetyMapper, alertMapper);
    }

    private CrmInventorySafety mockSafety() {
        CrmInventorySafety s = new CrmInventorySafety();
        s.setId(1L);
        s.setMaterialCode("WL-0001");
        s.setMaterialName("Q235 钢板");
        s.setMinQty(1000);
        s.setMaxQty(5000);
        s.setReorderQty(2000);
        s.setUnit("KG");
        s.setCurrentQty(1500);
        s.setEnabled(1);
        return s;
    }

    private CrmInventoryAlert mockAlert() {
        CrmInventoryAlert a = new CrmInventoryAlert();
        a.setId(1L);
        a.setMaterialCode("WL-0001");
        a.setAlertLevel(InventoryAlertService.ALERT_LEVEL_WARN);
        a.setCurrentQty(800);
        a.setMinQty(1000);
        a.setMessage("WL-0001 库存 800 接近安全库存 1000");
        a.setStatus(InventoryAlertService.STATUS_OPEN);
        a.setTriggeredAt(LocalDateTime.now());
        return a;
    }

    // ====== AC-4.4.1 安全库存配置 6 测例 ======
            @Test
    @DisplayName("AC-4.4.1 配置安全库存 happy path")
    void testUpdateSafetyConfig_Happy() {
        SafetyStockRequest req = new SafetyStockRequest();
        req.setMaterialCode("WL-0001");
        req.setMinQty(1000);
        req.setMaxQty(5000);
        req.setReorderQty(2000);

        Result<CrmInventorySafety> result = service.updateSafetyConfig(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("WL-0001", result.getData().getMaterialCode());
    }

    @Test
    @DisplayName("P1 修补 1 min_qty 负数")
    void testUpdateSafetyConfig_MinNegative() {
        SafetyStockRequest req = new SafetyStockRequest();
        req.setMaterialCode("WL-0001");
        req.setMinQty(-1);
        req.setMaxQty(5000);
        req.setReorderQty(2000);

        Result<CrmInventorySafety> result = service.updateSafetyConfig(req, 1L);
        assertEquals(40001, result.getCode());
        assertEquals("MIN_QTY_NON_NEGATIVE", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 1 max_qty 负数")
    void testUpdateSafetyConfig_MaxNegative() {
        SafetyStockRequest req = new SafetyStockRequest();
        req.setMaterialCode("WL-0001");
        req.setMinQty(1000);
        req.setMaxQty(-1);
        req.setReorderQty(2000);

        Result<CrmInventorySafety> result = service.updateSafetyConfig(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("min_qty > max_qty")
    void testUpdateSafetyConfig_MinGreaterMax() {
        SafetyStockRequest req = new SafetyStockRequest();
        req.setMaterialCode("WL-0001");
        req.setMinQty(5000);
        req.setMaxQty(1000);
        req.setReorderQty(200);

        Result<CrmInventorySafety> result = service.updateSafetyConfig(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.4.1 物料编码缺失")
    void testUpdateSafetyConfig_MaterialMissing() {
        SafetyStockRequest req = new SafetyStockRequest();
        req.setMinQty(1000);
        req.setMaxQty(5000);
        req.setReorderQty(2000);

        Result<CrmInventorySafety> result = service.updateSafetyConfig(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.4.1 更新已存在")
    void testUpdateSafetyConfig_Update() {
        SafetyStockRequest req = new SafetyStockRequest();
        req.setMaterialCode("WL-0001");
        req.setMinQty(2000);
        req.setMaxQty(6000);
        req.setReorderQty(3000);

        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());

        Result<CrmInventorySafety> result = service.updateSafetyConfig(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(2000, result.getData().getMinQty());
    }

    // ====== AC-4.4.2 预警触发 8 测例 ======
            @Test
    @DisplayName("AC-4.4.2 库存正常 → 不触发")
    void testCheckAlert_NormalStock() {
        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());

        Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 2000);
        assertEquals(0, result.getCode());
        assertNull(result.getData());
    }

    @Test
    @DisplayName("AC-4.4.2 库存 = 0 → CRITICAL")
    void testCheckAlert_Critical() {
        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());
        when(alertMapper.selectOpenByMaterial("WL-0001")).thenReturn(null);

        Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 0);
        assertEquals(0, result.getCode());
        assertEquals(InventoryAlertService.ALERT_LEVEL_CRITICAL, result.getData().getAlertLevel());
    }

    @Test
    @DisplayName("AC-4.4.2 库存 < min*0.5 → ERROR")
    void testCheckAlert_Error() {
        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());
        when(alertMapper.selectOpenByMaterial("WL-0001")).thenReturn(null);

        // min=1000, currentQty=400 ( < 500)
            Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 400);
        assertEquals(0, result.getCode());
        assertEquals(InventoryAlertService.ALERT_LEVEL_ERROR, result.getData().getAlertLevel());
    }

    @Test
    @DisplayName("AC-4.4.2 库存 min*0.5 ~ min*0.8 → WARN")
    void testCheckAlert_Warn() {
        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());
        when(alertMapper.selectOpenByMaterial("WL-0001")).thenReturn(null);

        // min=1000, currentQty=700 (between 500 and 800)
            Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 700);
        assertEquals(0, result.getCode());
        assertEquals(InventoryAlertService.ALERT_LEVEL_WARN, result.getData().getAlertLevel());
    }

    @Test
    @DisplayName("AC-4.4.2 库存 min*0.8 ~ min → INFO")
    void testCheckAlert_Info() {
        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());
        when(alertMapper.selectOpenByMaterial("WL-0001")).thenReturn(null);

        // min=1000, currentQty=900 (>= 800)
            Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 900);
        assertEquals(0, result.getCode());
        assertEquals(InventoryAlertService.ALERT_LEVEL_INFO, result.getData().getAlertLevel());
    }

    @Test
    @DisplayName("AC-4.4.2 已有 OPEN 预警 → 不重复")
    void testCheckAlert_ExistingOpen() {
        when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());
        when(alertMapper.selectOpenByMaterial("WL-0001")).thenReturn(mockAlert());

        Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 500);
        assertEquals(0, result.getCode());
        // 不创建新预警
            verify(alertMapper, never()).insert(any(CrmInventoryAlert.class));
    }

    @Test
    @DisplayName("AC-4.4.2 安全配置不存在")
    void testCheckAlert_ConfigNotFound() {
        when(safetyMapper.selectByMaterialCode("XX-9999")).thenReturn(null);

        Result<CrmInventoryAlert> result = service.checkAlert("XX-9999", 100);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 1 安全配置列表")
    void testListSafetyConfigs() {
        when(safetyMapper.selectAllEnabled()).thenReturn(new ArrayList<>());
        Result<List<CrmInventorySafety>> result = service.listSafetyConfigs();
        assertEquals(0, result.getCode());
    }

    // ====== 解决/归档/统计 4 测例 ======
            @Test
    @DisplayName("解决预警 happy path")
    void testResolveAlert_Happy() {
        when(alertMapper.selectById(1L)).thenReturn(mockAlert());

        AlertResolveRequest req = new AlertResolveRequest();
        req.setResolutionNote("已补货 1000 KG");

        Result<CrmInventoryAlert> result = service.resolveAlert(1L, req, 1L);
        assertEquals(0, result.getCode());
        assertEquals(InventoryAlertService.STATUS_RESOLVED, result.getData().getStatus());
        assertEquals("已补货 1000 KG", result.getData().getResolutionNote());
    }

    @Test
    @DisplayName("解决预警 不存在")
    void testResolveAlert_NotFound() {
        when(alertMapper.selectById(999L)).thenReturn(null);

        AlertResolveRequest req = new AlertResolveRequest();
        req.setResolutionNote("test");

        Result<CrmInventoryAlert> result = service.resolveAlert(999L, req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("解决预警 重复解决")
    void testResolveAlert_AlreadyResolved() {
        CrmInventoryAlert resolved = mockAlert();
        resolved.setStatus(InventoryAlertService.STATUS_RESOLVED);
        when(alertMapper.selectById(1L)).thenReturn(resolved);

        AlertResolveRequest req = new AlertResolveRequest();
        req.setResolutionNote("test");

        Result<CrmInventoryAlert> result = service.resolveAlert(1L, req, 1L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3 归档已解决预警")
    void testArchiveAlert_Happy() {
        CrmInventoryAlert resolved = mockAlert();
        resolved.setStatus(InventoryAlertService.STATUS_RESOLVED);
        when(alertMapper.selectById(1L)).thenReturn(resolved);

        Result<CrmInventoryAlert> result = service.archiveAlert(1L, 1L);
        assertEquals(0, result.getCode());
        assertEquals(InventoryAlertService.STATUS_ARCHIVED, result.getData().getStatus());
    }

    @Test
    @DisplayName("P1 修补 3 归档 未解决 → 40903")
    void testArchiveAlert_NotResolved() {
        when(alertMapper.selectById(1L)).thenReturn(mockAlert());

        Result<CrmInventoryAlert> result = service.archiveAlert(1L, 1L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("预警统计")
    void testAlertStats() {
        Map<String, Object> row = new HashMap<>();
        row.put("alert_level", "WARN");
        row.put("cnt", 3);
        when(alertMapper.aggregateByLevel()).thenReturn(new ArrayList<>(java.util.Arrays.asList(row)));

        Result<List<Map<String, Object>>> result = service.alertStats();
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("预警列表分页")
    void testListAlerts() {
        when(alertMapper.selectAlerts(any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());

        InventoryAlertQueryRequest q = new InventoryAlertQueryRequest();
        Result<Map<String, Object>> result = service.listAlerts(q);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("跨模块 1.13 → 1.14：库位 + 批次 → 安全库存")
    void testCrossModule_13_14() {
        // 1.13 提供库存数据，1.14 计算预警
            when(safetyMapper.selectByMaterialCode("WL-0001")).thenReturn(mockSafety());
        when(alertMapper.selectOpenByMaterial("WL-0001")).thenReturn(null);

        // 当前库存 700（来自 1.13 crm_batch 汇总，min=1000, 0.5*min=500, 0.8*min=800）
            Result<CrmInventoryAlert> result = service.checkAlert("WL-0001", 700);
        assertEquals(0, result.getCode());
        // 700 在 500-800 之间 → WARN
            assertEquals(InventoryAlertService.ALERT_LEVEL_WARN, result.getData().getAlertLevel());
    }
}
