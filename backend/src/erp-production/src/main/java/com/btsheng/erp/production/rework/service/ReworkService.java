package com.btsheng.erp.production.rework.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.production.rework.entity.CrmRework;
import com.btsheng.erp.production.rework.entity.CrmReworkAlert;
import com.btsheng.erp.production.rework.entity.CrmReworkHistory;
import com.btsheng.erp.production.rework.mapper.CrmReworkAlertMapper;
import com.btsheng.erp.production.rework.mapper.CrmReworkHistoryMapper;
import com.btsheng.erp.production.rework.mapper.CrmReworkMapper;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** V1.3.7 Story 1.23 - outsource rework service (FR-6-3) */
@Service
public class ReworkService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CANCELLED = "CANCELLED";

    public static final int MAX_REWORK_COUNT = 3;

    public static final int REWORK_LEVEL_INFO = 1;
    public static final int REWORK_LEVEL_WARN = 2;
    public static final int REWORK_LEVEL_CRITICAL = 3;
    public static final int REWORK_LEVEL_EXCEED = 4;

    private final CrmReworkMapper reworkMapper;
    private final CrmReworkHistoryMapper historyMapper;
    private final CrmReworkAlertMapper alertMapper;
    private final CrmOutsourceOrderMapper orderMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public ReworkService(CrmReworkMapper reworkMapper,
                          CrmReworkHistoryMapper historyMapper,
                          CrmReworkAlertMapper alertMapper,
                          CrmOutsourceOrderMapper orderMapper,
                          ErpDocNoGenerator docNoGenerator) {
        this.reworkMapper = reworkMapper;
        this.historyMapper = historyMapper;
        this.alertMapper = alertMapper;
        this.orderMapper = orderMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Transactional
    @AuditLog(module = "rework", action = "rework.create")
    public Result<CrmRework> createRework(Long outsourceId, String reason, BigDecimal cost, Long operatorUserId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (reason == null || reason.isEmpty()) {
            return Result.fail(40001, "REWORK_REASON_REQUIRED");
        }
        if (cost == null || cost.signum() < 0) {
            return Result.fail(40001, "REWORK_COST_NON_NEGATIVE");
        }

        CrmOutsourceOrder order = orderMapper.selectById(outsourceId);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }

        int currentCount = order.getReworkCount() == null ? 0 : order.getReworkCount();
        if (currentCount >= MAX_REWORK_COUNT) {
            return Result.fail(40905, "REWORK_COUNT_EXCEED_MAX_3");
        }

        String reworkNo = docNoGenerator.nextReworkNo();
        int newCount = currentCount + 1;

        CrmRework rework = new CrmRework();
        rework.setReworkNo(reworkNo);
        rework.setOutsourceId(outsourceId);
        rework.setOutsourceNo(order.getOutsourceNo());
        rework.setReason(reason);
        rework.setCost(cost);
        rework.setReworkCount(newCount);
        rework.setStatus(STATUS_IN_PROGRESS);
        rework.setCreatedBy(operatorUserId);
        rework.setCreatedAt(LocalDateTime.now());
        rework.setUpdatedAt(LocalDateTime.now());
        reworkMapper.insert(rework);

        order.setReworkCount(newCount);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        recordHistory(rework.getId(), "CREATE", null,
                "{\"status\":\"DRAFT\",\"rework_count\":" + newCount + "}", operatorUserId);
        recordAlert(outsourceId, order.getOutsourceNo(), newCount);

        return Result.ok(rework);
    }

    @Transactional
    @AuditLog(module = "rework", action = "rework.finish")
    public Result<CrmRework> finishRework(Long reworkId, Long operatorUserId) {
        if (reworkId == null) {
            return Result.fail(40001, "REWORK_ID_REQUIRED");
        }
        CrmRework rework = reworkMapper.selectById(reworkId);
        if (rework == null) {
            return Result.fail(40404, "REWORK_NOT_FOUND");
        }
        if (STATUS_COMPLETED.equals(rework.getStatus())) {
            return Result.fail(40903, "REWORK_ALREADY_COMPLETED");
        }
        if (STATUS_CANCELLED.equals(rework.getStatus())) {
            return Result.fail(40903, "REWORK_CANCELLED");
        }
        String before = "{\"status\":\"" + rework.getStatus() + "\"}";
        rework.setStatus(STATUS_COMPLETED);
        rework.setFinishedAt(LocalDateTime.now());
        rework.setUpdatedAt(LocalDateTime.now());
        reworkMapper.updateById(rework);

        recordHistory(reworkId, "FINISH", before, "{\"status\":\"COMPLETED\"}", operatorUserId);
        return Result.ok(rework);
    }

    @AuditLog(module = "rework", action = "rework.get_history")
    public Result<List<CrmReworkHistory>> getReworkHistory(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        return Result.ok(historyMapper.selectByOutsourceId(outsourceId));
    }

    @AuditLog(module = "rework", action = "rework.get_alert")
    public Result<Map<String, Object>> getReworkAlert(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        CrmOutsourceOrder order = orderMapper.selectById(outsourceId);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        int count = order.getReworkCount() == null ? 0 : order.getReworkCount();
        Map<String, Object> alert = new HashMap<>();
        alert.put("outsourceId", outsourceId);
        alert.put("outsourceNo", order.getOutsourceNo());
        alert.put("reworkCount", count);
        alert.put("maxAllowed", MAX_REWORK_COUNT);
        alert.put("level", computeAlertLevel(count));
        alert.put("message", computeAlertMessage(count));
        alert.put("history", alertMapper.selectByOutsourceId(outsourceId));
        return Result.ok(alert);
    }

    public Result<PageResponse<CrmRework>> listReworks(String keyword, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.max(pageSize, 1);
        int offset = (page - 1) * size;
        List<CrmRework> rows = reworkMapper.selectPage(keyword, size, offset);
        long total = reworkMapper.countByKeyword(keyword);
        return Result.ok(PageResponse.of(rows, total, page, size));
    }

    public Result<CrmRework> getReworkById(Long id) {
        if (id == null) {
            return Result.fail(40001, "REWORK_ID_REQUIRED");
        }
        CrmRework rework = reworkMapper.selectById(id);
        if (rework == null) {
            return Result.fail(40404, "REWORK_NOT_FOUND");
        }
        return Result.ok(rework);
    }

    public Result<PageResponse<Map<String, Object>>> listOpenAlerts(int pageNum, int pageSize) {
        List<CrmReworkAlert> alerts = alertMapper.selectOpenAlerts();
        int page = Math.max(pageNum, 1);
        int size = Math.max(pageSize, 1);
        int from = (page - 1) * size;
        int to = Math.min(from + size, alerts.size());
        List<CrmReworkAlert> slice = from >= alerts.size() ? List.of() : alerts.subList(from, to);
        List<Map<String, Object>> rows = new java.util.ArrayList<>();
        for (CrmReworkAlert alert : slice) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", alert.getId());
            row.put("reworkNo", alert.getOutsourceNo());
            row.put("workorderNo", alert.getOutsourceNo());
            row.put("reason", alert.getAlertMessage());
            row.put("alertedAt", alert.getAlertedAt());
            row.put("status", alert.getAlertLevel());
            rows.add(row);
        }
        return Result.ok(PageResponse.of(rows, alerts.size(), page, size));
    }

    public Result<Void> ackAlert(Long alertId) {
        if (alertId == null) {
            return Result.fail(40001, "ALERT_ID_REQUIRED");
        }
        if (alertMapper.deleteById(alertId) == 0) {
            return Result.fail(40404, "ALERT_NOT_FOUND");
        }
        return Result.ok();
    }

    public Result<BigDecimal> getTotalReworkCost(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        List<CrmRework> list = reworkMapper.selectByOutsourceId(outsourceId);
        BigDecimal total = BigDecimal.ZERO;
        for (CrmRework r : list) {
            if (r.getCost() != null) {
                total = total.add(r.getCost());
            }
        }
        return Result.ok(total);
    }

    private void recordHistory(Long reworkId, String operation, String beforeJson, String afterJson, Long userId) {
        CrmReworkHistory hist = new CrmReworkHistory();
        hist.setReworkId(reworkId);
        hist.setOperation(operation);
        hist.setBeforeJson(beforeJson);
        hist.setAfterJson(afterJson);
        hist.setChangedBy(userId);
        hist.setChangedAt(LocalDateTime.now());
        historyMapper.insert(hist);
    }

    private void recordAlert(Long outsourceId, String outsourceNo, int reworkCount) {
        CrmReworkAlert alert = new CrmReworkAlert();
        alert.setOutsourceId(outsourceId);
        alert.setOutsourceNo(outsourceNo);
        alert.setReworkCount(reworkCount);
        alert.setAlertLevel(computeAlertLevel(reworkCount));
        alert.setAlertMessage(computeAlertMessage(reworkCount));
        alert.setAlertedAt(LocalDateTime.now());
        alertMapper.insert(alert);
    }

    private String computeAlertLevel(int count) {
        if (count <= REWORK_LEVEL_INFO) return "INFO";
        if (count == REWORK_LEVEL_WARN) return "WARN";
        if (count == REWORK_LEVEL_CRITICAL) return "CRITICAL";
        return "EXCEED";
    }

    private String computeAlertMessage(int count) {
        if (count <= REWORK_LEVEL_INFO) return "????";
        if (count == REWORK_LEVEL_WARN) return "??? 2 ?????? 3 ?";
        if (count == REWORK_LEVEL_CRITICAL) return "??? 3 ??????";
        return "??? " + count + " ?????? 3 ?";
    }
}
