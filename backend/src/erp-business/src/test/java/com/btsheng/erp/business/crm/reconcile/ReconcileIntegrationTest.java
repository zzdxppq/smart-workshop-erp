package com.btsheng.erp.business.crm.reconcile;

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
import com.btsheng.erp.business.crm.reconcile.service.ReconcileService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * V1.3.7 · Story 1.21 · ReconcileService 集成测例 (6 测例)
 *
 * 覆盖：full_lifecycle_draft_to_closed / vendor_confirm_with_signature / amount_mismatch_blocks_advance
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ReconcileIntegrationTest {

    @Mock private CrmReconcileMapper reconcileMapper;
    @Mock private CrmReconcileItemMapper itemMapper;
    @Mock private CrmReconcileSignatureMapper signatureMapper;
    @Mock private DocNoGenerator docNoGenerator;
    @Mock private DrawingEncryptionService encryptionService;

    private ReconcileService service;

    @BeforeEach
    void setUp() {
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

        when(encryptionService.encrypt(any(byte[].class))).thenAnswer(inv -> {
            byte[] plain = inv.getArgument(0);
            byte[] combined = new byte[12 + 16 + plain.length];
            return Base64.getEncoder().encodeToString(combined);
        });

        service = new ReconcileService(reconcileMapper, itemMapper, signatureMapper, docNoGenerator, encryptionService);
    }

    private CrmReconcile mockReconcile(String status, int step) {
        CrmReconcile r = new CrmReconcile();
        r.setId(1L);
        r.setReconcileNo("RC202606-0001");
        r.setVendorId(100L);
        r.setVendorName("苏州精工外协厂");
        r.setPeriodYear(2026);
        r.setPeriodMonth(6);
        r.setTotalAmount(new BigDecimal("150.00"));
        r.setStatus(status);
        r.setCurrentStep(step);
        r.setIsLocked(0);
        r.setCreatedBy(1L);
        return r;
    }

    // ====== full_lifecycle_draft_to_closed 2 测例 ======
            @Test
    @DisplayName("AC-6.1.1+6.1.2+6.1.4 端到端：DRAFT → VENDOR_CONFIRMED → BOTH_CONFIRMED → FINANCE_CONFIRMED → CLOSED")
    void testFullLifecycle_DraftToClosed() {
        // Step 1: 创建
            ReconcileCreateRequest create = new ReconcileCreateRequest();
        create.setVendorId(100L);
        create.setVendorName("苏州精工外协厂");
        create.setPeriodYear(2026);
        create.setPeriodMonth(6);
        ReconcileItemRequest itemReq = new ReconcileItemRequest();
        itemReq.setOutsourceOrderId(1L);
        itemReq.setOutsourceOrderNo("WW20260612-0001");
        itemReq.setItemName("输出轴");
        itemReq.setQuantity(10);
        itemReq.setUnitPrice(new BigDecimal("15.00"));
        create.setItems(List.of(itemReq));

        Result<CrmReconcile> cr = service.createReconcile(create, 1L);
        assertEquals(0, cr.getCode());
        CrmReconcile created = cr.getData();
        assertEquals("DRAFT", created.getStatus());

        // Step 2: 厂商确认（mock 出已签字）
            CrmReconcileItem item = new CrmReconcileItem();
        item.setId(1L);
        item.setReconcileId(1L);
        item.setAmount(new BigDecimal("150.00"));

        CrmReconcileSignature sig = new CrmReconcileSignature();
        sig.setId(1L);
        sig.setReconcileId(1L);
        sig.setSignerName("厂商张总");

        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("DRAFT", 1));
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(sig));

        ReconcileVendorConfirmRequest vc = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("150.00"));
        vc.setVendorAmounts(List.of(va));

        Result<CrmReconcile> vcr = service.vendorConfirm(1L, vc, 100L);
        assertEquals(0, vcr.getCode());
        assertEquals("VENDOR_CONFIRMED", vcr.getData().getStatus());
        assertEquals(2, vcr.getData().getCurrentStep());

        // Step 3: 双方确认
            when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("VENDOR_CONFIRMED", 2));
        Result<CrmReconcile> bcr = service.bothConfirm(1L, 100L);
        assertEquals(0, bcr.getCode());
        assertEquals("BOTH_CONFIRMED", bcr.getData().getStatus());
        assertEquals(3, bcr.getData().getCurrentStep());

        // Step 4: 财务确认 → CLOSED
            when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("BOTH_CONFIRMED", 3));
        Result<CrmReconcile> fcr = service.financeConfirm(1L, 100L);
        assertEquals(0, fcr.getCode());
        assertEquals("CLOSED", fcr.getData().getStatus());
        assertEquals(4, fcr.getData().getCurrentStep());
        assertEquals(1, fcr.getData().getIsLocked());
    }

    @Test
    @DisplayName("AC-6.1.4 4 步状态机不可跳跃（DRAFT 不能直接 finance_confirm）")
    void testFullLifecycle_SkipStep_Blocked() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("DRAFT", 1));
        Result<CrmReconcile> r = service.financeConfirm(1L, 100L);
        assertEquals(40903, r.getCode());
        assertEquals("RECONCILE_NOT_BOTH_CONFIRMED", r.getMessage());
    }

    // ====== vendor_confirm_with_signature 2 测例 ======
            @Test
    @DisplayName("AC-6.1.3 厂商签字必传（P1 修补 3 · 红线）")
    void testVendorConfirm_RequiresSignature() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("DRAFT", 1));
        CrmReconcileItem item = new CrmReconcileItem();
        item.setId(1L);
        item.setAmount(new BigDecimal("150.00"));
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(new ArrayList<>());  // 关键：无签字
            ReconcileVendorConfirmRequest vc = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("150.00"));
        vc.setVendorAmounts(List.of(va));

        Result<CrmReconcile> r = service.vendorConfirm(1L, vc, 100L);
        assertEquals(40001, r.getCode());
        assertEquals("VENDOR_SIGNATURE_REQUIRED", r.getMessage());
    }

    @Test
    @DisplayName("AC-6.1.3 厂商签字 → vendor_confirm 一致性")
    void testVendorConfirm_WithSignature() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("DRAFT", 1));
        CrmReconcileItem item = new CrmReconcileItem();
        item.setId(1L);
        item.setAmount(new BigDecimal("150.00"));
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        // 已上传 1 个签字
            CrmReconcileSignature sig = new CrmReconcileSignature();
        sig.setId(1L);
        sig.setSignerName("厂商张总");
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(sig));

        ReconcileVendorConfirmRequest vc = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("150.00"));
        vc.setVendorAmounts(List.of(va));

        Result<CrmReconcile> r = service.vendorConfirm(1L, vc, 100L);
        assertEquals(0, r.getCode());
        assertEquals("VENDOR_CONFIRMED", r.getData().getStatus());
    }

    // ====== amount_mismatch_blocks_advance 2 测例 ======
            @Test
    @DisplayName("AC-6.1.2 金额不一致 → 40905 · 阻止推进 step 2")
    void testAmountMismatch_BlocksAdvance_40905() {
        when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("DRAFT", 1));
        CrmReconcileItem item = new CrmReconcileItem();
        item.setId(1L);
        item.setAmount(new BigDecimal("150.00"));
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        CrmReconcileSignature sig = new CrmReconcileSignature();
        sig.setId(1L);
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(sig));

        ReconcileVendorConfirmRequest vc = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va.setItemId(1L);
        va.setVendorAmount(new BigDecimal("100.00"));  // 不一致
            vc.setVendorAmounts(List.of(va));

        Result<CrmReconcile> r = service.vendorConfirm(1L, vc, 100L);
        assertEquals(40905, r.getCode());
        // 状态不前进
            verify(reconcileMapper, never()).updateById(argThat((CrmReconcile rr) ->
            "VENDOR_CONFIRMED".equals(rr.getStatus()) || "BOTH_CONFIRMED".equals(rr.getStatus())
        ));
    }

    @Test
    @DisplayName("P1 修补 1 40905 后再次以正确金额提交可推进")
    void testAmountMismatch_RetryAfterFix() {
        // 第一次：不匹配
            when(reconcileMapper.selectById(1L)).thenReturn(mockReconcile("DRAFT", 1));
        CrmReconcileItem item = new CrmReconcileItem();
        item.setId(1L);
        item.setAmount(new BigDecimal("150.00"));
        when(itemMapper.selectByReconcileId(1L)).thenReturn(List.of(item));
        CrmReconcileSignature sig = new CrmReconcileSignature();
        sig.setId(1L);
        when(signatureMapper.selectByReconcileId(1L)).thenReturn(List.of(sig));

        ReconcileVendorConfirmRequest vc1 = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va1 = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va1.setItemId(1L);
        va1.setVendorAmount(new BigDecimal("100.00"));
        vc1.setVendorAmounts(List.of(va1));

        Result<CrmReconcile> r1 = service.vendorConfirm(1L, vc1, 100L);
        assertEquals(40905, r1.getCode());

        // 第二次：修正后正确金额
            ReconcileVendorConfirmRequest vc2 = new ReconcileVendorConfirmRequest();
        ReconcileVendorConfirmRequest.VendorAmountItem va2 = new ReconcileVendorConfirmRequest.VendorAmountItem();
        va2.setItemId(1L);
        va2.setVendorAmount(new BigDecimal("150.00"));
        vc2.setVendorAmounts(List.of(va2));

        Result<CrmReconcile> r2 = service.vendorConfirm(1L, vc2, 100L);
        assertEquals(0, r2.getCode());
        assertEquals("VENDOR_CONFIRMED", r2.getData().getStatus());
    }
}
