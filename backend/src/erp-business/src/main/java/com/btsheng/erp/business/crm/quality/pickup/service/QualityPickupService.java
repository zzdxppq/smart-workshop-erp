package com.btsheng.erp.business.crm.quality.pickup.service;

import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickup;
import com.btsheng.erp.business.crm.quality.pickup.entity.CrmQualityPickupItem;
import com.btsheng.erp.business.crm.quality.pickup.mapper.CrmQualityPickupItemMapper;
import com.btsheng.erp.business.crm.quality.pickup.mapper.CrmQualityPickupMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.51 · 委外协同·品质领料后质检 Service
 *
 * 3 方法：createPickup / getPickup / inspectPickup
 * 3 P1 修补：领料单唯一 / �?1.50 仓管扫码 + 1.28 品质检�?/ 单一 163 邮箱（AD-3�? */
@Service
public class QualityPickupService {

    /** AD-3 单一 163 邮箱强制（V1.3.7 红线 · 1.51 复用 1.50 规则�?*/
    public static final String REQUIRED_EMAIL_DOMAIN = "@163.com";

    public static final int MAX_ITEM_PER_PICKUP = 50;

    private final CrmQualityPickupMapper pickupMapper;
    private final CrmQualityPickupItemMapper itemMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public QualityPickupService(CrmQualityPickupMapper pickupMapper,
                                 CrmQualityPickupItemMapper itemMapper,
                                 DocNoGenerator docNoGenerator) {
        this.pickupMapper = pickupMapper;
        this.itemMapper = itemMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-12.3.1 创建品质领料�?     * P1 修补 1：领料单唯一
     * P1 修补 3：单一 163 邮箱
     */
    @AuditLog(module = "QUALITY_PICKUP", action = "CREATE")
    @Transactional
    public Result<Map<String, Object>> createPickup(String scanNo, Long inspectorId,
                                                     String inspectorName, String vendorName,
                                                     String email,
                                                     List<CrmQualityPickupItem> items) {
        // P1 修补 3：AD-3 强制 163 邮箱
            if (email == null || !email.endsWith(REQUIRED_EMAIL_DOMAIN)) {
            return Result.fail(40009, "EMAIL_MUST_BE_163");
        }
        if (items == null || items.isEmpty() || items.size() > MAX_ITEM_PER_PICKUP) {
            return Result.fail(40010, "ITEMS_INVALID");
        }
        // P1 修补 1：领料单唯一（按 scanNo 唯一�?
            CrmQualityPickup existing = pickupMapper.selectByScanNo(scanNo);
        if (existing != null) {
            return Result.fail(40011, "PICKUP_EXISTS_FOR_SCAN");
        }

        CrmQualityPickup p = new CrmQualityPickup();
        p.setPickupNo(docNoGenerator.nextQualityPickupNo());
        p.setScanNo(scanNo);
        p.setInspectorId(inspectorId);
        p.setInspectorName(inspectorName);
        p.setVendorName(vendorName);
        p.setPickupType("POST_PICKUP");
        p.setInspectStatus("PENDING");
        p.setTotalCount(items.size());
        p.setPassCount(0);
        p.setFailCount(0);
        p.setEmail(email);
        p.setPickupTime(LocalDateTime.now());
        p.setCreatedAt(LocalDateTime.now());
        pickupMapper.insert(p);

        for (CrmQualityPickupItem it : items) {
            it.setPickupNo(p.getPickupNo());
            it.setInspectResult("PENDING");
            if (it.getPickupItemNo() == null) {
                it.setPickupItemNo("QI" + p.getPickupNo().substring(2) + "-" + String.format("%04d", items.indexOf(it) + 1));
            }
            it.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(it);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("pickup", p);
        data.put("itemCount", items.size());
        return Result.ok(data);
    }

    /**
     * AC-12.3.2 查询领料�?     * P1 修补 2：跨 1.50 仓管扫码 + 1.28 品质检�?     */
    @AuditLog(module = "QUALITY_PICKUP", action = "GET")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getPickup(String pickupNo) {
        CrmQualityPickup p = pickupMapper.selectByNo(pickupNo);
        if (p == null) {
            return Result.fail(40406, "PICKUP_NOT_FOUND");
        }
        List<CrmQualityPickupItem> items = itemMapper.selectByPickupNo(pickupNo);
        Map<String, Object> data = new HashMap<>();
        data.put("pickup", p);
        data.put("items", items);
        return Result.ok(data);
    }

    /**
     * AC-12.3.3 执行质检（PENDING→INSPECTED�?     * P1 修补 2：跨 1.50 + 1.28
     */
    @AuditLog(module = "QUALITY_PICKUP", action = "INSPECT")
    @Transactional
    public Result<Map<String, Object>> inspectPickup(String pickupNo,
                                                       List<CrmQualityPickupItem> inspectResults) {
        CrmQualityPickup p = pickupMapper.selectByNo(pickupNo);
        if (p == null) {
            return Result.fail(40406, "PICKUP_NOT_FOUND");
        }
        if (!"PENDING".equals(p.getInspectStatus())) {
            return Result.fail(40012, "PICKUP_NOT_PENDING");
        }
        int passCount = 0;
        int failCount = 0;
        for (CrmQualityPickupItem r : inspectResults) {
            r.setInspectTime(LocalDateTime.now());
            itemMapper.updateById(r);
            if ("PASS".equals(r.getInspectResult())) passCount++;
            else if ("FAIL".equals(r.getInspectResult())) failCount++;
        }
        p.setPassCount(passCount);
        p.setFailCount(failCount);
        p.setInspectStatus("INSPECTED");
        p.setInspectedAt(LocalDateTime.now());
        pickupMapper.updateById(p);

        Map<String, Object> data = new HashMap<>();
        data.put("pickup", p);
        return Result.ok(data);
    }
}
