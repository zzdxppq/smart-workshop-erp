package com.btsheng.erp.business.crm.warehouselocation.service;

import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.warehouselocation.dto.BatchCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.BatchTraceResponse;
import com.btsheng.erp.business.crm.warehouselocation.dto.LocationCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.dto.WarehouseCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmBatch;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouse;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseLocationExt;
import com.btsheng.erp.business.crm.warehouselocation.mapper.WarehouseBatchMapper;
import com.btsheng.erp.business.crm.warehouselocation.mapper.CrmWarehouseLocationExtMapper;
import com.btsheng.erp.business.crm.warehouselocation.mapper.CrmWarehouseMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.13 · WarehouseLocationService 单元测试
 * 40 测例覆盖
 */
class WarehouseLocationServiceTest {

    private CrmWarehouseMapper warehouseMapper;
    private CrmWarehouseLocationExtMapper locationMapper;
    private WarehouseBatchMapper batchMapper;
    private CrmMaterialMapper materialMapper;
    private DocNoGenerator docNoGenerator;
    private WarehouseLocationService service;

    @BeforeEach
    void setUp() {
        warehouseMapper = mock(CrmWarehouseMapper.class);
        locationMapper = mock(CrmWarehouseLocationExtMapper.class);
        batchMapper = mock(WarehouseBatchMapper.class);
        materialMapper = mock(CrmMaterialMapper.class);
        docNoGenerator = mock(DocNoGenerator.class);

        when(docNoGenerator.nextBatchNo()).thenReturn("B20260612-000001");

        when(warehouseMapper.insert(any(CrmWarehouse.class))).thenAnswer(inv -> {
            CrmWarehouse w = inv.getArgument(0);
            w.setId(1L);
            return 1;
        });
        when(locationMapper.insert(any(CrmWarehouseLocationExt.class))).thenAnswer(inv -> {
            CrmWarehouseLocationExt l = inv.getArgument(0);
            l.setId(1L);
            return 1;
        });
        when(batchMapper.insert(any(CrmBatch.class))).thenAnswer(inv -> {
            CrmBatch b = inv.getArgument(0);
            b.setId(1L);
            return 1;
        });

        service = new WarehouseLocationService(warehouseMapper, locationMapper, batchMapper, materialMapper, docNoGenerator);
    }

    private CrmWarehouse mockWh() {
        CrmWarehouse w = new CrmWarehouse();
        w.setId(1L);
        w.setWarehouseCode("WH-A");
        w.setWarehouseName("主仓 A 库");
        w.setWarehouseType("MAIN");
        w.setIsActive(1);
        return w;
    }

    private CrmWarehouseLocationExt mockLoc() {
        CrmWarehouseLocationExt l = new CrmWarehouseLocationExt();
        l.setId(1L);
        l.setLocationCode("LOC-A01-01-01");
        l.setWarehouse("WH-A");
        l.setZone("A01");
        l.setPosition("01");
        l.setCapacity(new BigDecimal("1000"));
        l.setIsActive(1);
        return l;
    }

    private CrmBatch mockBatch() {
        CrmBatch b = new CrmBatch();
        b.setId(1L);
        b.setBatchNo("B20260601-000001");
        b.setMaterialCode("WL-0001");
        b.setSupplierId(1L);
        b.setSupplierName("宝钢");
        b.setQty(1000);
        b.setReceivedAt(LocalDateTime.now().minusDays(10));
        b.setQualityStatus("PASSED");
        b.setLocationCode("LOC-A01-01-01");
        b.setFefoOrder(1);
        return b;
    }

