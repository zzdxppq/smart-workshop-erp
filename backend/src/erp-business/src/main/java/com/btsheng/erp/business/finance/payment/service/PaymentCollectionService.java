package com.btsheng.erp.business.finance.payment.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.finance.payment.dto.CreatePlanRequest;
import com.btsheng.erp.business.finance.payment.dto.MarkPaidRequest;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentAlert;
import com.btsheng.erp.business.finance.payment.entity.CrmPaymentPlan;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentAlertMapper;
import com.btsheng.erp.business.finance.payment.mapper.CrmPaymentPlanMapper;
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
 * V1.3.7 · Story 1.38 · 财务·回款控制 Service (FR-9-3)
 *
 * <p>4 业务方法：createPlan / listPendingPlans / markPaid / getOverduePlans
 * <p>3 P1 修补：回款金�?�?订单金额 / 提前 3 �?ALERT / 逾期 ALERT_CRITICAL
 * <p>4 状态：PENDING/ALERT（提�?3 天）/ALERT_CRITICAL（逾期�?PAID
 */
@Service
public class PaymentCollectionService {

    public static final String LEVEL_PENDING = "PENDING";
    public static final String LEVEL_ALERT = "ALERT";
    public static final String LEVEL_ALERT_CRITICAL = "ALERT_CRITICAL";
    public static final String LEVEL_PAID = "PAID";

    public static final int ALERT_DAYS_BEFORE = 3;

    private final CrmPaymentPlanMapper planMapper;
    private final CrmPaymentAlertMapper alertMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public PaymentCollectionService(CrmPaymentPlanMapper planMapper,
                                     CrmPaymentAlertMapper alertMapper,
                                     DocNoGenerator docNoGenerator) {
        this.planMapper = planMapper;
        this.alertMapper = alertMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-9.3.1：创建回款计�?     * P1 修补 1：计划金�?�?订单金额
     */
    @Transactional
    @AuditLog(module = "payment_collection", action = "plan.create")
    public Result<CrmPaymentPlan> createPlan(CreatePlanRequest req, Long operatorUserId) {
        if (req == null || req.getOrderId() == null || req.getOrderNo() == null) {
            return Result.fail(40001, "ORDER_REQUIRED");
        }
        if (req.getTotalAmount() == null || req.getTotalAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "ORDER_AMOUNT_INVALID");
        }
        if (req.getPlannedAmount() == null || req.getPlannedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "PLANNED_AMOUNT_INVALID");
        }
        // P1 修补 1
            if (req.getPlannedAmount().compareTo(req.getTotalAmount()) > 0) {
            return Result.fail(40909, "PLANNED_EXCEED_ORDER");
        }
        if (req.getPlannedDate() == null) {
            return Result.fail(40001, "PLANNED_DATE_REQUIRED");
        }
        if (planMapper.selectByOrderId(req.getOrderId()) != null) {
            return Result.fail(40902, "PLAN_DUPLICATE");
        }

