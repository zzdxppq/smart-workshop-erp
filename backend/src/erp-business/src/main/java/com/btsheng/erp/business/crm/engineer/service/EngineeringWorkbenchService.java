package com.btsheng.erp.business.crm.engineer.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
import com.btsheng.erp.business.crm.quote.entity.CrmQuoteItem;
import com.btsheng.erp.business.crm.quote.service.QuoteProcessService;
import com.btsheng.erp.core.model.Result;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * V2.1 · 工程转化工作台 Service
 */
@Service
public class EngineeringWorkbenchService {

    private final CrmEngineeringWorkbenchMapper workbenchMapper;
    private final CrmProcessDetailMapper processDetailMapper;
    private final CrmBomDetailItemMapper bomDetailMapper;
    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper orderItemMapper;
    private final QuoteProcessService quoteProcessService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // 状态常量
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_COMPLETED = "COMPLETED";

    @Autowired
    public EngineeringWorkbenchService(CrmEngineeringWorkbenchMapper workbenchMapper,
                                     CrmProcessDetailMapper processDetailMapper,
                                     CrmBomDetailItemMapper bomDetailMapper,
                                     CrmOrderMapper orderMapper,
                                     CrmOrderItemMapper orderItemMapper,
                                     QuoteProcessService quoteProcessService) {
        this.workbenchMapper = workbenchMapper;
        this.processDetailMapper = processDetailMapper;
        this.bomDetailMapper = bomDetailMapper;
        this.orderMapper = orderMapper;
        this.orderItemMapper = orderItemMapper;
        this.quoteProcessService = quoteProcessService;
    }

    /**
     * 创建工程转化工作台条目（订单提交后调用）
     * 对每个订单明细行创建一个工作台条目
     */
    @Transactional
    public List<CrmEngineeringWorkbench> createWorkbenchForOrder(CrmOrder order, List<CrmOrderItem> items, Long engineerUserId) {
        List<CrmEngineeringWorkbench> workbenches = new ArrayList<>();

        for (CrmOrderItem item : items) {
            CrmEngineeringWorkbench wb = new CrmEngineeringWorkbench();
            wb.setOrderId(order.getId());
            wb.setOrderItemId(item.getId());
            wb.setDrawingNo(item.getDrawingNo());
            wb.setMaterialNo(item.getMaterialNo());
            wb.setStatus(STATUS_PENDING);
            wb.setProcessStatus(STATUS_PENDING);
            wb.setBomStatus(STATUS_PENDING);
            wb.setEngineerUserId(engineerUserId);
            wb.setTotalHours(BigDecimal.ZERO);
            workbenchMapper.insert(wb);
            workbenches.add(wb);
        }

        return workbenches;
    }

    /**
     * 获取订单的工程转化工作台列表
     */
    public Result<List<CrmEngineeringWorkbench>> getWorkbenchByOrder(Long orderId) {
        List<CrmEngineeringWorkbench> list = workbenchMapper.selectByOrderId(orderId);
        return Result.ok(list);
    }

