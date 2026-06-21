package com.btsheng.erp.business.crm.purchaseinspection.service;

import com.btsheng.erp.business.crm.purchaseinspection.dto.AddItemRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.CreateInspectionRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.SubmitResultRequest;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingInspection;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingItem;
import com.btsheng.erp.business.crm.purchaseinspection.mapper.CrmPurchaseIncomingInspectionMapper;
import com.btsheng.erp.business.crm.purchaseinspection.mapper.CrmPurchaseIncomingItemMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.35 · 采购·来料质检 Service (FR-8-4)
 *
 * <p>5 业务方法：create / addItem / pass / reject / list
 * <p>模板：PI{yyyyMMdd}{seq:4}
 * <p>3 P1 修补：单一 163 邮箱（AD-3�? 抽样 AQL / 不良�?> 10% 阻断入库
 */
@Service
public class PurchaseIncomingInspectionService {

    public static final String RESULT_PENDING = "PENDING";
    public static final String RESULT_PASS = "PASS";
    public static final String RESULT_REJECT = "REJECT";

    /** P1 修补 1：单一 163 邮箱 AD-3 */
    public static final String AD_EMAIL_163 = "inspect@btsheng-163.com";

    /** P1 修补 3：不良率 > 10% 阻断入库 */
    public static final BigDecimal DEFECT_RATE_BLOCK = new BigDecimal("10.00");

