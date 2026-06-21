package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceStateHistory;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceStateHistoryMapper;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.7 · Story 1.22 · 委外 7 状态机 Service (FR-6-2)
 *
 * <p>7 状态：DRAFT �?SENT �?ACCEPTED �?IN_PRODUCTION �?INSPECTED �?COMPLETED �?CLOSED
 * <br>扩展�? REWORK（可多次，≤ 3�? REJECTED（拒收）
 * <br>终�?CLOSED / REJECTED 不可再开
 *
 * <p>5 业务方法：advanceState / rollbackState / getStateHistory / validateStateTransition / getOutsourceState
 * <p>3 P1 修补：状态守�?40904 OUTSOURCE_STATE_INVALID / 生管/采购分工（V1.3.7 AD-1�? 终态不可再开
 */
@Service
public class OutsourceStateMachineService {

    // ====== 7 状�?+ 2 扩展 ======
            public static final String STATE_DRAFT = "DRAFT";
    public static final String STATE_SENT = "SENT";
    public static final String STATE_ACCEPTED = "ACCEPTED";
    public static final String STATE_IN_PRODUCTION = "IN_PRODUCTION";
    public static final String STATE_INSPECTED = "INSPECTED";
    public static final String STATE_COMPLETED = "COMPLETED";
    public static final String STATE_CLOSED = "CLOSED";
    public static final String STATE_REWORK = "REWORK";
    public static final String STATE_REJECTED = "REJECTED";

    public static final int CODE_STATE_INVALID = 40904;

    // ====== 8 状态合法转换矩�?======
            private static final Map<String, Set<String>> TRANSITIONS = new HashMap<>();
    static {
        TRANSITIONS.put(STATE_DRAFT,          new HashSet<>(Arrays.asList(STATE_SENT, STATE_REJECTED)));
        TRANSITIONS.put(STATE_SENT,           new HashSet<>(Arrays.asList(STATE_ACCEPTED, STATE_REJECTED)));
        TRANSITIONS.put(STATE_ACCEPTED,       new HashSet<>(Arrays.asList(STATE_IN_PRODUCTION, STATE_REJECTED)));
        TRANSITIONS.put(STATE_IN_PRODUCTION,  new HashSet<>(Arrays.asList(STATE_INSPECTED, STATE_REWORK, STATE_REJECTED)));
        TRANSITIONS.put(STATE_INSPECTED,      new HashSet<>(Arrays.asList(STATE_COMPLETED, STATE_REWORK, STATE_REJECTED)));
        TRANSITIONS.put(STATE_COMPLETED,      new HashSet<>(Arrays.asList(STATE_CLOSED, STATE_REWORK)));
        // REWORK 可回退�?IN_PRODUCTION（重新生产），不可直�?CLOSED
            TRANSITIONS.put(STATE_REWORK,         new HashSet<>(Arrays.asList(STATE_IN_PRODUCTION, STATE_REJECTED)));
        // 终态：CLOSED / REJECTED 不可再开
            TRANSITIONS.put(STATE_CLOSED,         new HashSet<>());
        TRANSITIONS.put(STATE_REJECTED,       new HashSet<>());
    }