    /**
     * 确保订单已有工程转化工作台（历史订单补建，V2.1）
     */
    @Transactional
    public Result<List<CrmEngineeringWorkbench>> ensureWorkbenchForOrder(Long orderId, Long engineerUserId) {
        List<CrmEngineeringWorkbench> existing = workbenchMapper.selectByOrderId(orderId);
        if (!existing.isEmpty()) {
            return Result.ok(existing);
        }
        CrmOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            return Result.fail(40401, "ORDER_NOT_FOUND");
        }
        List<CrmOrderItem> items = orderItemMapper.selectByOrderId(orderId);
        if (items == null || items.isEmpty()) {
            return Result.fail(40001, "ORDER_ITEMS_EMPTY");
        }
        return Result.ok(createWorkbenchForOrder(order, items, engineerUserId));
    }

    /**
     * 获取工程师待处理的工作台列表
     */
    public Result<List<CrmEngineeringWorkbench>> getPendingWorkbenches(Long engineerUserId) {
        List<CrmEngineeringWorkbench> list = workbenchMapper.selectPendingByEngineer(engineerUserId);
        return Result.ok(list);
    }

    /**
     * 开始工程转化（领取任务）
     */
    @Transactional
    public Result<CrmEngineeringWorkbench> startWork(Long workbenchId, Long engineerUserId) {
        CrmEngineeringWorkbench wb = workbenchMapper.selectById(workbenchId);
        if (wb == null) {
            return Result.fail(40401, "WORKBENCH_NOT_FOUND");
        }

        wb.setStatus(STATUS_IN_PROGRESS);
        wb.setEngineerUserId(engineerUserId);
        wb.setStartedAt(LocalDateTime.now());
        workbenchMapper.updateById(wb);

        // 更新订单状态为 PROCESSING
        CrmOrder order = orderMapper.selectById(wb.getOrderId());
        if (order != null && "APPROVED".equals(order.getStatus())) {
            order.setStatus("PROCESSING");
            orderMapper.updateById(order);
        }

        return Result.ok(wb);
    }

    /**
     * 工艺明细化 - 保存详细工艺参数
     */
    @Transactional
    public Result<CrmEngineeringWorkbench> saveProcessDetail(Long workbenchId, List<CrmProcessDetail> processes, Long engineerUserId) {
        CrmEngineeringWorkbench wb = workbenchMapper.selectById(workbenchId);
        if (wb == null) {
            return Result.fail(40401, "WORKBENCH_NOT_FOUND");
        }

        // 删除旧的工艺明细
        LambdaQueryWrapper<CrmProcessDetail> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmProcessDetail::getWorkbenchId, workbenchId);
        processDetailMapper.delete(wrapper);

        // 计算总工时
        int totalMinutes = 0;
        int seq = 0;
        for (CrmProcessDetail process : processes) {
            process.setWorkbenchId(workbenchId);
            process.setSequence(seq++);
            if (process.getUnitTimeMinutes() != null) {
                totalMinutes += process.getUnitTimeMinutes();
            }
            processDetailMapper.insert(process);
        }

        // 更新工作台状态
        wb.setProcessStatus(STATUS_IN_PROGRESS);
        wb.setTotalHours(new BigDecimal(totalMinutes).divide(new BigDecimal(60), 2, BigDecimal.ROUND_HALF_UP));

        // 序列化工艺明细到JSON
        try {
            wb.setProcessDetail(objectMapper.writeValueAsString(processes));
        } catch (JsonProcessingException e) {
            // 忽略
        }

        workbenchMapper.updateById(wb);

        return Result.ok(wb);
    }

    /**
     * BOM编制 - 保存BOM明细
     */
    @Transactional
    public Result<CrmEngineeringWorkbench> saveBomDetail(Long workbenchId, List<CrmBomDetailItem> bomItems, Long engineerUserId) {
        CrmEngineeringWorkbench wb = workbenchMapper.selectById(workbenchId);
        if (wb == null) {
            return Result.fail(40401, "WORKBENCH_NOT_FOUND");
        }

        // 删除旧的BOM明细
        LambdaQueryWrapper<CrmBomDetailItem> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CrmBomDetailItem::getWorkbenchId, workbenchId);
        bomDetailMapper.delete(wrapper);

        // 保存新的BOM明细
        int seq = 0;
        for (CrmBomDetailItem item : bomItems) {
            item.setWorkbenchId(workbenchId);
            item.setSequence(seq++);
            bomDetailMapper.insert(item);
        }

        // 更新工作台状态
        wb.setBomStatus(STATUS_IN_PROGRESS);

        // 序列化BOM明细到JSON
        try {
            wb.setBomDetail(objectMapper.writeValueAsString(bomItems));
        } catch (JsonProcessingException e) {
            // 忽略
        }

        workbenchMapper.updateById(wb);

        return Result.ok(wb);
    }

    /**
     * 获取工作台详情（含工艺明细和BOM明细）
     */
    public Result<Map<String, Object>> getWorkbenchDetail(Long workbenchId) {
        CrmEngineeringWorkbench wb = workbenchMapper.selectById(workbenchId);
        if (wb == null) {
            return Result.fail(40401, "WORKBENCH_NOT_FOUND");
        }

        // 获取工艺明细
        List<CrmProcessDetail> processes = processDetailMapper.selectByWorkbenchId(workbenchId);

        // 获取BOM明细
        List<CrmBomDetailItem> bomItems = bomDetailMapper.selectByWorkbenchId(workbenchId);

        // 获取关联的订单明细信息
        CrmOrderItem orderItem = null;
        if (wb.getOrderItemId() != null) {
            orderItem = orderItemMapper.selectById(wb.getOrderItemId());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("workbench", wb);
        result.put("processes", processes);
        result.put("bomItems", bomItems);
        result.put("orderItem", orderItem);

        return Result.ok(result);
    }

    /**
     * 完成工程转化（提交）
     * 将订单状态改为 PENDING_PRODUCTION
     */
    @Transactional
    public Result<CrmOrder> submitWork(Long workbenchId, Long engineerUserId) {
        CrmEngineeringWorkbench wb = workbenchMapper.selectById(workbenchId);
        if (wb == null) {
            return Result.fail(40401, "WORKBENCH_NOT_FOUND");
        }

        // 检查工艺和BOM是否都已完成
        if (wb.getProcessDetail() == null || wb.getProcessDetail().isBlank()) {
            return Result.fail(40001, "PROCESS_NOT_COMPLETED");
        }
        if (wb.getBomDetail() == null || wb.getBomDetail().isBlank()) {
            return Result.fail(40001, "BOM_NOT_COMPLETED");
        }

        // 更新工作台状态
        wb.setStatus(STATUS_COMPLETED);
        wb.setBomStatus(STATUS_COMPLETED);
        wb.setProcessStatus(STATUS_COMPLETED);
        wb.setCompletedAt(LocalDateTime.now());
        workbenchMapper.updateById(wb);

        // 更新订单状态为 PENDING_PRODUCTION
        CrmOrder order = orderMapper.selectById(wb.getOrderId());
        if (order != null) {
            order.setStatus("PENDING_PRODUCTION");
            // 保存详细工艺和BOM到订单
            order.setDetailedProcess(wb.getProcessDetail());
            order.setBomData(wb.getBomDetail());
            orderMapper.updateById(order);
        }

        // 检查是否所有工作台都完成了
        List<CrmEngineeringWorkbench> remaining = workbenchMapper.selectByOrderId(wb.getOrderId());
        boolean allCompleted = remaining.stream().allMatch(w -> STATUS_COMPLETED.equals(w.getStatus()));

        Map<String, Object> result = new HashMap<>();
        result.put("workbench", wb);
        result.put("order", order);
        result.put("allWorkbenchesCompleted", allCompleted);

        return Result.ok(order);
    }

    /**
     * 从报价单引用工艺（如果有历史报价）
     */
    public Result<CrmEngineeringWorkbench> importFromQuote(Long workbenchId, Long quoteItemId, Long engineerUserId) {
        // 从报价明细获取工艺信息
        Result<Map<String, Object>> processResult = quoteProcessService.getProcessInfo(quoteItemId);
        if (!processResult.isSuccess()) {
            return Result.fail(processResult.getCode(), processResult.getMessage());
        }

        CrmEngineeringWorkbench wb = workbenchMapper.selectById(workbenchId);
        if (wb == null) {
            return Result.fail(40401, "WORKBENCH_NOT_FOUND");
        }

        @SuppressWarnings("unchecked")
        List<QuoteProcessService.ProcessDetail> quoteProcesses =
            (List<QuoteProcessService.ProcessDetail>) processResult.getData().get("processes");

        if (quoteProcesses == null || quoteProcesses.isEmpty()) {
            return Result.fail(40001, "NO_PROCESS_IN_QUOTE");
        }

        // 转换工艺明细
        List<CrmProcessDetail> processes = new ArrayList<>();
        int seq = 0;
        for (QuoteProcessService.ProcessDetail qp : quoteProcesses) {
            CrmProcessDetail pd = new CrmProcessDetail();
            pd.setWorkbenchId(workbenchId);
            pd.setSequence(seq++);
            pd.setProcessCode(qp.getProcessCode());
            pd.setProcessName(qp.getProcessName());
            pd.setMachineType(qp.getMachineType());
            pd.setUnitTimeMinutes(qp.getUnitTimeMinutes());
            pd.setCostPerHour(qp.getCostPerHour());
            pd.setOutsourceFlag(qp.getOutsourceFlag());
            pd.setRemark(qp.getRemark());
            processes.add(pd);
        }

        // 保存工艺明细
        Result<CrmEngineeringWorkbench> saveResult = saveProcessDetail(workbenchId, processes, engineerUserId);

        return saveResult;
    }

    /**
     * 根据订单ID获取工程转化进度
     */
    public Result<Map<String, Object>> getOrderEngineeringProgress(Long orderId) {
        List<CrmEngineeringWorkbench> workbenches = workbenchMapper.selectByOrderId(orderId);

        int total = workbenches.size();
        int completed = 0;
        int processCompleted = 0;
        int bomCompleted = 0;

        for (CrmEngineeringWorkbench wb : workbenches) {
            if (STATUS_COMPLETED.equals(wb.getStatus())) {
                completed++;
            }
            if (STATUS_COMPLETED.equals(wb.getProcessStatus()) || STATUS_COMPLETED.equals(wb.getBomStatus())) {
                processCompleted++;
            }
            if (STATUS_COMPLETED.equals(wb.getBomStatus())) {
                bomCompleted++;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalWorkbenches", total);
        result.put("completedWorkbenches", completed);
        result.put("processCompletedCount", processCompleted);
        result.put("bomCompletedCount", bomCompleted);
        result.put("progressPercent", total > 0 ? (completed * 100 / total) : 0);
        result.put("workbenches", workbenches);

        return Result.ok(result);
    }
}
