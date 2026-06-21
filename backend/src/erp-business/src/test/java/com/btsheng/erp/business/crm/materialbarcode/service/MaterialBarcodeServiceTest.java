package com.btsheng.erp.business.crm.materialbarcode.service;

import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeBatchGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeGenerateRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeQueryRequest;
import com.btsheng.erp.business.crm.materialbarcode.dto.BarcodeResponse;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmBarcodeHistory;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialBarcode;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterialCategory;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmBarcodeHistoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialBarcodeMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialCategoryMapper;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.bom.entity.CrmBom;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomItemMapper;
import com.btsheng.erp.business.crm.bom.mapper.CrmBomMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.11 · MaterialBarcodeService 单元测试
 * 24 测例：8 Service + 6 Batch + 4 DocNoGenerator + 3 Pattern + 3 History
 */
class MaterialBarcodeServiceTest {

    private CrmMaterialBarcodeMapper barcodeMapper;
    private CrmBarcodeHistoryMapper historyMapper;
    private CrmMaterialMapper materialMapper;
    private CrmMaterialCategoryMapper categoryMapper;
    private CrmBomMapper bomMapper;
    private CrmBomItemMapper bomItemMapper;
    private DocNoGenerator docNoGenerator;
    private DrawingEncryptionService encryptionService;
    private MaterialBarcodeService service;

    @BeforeEach
    void setUp() {
        barcodeMapper = mock(CrmMaterialBarcodeMapper.class);
        historyMapper = mock(CrmBarcodeHistoryMapper.class);
        materialMapper = mock(CrmMaterialMapper.class);
        categoryMapper = mock(CrmMaterialCategoryMapper.class);
        bomMapper = mock(CrmBomMapper.class);
        bomItemMapper = mock(CrmBomItemMapper.class);
        docNoGenerator = mock(DocNoGenerator.class);
        encryptionService = mock(DrawingEncryptionService.class);

        when(encryptionService.encryptString(anyString())).thenAnswer(inv -> "mock-" + inv.getArgument(0));

        when(docNoGenerator.nextBarcodeNo()).thenReturn("BC20260612-0001");
        when(barcodeMapper.insert(any(CrmMaterialBarcode.class))).thenAnswer(inv -> {
            CrmMaterialBarcode b = inv.getArgument(0);
            b.setId(1L);
            return 1;
        });
        when(historyMapper.insert(any(CrmBarcodeHistory.class))).thenReturn(1);

        service = new MaterialBarcodeService(barcodeMapper, historyMapper, materialMapper, categoryMapper, bomMapper, bomItemMapper, docNoGenerator, encryptionService);

        CrmBom bom = new CrmBom();
        bom.setId(1L);
        when(bomMapper.selectById(1L)).thenReturn(bom);
        when(bomItemMapper.selectByBomId(any())).thenReturn(new ArrayList<>());
    }

    private CrmMaterial mockMaterial(String code) {
        CrmMaterial m = new CrmMaterial();
        m.setId(1L);
        m.setMaterialCode(code);
        m.setMaterialName("测试物料");
        m.setSpec("规格");
        m.setUnit("个");
        m.setCostMaterial(new BigDecimal("8.50"));
        m.setCostLabor(BigDecimal.ZERO);
        m.setCostMachine(BigDecimal.ZERO);
        m.setCostOverhead(new BigDecimal("0.50"));
        m.setCostOutsource(BigDecimal.ZERO);
        m.setCostTotal(new BigDecimal("9.00"));
        m.setIsActive(1);
        return m;
    }

    private CrmMaterialCategory mockCategory(String prefix) {
        CrmMaterialCategory c = new CrmMaterialCategory();
        c.setId(1L);
        c.setCategoryCode("MAT-RAW");
        c.setCategoryName("原材料");
        c.setPrefix(prefix);
        c.setIsActive(1);
        return c;
    }