    private final CrmPurchaseIncomingInspectionMapper inspectionMapper;
    private final CrmPurchaseIncomingItemMapper itemMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public PurchaseIncomingInspectionService(CrmPurchaseIncomingInspectionMapper inspectionMapper,
                                             CrmPurchaseIncomingItemMapper itemMapper,
                                             DocNoGenerator docNoGenerator) {
        this.inspectionMapper = inspectionMapper;
        this.itemMapper = itemMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * 创建来料质检�?     * P1 修补 1：notifyEmail 必须为单一 163 邮箱
     * P1 修补 4：唯一索引 (po_id, material_id) 兜底
     */
    @Transactional
    @AuditLog(module = "purchase_inspection", action = "purchase_inspection.create")
    public Result<CrmPurchaseIncomingInspection> create(CreateInspectionRequest req, Long operatorUserId) {
        if (req == null || req.getPoId() == null || req.getPoNo() == null) {
            return Result.fail(40001, "PO_REQUIRED");
        }
        if (req.getMaterialId() == null) {
            return Result.fail(40001, "MATERIAL_ID_REQUIRED");
        }
        if (req.getInspectorId() == null) {
            return Result.fail(40001, "INSPECTOR_REQUIRED");
        }
        if (req.getSampleSize() == null || req.getSampleSize() <= 0) {
            return Result.fail(40001, "SAMPLE_SIZE_INVALID");
        }
        // P1 修补 1：单一 163 邮箱 AD-3
            String email = req.getNotifyEmail() == null || req.getNotifyEmail().isEmpty()
                ? AD_EMAIL_163 : req.getNotifyEmail();
        if (!email.endsWith("@btsheng-163.com")) {
            return Result.fail(40003, "EMAIL_NOT_163_AD3");
        }
        // P1 修补 4：唯一 (po_id, material_id)
            CrmPurchaseIncomingInspection existed = inspectionMapper.selectByPoAndMaterial(req.getPoId(), req.getMaterialId());
        if (existed != null) {
            return Result.fail(40902, "INSPECTION_DUPLICATE");
        }

        CrmPurchaseIncomingInspection ins = new CrmPurchaseIncomingInspection();
        ins.setInspectionNo(docNoGenerator.nextPurchaseIncomingInspectionNo());
        ins.setIncomingId(req.getIncomingId());
        ins.setPoId(req.getPoId());
        ins.setPoNo(req.getPoNo());
        ins.setVendorId(req.getVendorId());
        ins.setVendorName(req.getVendorName());
        ins.setMaterialId(req.getMaterialId());
        ins.setMaterialCode(req.getMaterialCode());
        ins.setMaterialName(req.getMaterialName());
        ins.setBatchNo(req.getBatchNo());
        ins.setInspectorId(req.getInspectorId());
        ins.setInspectorName(req.getInspectorName());
        ins.setSampleSize(req.getSampleSize());
        ins.setSamplePass(0);
        ins.setSampleFail(0);
        ins.setAqlLevel(req.getAqlLevel() == null ? "II" : req.getAqlLevel());
        ins.setResult(RESULT_PENDING);
        ins.setNotifyEmail(email);
        ins.setRemark(req.getRemark());
        ins.setCreatedAt(LocalDateTime.now());
        ins.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.insert(ins);
        return Result.ok(ins);
    }

    /**
     * 添加检验项（累�?pass/fail �?inspection�?     * P1 修补 2：抽�?AQL 等级
     * 关键�?FAIL �?整单 REJECT
     */
    @Transactional
    @AuditLog(module = "purchase_inspection", action = "purchase_inspection.add_item")
    public Result<CrmPurchaseIncomingItem> addItem(Long inspectionId, AddItemRequest req) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        if (req == null || req.getCheckItem() == null) {
            return Result.fail(40001, "CHECK_ITEM_REQUIRED");
        }
        if (req.getSampleQty() == null || req.getSampleQty() <= 0) {
            return Result.fail(40001, "SAMPLE_QTY_INVALID");
        }
        CrmPurchaseIncomingInspection ins = inspectionMapper.selectById(inspectionId);
        if (ins == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (RESULT_PASS.equals(ins.getResult()) || RESULT_REJECT.equals(ins.getResult())) {
            return Result.fail(40903, "INSPECTION_FINALIZED");
        }

        // 下一�?seq
            List<CrmPurchaseIncomingItem> existing = itemMapper.selectByInspectionId(inspectionId);
        int nextSeq = existing == null ? 1 : existing.size() + 1;

        CrmPurchaseIncomingItem it = new CrmPurchaseIncomingItem();
        it.setInspectionId(inspectionId);
        it.setSeqNo(nextSeq);
        it.setCheckItem(req.getCheckItem());
        it.setStandard(req.getStandard());
        it.setSampleQty(req.getSampleQty());
        it.setPassQty(req.getPassQty() == null ? 0 : req.getPassQty());
        it.setFailQty(req.getFailQty() == null ? 0 : req.getFailQty());
        it.setIsCritical(req.getIsCritical() == null ? 0 : req.getIsCritical());
        it.setRemark(req.getRemark());
        // item result
            String itemResult = (it.getFailQty() != null && it.getFailQty() > 0) ? "FAIL" : "PASS";
        it.setResult(itemResult);
        itemMapper.insert(it);

        // 累加
            ins.setSamplePass(ins.getSamplePass() == null ? it.getPassQty() : ins.getSamplePass() + it.getPassQty());
        ins.setSampleFail(ins.getSampleFail() == null ? it.getFailQty() : ins.getSampleFail() + it.getFailQty());
        // 不良�?= fail / (pass+fail) * 100
            int total = ins.getSamplePass() + ins.getSampleFail();
        if (total > 0) {
            BigDecimal rate = new BigDecimal(ins.getSampleFail())
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
            ins.setDefectRate(rate);
        }
        // 关键�?FAIL �?REJECT
            if (itemResult.equals("FAIL") && Integer.valueOf(1).equals(it.getIsCritical())) {
            ins.setResult(RESULT_REJECT);
            ins.setInspectedAt(LocalDateTime.now());
            ins.setRemark("关键�?FAIL · 1 票否�?· " + (ins.getRemark() == null ? "" : ins.getRemark()));
        }
        ins.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(ins);
        return Result.ok(it);
    }

    /**
     * 提交结论 PASS · 允许入库
     * P1 修补 3：不良率 > 10% 阻断入库
     */
    @Transactional
    @AuditLog(module = "purchase_inspection", action = "purchase_inspection.pass")
    public Result<CrmPurchaseIncomingInspection> pass(Long inspectionId, SubmitResultRequest req) {
        return submitFinal(inspectionId, req, RESULT_PASS);
    }

    /**
     * 提交结论 REJECT · 拒收
     */
    @Transactional
    @AuditLog(module = "purchase_inspection", action = "purchase_inspection.reject")
    public Result<CrmPurchaseIncomingInspection> reject(Long inspectionId, SubmitResultRequest req) {
        return submitFinal(inspectionId, req, RESULT_REJECT);
    }

    private Result<CrmPurchaseIncomingInspection> submitFinal(Long inspectionId, SubmitResultRequest req, String finalResult) {
        if (inspectionId == null) {
            return Result.fail(40001, "INSPECTION_ID_REQUIRED");
        }
        CrmPurchaseIncomingInspection ins = inspectionMapper.selectById(inspectionId);
        if (ins == null) {
            return Result.fail(40404, "INSPECTION_NOT_FOUND");
        }
        if (RESULT_PASS.equals(ins.getResult()) || RESULT_REJECT.equals(ins.getResult())) {
            return Result.fail(40903, "INSPECTION_FINALIZED");
        }
        if (req != null) {
            if (req.getSamplePass() != null) ins.setSamplePass(req.getSamplePass());
            if (req.getSampleFail() != null) ins.setSampleFail(req.getSampleFail());
        }
        int total = (ins.getSamplePass() == null ? 0 : ins.getSamplePass())
                + (ins.getSampleFail() == null ? 0 : ins.getSampleFail());
        if (total > 0) {
            BigDecimal rate = new BigDecimal(ins.getSampleFail() == null ? 0 : ins.getSampleFail())
                    .multiply(new BigDecimal(100))
                    .divide(new BigDecimal(total), 2, RoundingMode.HALF_UP);
            ins.setDefectRate(rate);
        }

        // P1 修补 3：不良率 > 10% 阻断入库（即便用户选择 PASS�?
            if (RESULT_PASS.equals(finalResult) && ins.getDefectRate() != null
                && ins.getDefectRate().compareTo(DEFECT_RATE_BLOCK) > 0) {
            return Result.fail(40909, "DEFECT_RATE_OVER_10_BLOCK");
        }

        ins.setResult(finalResult);
        ins.setInspectedAt(LocalDateTime.now());
        if (req != null && req.getRemark() != null) {
            ins.setRemark(req.getRemark());
        }
        ins.setUpdatedAt(LocalDateTime.now());
        inspectionMapper.updateById(ins);
        return Result.ok(ins);
    }

    /**
     * 列出全部质检�?     */
    @AuditLog(module = "purchase_inspection", action = "purchase_inspection.list")
    public Result<List<Map<String, Object>>> list() {
        List<CrmPurchaseIncomingInspection> ins = inspectionMapper.selectAll();
        List<Map<String, Object>> out = new ArrayList<>();
        if (ins == null) return Result.ok(out);
        for (CrmPurchaseIncomingInspection i : ins) {
            Map<String, Object> m = new HashMap<>();
            m.put("inspection", i);
            m.put("items", itemMapper.selectByInspectionId(i.getId()));
            out.add(m);
        }
        return Result.ok(out);
    }
}
