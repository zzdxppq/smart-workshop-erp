package com.btsheng.erp.business.crm.planner.service;

import com.btsheng.erp.business.crm.engineer.entity.CrmBomDetailItem;
import com.btsheng.erp.business.crm.engineer.entity.CrmEngineeringWorkbench;
import com.btsheng.erp.business.crm.engineer.entity.CrmProcessDetail;
import com.btsheng.erp.business.crm.engineer.mapper.CrmBomDetailItemMapper;
import com.btsheng.erp.business.crm.engineer.mapper.CrmEngineeringWorkbenchMapper;
import com.btsheng.erp.business.crm.engineer.mapper.CrmProcessDetailMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.planner.entity.CrmProcessAssignment;
import com.btsheng.erp.business.crm.planner.entity.CrmProductionNotification;
import com.btsheng.erp.business.crm.planner.entity.CrmProductionPlanning;
import com.btsheng.erp.business.crm.planner.mapper.CrmProcessAssignmentMapper;
import com.btsheng.erp.business.crm.planner.mapper.CrmProductionNotificationMapper;
import com.btsheng.erp.business.crm.planner.mapper.CrmProductionPlanningMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.core.model.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * V2.1 · 排产计划 Service
 */
@Slf4j
@Service
public class ProductionPlanningService {

    private final CrmProductionPlanningMapper planningMapper;
    private final CrmProcessAssignmentMapper assignmentMapper;
    private final CrmProductionNotificationMapper notificationMapper;
    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper orderItemMapper;
    private final CrmEngineeringWorkbenchMapper workbenchMapper;
    private final CrmProcessDetailMapper processDetailMapper;
    private final CrmBomDetailItemMapper bomDetailMapper;
    private final DocNoGenerator docNoGenerator;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 单号计数器
    private final AtomicLong planSeq = new AtomicLong(1);

    // 状态常量
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_ASSIGNED = "ASSIGNED";
    private static final String STATUS_SCHEDULED = "SCHEDULED";
    private static final String STATUS_IN_PRODUCTION = "IN_PRODUCTION";
    private static final String STATUS_COMPLETED = "COMPLETED";

    @Autowired
    public ProductionPlanningService(CrmProductionPlanningMapper planningMapper,
                                     CrmProcessAssignmentMapper assignmentMapper,
                                     CrmProductionNotificationMapper notificationMapper,
                                     CrmOrderMapper orderMapper,
                                     CrmOrderItemMapper orderItemMapper,
                                     CrmEngineeringWorkbenchMapper workbenchMapper,
                                     CrmProcessDetailMapper processDetailMapper,
                                     CrmBomDetailItemMapper bomDetailMapper,
                                     DocNoGenerator docNoGenerator) {
        this.planningMapper = planningMapper;
        this.assignmentMapper = assignmentMapper;
        this.notificationMapper = notificationMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.workbenchMapper = workbenchMapper;
        this.processDetailMapper = processDetailMapper;
        this.bomDetailMapper = bomDetailMapper;
        this.docNoGenerator = docNoGenerator;
    }

    /**
     * 获取待转产订单列表（PENDING_PRODUCTION状态）
     */
    public Result<List<CrmOrder>> listPendingProductionOrders() {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmOrder> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("status", "PENDING_PRODUCTION");
        qw.orderByAsc("delivery_date").orderByDesc("created_at");
        List<CrmOrder> list = orderMapper.selectList(qw);

        // 补充每个订单的明细信息
        for (CrmOrder order : list) {
            List<CrmOrderItem> items = orderItemMapper.selectByOrderId(order.getId());
            order.setItems(items);
        }

        return Result.ok(list);
    }

    /**
     * 获取订单详情（含工程转化数据）
     */
    public Result<Map<String, Object>> getOrderPlanningDetail(Long orderId) {
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(40401, "ORDER_NOT_FOUND");
        }

        List<CrmOrderItem> items = orderItemMapper.selectByOrderId(orderId);

        // 获取工程转化工作台信息
        List<CrmEngineeringWorkbench> workbenches = workbenchMapper.selectByOrderId(orderId);

        // 补充工艺和BOM详情
        for (CrmEngineeringWorkbench wb : workbenches) {
            List<CrmProcessDetail> processes = processDetailMapper.selectByWorkbenchId(wb.getId());
            List<CrmBomDetailItem> bomItems = bomDetailMapper.selectByWorkbenchId(wb.getId());
            wb.setProcessDetail(objectMapper.valueToTree(processes).toString());
            wb.setBomDetail(objectMapper.valueToTree(bomItems).toString());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("workbenches", workbenches);

        return Result.ok(result);
    }

    /**
     * 创建排产计划（转工单的第一步）
     */
    @Transactional
    public Result<CrmProductionPlanning> createPlanning(Long orderId, Long plannerUserId, String plannerName) {
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(40401, "ORDER_NOT_FOUND");
        }

        if (!"PENDING_PRODUCTION".equals(order.getStatus())) {
            return Result.fail(40903, "ORDER_NOT_PENDING_PRODUCTION");
        }

        // 检查是否已有排产计划
        CrmProductionPlanning existing = planningMapper.selectByOrderId(orderId);
        if (existing != null) {
            return Result.fail(40905, "PLANNING_ALREADY_EXISTS");
        }

        // 创建排产计划
        CrmProductionPlanning planning = new CrmProductionPlanning();
        planning.setOrderId(orderId);
        planning.setPlanningNo(generatePlanningNo());
        planning.setStatus(STATUS_PENDING);
        planning.setPlannedStart(LocalDateTime.now());
        planning.setPlannedEnd(order.getDeliveryDate().atStartOfDay());
        planning.setPlannerUserId(plannerUserId);
        planning.setPlannerName(plannerName);
        planningMapper.insert(planning);