        CrmPaymentPlan p = new CrmPaymentPlan();
        p.setPlanNo(docNoGenerator.nextPaymentPlanNo());
        p.setCustomerId(req.getCustomerId());
        p.setCustomerName(req.getCustomerName());
        p.setOrderId(req.getOrderId());
        p.setOrderNo(req.getOrderNo());
        p.setReceivableId(req.getReceivableId());
        p.setReceivableNo(req.getReceivableNo());
        p.setTotalAmount(req.getTotalAmount());
        p.setPlannedAmount(req.getPlannedAmount());
        p.setPaidAmount(BigDecimal.ZERO);
        p.setPlannedDate(req.getPlannedDate());
        p.setAlertLevel(LEVEL_PENDING);
        p.setRemark(req.getRemark());
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.insert(p);
        return Result.ok(p);
    }

    /**
     * 待办回款（PENDING/ALERT 提前 3 天）
     * P1 修补 2：提�?3 天自�?ALERT
     */
    @Transactional
    @AuditLog(module = "payment_collection", action = "plan.list_pending")
    public Result<List<CrmPaymentPlan>> listPendingPlans() {
        LocalDate today = LocalDate.now();
        List<CrmPaymentPlan> all = planMapper.selectPending();
        List<CrmPaymentPlan> result = new ArrayList<>();
        if (all == null) return Result.ok(result);
        for (CrmPaymentPlan p : all) {
            long days = ChronoUnit.DAYS.between(today, p.getPlannedDate());
            if (days < 0) {
                if (!LEVEL_ALERT_CRITICAL.equals(p.getAlertLevel())) {
                    p.setAlertLevel(LEVEL_ALERT_CRITICAL);
                    p.setUpdatedAt(LocalDateTime.now());
                    planMapper.updateById(p);
                    // 写告�?
            writeAlert(p.getId(), LEVEL_ALERT_CRITICAL,
                            "回款计划 " + p.getPlanNo() + " 逾期 " + Math.abs(days) + " 天", (int) days);
                }
            } else if (days <= ALERT_DAYS_BEFORE) {
                if (!LEVEL_ALERT.equals(p.getAlertLevel())) {
                    p.setAlertLevel(LEVEL_ALERT);
                    p.setUpdatedAt(LocalDateTime.now());
                    planMapper.updateById(p);
                    writeAlert(p.getId(), LEVEL_ALERT,
                            "回款计划 " + p.getPlanNo() + " 提前 3 天提醒", (int) days);
                }
            }
            result.add(p);
        }
        return Result.ok(result);
    }

    /**
     * 标记回款
     * P1 修补 1：本次回�?�?计划未回�?     */
    @Transactional
    @AuditLog(module = "payment_collection", action = "plan.mark_paid")
    public Result<CrmPaymentPlan> markPaid(Long planId, MarkPaidRequest req, Long operatorUserId) {
        if (planId == null) return Result.fail(40001, "PLAN_ID_REQUIRED");
        if (req == null || req.getPaidAmount() == null || req.getPaidAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "PAID_AMOUNT_INVALID");
        }
        CrmPaymentPlan p = planMapper.selectById(planId);
        if (p == null) return Result.fail(40404, "PLAN_NOT_FOUND");
        if (LEVEL_PAID.equals(p.getAlertLevel())) return Result.fail(40903, "ALREADY_PAID");

        BigDecimal remaining = p.getPlannedAmount().subtract(
                p.getPaidAmount() == null ? BigDecimal.ZERO : p.getPaidAmount());
        if (req.getPaidAmount().compareTo(remaining) > 0) {
            return Result.fail(40909, "PAID_EXCEED_REMAINING");
        }
        BigDecimal newPaid = (p.getPaidAmount() == null ? BigDecimal.ZERO : p.getPaidAmount())
                .add(req.getPaidAmount());
        p.setPaidAmount(newPaid);
        p.setPaidAt(LocalDateTime.now());
        p.setPaidBy(req.getPaidBy());
        if (newPaid.compareTo(p.getPlannedAmount()) >= 0) {
            p.setAlertLevel(LEVEL_PAID);
        } else {
            p.setAlertLevel(LEVEL_PENDING);
        }
        if (req.getRemark() != null) p.setRemark(req.getRemark());
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(p);
        return Result.ok(p);
    }

    /**
     * 逾期计划
     */
    @AuditLog(module = "payment_collection", action = "plan.list_overdue")
    public Result<Map<String, Object>> getOverduePlans() {
        List<CrmPaymentPlan> overdue = planMapper.selectOverdue();
        Map<String, Object> out = new HashMap<>();
        out.put("overdue", overdue);
        out.put("count", overdue == null ? 0 : overdue.size());
        return Result.ok(out);
    }

    private void writeAlert(Long planId, String level, String message, int daysToDue) {
        CrmPaymentAlert a = new CrmPaymentAlert();
        a.setPlanId(planId);
        a.setAlertLevel(level);
        a.setAlertMessage(message);
        a.setDaysToDue(daysToDue);
        a.setNotifiedAt(LocalDateTime.now());
        a.setNotifiedChannel("EMAIL");
        a.setAcknowledged(0);
        a.setCreatedAt(LocalDateTime.now());
        alertMapper.insert(a);
    }
}
