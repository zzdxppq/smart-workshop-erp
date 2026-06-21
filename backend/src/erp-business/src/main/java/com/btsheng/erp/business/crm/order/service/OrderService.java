package com.btsheng.erp.business.crm.order.service;

import com.btsheng.erp.business.crm.order.entity.CrmOrder;
import com.btsheng.erp.business.crm.order.entity.CrmOrderHistory;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import com.btsheng.erp.business.crm.order.entity.CrmOrderPayment;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderHistoryMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderItemMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderMapper;
import com.btsheng.erp.business.crm.order.mapper.CrmOrderPaymentMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.engineer.service.EngineeringWorkbenchService;
import com.btsheng.erp.business.finance.profit.service.ProfitAnalysisService;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.business.integration.client.DictClient;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * V1.3.7 · Story 1.6 · 订单 Service
 *
 * <p>AC-2.3.1 CRUD + AC-2.3.2 状态机 + AC-2.3.3 信用额度 + AC-2.3.4 转下�? *
 * <p>复用 Story 1.5 DocNoGenerator XS 单号 + QuoteApprovalRouter 4 阈值路�? * <br>复用 Story 1.3 sys_dict (黑名�?+ 信用额度) + AuditLog AFTER_COMMIT
 */
@Service
public class OrderService {

    private final CrmOrderMapper orderMapper;
    private final CrmOrderItemMapper itemMapper;
    private final CrmOrderHistoryMapper historyMapper;
    private final CrmOrderPaymentMapper paymentMapper;
    private final DocNoGenerator docNoGenerator;
    private final DictClient dictClient;
    private final ProfitAnalysisService profitAnalysisService;
    private final OrderProductionService orderProductionService;
    private final MaterialNoGenerator materialNoGenerator;
    private final EngineeringWorkbenchService engineeringWorkbenchService;
    private final ObjectMapper mapper = new ObjectMapper();

    // 内部单号计数器（部署阶段可去掉，�?DocNoGenerator 守）
            private final AtomicLong gdSeq = new AtomicLong(1);
    private final AtomicLong wwSeq = new AtomicLong(1);
    private final AtomicLong paymentSeq = new AtomicLong(1);

    // V2.1 状态机守卫
            private static final String STATUS_DRAFT = "DRAFT";
    private static final String STATUS_APPROVED = "APPROVED";      // V2.1 新增：订单提交后直接生效
    private static final String STATUS_PROCESSING = "PROCESSING";   // V2.1 新增：工程转化中
    private static final String STATUS_PENDING_PRODUCTION = "PENDING_PRODUCTION";  // V2.1 新增：待转产
    private static final String STATUS_IN_PRODUCTION = "IN_PRODUCTION";  // V2.1 新增：已转工单
    private static final String STATUS_CONFIRMED = "CONFIRMED";    // 兼容旧版
    private static final String STATUS_PRODUCING = "PRODUCING";   // 兼容旧版
    private static final String STATUS_PARTIAL_SHIPPED = "PARTIAL_SHIPPED";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_SETTLED = "SETTLED";
    private static final String STATUS_CLOSED = "CLOSED";
    private static final String STATUS_CANCELLED = "CANCELLED";

    // 4 阈�?
            private static final BigDecimal SELF_THRESHOLD = new BigDecimal("50000");
    private static final BigDecimal DEPT_THRESHOLD = new BigDecimal("200000");

