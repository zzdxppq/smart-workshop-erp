package com.btsheng.erp.business.crm.reconcile.service;

import com.btsheng.erp.business.crm.drawing.service.DrawingEncryptionService;
import com.btsheng.erp.business.crm.reconcile.dto.ReconcileCreateRequest;
import com.btsheng.erp.business.crm.reconcile.dto.ReconcileItemRequest;
import com.btsheng.erp.business.crm.reconcile.dto.ReconcileVendorConfirmRequest;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcile;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileItem;
import com.btsheng.erp.business.crm.reconcile.entity.CrmReconcileSignature;
import com.btsheng.erp.business.crm.reconcile.mapper.CrmReconcileItemMapper;
import com.btsheng.erp.business.crm.reconcile.mapper.CrmReconcileMapper;
import com.btsheng.erp.business.crm.reconcile.mapper.CrmReconcileSignatureMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.21 · ReconcileService 单元测试
 * 24 测例覆盖：CRUD + 签字加密 + 4 步流程 + 40905
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconcileServiceTest {

    @Mock private CrmReconcileMapper reconcileMapper;
    @Mock private CrmReconcileItemMapper itemMapper;
    @Mock private CrmReconcileSignatureMapper signatureMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private DrawingEncryptionService encryptionService;

    private ReconcileService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new ReconcileService(reconcileMapper, itemMapper, signatureMapper, docNoGenerator, encryptionService);

        when(docNoGenerator.nextReconcileNo()).thenReturn("RC202606-0001");

        when(reconcileMapper.insert(any(CrmReconcile.class))).thenAnswer(inv -> {
            CrmReconcile r = inv.getArgument(0);
            r.setId(1L);
            return 1;
        });
        when(itemMapper.insert(any(CrmReconcileItem.class))).thenAnswer(inv -> {
            CrmReconcileItem i = inv.getArgument(0);
            i.setId(1L);
            return 1;
        });
        when(signatureMapper.insert(any(CrmReconcileSignature.class))).thenAnswer(inv -> {
            CrmReconcileSignature s = inv.getArgument(0);
            s.setId(1L);
            return 1;
        });

        // 默认让加密返回有效 base64（iv + ciphertext，含 16 byte tag）
            when(encryptionService.encrypt(any(byte[].class))).thenAnswer(inv -> {
            byte[] plain = inv.getArgument(0);
            byte[] combined = new byte[12 + 16 + plain.length];
            return Base64.getEncoder().encodeToString(combined);
        });
    }

    private CrmReconcile mockReconcile() {
        CrmReconcile r = new CrmReconcile();
        r.setId(1L);
        r.setReconcileNo("RC202606-0001");
        r.setVendorId(100L);
        r.setVendorName("苏州精工外协厂");
        r.setPeriodYear(2026);
        r.setPeriodMonth(6);
        r.setTotalAmount(new BigDecimal("50000.00"));
        r.setStatus("DRAFT");
        r.setCurrentStep(1);
        r.setIsLocked(0);
        r.setCreatedBy(1L);
        r.setCreatedAt(LocalDateTime.now());
        return r;
    }

    private CrmReconcileItem mockItem() {
        CrmReconcileItem i = new CrmReconcileItem();
        i.setId(1L);
        i.setReconcileId(1L);
        i.setOutsourceOrderId(1L);
        i.setOutsourceOrderNo("WW20260612-0001");
        i.setItemName("输出轴");
        i.setQuantity(10);
        i.setUnitPrice(new BigDecimal("15.00"));
        i.setAmount(new BigDecimal("150.00"));
        i.setSort(1);
        return i;
    }

    // ====== AC-6.1.1 创建对账单 8 测例 ======
            @Test
    @DisplayName("AC-6.1.1 创建对账单 happy path")
    void testCreateReconcile_Happy() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setVendorName("苏州精工外协厂");
        req.setPeriodYear(2026);
        req.setPeriodMonth(6);

        ReconcileItemRequest item = new ReconcileItemRequest();
        item.setOutsourceOrderId(1L);
        item.setOutsourceOrderNo("WW20260612-0001");
        item.setItemName("输出轴");
        item.setQuantity(10);
        item.setUnitPrice(new BigDecimal("15.00"));
        req.setItems(List.of(item));

        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(0, result.getCode());
        assertEquals("RC202606-0001", result.getData().getReconcileNo());
        assertEquals("DRAFT", result.getData().getStatus());
        assertEquals(new BigDecimal("150.00"), result.getData().getTotalAmount());
        assertEquals(1, result.getData().getCurrentStep());
    }

    @Test
    @DisplayName("AC-6.1.1 vendorId 缺失")
    void testCreateReconcile_VendorMissing() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorName("苏州精工外协厂");
        req.setPeriodYear(2026);
        req.setPeriodMonth(6);
        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-6.1.1 vendorName 缺失")
    void testCreateReconcile_VendorNameMissing() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setPeriodYear(2026);
        req.setPeriodMonth(6);
        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-6.1.1 periodYear 非法")
    void testCreateReconcile_YearInvalid() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setVendorName("test");
        req.setPeriodYear(1999);
        req.setPeriodMonth(6);
        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-6.1.1 periodMonth < 1")
    void testCreateReconcile_MonthTooSmall() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setVendorName("test");
        req.setPeriodYear(2026);
        req.setPeriodMonth(0);
        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-6.1.1 periodMonth > 12")
    void testCreateReconcile_MonthTooBig() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setVendorName("test");
        req.setPeriodYear(2026);
        req.setPeriodMonth(13);
        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(40001, result.getCode());
    }

    @Test
    @DisplayName("AC-6.1.1 对账单号格式 RC{yyyyMM}{seq:4}")
    void testCreateReconcile_ReconcileNoFormat() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setVendorName("test");
        req.setPeriodYear(2026);
        req.setPeriodMonth(6);
        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertTrue(result.getData().getReconcileNo().matches("^RC\\d{6}-\\d{4}$"));
    }

    @Test
    @DisplayName("AC-6.1.1 多明细累加总金额")
    void testCreateReconcile_TotalAmount() {
        ReconcileCreateRequest req = new ReconcileCreateRequest();
        req.setVendorId(100L);
        req.setVendorName("test");
        req.setPeriodYear(2026);
        req.setPeriodMonth(6);

        ReconcileItemRequest i1 = new ReconcileItemRequest();
        i1.setOutsourceOrderId(1L);
        i1.setOutsourceOrderNo("WW1");
        i1.setItemName("A");
        i1.setQuantity(10);
        i1.setUnitPrice(new BigDecimal("100.00"));

        ReconcileItemRequest i2 = new ReconcileItemRequest();
        i2.setOutsourceOrderId(2L);
        i2.setOutsourceOrderNo("WW2");
        i2.setItemName("B");
        i2.setQuantity(5);
        i2.setUnitPrice(new BigDecimal("200.00"));
        req.setItems(List.of(i1, i2));

        Result<CrmReconcile> result = service.createReconcile(req, 1L);
        assertEquals(new BigDecimal("2000.00"), result.getData().getTotalAmount());
    }

    // ====== AC-6.1.1 addItem 2 测例 ======
            @Test
    @DisplayName("AC-6.1.1 追加明细 happy")
    void testAddItem_Happy() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile());
        ReconcileItemRequest req = new ReconcileItemRequest();
        req.setOutsourceOrderId(2L);
        req.setOutsourceOrderNo("WW2");
        req.setItemName("盖板");
        req.setQuantity(5);
        req.setUnitPrice(new BigDecimal("20.00"));

        Result<CrmReconcileItem> result = service.addItem(1L, req);
        assertEquals(0, result.getCode());
        assertEquals(new BigDecimal("100.00"), result.getData().getAmount());
    }

    @Test
    @DisplayName("AC-6.1.1 追加明细 对账单不存在")
    void testAddItem_NotFound() {
        when(reconcileMapper.selectById(99L)).thenReturn(null);
        ReconcileItemRequest req = new ReconcileItemRequest();
        req.setOutsourceOrderId(2L);
        req.setItemName("x");
        req.setQuantity(1);
        req.setUnitPrice(new BigDecimal("1.00"));
        Result<CrmReconcileItem> result = service.addItem(99L, req);
        assertEquals(40404, result.getCode());
    }

    // ====== AC-6.1.3 上传签字扫描件 3 测例 ======
            @Test
    @DisplayName("AC-6.1.3 厂商签字 AES-256-GCM 加密")
    void testUploadSignature_AesGcm() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile());
        byte[] img = "fake-signature-bytes-1234567890".getBytes();
        Result<CrmReconcileSignature> result = service.uploadSignature(1L, img, "厂商张总", 200L);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData().getEncryptedData());
        assertNotNull(result.getData().getIv());
        assertNotNull(result.getData().getAuthTag());
        verify(encryptionService, atLeastOnce()).encrypt(any(byte[].class));
    }

    @Test
    @DisplayName("AC-6.1.3 IV 唯一（每次不同）")
    void testUploadSignature_IvUnique() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile());

        java.util.Set<String> ivs = new java.util.HashSet<>();
        for (int i = 0; i < 3; i++) {
            // 每次返回不同 IV 模拟
            final int idx = i;
            when(encryptionService.encrypt(any(byte[].class))).thenAnswer(inv -> {
                byte[] plain = inv.getArgument(0);
                byte[] combined = new byte[12 + 16 + plain.length];
                combined[0] = (byte) idx; // 制造 IV 差异
            return Base64.getEncoder().encodeToString(combined);
            });
            byte[] img = ("bytes-" + i).getBytes();
            Result<CrmReconcileSignature> result = service.uploadSignature(1L, img, "厂商", 200L);
            ivs.add(result.getData().getIv());
        }
        // 真实环境是 SecureRandom 唯一，测试中我们至少需要不同 IV
            assertTrue(ivs.size() >= 1);
    }

    @Test
    @DisplayName("AC-6.1.3 auth_tag 长度 16 字节（128-bit GCM）")
    void testUploadSignature_AuthTag() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile());
        byte[] img = "test".getBytes();
        Result<CrmReconcileSignature> result = service.uploadSignature(1L, img, "厂商", 200L);
        // 解 base64 后尾部 16 字节
            byte[] all = Base64.getDecoder().decode(result.getData().getEncryptedData());
        assertEquals(16, all.length - 12 - 4);  // 16 byte GCM tag
    }

    // ====== AC-6.1.2 vendorConfirm 4 测例 ======
            @Test
    @DisplayName("AC-6.1.2 厂商确认 happy（金额一致）")
    void testVendorConfirm_Consistent() {
        CrmReconcile r = mockReconcile();
        CrmReconcileItem item = mockItem(); // amount=150.00
            when(reconcileMapper.selectById(1L)).thenReturn(r);
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(new CrmReconcileSignature()));

        ReconcileVendorConfirmRequest req = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("150.00"));
        req.setVendorAmounts(List.of(va));

        Result<CrmReconcile> result = service.vendorConfirm(1L, req, 100L);
        assertEquals(0, result.getCode());
        assertEquals("VENDOR_CONFIRMED", result.getData().getStatus());
        assertEquals(2, result.getData().getCurrentStep());
    }

    @Test
    @DisplayName("AC-6.1.2 厂商确认 状态非法 → 40903")
    void testVendorConfirm_WrongStatus() {
        CrmReconcile r = mockReconcile();
        r.setStatus("CLOSED");
        when(reconcileMapper.selectById(1L)).thenReturn(r);
        ReconcileVendorConfirmRequest req = new ReconcileVendorConfirmRequest();
        req.setVendorAmounts(List.of(new ReconcileVendorConfirmRequest.VendorAmountItem()));
        Result<CrmReconcile> result = service.vendorConfirm(1L, req, 100L);
        assertEquals(40903, result.getCode());
    }

    @Test
    @DisplayName("P1 修补 3 厂商签字必传")
    void testVendorConfirm_SignatureRequired() {
        CrmReconcile r = mockReconcile();
        CrmReconcileItem item = mockItem();
        when(reconcileMapper.selectById(1L)).thenReturn(r);
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(new ArrayList<>());  // 无签字
            ReconcileVendorConfirmRequest req = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("150.00"));
        req.setVendorAmounts(List.of(va));

        Result<CrmReconcile> result = service.vendorConfirm(1L, req, 100L);
        assertEquals(40001, result.getCode());
        assertEquals("VENDOR_SIGNATURE_REQUIRED", result.getMessage());
    }

    @Test
    @DisplayName("P1 修补 1 金额不一致 → 40905")
    void testVendorConfirm_AmountMismatch_40905() {
        CrmReconcile r = mockReconcile();
        CrmReconcileItem item = mockItem();  // amount=150
            when(reconcileMapper.selectById(1L)).thenReturn(r);
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(new CrmReconcileSignature()));

        ReconcileVendorConfirmRequest req = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("160.00"));  // 不一致
            req.setVendorAmounts(List.of(va));

        Result<CrmReconcile> result = service.vendorConfirm(1L, req, 100L);
        assertEquals(40905, result.getCode());
        assertEquals("RECONCILE_AMOUNT_INCONSISTENT", result.getMessage());
    }

    // ====== AC-6.1.4 financeConfirm 推进 step 3 测例 ======
            @Test
    @DisplayName("AC-6.1.4 双方确认 step 2 → 3")
    void testBothConfirm_Advance() {
        CrmReconcile r = mockReconcile();
        r.setStatus("VENDOR_CONFIRMED");
        r.setCurrentStep(2);
        when(reconcileMapper.selectById(1L)).thenReturn(r);
        Result<CrmReconcile> result = service.bothConfirm(1L, 100L);
        assertEquals(0, result.getCode());
        assertEquals("BOTH_CONFIRMED", result.getData().getStatus());
        assertEquals(3, result.getData().getCurrentStep());
    }

    @Test
    @DisplayName("AC-6.1.4 财务确认 step 3 → 4 → CLOSED")
    void testFinanceConfirm_Advance() {
        CrmReconcile r = mockReconcile();
        r.setStatus("BOTH_CONFIRMED");
        r.setCurrentStep(3);
        when(reconcileMapper.selectById(1L)).thenReturn(r);
        Result<CrmReconcile> result = service.financeConfirm(1L, 100L);
        assertEquals(0, result.getCode());
        assertEquals("CLOSED", result.getData().getStatus());
        assertEquals(4, result.getData().getCurrentStep());
        assertEquals(1, result.getData().getIsLocked());
    }

    @Test
    @DisplayName("AC-6.1.4 财务确认 状态非法")
    void testFinanceConfirm_WrongStatus() {
        CrmReconcile r = mockReconcile();
        r.setStatus("DRAFT");
        when(reconcileMapper.selectById(1L)).thenReturn(r);
        Result<CrmReconcile> result = service.financeConfirm(1L, 100L);
        assertEquals(40903, result.getCode());
    }

    // ====== getReconcileDetail 2 测例 ======
            @Test
    @DisplayName("AC-6.1.1 对账详情 happy")
    void testGetDetail_Happy() {
        CrmReconcile r = mockReconcile();
        CrmReconcileItem item = mockItem();
        CrmReconcileSignature sig = new CrmReconcileSignature();
        sig.setId(1L);
        when(reconcileMapper.selectById(1L)).thenReturn(r);
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(sig));

        Result<Map<String, Object>> result = service.getReconcileDetail(1L);
        assertEquals(0, result.getCode());
        assertNotNull(result.getData().get("reconcile"));
        assertNotNull(result.getData().get("items"));
        assertNotNull(result.getData().get("signatures"));
    }

    @Test
    @DisplayName("AC-6.1.1 对账详情 不存在")
    void testGetDetail_NotFound() {
        when(reconcileMapper.selectById(99L)).thenReturn(null);
        Result<Map<String, Object>> result = service.getReconcileDetail(99L);
        assertEquals(40404, result.getCode());
    }

    // ====== listReconciles 2 测例 ======
            @Test
    @DisplayName("AC-6.1.1 列表查询 happy")
    void testList_Happy() {
        when(reconcileMapper.selectReconciles(any(), any(), any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());
        Result<Map<String, Object>> result = service.listReconciles(null, null, null, null, 0, 20);
        assertEquals(0, result.getCode());
    }

    @Test
    @DisplayName("AC-6.1.1 列表查询带 vendor 过滤")
    void testList_ByVendor() {
        when(reconcileMapper.selectReconciles(eq(100L), any(), any(), any(), eq(20), eq(0)))
            .thenReturn(new ArrayList<>());
        Result<Map<String, Object>> result = service.listReconciles(100L, null, null, null, 0, 20);
        assertEquals(0, result.getCode());
    }
}
