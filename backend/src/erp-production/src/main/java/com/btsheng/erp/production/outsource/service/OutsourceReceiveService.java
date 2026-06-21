package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.production.integration.client.BusinessQualityInspectionClient;
import com.btsheng.erp.production.outsource.dto.OutsourceArriveRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceStateHistoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.5 · E12-S2 · 仓管扫 WW- 委外到货（SHIPPING/在途 → INSPECTED 待检）
 * V104 · 委外工序返回自动生成 IPQC 待检（来源=委外）；整单委外生成 IQC
 */
@Service
public class OutsourceReceiveService {

    private static final String STATE_INSPECTED = OutsourceStateMachineService.STATE_INSPECTED;
    private static final Set<String> RECEIVABLE = new HashSet<>(Arrays.asList(
            OutsourceStateMachineService.STATE_SENT,
            OutsourceStateMachineService.STATE_ACCEPTED,
            OutsourceStateMachineService.STATE_IN_PRODUCTION
    ));

    private final CrmOutsourceOrderMapper orderMapper;
    private final CrmOutsourceStateHistoryMapper stateHistoryMapper;
    private final BusinessQualityInspectionClient qualityInspectionClient;

    @Autowired
    public OutsourceReceiveService(CrmOutsourceOrderMapper orderMapper,
                                   CrmOutsourceStateHistoryMapper stateHistoryMapper,
                                   @Autowired(required = false) BusinessQualityInspectionClient qualityInspectionClient) {
        this.orderMapper = orderMapper;
        this.stateHistoryMapper = stateHistoryMapper;
        this.qualityInspectionClient = qualityInspectionClient;
    }

    /** 归一化 WW-20260612-0001 → WW20260612-0001 */
    public static String normalizeOutsourceNo(String code) {
        if (code == null) return null;
        String t = code.trim().toUpperCase();
        if (t.matches("^WW-\\d{8}-\\d{4}$")) {
            return "WW" + t.substring(3);
        }
        return t;
    }

    @AuditLog(module = "outsource_receive", action = "receive")
    @Transactional
    public Result<CrmOutsourceOrder> receive(Long outsourceId, OutsourceArriveRequest req, Long userId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (req == null || req.getActualQty() == null || req.getActualQty() < 1) {
            return Result.fail(40001, "ACTUAL_QTY_REQUIRED");
        }

        CrmOutsourceOrder order = orderMapper.selectById(outsourceId);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        if (req.getOutsourceNo() != null && !req.getOutsourceNo().isBlank()) {
            String normalized = normalizeOutsourceNo(req.getOutsourceNo());
            if (!order.getOutsourceNo().equalsIgnoreCase(normalized)) {
                return Result.fail(40009, "OUTSOURCE_NO_MISMATCH");
            }
        }

        String from = order.getStatus();
        if (STATE_INSPECTED.equals(from)) {
            return Result.ok(order);
        }
        if (!RECEIVABLE.contains(from)) {
            return Result.fail(OutsourceStateMachineService.CODE_STATE_INVALID, "OUTSOURCE_NOT_RECEIVABLE");
        }

        order.setStatus(STATE_INSPECTED);
        order.setUpdatedAt(LocalDateTime.now());
        String note = "仓管到货扫码 qty=" + req.getActualQty();
        if (req.getActualWeight() != null) {
            note += " weight=" + req.getActualWeight() + "kg";
        }
        if (req.getRemark() != null && !req.getRemark().isBlank()) {
            note += " " + req.getRemark();
        }
        order.setRemark(appendRemark(order.getRemark(), note));
        orderMapper.updateById(order);

        CrmOutsourceStateHistory h = new CrmOutsourceStateHistory();
        h.setOutsourceId(order.getId());
        h.setOutsourceNo(order.getOutsourceNo());
        h.setFromState(from);
        h.setToState(STATE_INSPECTED);
        h.setTransitionType("RECEIVE");
        h.setOperatorUserId(userId);
        h.setOperatorRole("仓管");
        h.setReason(note);
        h.setOccurredAt(LocalDateTime.now());
        stateHistoryMapper.insert(h);

        pushQualityPending(order, req.getActualQty(), userId);

        return Result.ok(order);
    }

    private void pushQualityPending(CrmOutsourceOrder order, int qty, Long userId) {
        if (qualityInspectionClient == null || order == null) {
            return;
        }
        try {
            boolean processOutsource = order.getStepNo() != null && order.getStepNo() > 0;
            Map<String, Object> body = new HashMap<>();
            body.put("inspectSource", "OUTSOURCE");
            body.put("sourceRef", "OUTSOURCE:" + order.getOutsourceNo());
            body.put("materialCode", order.getMaterialCode());
            body.put("workOrderNo", order.getWorkorderNo());
            body.put("qty", qty);
            if (processOutsource) {
                body.put("inspectType", "IPQC");
                body.put("processName", order.getProcessName() != null ? order.getProcessName()
                        : "工序" + order.getStepNo());
                body.put("remark", "委外工序返回自动生成过程检 · " + order.getOutsourceNo());
            } else {
                body.put("inspectType", "IQC");
                body.put("remark", "整单委外返回自动生成来料检 · " + order.getOutsourceNo());
            }
            qualityInspectionClient.createPending(body);
        } catch (Exception ignored) {
            // 跨服务推送失败不阻塞到货
        }
    }

    public Result<CrmOutsourceOrder> receiveByNo(String outsourceNo, OutsourceArriveRequest req, Long userId) {
        String normalized = normalizeOutsourceNo(outsourceNo);
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(normalized);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        if (req.getOutsourceNo() == null || req.getOutsourceNo().isBlank()) {
            req.setOutsourceNo(normalized);
        }
        return receive(order.getId(), req, userId);
    }

    private static String appendRemark(String existing, String addition) {
        if (existing == null || existing.isBlank()) return addition;
        return existing + " | " + addition;
    }
}