    @Autowired
    public OrderService(CrmOrderMapper orderMapper, CrmOrderItemMapper itemMapper,
                        CrmOrderHistoryMapper historyMapper, CrmOrderPaymentMapper paymentMapper,
                        DocNoGenerator docNoGenerator, DictClient dictClient,
                        ProfitAnalysisService profitAnalysisService,
                        OrderProductionService orderProductionService,
                        MaterialNoGenerator materialNoGenerator,
                        EngineeringWorkbenchService engineeringWorkbenchService) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
        this.paymentMapper = paymentMapper;
        this.docNoGenerator = docNoGenerator;
        this.dictClient = dictClient;
        this.profitAnalysisService = profitAnalysisService;
        this.orderProductionService = orderProductionService;
        this.materialNoGenerator = materialNoGenerator;
        this.engineeringWorkbenchService = engineeringWorkbenchService;
    }

    // =========================================================
    // T1: CRUD (AC-2.3.1)
    // =========================================================
            @Transactional
    @AuditLog(module = "order", action = "order.create")
    public Result<CrmOrder> createOrder(CrmOrder order, List<CrmOrderItem> items, Long operatorUserId) {
        // 1. 字段校验
            if (order.getCustomerId() == null) return Result.fail(40001, "ORDER_CUSTOMER_REQUIRED");
        if (items != null) {
            for (CrmOrderItem item : items) {
                if (item.getQuantity() != null && item.getQuantity() <= 0) {
                    return Result.fail(40003, "ORDER_QUANTITY_INVALID");
                }
                if (item.getUnitPrice() != null && item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    return Result.fail(40003, "ORDER_UNIT_PRICE_INVALID");
                }
                if (item.getQuantityAdjustment() != null && item.getQuantityAdjustment() < 0) {
                    return Result.fail(40003, "ORDER_QUANTITY_ADJUSTMENT_INVALID");
                }
            }
        }
        if (order.getDeliveryDate() != null && order.getDeliveryDate().isBefore(LocalDate.now())) {
            return Result.fail(40001, "ORDER_DELIVERY_DATE_INVALID");
        }

        // 2. 黑名单校验（V1.3.7 红线 1 + P1 修补�?
            Result<List<Dict>> dicts = null;
        try {
            dicts = dictClient.listByType("CUSTOMER_STATUS");
        } catch (Exception e) {
            // 字典查询失败时不阻断
        }
        if (dicts != null && dicts.getCode() == 0 && dicts.getData() != null) {
            String customerCode = "C" + String.format("%04d", order.getCustomerId()) + "-BL";
            boolean isBlacklisted = dicts.getData().stream()
                .anyMatch(d -> customerCode.equals(d.getDictCode()));
            if (isBlacklisted) {
                return Result.fail(40902, "CUSTOMER_BLACKLIST");
            }
        }

        // 3. 金额自动计算
            BigDecimal total = BigDecimal.ZERO;
        if (items != null && !items.isEmpty()) {
            total = items.stream()
                .map(item -> {
                    BigDecimal price = item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO;
                    int qty = item.getQuantity() != null ? item.getQuantity() : 0;
                    return price.multiply(BigDecimal.valueOf(qty));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        order.setTotalAmount(total);

        // 4. 信用额度校验（V1.3.7 P2 修补 3�?
            int creditCheck = checkCreditLimit(order.getCustomerId(), total);
        if (creditCheck == 40909) {
            return Result.fail(40909, "CREDIT_LIMIT_EXCEEDED");
        }
        order.setCreditLimitCheck(creditCheck == -1 ? -1 : 1);

        // 5. 生成订单�?(1.5 DocNoGenerator 复用)
            order.setOrderNo(docNoGenerator.nextOrderNo());
        order.setStatus(STATUS_DRAFT);

        orderMapper.insert(order);
        if (items != null) {
            int sort = 0;
            for (CrmOrderItem item : items) {
                item.setOrderId(order.getId());
                item.setSort(sort++);
                normalizeOrderItem(item);
                if (item.getQuantityAdjustment() != null && item.getQuantityAdjustment() != 0) {
                    item.setQuantity(item.getQuantity() + item.getQuantityAdjustment());
                }
                item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                itemMapper.insert(item);
            }
        }

        // 6. 留痕
            recordHistory(order.getId(), "CREATE", null, order, operatorUserId);
        recordHistory(order.getId(), "CREDIT_CHECK", null, Map.of(
            "customerId", order.getCustomerId(),
            "creditLimit", creditCheck == -1 ? "UNLIMITED" : String.valueOf(creditCheck),
            "totalAmount", total,
            "exceeded", false
        ), operatorUserId);
        return Result.ok(order);
    }

    /**
     * V2.1 · 订单提交（无需审批，直接生效）
     *
     * <p>核心逻辑：
     * - 订单状态：DRAFT → APPROVED
     * - 对每个明细行：检查图号是否已有料号
     *   - 有 → 复用已有料号
     *   - 无 → 生成新料号（WL-），写入物料主数据
     * - 料号写入订单明细行
     * - 通知工程师进行工程转化
     *
     * @param orderId 订单ID
     * @param operatorUserId 操作人ID
     * @return 提交结果（含料号生成信息）
     */
    @Transactional
    @AuditLog(module = "order", action = "order.submit")
    public Result<Map<String, Object>> submitOrder(Long orderId, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(orderId);
        if (existing == null) {
            return Result.fail(40401, "ORDER_NOT_FOUND");
        }
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            return Result.fail(40903, "ORDER_NOT_SUBMITTABLE");
        }

        if (existing.getDeliveryDate() == null) {
            return Result.fail(40001, "ORDER_DELIVERY_DATE_REQUIRED");
        }

        // 1. 查询订单明细
        List<CrmOrderItem> items = itemMapper.selectByOrderId(orderId);
        if (items == null || items.isEmpty()) {
            return Result.fail(40001, "ORDER_ITEMS_EMPTY");
        }

        for (CrmOrderItem item : items) {
            if (item.getDrawingId() == null) {
                return Result.fail(40001, "ORDER_DRAWING_REQUIRED");
            }
            if (item.getDrawingNo() == null || item.getDrawingNo().isBlank()) {
                return Result.fail(40001, "ORDER_DRAWING_REQUIRED");
            }
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                return Result.fail(40003, "ORDER_QUANTITY_INVALID");
            }
            if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                return Result.fail(40003, "ORDER_UNIT_PRICE_INVALID");
            }
        }

        // 2. 生成/复用料号
        List<MaterialNoGenerator.MaterialNoResult> materialResults =
                materialNoGenerator.generateMaterialNos(items, existing.getOrderNo(), operatorUserId);

        // 3. 更新订单明细行的料号
        for (CrmOrderItem item : items) {
            itemMapper.updateById(item);
        }

        // 4. 订单状态：DRAFT → APPROVED（直接生效，无需审批）
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_APPROVED);
        orderMapper.updateById(existing);

        // 5. 创建工程转化工作台（通知工程师）
        engineeringWorkbenchService.createWorkbenchForOrder(existing, items, operatorUserId);

        // 6. 留痕
        recordHistory(orderId, "SUBMIT", before, existing, operatorUserId);

        // 7. 返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("order", existing);
        result.put("orderNo", existing.getOrderNo());
        result.put("status", STATUS_APPROVED);
        result.put("materialResults", materialResults);
        result.put("message", "订单已提交，无需审批，已通知工程师进行工程转化");

        return Result.ok(result);
    }

    /**
     * V2.1 · 保存订单草稿（含明细行，仅 DRAFT）
     */
    @Transactional
    @AuditLog(module = "order", action = "order.save_draft")
    public Result<CrmOrder> saveDraftWithItems(Long id, CrmOrder order, List<CrmOrderItem> items, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            return Result.fail(40903, "ORDER_NOT_EDITABLE");
        }
        if (order.getCustomerId() == null) {
            return Result.fail(40001, "ORDER_CUSTOMER_REQUIRED");
        }
        if (order.getDeliveryDate() != null && order.getDeliveryDate().isBefore(LocalDate.now())) {
            return Result.fail(40001, "ORDER_DELIVERY_DATE_INVALID");
        }

        CrmOrder before = clone(existing);
        existing.setCustomerId(order.getCustomerId());
        existing.setCustomerName(order.getCustomerName());
        existing.setDeliveryDate(order.getDeliveryDate());
        existing.setIsFa(order.getIsFa());
        existing.setIsNew(order.getIsNew());
        existing.setIsUrgent(order.getIsUrgent());
        existing.setComment(order.getComment());

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmOrderItem> del =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        del.eq("order_id", id);
        itemMapper.delete(del);

        BigDecimal total = BigDecimal.ZERO;
        if (items != null && !items.isEmpty()) {
            int sort = 0;
            for (CrmOrderItem item : items) {
                item.setId(null);
                item.setOrderId(id);
                item.setSort(sort++);
                normalizeOrderItem(item);
                if (item.getQuantity() == null || item.getQuantity() <= 0) {
                    return Result.fail(40003, "ORDER_QUANTITY_INVALID");
                }
                if (item.getUnitPrice() == null || item.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
                    return Result.fail(40003, "ORDER_UNIT_PRICE_INVALID");
                }
                if (item.getDrawingId() == null && (item.getDrawingNo() == null || item.getDrawingNo().isBlank())) {
                    continue;
                }
                item.setAmount(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
                total = total.add(item.getAmount());
                itemMapper.insert(item);
            }
        }
        existing.setTotalAmount(total);
        orderMapper.updateById(existing);
        recordHistory(id, "UPDATE", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    private void normalizeOrderItem(CrmOrderItem item) {
        if (item.getUnitPrice() == null) {
            item.setUnitPrice(BigDecimal.ZERO);
        }
        if (item.getQuantity() == null) {
            item.setQuantity(1);
        }
        if (item.getMaterial() == null || item.getMaterial().isBlank()) {
            item.setMaterial("—");
        }
        if (item.getDrawingNo() == null || item.getDrawingNo().isBlank()) {
            item.setDrawingNo("—");
        }
    }

    /**
     * V2.1 · 批量检查图号是否有已有料号
     */
    public Result<Map<String, String>> checkMaterialNos(List<String> drawingNos) {
        if (drawingNos == null || drawingNos.isEmpty()) {
            return Result.ok(new HashMap<>());
        }
        Map<String, String> result = materialNoGenerator.checkExistingMaterialNos(drawingNos);
        return Result.ok(result);
    }

    @AuditLog(module = "order", action = "order.update")
    public Result<CrmOrder> updateOrder(Long id, CrmOrder order, List<CrmOrderItem> items, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            return Result.fail(40903, "ORDER_NOT_EDITABLE");
        }
        CrmOrder before = clone(existing);
        existing.setCustomerName(order.getCustomerName());
        existing.setDeliveryDate(order.getDeliveryDate());
        existing.setIsFa(order.getIsFa());
        existing.setIsNew(order.getIsNew());
        existing.setIsUrgent(order.getIsUrgent());
        existing.setComment(order.getComment());
        if (items != null && !items.isEmpty()) {
            // 重新计算金额
            BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            existing.setTotalAmount(total);
        }
        orderMapper.updateById(existing);
        recordHistory(id, "UPDATE", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    public Result<CrmOrder> getOrder(Long id) {
        CrmOrder o = orderMapper.selectById(id);
        if (o == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        return Result.ok(o);
    }

    public Result<Map<String, Object>> getOrderWithDetail(Long id) {
        CrmOrder o = orderMapper.selectById(id);
        if (o == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        List<CrmOrderItem> items = itemMapper.selectByOrderId(id);
        List<CrmOrderHistory> history = historyMapper.selectByOrderId(id);
        Map<String, Object> result = new HashMap<>();
        result.put("order", o);
        result.put("items", items);
        result.put("history", history);
        result.put("orderNo", o.getOrderNo());
        result.put("customerName", o.getCustomerName());
        result.put("amount", o.getTotalAmount());
        result.put("status", o.getStatus());
        result.put("deliveryDate", o.getDeliveryDate());
        result.put("createdAt", o.getCreatedAt());
        result.put("productionOrderNo", o.getProductionOrderNo());
        result.put("quoteId", o.getQuoteId());
        Result<Map<String, Object>> profitRes = profitAnalysisService.resolveOrderProfit(id);
        if (profitRes.isSuccess() && profitRes.getData() != null) {
            Map<String, Object> profit = profitRes.getData();
            Map<String, Object> alert = new HashMap<>();
            alert.put("profitRate", profit.get("profitRate"));
            alert.put("alertLevel", profit.get("alertLevel"));
            alert.put("revenue", profit.get("revenue"));
            alert.put("cost", profit.get("cost"));
            alert.put("profit", profit.get("profit"));
            String level = String.valueOf(profit.get("alertLevel"));
            if ("WARNING".equals(level)) {
                alert.put("message", "订单利润率低于预警黄线（" + profit.get("profitRate") + "%）");
            } else if ("CRITICAL".equals(level)) {
                alert.put("message", "订单利润率跌破红线（" + profit.get("profitRate") + "%），请关注成本");
            }
            result.put("profitAlert", alert);
        }
        return Result.ok(result);
    }

    /**
     * 列表查询（按权限过滤：业务员 owner / 经理 dept / 总经�?all�?     */
    public Result<List<CrmOrder>> listOrders(int pageNum, int pageSize, String status, Long customerId,
                                             Long ownerUserId, Long deptId, String role) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmOrder> qw =
            new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        if (status != null) qw.eq("status", status);
        if (customerId != null) qw.eq("customer_id", customerId);
        if (ownerUserId != null) qw.eq("owner_user_id", ownerUserId);
        // 权限过滤
            if ("salesperson".equals(role) && ownerUserId != null) {
            qw.eq("owner_user_id", ownerUserId);
        } else if ("dept_manager".equals(role) && deptId != null) {
            qw.eq("dept_id", deptId);
        }
        // gm 角色见全部，不加过滤
            qw.eq("is_deleted", 0);
        qw.orderByDesc("created_at");
        qw.last("LIMIT " + pageSize + " OFFSET " + (pageNum * pageSize - pageSize));
        return Result.ok(orderMapper.selectList(qw));
    }

    @Transactional
    @AuditLog(module = "order", action = "order.delete")
    public Result<Void> deleteOrder(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        CrmOrder before = clone(existing);
        existing.setIsDeleted(1);
        orderMapper.updateById(existing);
        recordHistory(id, "UPDATE", before, existing, operatorUserId);
        return Result.ok();
    }

    // =========================================================
    // T2: 状态机 (AC-2.3.2)
    // =========================================================
            @Transactional
    @AuditLog(module = "order", action = "order.confirm")
    public Result<CrmOrder> confirmOrder(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_DRAFT.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_CONFIRMED);
        orderMapper.updateById(existing);
        recordHistory(id, "CONFIRM", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.approve")
    public Result<CrmOrder> approveOrder(Long id, Long approverUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_CONFIRMED.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_PRODUCING);
        existing.setCurrentNode(99);
        orderMapper.updateById(existing);
        recordHistory(id, "APPROVE", before, existing, approverUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.reject")
    public Result<CrmOrder> rejectOrder(Long id, String reason, Long approverUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_CONFIRMED.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_DRAFT);
        existing.setComment((existing.getComment() == null ? "" : existing.getComment()) + " [REJECT:" + reason + "]");
        orderMapper.updateById(existing);
        recordHistory(id, "REJECT", before, existing, approverUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.start_production")
    public Result<Map<String, Object>> startProduction(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_CONFIRMED.equals(existing.getStatus())) {
            if (STATUS_PRODUCING.equals(existing.getStatus())
                    && existing.getProductionOrderNo() != null
                    && !existing.getProductionOrderNo().isBlank()) {
                return buildConvertResult(existing, null);
            }
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        Result<Map<String, Object>> woRes = orderProductionService.createWorkorderFromOrder(existing, operatorUserId);
        if (woRes == null || !woRes.isSuccess() || woRes.getData() == null) {
            return woRes != null ? Result.fail(woRes.getCode(), woRes.getMessage()) : Result.fail(50301, "PRODUCTION_SERVICE_UNAVAILABLE");
        }
        Map<String, Object> wo = woRes.getData();
        Object workorderNo = wo.get("workorderNo");
        if (workorderNo != null) {
            existing.setProductionOrderNo(String.valueOf(workorderNo));
        } else if (existing.getProductionOrderNo() == null) {
            existing.setProductionOrderNo(generateProductionNo());
        }
        existing.setStatus(STATUS_PRODUCING);
        orderMapper.updateById(existing);
        recordHistory(id, "CONVERT_PROD", before, existing, operatorUserId);
        return buildConvertResult(existing, wo);
    }

    /**
     * V2.1 待转产订单：状态为 PENDING_PRODUCTION 且尚未生成工单号
     *
     * <p>V2.1 状态机：
     * - DRAFT → APPROVED（订单提交）
     * - APPROVED → PROCESSING（工程师开始工程转化）
     * - PROCESSING → PENDING_PRODUCTION（工程师完成工程转化）
     * - PENDING_PRODUCTION → IN_PRODUCTION（生管转工单）
     */
    public Result<Map<String, Object>> listPendingProduction(int pageNum, int pageSize) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<CrmOrder> qw =
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<>();
        qw.eq("status", STATUS_PENDING_PRODUCTION);
        qw.and(w -> w.isNull("production_order_no").or().eq("production_order_no", ""));
        qw.orderByAsc("delivery_date").orderByDesc("created_at");
        long total = orderMapper.selectCount(qw);
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        qw.last("LIMIT " + pageSize + " OFFSET " + offset);
        List<CrmOrder> list = orderMapper.selectList(qw);
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        return Result.ok(result);
    }

    private Result<Map<String, Object>> buildConvertResult(CrmOrder order, Map<String, Object> workorder) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("order", order);
        payload.put("orderNo", order.getOrderNo());
        payload.put("status", order.getStatus());
        payload.put("productionOrderNo", order.getProductionOrderNo());
        if (workorder != null) {
            payload.put("workorder", workorder);
            payload.put("workorderId", workorder.get("id"));
            payload.put("workorderNo", workorder.get("workorderNo"));
        }
        return Result.ok(payload);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.partial_ship")
    public Result<CrmOrder> partialShip(Long id, Map<Long, Integer> itemShipMap, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_PRODUCING.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        // 累计 shippedQty
            for (Map.Entry<Long, Integer> e : itemShipMap.entrySet()) {
            CrmOrderItem item = itemMapper.selectById(e.getKey());
            if (item == null) continue;
            int newShipped = item.getShippedQty() + e.getValue();
            if (newShipped > item.getQuantity()) {
                return Result.fail(40003, "ORDER_SHIP_QTY_EXCEEDED");
            }
            item.setShippedQty(newShipped);
            itemMapper.updateById(item);
        }
        // 状态推�?
            List<CrmOrderItem> items = itemMapper.selectByOrderId(id);
        boolean allShipped = items.stream().allMatch(it -> it.getShippedQty() >= it.getQuantity());
        existing.setStatus(allShipped ? STATUS_SHIPPED : STATUS_PARTIAL_SHIPPED);
        orderMapper.updateById(existing);
        recordHistory(id, allShipped ? "SHIP" : "PARTIAL_SHIP", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.ship")
    public Result<CrmOrder> ship(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        String st = existing.getStatus();
        if (!STATUS_PRODUCING.equals(st) && !STATUS_PARTIAL_SHIPPED.equals(st)) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        List<CrmOrderItem> items = itemMapper.selectByOrderId(id);
        // 标记全部已发
            for (CrmOrderItem it : items) {
            it.setShippedQty(it.getQuantity());
            itemMapper.updateById(it);
        }
        existing.setStatus(STATUS_SHIPPED);
        orderMapper.updateById(existing);
        recordHistory(id, "SHIP", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.settle")
    public Result<CrmOrder> settle(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_SHIPPED.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_SETTLED);
        orderMapper.updateById(existing);
        // 自动创建回款单（占位 - Epic 9 利润分析 hook 触发�?
            CrmOrderPayment p = new CrmOrderPayment();
        p.setOrderId(id);
        p.setPaymentNo(generatePaymentNo());
        p.setAmount(existing.getTotalAmount());
        p.setPaymentDate(LocalDate.now());
        p.setStatus("PENDING");
        paymentMapper.insert(p);
        recordHistory(id, "SETTLE", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.close")
    public Result<CrmOrder> closeOrder(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_SETTLED.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_CLOSED);
        orderMapper.updateById(existing);
        recordHistory(id, "CLOSE", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.cancel")
    public Result<CrmOrder> cancelOrder(Long id, String reason, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        String st = existing.getStatus();
        if (STATUS_CLOSED.equals(st) || STATUS_CANCELLED.equals(st) || STATUS_SETTLED.equals(st)) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setStatus(STATUS_CANCELLED);
        existing.setComment((existing.getComment() == null ? "" : existing.getComment()) + " [CANCEL:" + reason + "]");
        orderMapper.updateById(existing);
        recordHistory(id, "CANCEL", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @Transactional
    @AuditLog(module = "order", action = "order.transfer_to_outsource")
    public Result<CrmOrder> transferToOutsource(Long id, Long operatorUserId) {
        CrmOrder existing = orderMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        if (!STATUS_CONFIRMED.equals(existing.getStatus()) && !STATUS_PRODUCING.equals(existing.getStatus())) {
            return Result.fail(40904, "ORDER_STATE_INVALID");
        }
        CrmOrder before = clone(existing);
        existing.setOutsourceOrderNo(generateOutsourceNo());
        orderMapper.updateById(existing);
        recordHistory(id, "CONVERT_OUTSUB", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    // =========================================================
    // AC-2.3.3: 信用额度校验 hook
    // =========================================================

    /**
     * 检查客户信用额�?     * @return -1 = 无限制（creditLimit=-1�? 0 = OK / 40909 = 超限
     */
    public int checkCreditLimit(Long customerId, BigDecimal orderAmount) {
        try {
            Result<List<Dict>> dicts = dictClient.listByType("CREDIT_LIMIT");
            if (dicts == null || dicts.getCode() != 0 || dicts.getData() == null) return 0;
            String creditCode = "C" + String.format("%04d", customerId) + "-LIMIT";
            return dicts.getData().stream()
                .filter(d -> creditCode.equals(d.getDictCode()))
                .findFirst()
                .map(d -> {
                    String label = d.getDictLabel();
                    if ("-1".equals(label)) return -1;  // 无限�?
            try {
                        BigDecimal limit = new BigDecimal(label);
                        if (orderAmount.compareTo(limit) > 0) return 40909;
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                    return 0;
                })
                .orElse(0);
        } catch (Exception e) {
            return 0;  // 字典查询失败时不阻断业务
        }
    }

    /**
     * 4 阈值路由决策（复用 1.5 QuoteApprovalRouter）
     */
    public String routeDecision(BigDecimal amount) {
        if (amount == null) return "SELF";
        if (amount.compareTo(SELF_THRESHOLD) < 0) return "SELF";
        if (amount.compareTo(DEPT_THRESHOLD) < 0) return "DEPT_MANAGER_OR_SIGN";
        return "GM_FINANCE_DUAL_SIGN";
    }

    // =========================================================
    // 公共辅助
    // =========================================================
            public void recordHistory(Long orderId, String operation, CrmOrder before, Object afterOrPayload, Long userId) {
        CrmOrderHistory h = new CrmOrderHistory();
        h.setOrderId(orderId);
        h.setOperation(operation);
        h.setChangedBy(userId);
        h.setChangedAt(LocalDateTime.now());
        try {
            h.setBeforeJson(before == null ? null : mapper.writeValueAsString(before));
            h.setAfterJson(afterOrPayload == null ? null : mapper.writeValueAsString(afterOrPayload));
        } catch (Exception e) {
            // ignore
        }
        historyMapper.insert(h);
    }

    private CrmOrder clone(CrmOrder o) {
        try { return mapper.readValue(mapper.writeValueAsString(o), CrmOrder.class); }
        catch (Exception e) { return o; }
    }

    public String generateProductionNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long n = gdSeq.getAndIncrement();
        return String.format("GD%s-%04d", date, n);
    }

    public String generateOutsourceNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long n = wwSeq.getAndIncrement();
        return String.format("WW%s-%04d", date, n);
    }

    public String generatePaymentNo() {
        String date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long n = paymentSeq.getAndIncrement();
        return String.format("SK%s-%04d", date, n);
    }

    /** E2-S3 · 订单时间线（Web 订单详情页） */
    public Result<List<Map<String, Object>>> getOrderTimeline(Long orderId) {
        CrmOrder o = orderMapper.selectById(orderId);
        if (o == null) return Result.fail(40401, "ORDER_NOT_FOUND");
        List<CrmOrderHistory> history = historyMapper.selectByOrderId(orderId);
        List<Map<String, Object>> events = new ArrayList<>();
        for (CrmOrderHistory h : history) {
            Map<String, Object> ev = new HashMap<>();
            ev.put("id", h.getId());
            ev.put("time", h.getChangedAt() != null
                    ? h.getChangedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : null);
            ev.put("title", timelineTitle(h.getOperation()));
            ev.put("detail", timelineDetail(h.getOperation(), h.getChangedBy()));
            ev.put("type", timelineType(h.getOperation()));
            events.add(ev);
        }
        return Result.ok(events);
    }

    private static String timelineTitle(String op) {
        if (op == null) return "状态变更";
        return switch (op) {
            case "CREATE" -> "创建订单";
            case "UPDATE" -> "修改订单";
            case "SUBMIT" -> "提交订单（无需审批）";
            case "CONFIRM" -> "确认订单";
            case "APPROVE" -> "审批通过";
            case "REJECT" -> "审批驳回";
            case "CONVERT_PROD" -> "转生产";
            case "CONVERT_OUTSUB" -> "转委外";
            case "SHIP" -> "发货";
            case "PARTIAL_SHIP" -> "部分发货";
            case "SETTLE" -> "结算";
            case "CLOSE" -> "关闭";
            case "CANCEL" -> "取消";
            case "PROFIT_ANALYSIS" -> "利润分析";
            default -> op;
        };
    }

    private static String timelineDetail(String op, Long changedBy) {
        String actor = changedBy != null ? "操作人 #" + changedBy : "系统";
        if ("CREATE".equals(op)) return "业务员提交 · " + actor;
        if ("SUBMIT".equals(op)) return "订单已生效，已通知工程师 · " + actor;
        if ("APPROVE".equals(op)) return "部门经理 · " + actor;
        if ("CONFIRM".equals(op)) return "客户确认 · " + actor;
        return actor;
    }

    private static String timelineType(String op) {
        if (op == null) return "primary";
        return switch (op) {
            case "APPROVE", "CONVERT_PROD", "SHIP", "SETTLE", "CLOSE", "SUBMIT" -> "success";
            case "REJECT", "CANCEL" -> "danger";
            case "PARTIAL_SHIP" -> "warning";
            default -> "primary";
        };
    }
}