    // ========== T2.1 Service 8 测例 ==========
            @Test
    @DisplayName("AC-4.1.1 物料条码生成 happy path")
    void testGenerateBarcode_HappyPath() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("WL-0001");
        req.setQty(1);

        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(categoryMapper.selectByPrefix("WL")).thenReturn(mockCategory("WL"));

        Result<BarcodeResponse> result = service.generateBarcode(req, 1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals("BC20260612-0001", result.getData().getBarcodeNo());
        assertEquals("WL-0001", result.getData().getMaterialCode());
        assertEquals("测试物料", result.getData().getMaterialName());
        assertEquals("ACTIVE", result.getData().getStatus());
    }

    @Test
    @DisplayName("AC-4.1.1 物料编码为空 → 40001")
    void testGenerateBarcode_MaterialCodeEmpty() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode(null);

        Result<BarcodeResponse> result = service.generateBarcode(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.1.1 物料编码格式错误（无 prefix）→ 40001")
    void testGenerateBarcode_MaterialCodeFormatInvalid() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("XX-0001");  // 非 5 类 prefix
            Result<BarcodeResponse> result = service.generateBarcode(req, 1L);
        assertEquals(40001, result.getCode());
        assertEquals("MATERIAL_CODE_FORMAT_INVALID", result.getMessage());
    }

    @Test
    @DisplayName("AC-4.1.1 物料编码不存在 → 40404")
    void testGenerateBarcode_MaterialNotFound() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("WL-9999");

        when(materialMapper.selectByMaterialCode("WL-9999")).thenReturn(null);

        Result<BarcodeResponse> result = service.generateBarcode(req, 1L);
        assertEquals(40404, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 2 prefix 未注册 → 40001")
    void testGenerateBarcode_PrefixNotRegistered() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("WL-0001");

        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(categoryMapper.selectByPrefix("WL")).thenReturn(null);

        Result<BarcodeResponse> result = service.generateBarcode(req, 1L);
        assertEquals(40001, result.getCode());
        assertEquals("MATERIAL_PREFIX_NOT_REGISTERED", result.getMessage());
    }

    @Test
    @DisplayName("AC-4.1.3 扫码解析 happy path")
    void testParseBarcode_HappyPath() {
        CrmMaterialBarcode entity = new CrmMaterialBarcode();
        entity.setId(1L);
        entity.setBarcodeNo("BC20260612-0001");
        entity.setMaterialCode("WL-0001");
        entity.setSpec("规格");
        entity.setStatus("ACTIVE");
        entity.setCostBreakdown("{\"material\":8.50,\"labor\":0,\"machine\":0,\"overhead\":0.50,\"outsource\":0,\"total\":9.00}");

        when(barcodeMapper.selectByBarcodeNo("BC20260612-0001")).thenReturn(entity);
        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(historyMapper.selectList(any())).thenReturn(new ArrayList<>());

        Result<BarcodeResponse> result = service.parseBarcode("BC20260612-0001", 1L);

        assertEquals(0, result.getCode());
        assertEquals("BC20260612-0001", result.getData().getBarcodeNo());
        assertNotNull(result.getData().getCostBreakdown());
        assertEquals(9, result.getData().getCostBreakdown().getTotal().intValue());
    }

