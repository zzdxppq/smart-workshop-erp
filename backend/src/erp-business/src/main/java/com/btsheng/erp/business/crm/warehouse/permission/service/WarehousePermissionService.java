package com.btsheng.erp.business.crm.warehouse.permission.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.warehouse.permission.entity.CrmWarehouseIncomingPermission;
import com.btsheng.erp.business.crm.warehouse.permission.mapper.CrmWarehouseIncomingPermissionMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * V1.3.7 · Story 1.49 · 委外协同·仓管到货扫码权限 Service
 *
 * 2 方法：grantPermission / getPermission
 * 3 P1 修补：仓管角色强�?/ 扫码权限时效 8h / �?1.4 APP + 1.12 扫码
 */
@Service
public class WarehousePermissionService {

    public static final long DEFAULT_VALID_HOURS = 8L;
    public static final String ROLE_WAREHOUSE = "WAREHOUSE";

    private final CrmWarehouseIncomingPermissionMapper permissionMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public WarehousePermissionService(CrmWarehouseIncomingPermissionMapper permissionMapper,
                                       DocNoGenerator docNoGenerator) {
        this.permissionMapper = permissionMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-12.1.1 授予仓管扫码权限
     * P1 修补 1：仓管角色强�?     * P1 修补 2：扫码权限时�?8h
     */
    @AuditLog(module = "WAREHOUSE_PERMISSION", action = "GRANT")
    @Transactional
    public Result<Map<String, Object>> grantPermission(Long userId, String userName,
                                                        String role, String grantedBy, String email) {
        // P1 修补 1：仓管角色强�?
            if (!ROLE_WAREHOUSE.equals(role)) {
            return Result.fail(40004, "ROLE_NOT_WAREHOUSE");
        }
        // P1 修补 2：权限时�?8h
            LocalDateTime now = LocalDateTime.now();
        LocalDateTime validTo = now.plusHours(DEFAULT_VALID_HOURS);

        CrmWarehouseIncomingPermission p = new CrmWarehouseIncomingPermission();
        p.setPermissionNo(docNoGenerator.nextWarehousePermissionNo());
        p.setUserId(userId);
        p.setUserName(userName);
        p.setRole(role);
        p.setPermissionType("SCAN_INCOMING");
        p.setValidFrom(now);
        p.setValidTo(validTo);
        p.setGrantedBy(grantedBy);
        p.setStatus("ACTIVE");
        p.setEmail(email);
        p.setCreatedAt(now);
        permissionMapper.insert(p);

        Map<String, Object> data = new HashMap<>();
        data.put("permission", p);
        return Result.ok(data);
    }

    /**
     * AC-12.1.2 查询扫码权限
     * P1 修补 3：跨 1.4 APP + 1.12 扫码
     */
    @AuditLog(module = "WAREHOUSE_PERMISSION", action = "GET")
    @Transactional(readOnly = true)
    public Result<Map<String, Object>> getPermission(String permissionNo) {
        CrmWarehouseIncomingPermission p = permissionMapper.selectByNo(permissionNo);
        if (p == null) {
            return Result.fail(40404, "PERMISSION_NOT_FOUND");
        }
        // 校验过期
            if (p.getValidTo() != null && p.getValidTo().isBefore(LocalDateTime.now())) {
            p.setStatus("EXPIRED");
        }
        Map<String, Object> data = new HashMap<>();
        data.put("permission", p);
        return Result.ok(data);
    }
}
