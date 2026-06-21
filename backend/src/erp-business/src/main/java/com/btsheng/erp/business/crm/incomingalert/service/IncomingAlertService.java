package com.btsheng.erp.business.crm.incomingalert.service;

import com.btsheng.erp.business.crm.incomingalert.dto.CreateAlertRequest;
import com.btsheng.erp.business.crm.incomingalert.dto.MarkArrivedRequest;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncoming;
import com.btsheng.erp.business.crm.incomingalert.entity.CrmIncomingAlert;
import com.btsheng.erp.business.crm.incomingalert.mapper.CrmIncomingAlertMapper;
import com.btsheng.erp.business.crm.incomingalert.mapper.CrmIncomingMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.34 · 采购·到货提醒 Service (FR-8-3)
 *
 * <p>4 业务方法：createAlert / listPendingAlerts / markArrived / getOverdueAlerts
 * <p>提醒单号：IA{yyyyMMdd}{seq:4}
 * <p>4 状态：PENDING/ALERT（提�?3 天）/ALERT_CRITICAL（逾期�?ARRIVED
 * <p>3 P1 修补：预估到货日必填 / 提前 3 �?ALERT / 逾期 ALERT_CRITICAL / 唯一索引 (po_id, material_id)
 */
@Service
public class IncomingAlertService {

    public static final String LEVEL_PENDING = "PENDING";
    public static final String LEVEL_ALERT = "ALERT";
    public static final String LEVEL_ALERT_CRITICAL = "ALERT_CRITICAL";
    public static final String LEVEL_ARRIVED = "ARRIVED";

    /** P1 修补 2：提�?3 �?ALERT */
    public static final int ALERT_DAYS_BEFORE = 3;