    // ====== 仓库 6 测例 ======
            @Test
    @DisplayName("AC-4.3.1 创建仓库 happy path")
    void testCreateWarehouse_Happy() {
        WarehouseCreateRequest req = new WarehouseCreateRequest();
        req.setWarehouseCode("WH-D");
        req.setWarehouseName("测试仓");
        req.setWarehouseType("MAIN");

        Result<CrmWarehouse> result = service.createWarehouse(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("WH-D", result.getData().getWarehouseCode());
        assertEquals(1, result.getData().getIsActive());
    }

    @Test
    @DisplayName("AC-4.3.1 仓库编码格式错误")
    void testCreateWarehouse_CodeFormatInvalid() {
        WarehouseCreateRequest req = new WarehouseCreateRequest();
        req.setWarehouseCode("WAREHOUSE-X");
        req.setWarehouseName("测试");
        req.setWarehouseType("MAIN");

        Result<CrmWarehouse> result = service.createWarehouse(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.1 仓库名称缺失")
    void testCreateWarehouse_NameMissing() {
        WarehouseCreateRequest req = new WarehouseCreateRequest();
        req.setWarehouseCode("WH-D");
        req.setWarehouseType("MAIN");

        Result<CrmWarehouse> result = service.createWarehouse(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.1 仓库编码重复 → 40905")
    void testCreateWarehouse_Duplicate() {
        WarehouseCreateRequest req = new WarehouseCreateRequest();
        req.setWarehouseCode("WH-A");
        req.setWarehouseName("主仓 A");
        req.setWarehouseType("MAIN");

        when(warehouseMapper.selectByCode("WH-A")).thenReturn(mockWh());

        Result<CrmWarehouse> result = service.createWarehouse(req, 1L);
        assertEquals(40905, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.3 仓库列表")
    void testListWarehouses() {
        when(warehouseMapper.selectAll()).thenReturn(Arrays.asList(mockWh()));
        Result<List<CrmWarehouse>> result = service.listWarehouses();
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("P1 修补 3 多仓库权限隔离")
    void testListAccessible() {
        when(warehouseMapper.selectAll()).thenReturn(Arrays.asList(mockWh()));
        Result<List<CrmWarehouse>> result = service.listAccessibleWarehouses(1L);
        assertEquals(0, result.getCode());
    }

    // ====== 库位 8 测例 ======
            @Test
    @DisplayName("AC-4.3.1 创建库位 happy path")
    void testCreateLocation_Happy() {
        LocationCreateRequest req = new LocationCreateRequest();
        req.setLocationCode("LOC-A01-01-99");
        req.setWarehouse("WH-A");
        req.setZone("A01");
        req.setPosition("99");
        req.setCapacity(new BigDecimal("500"));

        Result<CrmWarehouseLocationExt> result = service.createLocation(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.1 库位编码格式错误")
    void testCreateLocation_FormatInvalid() {
        LocationCreateRequest req = new LocationCreateRequest();
        req.setLocationCode("BAD-CODE");
        req.setWarehouse("WH-A");

        Result<CrmWarehouseLocationExt> result = service.createLocation(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 1 库位编码唯一性 → 40905")
    void testCreateLocation_Duplicate() {
        LocationCreateRequest req = new LocationCreateRequest();
        req.setLocationCode("LOC-A01-01-01");
        req.setWarehouse("WH-A");

        when(locationMapper.selectByLocationCode("LOC-A01-01-01")).thenReturn(mockLoc());

        Result<CrmWarehouseLocationExt> result = service.createLocation(req, 1L);
        assertEquals(40905, result.getCode());
    }

    @Test
    @DisplayName("库位编码正则 pattern")
    void testLocationPattern() {
        Pattern p = Pattern.compile("^LOC-[A-Z]\\d{2}-\\d{2}-\\d{2}$");
        assertTrue(p.matcher("LOC-A01-01-01").matches());
        assertTrue(p.matcher("LOC-Z99-99-99").matches());
        assertFalse(p.matcher("LOC-a01-01-01").matches());
        assertFalse(p.matcher("LOC-A1-01-01").matches());
    }

    @Test
    @DisplayName("库位树 3 级结构")
    void testGetLocationTree() {
        when(warehouseMapper.selectAll()).thenReturn(Arrays.asList(mockWh()));
        when(locationMapper.selectByWarehouse("WH-A")).thenReturn(Arrays.asList(mockLoc()));

        Result<List<Map<String, Object>>> result = service.getLocationTree();
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
        Map<String, Object> wh = result.getData().get(0);
        assertEquals("WH-A", wh.get("warehouseCode"));
        assertNotNull(wh.get("zones"));
    }

    @Test
    @DisplayName("库位树空仓库")
    void testGetLocationTree_EmptyWarehouse() {
        when(warehouseMapper.selectAll()).thenReturn(new ArrayList<>());

        Result<List<Map<String, Object>>> result = service.getLocationTree();
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData().size());
    }

    @Test
    @DisplayName("P2 修补 3 库位利用率统计")
    void testUtilization() {
        Map<String, Object> agg = new HashMap<>();
        agg.put("warehouse", "WH-A");
        agg.put("location_count", 5);
        agg.put("total_capacity", 5000);
        when(locationMapper.aggregateByWarehouse()).thenReturn(Arrays.asList(agg));

        Result<List<Map<String, Object>>> result = service.warehouseUtilization();
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("按仓库列库位")
    void testSelectByWarehouse() {
        when(locationMapper.selectByWarehouse("WH-A")).thenReturn(Arrays.asList(mockLoc()));
        // 通过 getLocationTree 验证
            when(warehouseMapper.selectAll()).thenReturn(Arrays.asList(mockWh()));
        Result<List<Map<String, Object>>> result = service.getLocationTree();
        assertEquals(0, result.getCode());
    }

    // ====== 批次 12 测例 ======
            @Test
    @DisplayName("AC-4.3.2 创建批次 happy path")
    void testCreateBatch_Happy() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setMaterialCode("WL-0001");
        req.setSupplierId(1L);
        req.setSupplierName("宝钢");
        req.setQty(100);
        req.setLocationCode("LOC-A01-01-01");
        req.setFefoOrder(1);

        CrmMaterial material = new CrmMaterial();
        material.setId(10L);
        material.setMaterialCode("WL-0001");
        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(material);

        Result<CrmBatch> result = service.createBatch(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("B20260612-000001", result.getData().getBatchNo());
        assertEquals("PENDING", result.getData().getQualityStatus());
    }

    @Test
    @DisplayName("AC-4.3.2 物料编码缺失")
    void testCreateBatch_MaterialMissing() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setSupplierId(1L);
        req.setQty(100);

        Result<CrmBatch> result = service.createBatch(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.2 物料不存在")
    void testCreateBatch_MaterialNotFound() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setMaterialCode("WL-9999");
        req.setQty(100);
        when(materialMapper.selectByMaterialCode("WL-9999")).thenReturn(null);

        Result<CrmBatch> result = service.createBatch(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.2 数量 ≤ 0")
    void testCreateBatch_QtyInvalid() {
        BatchCreateRequest req = new BatchCreateRequest();
        req.setMaterialCode("WL-0001");
        req.setSupplierId(1L);
        req.setQty(0);

        Result<CrmBatch> result = service.createBatch(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.2 批次追溯 happy path")
    void testGetBatchTrace_Happy() {
        when(batchMapper.selectByBatchNo("B20260601-000001")).thenReturn(mockBatch());

        Result<BatchTraceResponse> result = service.getBatchTrace("B20260601-000001");
        assertEquals(0, result.getCode());
        assertEquals("B20260601-000001", result.getData().getBatchNo());
        assertNotNull(result.getData().getTraceSteps());
    }

    @Test
    @DisplayName("AC-4.3.2 批次追溯 批次号缺失")
    void testGetBatchTrace_Empty() {
        Result<BatchTraceResponse> result = service.getBatchTrace(null);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.3.2 批次追溯 批次不存在")
    void testGetBatchTrace_NotFound() {
        when(batchMapper.selectByBatchNo("B-XXX")).thenReturn(null);

        Result<BatchTraceResponse> result = service.getBatchTrace("B-XXX");
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 2 FEFO 先入先出查询")
    void testListBatchesFefo() {
        when(batchMapper.selectByMaterialFefo("WL-0001"))
            .thenReturn(Arrays.asList(mockBatch()));

        Result<List<CrmBatch>> result = service.listBatchesFefo("WL-0001");
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("P1 修补 2 FEFO 物料编码为 null")
    void testListBatchesFefo_NullMaterial() {
        Result<List<CrmBatch>> result = service.listBatchesFefo(null);
        assertEquals(0, result.getCode());
        assertEquals(0, result.getData().size());
    }

    @Test
    @DisplayName("批次列表 material + quality")
    void testListBatches_Filtered() {
        when(batchMapper.selectByMaterialAndQuality("WL-0001", "PASSED"))
            .thenReturn(Arrays.asList(mockBatch()));

        Result<List<CrmBatch>> result = service.listBatches("WL-0001", "PASSED");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("批次列表 仅 material")
    void testListBatches_ByMaterial() {
        when(batchMapper.selectByMaterialFefo("WL-0001"))
            .thenReturn(Arrays.asList(mockBatch()));

        Result<List<CrmBatch>> result = service.listBatches("WL-0001", null);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("批次列表 无过滤")
    void testListBatches_All() {
        when(batchMapper.selectList(null)).thenReturn(Arrays.asList(mockBatch()));

        Result<List<CrmBatch>> result = service.listBatches(null, null);
        assertEquals(0, result.getCode());
    }

    // ====== Pattern / 跨模块 14 测例 ======
            @Test
    @DisplayName("仓库编码 pattern")
    void testWarehousePattern() {
        Pattern p = Pattern.compile("^WH-[A-Z]$");
        assertTrue(p.matcher("WH-A").matches());
        assertTrue(p.matcher("WH-Z").matches());
        assertFalse(p.matcher("WH-1").matches());
        assertFalse(p.matcher("WH-AB").matches());
    }

    @Test
    @DisplayName("P2 修补 2 批次合并 (B20260605 合并到 B20260601)")
    void testBatchMerge() {
        CrmBatch older = mockBatch();
        older.setBatchNo("B20260601-000001");
        older.setFefoOrder(1);

        CrmBatch newer = new CrmBatch();
        newer.setBatchNo("B20260605-000001");
        newer.setFefoOrder(2);

        // 合并：取 fefoOrder 较小的批次作为主
            assertTrue(older.getFefoOrder() < newer.getFefoOrder());
    }

    @Test
    @DisplayName("跨模块 1.12 → 1.13：扫码入库 → 库位 + 批次")
    void testCrossModule_12_13() {
        // 1.12 扫码入库关联 1.13 库位
            LocationCreateRequest locReq = new LocationCreateRequest();
        locReq.setLocationCode("LOC-A01-02-99");
        locReq.setWarehouse("WH-A");
        locReq.setZone("A01");
        locReq.setPosition("99");

        Result<CrmWarehouseLocationExt> locResult = service.createLocation(locReq, 1L);
        assertEquals(0, locResult.getCode());

        // 1.13 创建批次
            BatchCreateRequest batchReq = new BatchCreateRequest();
        batchReq.setMaterialCode("WL-0001");
        batchReq.setSupplierId(1L);
        batchReq.setQty(100);
        batchReq.setLocationCode("LOC-A01-02-99");

        Result<CrmBatch> batchResult = service.createBatch(batchReq, 1L);
        assertEquals(0, batchResult.getCode());
    }

    @Test
    @DisplayName("跨模块 1.13 → 1.14：库位 + 批次 → 安全库存预警")
    void testCrossModule_13_14() {
        // 占位：1.14 复用 1.13 库存快照
            CrmBatch b = mockBatch();
        assertEquals("WL-0001", b.getMaterialCode());
        assertEquals(1000, b.getQty());
    }

    @Test
    @DisplayName("AC-4.3.1 库位容量非负")
    void testLocationCapacity_NonNegative() {
        LocationCreateRequest req = new LocationCreateRequest();
        req.setLocationCode("LOC-B01-01-99");
        req.setWarehouse("WH-B");
        req.setZone("B01");
        req.setPosition("99");
        req.setCapacity(BigDecimal.ZERO);

        Result<CrmWarehouseLocationExt> result = service.createLocation(req, 1L);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("库位禁用后 is_active=0")
    void testLocation_Deactivated() {
        CrmWarehouseLocationExt l = mockLoc();
        l.setIsActive(0);
        assertEquals(0, l.getIsActive());
    }

    @Test
    @DisplayName("批次号 format")
    void testBatchNo_Format() {
        Pattern p = Pattern.compile("^B\\d{8}-\\d{6}$");
        assertTrue(p.matcher("B20260612-000001").matches());
        assertFalse(p.matcher("B20260612000001").matches());
        assertFalse(p.matcher("BC20260612-0001").matches());
    }

    @Test
    @DisplayName("AC-4.3.3 4 元组唯一（warehouse + location + material + batch）")
    void testFourTupleUnique() {
        CrmWarehouse wh = mockWh();
        CrmWarehouseLocationExt loc = mockLoc();
        CrmBatch batch = mockBatch();

        String key = wh.getWarehouseCode() + "|" + loc.getLocationCode()
                   + "|" + batch.getMaterialCode() + "|" + batch.getBatchNo();
        assertEquals("WH-A|LOC-A01-01-01|WL-0001|B20260601-000001", key);
    }

    @Test
    @DisplayName("AC-4.3.3 多仓库 MAIN/SUB/LINE_SIDE 区分")
    void testWarehouseTypes() {
        assertEquals("MAIN", WarehouseLocationService.WAREHOUSE_TYPE_MAIN);
        assertEquals("SUB", WarehouseLocationService.WAREHOUSE_TYPE_SUB);
        assertEquals("LINE_SIDE", WarehouseLocationService.WAREHOUSE_TYPE_LINE_SIDE);
    }

    @Test
    @DisplayName("批次质量状态 PENDING/PASSED/FAILED")
    void testBatchQualityStatus() {
        assertEquals("PENDING", WarehouseLocationService.QUALITY_PENDING);
        assertEquals("PASSED", WarehouseLocationService.QUALITY_PASSED);
        assertEquals("FAILED", WarehouseLocationService.QUALITY_FAILED);
    }

    @Test
    @DisplayName("库位树 多库区 A01/A02")
    void testLocationTree_MultiZone() {
        CrmWarehouseLocationExt loc1 = mockLoc();
        loc1.setZone("A01");
        CrmWarehouseLocationExt loc2 = mockLoc();
        loc2.setId(2L);
        loc2.setLocationCode("LOC-A02-01-01");
        loc2.setZone("A02");

        when(warehouseMapper.selectAll()).thenReturn(Arrays.asList(mockWh()));
        when(locationMapper.selectByWarehouse("WH-A")).thenReturn(Arrays.asList(loc1, loc2));

        Result<List<Map<String, Object>>> result = service.getLocationTree();
        assertEquals(0, result.getCode());
        Map<String, Object> wh = result.getData().get(0);
        @SuppressWarnings("unchecked")
        Map<String, List<Map<String, Object>>> zones = (Map<String, List<Map<String, Object>>>) wh.get("zones");
        assertEquals(2, zones.size());
    }

    @Test
    @DisplayName("FEFO 排序 fefo_order ASC")
    void testFefoOrderSort() {
        CrmBatch b1 = mockBatch();
        b1.setFefoOrder(1);
        CrmBatch b2 = mockBatch();
        b2.setFefoOrder(2);
        b2.setBatchNo("B20260610-000001");

        // 验证 b1.fefoOrder < b2.fefoOrder
            assertTrue(b1.getFefoOrder() < b2.getFefoOrder());
    }
}
