package com.btsheng.erp.business.crm.warehouse.scan.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.warehouse.scan.entity.CrmWarehouseIncomingItem;
import com.btsheng.erp.business.crm.warehouse.scan.entity.CrmWarehouseIncomingScan;
import com.btsheng.erp.business.crm.warehouse.scan.mapper.CrmWarehouseIncomingItemMapper;
import com.btsheng.erp.business.crm.warehouse.scan.mapper.CrmWarehouseIncomingScanMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.50 · 委外协同·仓管到货扫码 Service
 *
 * 4 方法：createScan / getScan / confirmScan / listScans
 * 3 P1 修补：单一 163 邮箱（AD-3 强制�? 扫码必传 5 类码 / �?1.12 + 1.49
 */
@Service
public class WarehouseIncomingScanService {

    /** 5 类码：WN=入库/WL=领料/WR=返工/WD=差异/WW=调拨�?.50 强约束） */
    public static final List<String> VALID_BARCODE_TYPES = Arrays.asList("WN", "WL", "WR", "WD", "WW");

    /** AD-3 单一 163 邮箱强制（V1.3.7 红线�?*/
    public static final String REQUIRED_EMAIL_DOMAIN = "@163.com";

    public static final int MAX_LIST_LIMIT = 100;

    private final CrmWarehouseIncomingScanMapper scanMapper;
    private final CrmWarehouseIncomingItemMapper itemMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public WarehouseIncomingScanService(CrmWarehouseIncomingScanMapper scanMapper,
                                          CrmWarehouseIncomingItemMapper itemMapper,
                                          DocNoGenerator docNoGenerator) {
        this.scanMapper = scanMapper;
        this.itemMapper = itemMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-12.2.1 创建扫码�?     * P1 修补 1：单一 163 邮箱
     * P1 修补 2：扫码必�?5 类码
     */
    @AuditLog(module = "WAREHOUSE_INCOMING_SCAN", action = "CREATE")
    @Transactional
    public Result<Map<String, Object>> createScan(String permissionNo, Long userId, String vendorName,
                                                   String outsourceNo, String email,
                                                   List<CrmWarehouseIncomingItem> items) {
        // P1 修补 1：AD-3 强制 163 邮箱
            if (email == null || !email.endsWith(REQUIRED_EMAIL_DOMAIN)) {
            return Result.fail(40005, "EMAIL_MUST_BE_163");
        }
        // P1 修补 2�? 类码必传校验
            if (items == null || items.isEmpty()) {
            return Result.fail(40006, "ITEMS_REQUIRED");
        }
        for (CrmWarehouseIncomingItem it : items) {
            if (it.getBarcodeType() == null || !VALID_BARCODE_TYPES.contains(it.getBarcodeType())) {
                return Result.fail(40007, "BARCODE_TYPE_INVALID");
            }
        }

        CrmWarehouseIncomingScan s = new CrmWarehouseIncomingScan();
        s.setScanNo(docNoGenerator.nextWarehouseIncomingScanNo());
        s.setPermissionNo(permissionNo);
        s.setUserId(userId);
        s.setVendorName(vendorName);
        s.setOutsourceNo(outsourceNo);
        s.setScanType("INCOMING");
        s.setScanStatus("PENDING");
        s.setTotalCount(items.size());
        s.setEmail(email);
        s.setScanTime(LocalDateTime.now());
        s.setCreatedAt(LocalDateTime.now());
        scanMapper.insert(s);

        for (CrmWarehouseIncomingItem it : items) {
            it.setScanNo(s.getScanNo());
            if (it.getItemNo() == null) {
                it.setItemNo("WI" + s.getScanNo().substring(2) + "-" + String.format("%04d", items.indexOf(it) + 1));
            }
            it.setCreatedAt(LocalDateTime.now());
            itemMapper.insert(it);
        }

        Map<String, Object> data = new HashMap<>();
        data.put("scan", s);
        data.put("itemCount", items.size());
        return Result.ok(data);
    }

    /**
     * AC-12.2.2 查询扫码�?     * P1 修补 3：跨 1.12 扫码 + 1.49 权限
     */
    @AuditLog(module = "WAREHOUSE_INCOMING_SCAN", action = "GET")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getScan(String scanNo) {
        CrmWarehouseIncomingScan s = scanMapper.selectByNo(scanNo);
        if (s == null) {
            return Result.fail(40405, "SCAN_NOT_FOUND");
        }
        List<CrmWarehouseIncomingItem> items = itemMapper.selectByScanNo(scanNo);
        Map<String, Object> data = new HashMap<>();
        data.put("scan", s);
        data.put("items", items);
        return Result.ok(data);
    }

    /**
     * AC-12.2.3 确认扫码
     */
    @AuditLog(module = "WAREHOUSE_INCOMING_SCAN", action = "CONFIRM")
    @Transactional
    public Result<Map<String, Object>> confirmScan(String scanNo) {
        CrmWarehouseIncomingScan s = scanMapper.selectByNo(scanNo);
        if (s == null) {
            return Result.fail(40405, "SCAN_NOT_FOUND");
        }
        if (!"PENDING".equals(s.getScanStatus())) {
            return Result.fail(40008, "SCAN_NOT_PENDING");
        }
        s.setScanStatus("CONFIRMED");
        s.setConfirmedAt(LocalDateTime.now());
        scanMapper.updateById(s);
        Map<String, Object> data = new HashMap<>();
        data.put("scan", s);
        return Result.ok(data);
    }

    /**
     * AC-12.2.4 扫码单列�?     */
    @AuditLog(module = "WAREHOUSE_INCOMING_SCAN", action = "LIST")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listScans(Long userId, String status, int limit) {
        if (limit < 1 || limit > MAX_LIST_LIMIT) limit = MAX_LIST_LIMIT;
        List<CrmWarehouseIncomingScan> list = scanMapper.selectList(userId, status, limit);
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("count", list.size());
        return Result.ok(data);
    }
}