    private final CrmIncomingAlertMapper alertMapper;
    private final CrmIncomingMapper incomingMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public IncomingAlertService(CrmIncomingAlertMapper alertMapper,
                                CrmIncomingMapper incomingMapper,
                                DocNoGenerator docNoGenerator) {
        this.alertMapper = alertMapper;
        this.incomingMapper = incomingMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-8.3.1：创建到货提醒（PO 创建�?hook�?     * P1 修补 1：预估到货日必填
     * P1 修补 4：唯一索引 (po_id, material_id) 兜底
     */
    @Transactional
    @AuditLog(module = "incoming_alert", action = "incoming_alert.create")
    public Result<CrmIncomingAlert> createAlert(CreateAlertRequest req, Long operatorUserId) {
        if (req == null || req.getPoId() == null || req.getPoNo() == null) {
            return Result.fail(40001, "PO_REQUIRED");
        }
        if (req.getMaterialId() == null) {
            return Result.fail(40001, "MATERIAL_ID_REQUIRED");
        }
        if (req.getQty() == null || req.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }
        // P1 修补 1：预估到货日必填
            if (req.getExpectedDate() == null) {
            return Result.fail(40001, "EXPECTED_DATE_REQUIRED");
        }
        // P1 修补 4：唯一 (po_id, material_id)
            CrmIncomingAlert existed = alertMapper.selectByPoAndMaterial(req.getPoId(), req.getMaterialId());
        if (existed != null) {
            return Result.fail(40902, "ALERT_DUPLICATE");
        }

        CrmIncomingAlert a = new CrmIncomingAlert();
        a.setAlertNo(docNoGenerator.nextIncomingAlertNo());
        a.setPoId(req.getPoId());
        a.setPoNo(req.getPoNo());
        a.setVendorId(req.getVendorId());
        a.setVendorName(req.getVendorName());
        a.setMaterialId(req.getMaterialId());
        a.setMaterialCode(req.getMaterialCode());
        a.setMaterialName(req.getMaterialName());
        a.setQty(req.getQty());
        a.setUnit(req.getUnit());
        a.setExpectedDate(req.getExpectedDate());
        a.setAlertLevel(LEVEL_PENDING);
        a.setRemindedCount(0);
        a.setCreatedAt(LocalDateTime.now());
        a.setUpdatedAt(LocalDateTime.now());
        alertMapper.insert(a);
        return Result.ok(a);
    }

    /**
     * AC-8.3.1：在途提醒（�?3 天的 PENDING + 提前 ALERT�?     * P1 修补 2：提�?3 天自�?ALERT
     * P1 修补 3：逾期自动 ALERT_CRITICAL
     */
    @Transactional
    @AuditLog(module = "incoming_alert", action = "incoming_alert.list_pending")
    public Result<List<CrmIncomingAlert>> listPendingAlerts() {
        LocalDate today = LocalDate.now();
        List<CrmIncomingAlert> all = alertMapper.selectPendingAll();
        List<CrmIncomingAlert> result = new ArrayList<>();
        if (all == null) return Result.ok(result);

        for (CrmIncomingAlert a : all) {
            long days = ChronoUnit.DAYS.between(today, a.getExpectedDate());
            if (days < 0) {
                // P1 修补 3：逾期
            if (!LEVEL_ALERT_CRITICAL.equals(a.getAlertLevel())) {
                    a.setAlertLevel(LEVEL_ALERT_CRITICAL);
                    a.setAlertMessage("P1 修补 3：逾期 " + Math.abs(days) + " 天");
                    a.setUpdatedAt(LocalDateTime.now());
                    alertMapper.updateById(a);
                }
                result.add(a);
            } else if (days <= ALERT_DAYS_BEFORE) {
                // P1 修补 2：提�?3 �?
            if (!LEVEL_ALERT.equals(a.getAlertLevel())) {
                    a.setAlertLevel(LEVEL_ALERT);
                    a.setAlertMessage("P1 修补 2：距离到货 " + days + " 天");
                    a.setRemindedAt(LocalDateTime.now());
                    a.setRemindedCount(a.getRemindedCount() == null ? 1 : a.getRemindedCount() + 1);
                    a.setUpdatedAt(LocalDateTime.now());
                    alertMapper.updateById(a);
                }
                result.add(a);
            } else {
                // 远期：保�?PENDING
            if (!LEVEL_PENDING.equals(a.getAlertLevel())) {
                    a.setAlertLevel(LEVEL_PENDING);
                    a.setAlertMessage("距离到货还有 " + days + " 天");
                    a.setUpdatedAt(LocalDateTime.now());
                    alertMapper.updateById(a);
                }
                // 仍然展示
            result.add(a);
            }
        }
        return Result.ok(result);
    }

    /**
     * 标记到货
     */
    @Transactional
    @AuditLog(module = "incoming_alert", action = "incoming_alert.mark_arrived")
    public Result<Map<String, Object>> markArrived(Long alertId, MarkArrivedRequest req, Long operatorUserId) {
        if (alertId == null) {
            return Result.fail(40001, "ALERT_ID_REQUIRED");
        }
        if (req == null || req.getArrivedQty() == null || req.getArrivedQty().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "ARRIVED_QTY_INVALID");
        }
        CrmIncomingAlert a = alertMapper.selectById(alertId);
        if (a == null) {
            return Result.fail(40404, "ALERT_NOT_FOUND");
        }
        if (LEVEL_ARRIVED.equals(a.getAlertLevel())) {
            return Result.fail(40903, "ALREADY_ARRIVED");
        }

        // �?crm_incoming
            CrmIncoming inc = new CrmIncoming();
        inc.setIncomingNo("IN" + LocalDate.now().toString().replace("-", "") + "-"
                + String.format("%04d", alertId));
        inc.setAlertId(alertId);
        inc.setPoId(a.getPoId());
        inc.setPoNo(a.getPoNo());
        inc.setVendorId(a.getVendorId());
        inc.setVendorName(a.getVendorName());
        inc.setMaterialId(a.getMaterialId());
        inc.setMaterialCode(a.getMaterialCode());
        inc.setMaterialName(a.getMaterialName());
        inc.setArrivedQty(req.getArrivedQty());
        inc.setExpectedQty(a.getQty());
        inc.setUnit(a.getUnit());
        inc.setArrivedAt(LocalDateTime.now());
        inc.setArrivedBy(operatorUserId);
        inc.setQualityStatus("PENDING");
        inc.setScanBatchNo(req.getScanBatchNo());
        inc.setRemark(req.getRemark());
        inc.setCreatedAt(LocalDateTime.now());
        incomingMapper.insert(inc);

        // 累加 arrivedQty�?= qty 时改 ARRIVED
            BigDecimal newArrived = a.getArrivedQty() == null
                ? req.getArrivedQty()
                : a.getArrivedQty().add(req.getArrivedQty());
        a.setArrivedQty(newArrived);
        if (newArrived.compareTo(a.getQty()) >= 0) {
            a.setAlertLevel(LEVEL_ARRIVED);
            a.setAlertMessage("已全部到�?" + newArrived + "/" + a.getQty());
        } else {
            a.setAlertMessage("部分到货 " + newArrived + "/" + a.getQty());
        }
        a.setArrivedAt(LocalDateTime.now());
        a.setArrivedBy(operatorUserId);
        a.setUpdatedAt(LocalDateTime.now());
        alertMapper.updateById(a);

        Map<String, Object> data = new HashMap<>();
        data.put("alert", a);
        data.put("incoming", inc);
        return Result.ok(data);
    }

    /**
     * 逾期提醒（ALERT_CRITICAL�?     */
    @AuditLog(module = "incoming_alert", action = "incoming_alert.list_overdue")
    public Result<List<CrmIncomingAlert>> getOverdueAlerts() {
        return Result.ok(alertMapper.selectByAlertLevel(LEVEL_ALERT_CRITICAL));
    }
}