    // V1.3.7 AD-1：生�?采购分工 �?不同操作允许的角�?
            private static final Map<String, Set<String>> STATE_OPERATOR_ROLES = new HashMap<>();
    static {
        STATE_OPERATOR_ROLES.put(STATE_SENT, new HashSet<>(Arrays.asList("生管", "采购", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_ACCEPTED, new HashSet<>(Arrays.asList("采购", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_IN_PRODUCTION, new HashSet<>(Arrays.asList("采购", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_INSPECTED, new HashSet<>(Arrays.asList("品检", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_COMPLETED, new HashSet<>(Arrays.asList("品检", "采购", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_CLOSED, new HashSet<>(Arrays.asList("财务", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_REWORK, new HashSet<>(Arrays.asList("品检", "采购", "admin")));
        STATE_OPERATOR_ROLES.put(STATE_REJECTED, new HashSet<>(Arrays.asList("品检", "采购", "财务", "admin")));
    }

    private final CrmOutsourceOrderMapper orderMapper;
    private final CrmOutsourceStateHistoryMapper stateHistoryMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public OutsourceStateMachineService(CrmOutsourceOrderMapper orderMapper,
                                          CrmOutsourceStateHistoryMapper stateHistoryMapper,
                                          ErpDocNoGenerator docNoGenerator) {
        this.orderMapper = orderMapper;
        this.stateHistoryMapper = stateHistoryMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * AC-6.2.1 + AC-6.2.2：状态机推进（带状态守卫）
     */
    @Transactional
    @AuditLog(module = "outsource_state", action = "state.advance")
    public Result<CrmOutsourceOrder> advanceState(Long outsourceId, String targetState, String operatorRole, Long operatorUserId, String reason) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (targetState == null || targetState.isEmpty()) {
            return Result.fail(40001, "TARGET_STATE_REQUIRED");
        }

        CrmOutsourceOrder order = orderMapper.selectById(outsourceId);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }

        String fromState = order.getStatus();

        // 状态守卫：终态不可再开
            if (STATE_CLOSED.equals(fromState) || STATE_REJECTED.equals(fromState)) {
            return Result.fail(CODE_STATE_INVALID, "OUTSOURCE_STATE_TERMINAL");
        }

        // 状态守卫：合法转换校验
            if (!validateStateTransition(fromState, targetState)) {
            return Result.fail(CODE_STATE_INVALID, "OUTSOURCE_STATE_INVALID");
        }

        // V1.3.7 AD-1：操作员角色校验
            String role = operatorRole != null ? operatorRole : "admin";
        Set<String> allowedRoles = STATE_OPERATOR_ROLES.get(targetState);
        if (allowedRoles != null && !allowedRoles.contains(role)) {
            return Result.fail(40301, "OUTSOURCE_OPERATOR_ROLE_FORBIDDEN_" + targetState);
        }

        // 推进状�?
            order.setStatus(targetState);
        order.setUpdatedAt(LocalDateTime.now());
        if (STATE_ACCEPTED.equals(targetState)) {
            order.setAcceptedAt(LocalDateTime.now());
        } else if (STATE_COMPLETED.equals(targetState)) {
            order.setCompletedAt(LocalDateTime.now());
        } else if (STATE_CLOSED.equals(targetState)) {
            order.setClosedAt(LocalDateTime.now());
        }
        orderMapper.updateById(order);

        // 写状态历�?
            recordStateHistory(outsourceId, order.getOutsourceNo(), fromState, targetState, "ADVANCE", operatorUserId, role, reason);

        return Result.ok(order);
    }

    /**
     * AC-6.2.2：状态机回退（含 REJECTED 拒收路径�?     */
    @Transactional
    @AuditLog(module = "outsource_state", action = "state.rollback")
    public Result<CrmOutsourceOrder> rollbackState(Long outsourceId, String reason, String operatorRole, Long operatorUserId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        if (reason == null || reason.isEmpty()) {
            return Result.fail(40001, "ROLLBACK_REASON_REQUIRED");
        }

        CrmOutsourceOrder order = orderMapper.selectById(outsourceId);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }

        String fromState = order.getStatus();
        if (STATE_CLOSED.equals(fromState) || STATE_REJECTED.equals(fromState)) {
            return Result.fail(CODE_STATE_INVALID, "OUTSOURCE_STATE_TERMINAL");
        }

        // REJECTED 路径：任何非终态都可拒�?
            String targetState = STATE_REJECTED;
        String role = operatorRole != null ? operatorRole : "admin";

        order.setStatus(targetState);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);

        recordStateHistory(outsourceId, order.getOutsourceNo(), fromState, targetState, "ROLLBACK", operatorUserId, role, reason);

        return Result.ok(order);
    }

    /**
     * AC-6.2.3：状态历史时间线（按 outsourceId 查询�?     */
    @AuditLog(module = "outsource_state", action = "state.get_history")
    public Result<List<CrmOutsourceStateHistory>> getStateHistory(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        List<CrmOutsourceStateHistory> history = stateHistoryMapper.selectByOutsourceId(outsourceId);
        return Result.ok(history);
    }

    /**
     * 获取当前委外单状态（封装�?     */
    public Result<CrmOutsourceOrder> getOutsourceState(Long outsourceId) {
        if (outsourceId == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        CrmOutsourceOrder order = orderMapper.selectById(outsourceId);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        return Result.ok(order);
    }

    /**
     * 8 状态转换矩阵（OpenAPI 元数据）
     */
    public Result<Map<String, Object>> getTransitionMatrix() {
        Map<String, Object> matrix = new HashMap<>();
        matrix.put("states", Arrays.asList(
            STATE_DRAFT, STATE_SENT, STATE_ACCEPTED, STATE_IN_PRODUCTION,
            STATE_INSPECTED, STATE_COMPLETED, STATE_CLOSED, STATE_REWORK, STATE_REJECTED
        ));
        matrix.put("transitions", TRANSITIONS);
        matrix.put("terminalStates", Arrays.asList(STATE_CLOSED, STATE_REJECTED));
        matrix.put("operatorRoles", STATE_OPERATOR_ROLES);
        return Result.ok(matrix);
    }

    /**
     * 8 状态转换矩阵校验（pure function�?     */
    public boolean validateStateTransition(String fromState, String toState) {
        if (fromState == null || toState == null) {
            return false;
        }
        Set<String> allowed = TRANSITIONS.get(fromState);
        if (allowed == null) {
            return false;
        }
        return allowed.contains(toState);
    }

    // ====== 私有 ======
            private void recordStateHistory(Long outsourceId, String outsourceNo, String fromState, String toState,
                                     String transitionType, Long operatorUserId, String operatorRole, String reason) {
        CrmOutsourceStateHistory hist = new CrmOutsourceStateHistory();
        hist.setOutsourceId(outsourceId);
        hist.setOutsourceNo(outsourceNo);
        hist.setFromState(fromState);
        hist.setToState(toState);
        hist.setTransitionType(transitionType);
        hist.setOperatorUserId(operatorUserId);
        hist.setOperatorRole(operatorRole);
        hist.setReason(reason);
        hist.setOccurredAt(LocalDateTime.now());
        stateHistoryMapper.insert(hist);
    }
}