        // 更新订单状态
        order.setStatus(STATUS_IN_PRODUCTION);
        orderMapper.updateById(order);

        return Result.ok(planning);
    }

    /**
     * 工序分配
     */
    @Transactional
    public Result<CrmProductionPlanning> assignProcesses(Long planningId, List<CrmProcessAssignment> assignments, Long plannerUserId) {
        CrmProductionPlanning planning = planningMapper.selectById(planningId);
        if (planning == null) {
            return Result.fail(40401, "PLANNING_NOT_FOUND");
        }

        // 删除旧的分配
        List<CrmProcessAssignment> oldAssignments = assignmentMapper.selectByPlanningId(planningId);
        for (CrmProcessAssignment old : oldAssignments) {
            assignmentMapper.deleteById(old.getId());
        }

        // 保存新的分配
        int seq = 0;
        for (CrmProcessAssignment assignment : assignments) {
            assignment.setPlanningId(planningId);
            assignment.setSequence(seq++);
            assignmentMapper.insert(assignment);
        }

        // 更新状态
        planning.setStatus(STATUS_ASSIGNED);
        planningMapper.updateById(planning);

        return Result.ok(planning);
    }

    /**
     * 排产确认
     */
    @Transactional
    public Result<Map<String, Object>> confirmScheduling(Long planningId, Long plannerUserId) {
        CrmProductionPlanning planning = planningMapper.selectById(planningId);
        if (planning == null) {
            return Result.fail(40401, "PLANNING_NOT_FOUND");
        }

        List<CrmProcessAssignment> assignments = assignmentMapper.selectByPlanningId(planningId);
        if (assignments.isEmpty()) {
            return Result.fail(40001, "NO_ASSIGNMENTS");
        }

        // 更新状态
        planning.setStatus(STATUS_SCHEDULED);
        planningMapper.updateById(planning);

        // 生成工单通知
        Map<String, Object> result = new HashMap<>();
        result.put("planning", planning);
        result.put("assignments", assignments);
        result.put("message", "排产已确认，工单已排入排产看板");

        return Result.ok(result);
    }

    /**
     * 发送工单通知
     */
    @Transactional
    public Result<List<CrmProductionNotification>> sendNotifications(Long planningId, List<Long> operatorUserIds) {
        CrmProductionPlanning planning = planningMapper.selectById(planningId);
        if (planning == null) {
            return Result.fail(40401, "PLANNING_NOT_FOUND");
        }

        CrmOrder order = orderMapper.selectById(planning.getOrderId());
        List<CrmProductionNotification> notifications = new ArrayList<>();

        for (Long userId : operatorUserIds) {
            CrmProductionNotification notification = new CrmProductionNotification();
            notification.setPlanningId(planningId);
            notification.setRecipientUserId(userId);
            notification.setRecipientType("OPERATOR");
            notification.setChannel("APP");
            notification.setTitle("新工单待生产");
            notification.setContent(String.format("工单已排产，请及时查看。订单号：%s，计划开始：%s",
                    order != null ? order.getOrderNo() : "",
                    planning.getPlannedStart()));
            notification.setStatus("PENDING");
            notificationMapper.insert(notification);
            notifications.add(notification);
        }

        return Result.ok(notifications);
    }

    /**
     * 生成排产计划编号 SCH-YYYYMMDD-NNNN
     */
    private String generatePlanningNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long seq = planSeq.getAndIncrement();
        return String.format("SCH-%s-%04d", date, seq);
    }

    /**
     * 获取排产计划的分配详情
     */
    public Result<Map<String, Object>> getPlanningDetail(Long planningId) {
        CrmProductionPlanning planning = planningMapper.selectById(planningId);
        if (planning == null) {
            return Result.fail(40401, "PLANNING_NOT_FOUND");
        }

        List<CrmProcessAssignment> assignments = assignmentMapper.selectByPlanningId(planningId);
        CrmOrder order = orderMapper.selectById(planning.getOrderId());
        List<CrmProductionNotification> notifications = notificationMapper.selectByPlanningId(planningId);

        Map<String, Object> result = new HashMap<>();
        result.put("planning", planning);
        result.put("assignments", assignments);
        result.put("order", order);
        result.put("notifications", notifications);

        return Result.ok(result);
    }

    /**
     * 获取生管的待排产计划列表
     */
    public Result<List<CrmProductionPlanning>> listPendingPlannings() {
        return Result.ok(planningMapper.selectPending());
    }

    /**
     * 计算预计完成日期（基于工序工时）
     */
    public Result<LocalDateTime> calculateEndDate(Long planningId) {
        CrmProductionPlanning planning = planningMapper.selectById(planningId);
        if (planning == null) {
            return Result.fail(40401, "PLANNING_NOT_FOUND");
        }

        List<CrmProcessAssignment> assignments = assignmentMapper.selectByPlanningId(planningId);

        // 累加工序工时
        long totalMinutes = 0;
        for (CrmProcessAssignment a : assignments) {
            if (a.getPlannedStart() != null && a.getPlannedEnd() != null) {
                totalMinutes += java.time.Duration.between(a.getPlannedStart(), a.getPlannedEnd()).toMinutes();
            }
        }

        LocalDateTime estimatedEnd = planning.getPlannedStart().plusMinutes(totalMinutes);
        return Result.ok(estimatedEnd);
    }
}
