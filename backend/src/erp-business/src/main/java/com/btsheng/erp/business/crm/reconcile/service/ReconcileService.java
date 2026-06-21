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
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.21 · 月度对账 Service (FR-6-1)
 *
 * 7 业务方法：createReconcile / addItem / uploadSignature / vendorConfirm / financeConfirm / getReconcileDetail / listReconciles
 * 对账单号：RC{yyyyMM}{seq:4}（按月隔离）
 * 5 状态机：DRAFT/VENDOR_CONFIRMED/BOTH_CONFIRMED/FINANCE_CONFIRMED/CLOSED
 * 3 P1 修补：不�?线下"动作（V1.3.7 AD-2�? 40905 金额不一�?/ 厂商签字必传
 * 4 步流程：草稿(1) �?厂商确认(2) �?双方确认(3) �?财务确认(4) �?CLOSED
 */
@Service
public class ReconcileService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_VENDOR_CONFIRMED = "VENDOR_CONFIRMED";
    public static final String STATUS_BOTH_CONFIRMED = "BOTH_CONFIRMED";
    public static final String STATUS_FINANCE_CONFIRMED = "FINANCE_CONFIRMED";
    public static final String STATUS_CLOSED = "CLOSED";

    private final CrmReconcileMapper reconcileMapper;
    private final CrmReconcileItemMapper itemMapper;
    private final CrmReconcileSignatureMapper signatureMapper;
    private final DocNoGenerator docNoGenerator;
    private final DrawingEncryptionService encryptionService;

    @Autowired
    public ReconcileService(CrmReconcileMapper reconcileMapper,
                            CrmReconcileItemMapper itemMapper,
                            CrmReconcileSignatureMapper signatureMapper,
                            DocNoGenerator docNoGenerator,
                            DrawingEncryptionService encryptionService) {
        this.reconcileMapper = reconcileMapper;
        this.itemMapper = itemMapper;
        this.signatureMapper = signatureMapper;
        this.docNoGenerator = docNoGenerator;
        this.encryptionService = encryptionService;
    }

    /**
     * AC-6.1.1 创建月度对账单（草稿状态）
     */
    @Transactional
    @AuditLog(module = "reconcile", action = "reconcile.create")
    public Result<CrmReconcile> createReconcile(ReconcileCreateRequest req, Long operatorUserId) {
        if (req.getVendorId() == null) {
            return Result.fail(40001, "VENDOR_ID_REQUIRED");
        }
        if (req.getVendorName() == null || req.getVendorName().isEmpty()) {
            return Result.fail(40001, "VENDOR_NAME_REQUIRED");
        }
        if (req.getPeriodYear() == null || req.getPeriodYear() < 2000) {
            return Result.fail(40001, "PERIOD_YEAR_INVALID");
        }
        if (req.getPeriodMonth() == null || req.getPeriodMonth() < 1 || req.getPeriodMonth() > 12) {
            return Result.fail(40001, "PERIOD_MONTH_INVALID");
        }

        String reconcileNo = docNoGenerator.nextReconcileNo();

        CrmReconcile reconcile = new CrmReconcile();
        reconcile.setReconcileNo(reconcileNo);
        reconcile.setVendorId(req.getVendorId());
        reconcile.setVendorName(req.getVendorName());
        reconcile.setPeriodYear(req.getPeriodYear());
        reconcile.setPeriodMonth(req.getPeriodMonth());
        reconcile.setStatus(STATUS_DRAFT);
        reconcile.setCurrentStep(1);
        reconcile.setIsLocked(0);
        reconcile.setCreatedBy(operatorUserId);
        reconcile.setCreatedAt(LocalDateTime.now());
        reconcile.setUpdatedAt(LocalDateTime.now());
        reconcileMapper.insert(reconcile);

        BigDecimal total = BigDecimal.ZERO;
        if (req.getItems() != null) {
            int sort = 1;
            for (ReconcileItemRequest itemReq : req.getItems()) {
                BigDecimal amount = itemReq.getUnitPrice().multiply(new BigDecimal(itemReq.getQuantity()));
                CrmReconcileItem item = new CrmReconcileItem();
                item.setReconcileId(reconcile.getId());
                item.setOutsourceOrderId(itemReq.getOutsourceOrderId());
                item.setOutsourceOrderNo(itemReq.getOutsourceOrderNo());
                item.setItemName(itemReq.getItemName());
                item.setQuantity(itemReq.getQuantity());
                item.setUnitPrice(itemReq.getUnitPrice());
                item.setAmount(amount);
                item.setSort(sort++);
                itemMapper.insert(item);
                total = total.add(amount);
            }
        }
        reconcile.setTotalAmount(total);
        reconcileMapper.updateById(reconcile);

        return Result.ok(reconcile);
    }

    /**
     * AC-6.1.1 追加对账明细
     */
    @Transactional
    public Result<CrmReconcileItem> addItem(Long reconcileId, ReconcileItemRequest req) {
        CrmReconcile reconcile = reconcileMapper.selectById(reconcileId);
        if (reconcile == null) {
            return Result.fail(40404, "RECONCILE_NOT_FOUND");
        }
        if (STATUS_CLOSED.equals(reconcile.getStatus())) {
            return Result.fail(40903, "RECONCILE_CLOSED");
        }
        BigDecimal amount = req.getUnitPrice().multiply(new BigDecimal(req.getQuantity()));
        CrmReconcileItem item = new CrmReconcileItem();
        item.setReconcileId(reconcileId);
        item.setOutsourceOrderId(req.getOutsourceOrderId());
        item.setOutsourceOrderNo(req.getOutsourceOrderNo());
        item.setItemName(req.getItemName());
        item.setQuantity(req.getQuantity());
        item.setUnitPrice(req.getUnitPrice());
        item.setAmount(amount);
        item.setSort(99);
        itemMapper.insert(item);

        // 累加总金�?
            reconcile.setTotalAmount(reconcile.getTotalAmount() == null ? amount : reconcile.getTotalAmount().add(amount));
        reconcile.setUpdatedAt(LocalDateTime.now());
        reconcileMapper.updateById(reconcile);

        return Result.ok(item);
    }

    /**
     * AC-6.1.3 上传厂商签字扫描件（AES-256-GCM 加密�?     */
    @Transactional
    @AuditLog(module = "reconcile", action = "reconcile.upload_signature")
    public Result<CrmReconcileSignature> uploadSignature(Long reconcileId, byte[] imageBytes, String signerName, Long signerUserId) {
        CrmReconcile reconcile = reconcileMapper.selectById(reconcileId);
        if (reconcile == null) {
            return Result.fail(40404, "RECONCILE_NOT_FOUND");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            return Result.fail(40001, "SIGNATURE_IMAGE_REQUIRED");
        }
        if (signerName == null || signerName.isEmpty()) {
            return Result.fail(40001, "SIGNER_NAME_REQUIRED");
        }

        String encrypted = encryptionService.encrypt(imageBytes);
        // 从加密结果中�?IV �?auth tag（结构：iv(12 bytes) + ciphertext(�?16 byte tag)�?
            byte[] all = Base64.getDecoder().decode(encrypted);
        String iv = Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(all, 0, 12));
        // GCM tag 是密文末�?16 字节
            String authTag = Base64.getEncoder().encodeToString(java.util.Arrays.copyOfRange(all, all.length - 16, all.length));

        CrmReconcileSignature sig = new CrmReconcileSignature();
        sig.setReconcileId(reconcileId);
        sig.setSignerUserId(signerUserId);
        sig.setSignerName(signerName);
        sig.setSignatureImagePath("/signatures/reconcile/" + reconcileId + "_" + System.currentTimeMillis() + ".bin");
        sig.setEncryptedData(encrypted);
        sig.setIv(iv);
        sig.setAuthTag(authTag);
        sig.setSignedAt(LocalDateTime.now());
        signatureMapper.insert(sig);

        return Result.ok(sig);
    }

    /**
     * AC-6.1.2 厂商对账确认（含金额校验 · 40905�?     * 同时要求厂商签字扫描件必传（P1 修补 3�?     */
    @Transactional
    @AuditLog(module = "reconcile", action = "reconcile.vendor_confirm")
    public Result<CrmReconcile> vendorConfirm(Long reconcileId, ReconcileVendorConfirmRequest req, Long operatorUserId) {
        CrmReconcile reconcile = reconcileMapper.selectById(reconcileId);
        if (reconcile == null) {
            return Result.fail(40404, "RECONCILE_NOT_FOUND");
        }
        if (!STATUS_DRAFT.equals(reconcile.getStatus())) {
            return Result.fail(40903, "RECONCILE_NOT_DRAFT");
        }
        if (req.getVendorAmounts() == null || req.getVendorAmounts().isEmpty()) {
            return Result.fail(40001, "VENDOR_AMOUNTS_REQUIRED");
        }

        // 厂商签字必传（P1 修补 3�?
            List<CrmReconcileSignature> sigs = signatureMapper.selectByReconcileId(reconcileId);
        if (sigs == null || sigs.isEmpty()) {
            return Result.fail(40001, "VENDOR_SIGNATURE_REQUIRED");
        }

        // 校验金额一致性（40905�?
            List<CrmReconcileItem> items = itemMapper.selectByReconcileId(reconcileId);
        Map<Long, BigDecimal> vendorMap = new HashMap<>();
        for (ReconcileVendorConfirmRequest.VendorAmountItem va : req.getVendorAmounts()) {
            vendorMap.put(va.getItemId(), va.getVendorAmount());
        }
        boolean allConsistent = true;
        for (CrmReconcileItem item : items) {
            BigDecimal vendorAmount = vendorMap.get(item.getId());
            if (vendorAmount == null) {
                return Result.fail(40001, "VENDOR_AMOUNT_MISSING_FOR_ITEM_" + item.getId());
            }
            boolean consistent = vendorAmount.compareTo(item.getAmount()) == 0;
            item.setVendorAmount(vendorAmount);
            item.setFinalAmount(vendorAmount);
            item.setIsConsistent(consistent ? 1 : 0);
            itemMapper.updateById(item);
            if (!consistent) {
                allConsistent = false;
            }
        }
        if (!allConsistent) {
            return Result.fail(40905, "RECONCILE_AMOUNT_INCONSISTENT");
        }

        // 推进�?step 2
            reconcile.setStatus(STATUS_VENDOR_CONFIRMED);
        reconcile.setCurrentStep(2);
        reconcile.setUpdatedAt(LocalDateTime.now());
        reconcileMapper.updateById(reconcile);
        return Result.ok(reconcile);
    }

    /**
     * 双方对账确认（厂�?+ 内部）→ step 3
     */
    @Transactional
    @AuditLog(module = "reconcile", action = "reconcile.both_confirm")
    public Result<CrmReconcile> bothConfirm(Long reconcileId, Long operatorUserId) {
        CrmReconcile reconcile = reconcileMapper.selectById(reconcileId);
        if (reconcile == null) {
            return Result.fail(40404, "RECONCILE_NOT_FOUND");
        }
        if (!STATUS_VENDOR_CONFIRMED.equals(reconcile.getStatus())) {
            return Result.fail(40903, "RECONCILE_NOT_VENDOR_CONFIRMED");
        }
        reconcile.setStatus(STATUS_BOTH_CONFIRMED);
        reconcile.setCurrentStep(3);
        reconcile.setUpdatedAt(LocalDateTime.now());
        reconcileMapper.updateById(reconcile);
        return Result.ok(reconcile);
    }

    /**
     * AC-6.1.4 财务对账确认 �?step 4 �?CLOSED
     */
    @Transactional
    @AuditLog(module = "reconcile", action = "reconcile.finance_confirm")
    public Result<CrmReconcile> financeConfirm(Long reconcileId, Long operatorUserId) {
        CrmReconcile reconcile = reconcileMapper.selectById(reconcileId);
        if (reconcile == null) {
            return Result.fail(40404, "RECONCILE_NOT_FOUND");
        }
        if (!STATUS_BOTH_CONFIRMED.equals(reconcile.getStatus())) {
            return Result.fail(40903, "RECONCILE_NOT_BOTH_CONFIRMED");
        }
        reconcile.setStatus(STATUS_FINANCE_CONFIRMED);
        reconcile.setCurrentStep(4);
        reconcile.setUpdatedAt(LocalDateTime.now());
        reconcileMapper.updateById(reconcile);

        // 财务确认后自�?CLOSED
            reconcile.setStatus(STATUS_CLOSED);
        reconcile.setIsLocked(1);
        reconcileMapper.updateById(reconcile);
        return Result.ok(reconcile);
    }

    /**
     * 获取对账单详情（主单 + 明细 + 签字�?     */
    public Result<Map<String, Object>> getReconcileDetail(Long id) {
        CrmReconcile reconcile = reconcileMapper.selectById(id);
        if (reconcile == null) {
            return Result.fail(40404, "RECONCILE_NOT_FOUND");
        }
        Map<String, Object> detail = new HashMap<>();
        detail.put("reconcile", reconcile);
        detail.put("items", itemMapper.selectByReconcileId(id));
        detail.put("signatures", signatureMapper.selectByReconcileId(id));
        return Result.ok(detail);
    }

    /**
     * 列表查询（按 vendor / period / status 过滤�?     */
    public Result<Map<String, Object>> listReconciles(Long vendorId, Integer periodYear, Integer periodMonth, String status, int page, int size) {
        int limit = size > 0 ? size : 20;
        int offset = Math.max(page, 0) * limit;
        List<Map<String, Object>> list = reconcileMapper.selectReconciles(vendorId, periodYear, periodMonth, status, limit, offset);
        Map<String, Object> p = new HashMap<>();
        p.put("list", list);
        p.put("page", page);
        p.put("size", limit);
        return Result.ok(p);
    }
}