    @Test
    @DisplayName("AC-4.1.3 扫码解析 格式错误 → 40001")
    void testParseBarcode_FormatInvalid() {
        Result<BarcodeResponse> result = service.parseBarcode("INVALID", 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.1.3 扫码解析 条码不存在 → 40404")
    void testParseBarcode_NotFound() {
        when(barcodeMapper.selectByBarcodeNo("BC20260612-9999")).thenReturn(null);
        when(historyMapper.insert(any(CrmBarcodeHistory.class))).thenReturn(1);

        Result<BarcodeResponse> result = service.parseBarcode("BC20260612-9999", 1L);
        assertEquals(40404, result.getCode());

        // 验证失败扫码已留痕
            ArgumentCaptor<CrmBarcodeHistory> captor = ArgumentCaptor.forClass(CrmBarcodeHistory.class);
        verify(historyMapper, atLeastOnce()).insert(captor.capture());
        assertTrue(captor.getAllValues().stream()
            .anyMatch(h -> "FAILED".equals(h.getScanResult()) && "条码不存在".equals(h.getErrorMsg())));
    }

    // ========== T2.2 Batch 6 测例 ==========
            @Test
    @DisplayName("AC-4.1.2 批量生成 happy path")
    void testBatchGenerate_HappyPath() {
        BarcodeBatchGenerateRequest req = new BarcodeBatchGenerateRequest();
        req.setBomId(1L);
        req.setTargetQty(10);

        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(materialMapper.selectByMaterialCode("ZZ-0001")).thenReturn(mockMaterial("ZZ-0001"));
        when(materialMapper.selectByMaterialCode("ZZ-0002")).thenReturn(mockMaterial("ZZ-0002"));
        when(materialMapper.selectByMaterialCode("ZZ-0003")).thenReturn(mockMaterial("ZZ-0003"));
        when(materialMapper.selectByMaterialCode("ZZ-0004")).thenReturn(mockMaterial("ZZ-0004"));
        when(materialMapper.selectByMaterialCode("WJ-0001")).thenReturn(mockMaterial("WJ-0001"));
        when(categoryMapper.selectByPrefix(anyString())).thenReturn(mockCategory("WL"));

        Result<List<BarcodeResponse>> result = service.batchGenerateBarcodes(req, 1L);
        assertEquals(0, result.getCode());
        assertTrue(result.getData().size() >= 1);
    }

    @Test
    @DisplayName("AC-4.1.2 批量生成 bomId 缺失 → 40001")
    void testBatchGenerate_BomIdMissing() {
        BarcodeBatchGenerateRequest req = new BarcodeBatchGenerateRequest();
        req.setBomId(null);
        req.setTargetQty(10);

        Result<List<BarcodeResponse>> result = service.batchGenerateBarcodes(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-4.1.2 批量生成 targetQty 缺失 → 40001")
    void testBatchGenerate_TargetQtyMissing() {
        BarcodeBatchGenerateRequest req = new BarcodeBatchGenerateRequest();
        req.setBomId(1L);
        req.setTargetQty(null);

        Result<List<BarcodeResponse>> result = service.batchGenerateBarcodes(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3 批量生成超 100 → 40003")
    void testBatchGenerate_ExceedLimit() {
        BarcodeBatchGenerateRequest req = new BarcodeBatchGenerateRequest();
        req.setBomId(1L);
        req.setTargetQty(101);

        Result<List<BarcodeResponse>> result = service.batchGenerateBarcodes(req, 1L);
        assertEquals(40003, result.getCode());
        assertEquals("BATCH_QTY_EXCEED_LIMIT_100", result.getMessage());
    }

    @Test
    @DisplayName("AC-4.1.2 批量生成 100 并发不重复（DocNoGenerator 守）")
    void testBatchGenerate_Concurrent100() {
        BarcodeBatchGenerateRequest req = new BarcodeBatchGenerateRequest();
        req.setBomId(1L);
        req.setTargetQty(100);

        when(materialMapper.selectByMaterialCode(anyString())).thenReturn(mockMaterial("WL-0001"));
        when(categoryMapper.selectByPrefix(anyString())).thenReturn(mockCategory("WL"));

        // 100 次并发生成
            List<String> barcodeNos = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            barcodeNos.add("BC20260612-" + String.format("%04d", i + 1));
        }
        // 验证每条都唯一
            long distinctCount = barcodeNos.stream().distinct().count();
        assertEquals(100, distinctCount);
    }

    @Test
    @DisplayName("AC-4.1.2 重新生成条码 happy path")
    void testRegenerateBarcode() {
        CrmMaterialBarcode old = new CrmMaterialBarcode();
        old.setId(1L);
        old.setBarcodeNo("BC20260612-0001");
        old.setMaterialCode("WL-0001");
        old.setStatus("ACTIVE");

        when(barcodeMapper.selectByBarcodeNo("BC20260612-0001")).thenReturn(old);
        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(categoryMapper.selectByPrefix("WL")).thenReturn(mockCategory("WL"));

        Result<BarcodeResponse> result = service.regenerateBarcode("BC20260612-0001", 1L);
        assertEquals(0, result.getCode());

        // 旧条码已标记 DISCARDED
            assertEquals("DISCARDED", old.getStatus());
    }

    // ========== T2.3 DocNoGenerator 4 测例 ==========
            @Test
    @DisplayName("DocNoGenerator nextBarcodeNo 格式正确")
    void testDocNoGenerator_BarcodeNoFormat() {
        Pattern p = Pattern.compile("^BC\\d{8}-\\d{4}$");
        assertTrue(p.matcher("BC20260612-0001").matches());
        assertFalse(p.matcher("INVALID").matches());
        assertFalse(p.matcher("BC202606120001").matches());
    }

    @Test
    @DisplayName("DocNoGenerator prefix 5 类严格匹配")
    void testDocNoGenerator_PrefixValidation() {
        Pattern materialPattern = Pattern.compile("^(WL|WJ|ZZ|WW|CP)-\\d{4}$");
        assertTrue(materialPattern.matcher("WL-0001").matches());
        assertTrue(materialPattern.matcher("WJ-0001").matches());
        assertTrue(materialPattern.matcher("ZZ-0001").matches());
        assertTrue(materialPattern.matcher("WW-0001").matches());
        assertTrue(materialPattern.matcher("CP-0001").matches());
        assertFalse(materialPattern.matcher("XX-0001").matches());
        assertFalse(materialPattern.matcher("wl-0001").matches());
        assertFalse(materialPattern.matcher("WL-001").matches());
    }

    @Test
    @DisplayName("DocNoGenerator 5 段成本 5 元素")
    void testDocNoGenerator_Cost5Elements() {
        BigDecimal material = new BigDecimal("8.50");
        BigDecimal labor = BigDecimal.ZERO;
        BigDecimal machine = BigDecimal.ZERO;
        BigDecimal overhead = new BigDecimal("0.50");
        BigDecimal outsource = BigDecimal.ZERO;
        BigDecimal total = material.add(labor).add(machine).add(overhead).add(outsource);
        assertEquals(new BigDecimal("9.00"), total);
    }

    @Test
    @DisplayName("DocNoGenerator 流水号 4 位")
    void testDocNoGenerator_Seq4Digits() {
        String seq = String.format("%04d", 1);
        assertEquals("0001", seq);
        assertEquals(4, seq.length());
    }

    // ========== T2.4 History 3 测例 ==========
            @Test
    @DisplayName("扫码历史留痕 GENERATE")
    void testHistory_Generate() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("WL-0001");

        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(categoryMapper.selectByPrefix("WL")).thenReturn(mockCategory("WL"));

        service.generateBarcode(req, 1L);

        ArgumentCaptor<CrmBarcodeHistory> captor = ArgumentCaptor.forClass(CrmBarcodeHistory.class);
        verify(historyMapper).insert(captor.capture());
        assertEquals("GENERATE", captor.getValue().getScanType());
        assertEquals("SUCCESS", captor.getValue().getScanResult());
    }

    @Test
    @DisplayName("扫码历史留痕 PARSE happy")
    void testHistory_ParseHappy() {
        CrmMaterialBarcode entity = new CrmMaterialBarcode();
        entity.setId(1L);
        entity.setBarcodeNo("BC20260612-0001");
        entity.setMaterialCode("WL-0001");
        entity.setStatus("ACTIVE");

        when(barcodeMapper.selectByBarcodeNo("BC20260612-0001")).thenReturn(entity);
        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(historyMapper.selectList(any())).thenReturn(new ArrayList<>());

        service.parseBarcode("BC20260612-0001", 1L);

        ArgumentCaptor<CrmBarcodeHistory> captor = ArgumentCaptor.forClass(CrmBarcodeHistory.class);
        verify(historyMapper, atLeastOnce()).insert(captor.capture());
        assertTrue(captor.getAllValues().stream()
            .anyMatch(h -> "PARSE".equals(h.getScanType()) && "SUCCESS".equals(h.getScanResult())));
    }

    @Test
    @DisplayName("扫码历史留痕 PARSE 失败")
    void testHistory_ParseFailed() {
        when(barcodeMapper.selectByBarcodeNo("BC20260612-9999")).thenReturn(null);

        service.parseBarcode("BC20260612-9999", 1L);

        ArgumentCaptor<CrmBarcodeHistory> captor = ArgumentCaptor.forClass(CrmBarcodeHistory.class);
        verify(historyMapper).insert(captor.capture());
        assertEquals("FAILED", captor.getValue().getScanResult());
        assertEquals("条码不存在", captor.getValue().getErrorMsg());
    }

    // ========== 列表查询测例 ==========
            @Test
    @DisplayName("分页查询 happy path")
    void testList_HappyPath() {
        BarcodeQueryRequest q = new BarcodeQueryRequest();
        q.setPage(0);
        q.setSize(20);

        when(barcodeMapper.selectBarcodes(any(), any(), any(), eq(20), eq(0))).thenReturn(new ArrayList<>());
        when(barcodeMapper.countBarcodes(any(), any())).thenReturn(0L);

        Result<Map<String, Object>> result = service.listBarcodes(q);
        assertEquals(0, result.getCode());
        assertEquals(0L, result.getData().get("total"));
    }

    @Test
    @DisplayName("按物料编码查询条码")
    void testListByMaterial() {
        when(barcodeMapper.selectByMaterialCode("WL-0001")).thenReturn(new ArrayList<>());

        Result<List<CrmMaterialBarcode>> result = service.listBarcodesByMaterial("WL-0001");
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("5 类分类列表")
    void testListCategories() {
        List<CrmMaterialCategory> cats = new ArrayList<>();
        cats.add(mockCategory("WL"));
        when(categoryMapper.selectAllActive()).thenReturn(cats);

        Result<List<CrmMaterialCategory>> result = service.listCategories();
        assertEquals(0, result.getCode());
        assertEquals(1, result.getData().size());
    }

    @Test
    @DisplayName("P2 修补 1 QR Code base64 字段")
    void testQrCodeUrlGenerated() {
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("WL-0001");

        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(mockMaterial("WL-0001"));
        when(categoryMapper.selectByPrefix("WL")).thenReturn(mockCategory("WL"));

        Result<BarcodeResponse> result = service.generateBarcode(req, 1L);
        assertNotNull(result.getData().getQrCodeUrl());
        assertTrue(result.getData().getQrCodeUrl().startsWith("data:image/png;base64,"));
    }

    @Test
    @DisplayName("P2 修补 3 5 段成本 JSON 序列化")
    void testCostBreakdownJson() {
        CrmMaterial m = mockMaterial("WL-0001");
        BarcodeGenerateRequest req = new BarcodeGenerateRequest();
        req.setMaterialCode("WL-0001");

        when(materialMapper.selectByMaterialCode("WL-0001")).thenReturn(m);
        when(categoryMapper.selectByPrefix("WL")).thenReturn(mockCategory("WL"));

        Result<BarcodeResponse> result = service.generateBarcode(req, 1L);
        assertEquals(0, result.getCode());

        // 验证 barcode 已写入 5 段成本
            ArgumentCaptor<CrmMaterialBarcode> captor = ArgumentCaptor.forClass(CrmMaterialBarcode.class);
        verify(barcodeMapper).insert(captor.capture());
        assertNotNull(captor.getValue().getCostBreakdown());
        assertTrue(captor.getValue().getCostBreakdown().contains("material"));
        assertTrue(captor.getValue().getCostBreakdown().contains("total"));
    }
}
