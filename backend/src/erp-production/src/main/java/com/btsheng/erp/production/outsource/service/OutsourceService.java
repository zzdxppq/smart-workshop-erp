package com.btsheng.erp.production.outsource.service;

import com.btsheng.erp.core.integration.dto.OutsourceHistoryPriceResult;
import com.btsheng.erp.core.integration.dto.OutsourceOrderRef;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.production.outsource.dto.OutsourceCreateRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceQueryRequest;
import com.btsheng.erp.production.outsource.dto.OutsourceSubmitRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceHistory;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceItem;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceHistoryMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceItemMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class OutsourceService {

    public static final Pattern OUTSOURCE_NO_PATTERN = Pattern.compile("^WW-?\\d{8}-\\d{4}$");
    public static final int MAX_REWORK_COUNT = 3;

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_ACCEPTED = "ACCEPTED";
    public static final String STATUS_IN_PRODUCTION = "IN_PRODUCTION";
    public static final String STATUS_INSPECTED = "INSPECTED";
    public static final String STATUS_COMPLETED = "COMPLETED";
    public static final String STATUS_CLOSED = "CLOSED";
    public static final String STATUS_REWORK = "REWORK";

    private final CrmOutsourceOrderMapper orderMapper;
    private final CrmOutsourceItemMapper itemMapper;
    private final CrmOutsourceHistoryMapper historyMapper;
    private final ErpDocNoGenerator docNoGenerator;

    @Autowired
    public OutsourceService(CrmOutsourceOrderMapper orderMapper,
                            CrmOutsourceItemMapper itemMapper,
                            CrmOutsourceHistoryMapper historyMapper,
                            ErpDocNoGenerator docNoGenerator) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.historyMapper = historyMapper;
        this.docNoGenerator = docNoGenerator;
    }

    @Transactional
    @AuditLog(module = "outsource", action = "outsource.create")
    public Result<CrmOutsourceOrder> createOutsourceOrder(OutsourceCreateRequest req, Long userId) {
        if (req == null) {
            return Result.fail(40001, "OUTSOURCE_REQUEST_REQUIRED");
        }
        if (req.getWorkorderNo() == null || req.getWorkorderNo().isBlank()) {
            return Result.fail(40001, "OUTSOURCE_WORKORDER_REQUIRED");
        }
        if (req.getSupplierId() == null) {
            return Result.fail(40001, "OUTSOURCE_SUPPLIER_REQUIRED");
        }
        if (req.getQty() == null || req.getQty() <= 0) {
            return Result.fail(40001, "OUTSOURCE_QTY_INVALID");
        }
        if (req.getUnitPrice() == null || req.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "OUTSOURCE_UNIT_PRICE_INVALID");
        }
        if (req.getDeliveryDate() == null) {
            return Result.fail(40001, "OUTSOURCE_DELIVERY_DATE_REQUIRED");
        }

        CrmOutsourceOrder existing = findExisting(req.getWorkorderNo(), req.getStepNo(), req.getSupplierId());
        if (existing != null) {
            return Result.fail(40905, "OUTSOURCE_DUPLICATE");
        }

        String outsourceNo = docNoGenerator.nextOutsourceOrderNo();
        CrmOutsourceOrder order = new CrmOutsourceOrder();
        order.setOutsourceNo(outsourceNo);
        order.setWorkorderNo(req.getWorkorderNo());
        order.setStepNo(req.getStepNo());
        order.setSupplierId(req.getSupplierId());
        order.setSupplierName(req.getSupplierName());
        order.setProcessName(req.getProcessName());
        order.setMaterialCode(req.getMaterialCode());
        order.setQty(req.getQty());
        order.setUnitPrice(req.getUnitPrice());
        order.setTotalAmount(req.getUnitPrice().multiply(new BigDecimal(req.getQty())));
        order.setDeliveryDate(req.getDeliveryDate());
        order.setStatus(STATUS_DRAFT);
        order.setReworkCount(0);
        order.setIsUrgent(req.getIsUrgent() != null ? req.getIsUrgent() : 0);
        order.setRemark(req.getRemark());
        order.setCreatorUserId(userId);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        if (req.getItems() != null) {
            for (OutsourceCreateRequest.OutsourceItemRequest itemReq : req.getItems()) {
                CrmOutsourceItem item = new CrmOutsourceItem();
                item.setOutsourceNo(outsourceNo);
                item.setMaterialCode(itemReq.getMaterialCode());
                item.setMaterialName(itemReq.getMaterialName());
                item.setSpec(itemReq.getSpec());
                item.setQty(itemReq.getQty());
                item.setUnit(itemReq.getUnit());
                item.setUnitPrice(itemReq.getUnitPrice());
                itemMapper.insert(item);
            }
        }

        recordHistory(outsourceNo, "CREATE", null, STATUS_DRAFT, userId, "创建委外单");
        return Result.ok(order);
    }

    @Transactional
    @AuditLog(module = "outsource", action = "outsource.submit")
    public Result<CrmOutsourceOrder> submitOutsource(String outsourceNo, OutsourceSubmitRequest req, Long userId) {
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(outsourceNo);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        if (!STATUS_DRAFT.equals(order.getStatus())) {
            return Result.fail(40903, "OUTSOURCE_STATE_INVALID");
        }

        String note = req != null ? req.getNote() : null;
        order.setStatus(STATUS_SENT);
        order.setSubmittedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        recordHistory(outsourceNo, "SUBMIT", STATUS_DRAFT, STATUS_SENT, userId, note);
        return Result.ok(order);
    }

    @Transactional
    @AuditLog(module = "outsource", action = "outsource.accept")
    public Result<CrmOutsourceOrder> acceptOutsource(String outsourceNo, Long userId) {
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(outsourceNo);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        if (!STATUS_SENT.equals(order.getStatus())) {
            return Result.fail(40903, "OUTSOURCE_STATE_INVALID");
        }

        order.setStatus(STATUS_ACCEPTED);
        order.setAcceptedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        recordHistory(outsourceNo, "ACCEPT", STATUS_SENT, STATUS_ACCEPTED, userId, null);
        return Result.ok(order);
    }

    @Transactional
    @AuditLog(module = "outsource", action = "outsource.complete")
    public Result<CrmOutsourceOrder> completeOutsource(String outsourceNo, Long userId) {
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(outsourceNo);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        if (!STATUS_IN_PRODUCTION.equals(order.getStatus()) && !STATUS_INSPECTED.equals(order.getStatus())) {
            return Result.fail(40903, "OUTSOURCE_STATE_INVALID");
        }

        String fromStatus = order.getStatus();
        order.setStatus(STATUS_COMPLETED);
        order.setCompletedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        recordHistory(outsourceNo, "COMPLETE", fromStatus, STATUS_COMPLETED, userId, null);
        return Result.ok(order);
    }

    @Transactional
    @AuditLog(module = "outsource", action = "outsource.close")
    public Result<CrmOutsourceOrder> closeOutsource(String outsourceNo, Long userId) {
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(outsourceNo);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        if (!STATUS_COMPLETED.equals(order.getStatus())) {
            return Result.fail(40903, "OUTSOURCE_STATE_INVALID");
        }

        order.setStatus(STATUS_CLOSED);
        order.setClosedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        recordHistory(outsourceNo, "CLOSE", STATUS_COMPLETED, STATUS_CLOSED, userId, null);
        return Result.ok(order);
    }

    @Transactional
    @AuditLog(module = "outsource", action = "outsource.rework")
    public Result<CrmOutsourceOrder> reworkOutsource(String outsourceNo, String note, Long userId) {
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(outsourceNo);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        int currentCount = order.getReworkCount() != null ? order.getReworkCount() : 0;
        if (currentCount >= MAX_REWORK_COUNT) {
            return Result.fail(40903, "REWORK_COUNT_EXCEED_MAX_3");
        }
        if (!STATUS_COMPLETED.equals(order.getStatus()) && !STATUS_REWORK.equals(order.getStatus())) {
            return Result.fail(40903, "OUTSOURCE_STATE_INVALID");
        }

        String fromStatus = order.getStatus();
        order.setStatus(STATUS_REWORK);
        order.setReworkCount(currentCount + 1);
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.updateById(order);
        recordHistory(outsourceNo, "REWORK", fromStatus, STATUS_REWORK, userId, note);
        return Result.ok(order);
    }

    public Result<Map<String, Object>> listOutsourceOrders(OutsourceQueryRequest query) {
        int page = query != null && query.getPage() != null ? Math.max(query.getPage(), 0) : 0;
        int size = query != null && query.getSize() != null ? Math.max(query.getSize(), 1) : 20;
        String status = query != null ? query.getStatus() : null;
        String workorderNo = query != null ? query.getWorkorderNo() : null;
        Long supplierId = query != null ? query.getSupplierId() : null;

        List<Map<String, Object>> rows = orderMapper.selectOutsourceOrders(
                status, workorderNo, supplierId, size, page * size);

        Map<String, Object> result = new HashMap<>();
        result.put("items", rows);
        result.put("total", rows.size());
        result.put("page", page);
        result.put("size", size);
        return Result.ok(result);
    }

    public Result<OutsourceOrderRef> getOutsourceOrderById(Long id) {
        if (id == null) {
            return Result.fail(40001, "OUTSOURCE_ID_REQUIRED");
        }
        CrmOutsourceOrder order = orderMapper.selectById(id);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        OutsourceOrderRef ref = new OutsourceOrderRef();
        ref.setId(order.getId());
        ref.setOutsourceNo(order.getOutsourceNo());
        ref.setStatus(order.getStatus());
        return Result.ok(ref);
    }

    public Result<CrmOutsourceOrder> getOutsourceOrder(String outsourceNo) {
        CrmOutsourceOrder order = orderMapper.selectByOutsourceNo(outsourceNo);
        if (order == null) {
            return Result.fail(40404, "OUTSOURCE_ORDER_NOT_FOUND");
        }
        return Result.ok(order);
    }

    public Result<List<CrmOutsourceItem>> listItems(String outsourceNo) {
        return Result.ok(itemMapper.selectByOutsourceNo(outsourceNo));
    }

    public Result<List<CrmOutsourceHistory>> listHistory(String outsourceNo) {
        return Result.ok(historyMapper.selectByOutsourceNo(outsourceNo));
    }

    /** FR-6-3: vendorId + processName，最近 3 次成交价中位数 */
    public Result<OutsourceHistoryPriceResult> getHistoryPrice(Long vendorId, String processName) {
        if (vendorId == null) {
            return Result.fail(40001, "SUPPLIER_ID_REQUIRED");
        }
        if (processName == null || processName.isBlank()) {
            return Result.fail(40001, "PROCESS_NAME_REQUIRED");
        }
        List<BigDecimal> prices = orderMapper.selectRecentPrices(vendorId, processName.trim());
        return Result.ok(buildHistoryPriceResult(vendorId, processName.trim(), null, prices));
    }

    /** 兼容旧接口：按供应商 + 物料编码查最近成交价 */
    public Result<List<BigDecimal>> getPriceHistory(Long supplierId, String materialCode) {
        if (supplierId == null || materialCode == null || materialCode.isBlank()) {
            return Result.ok(List.of());
        }
        return Result.ok(orderMapper.selectRecentPricesByMaterial(supplierId, materialCode.trim()));
    }

    public Result<OutsourceHistoryPriceResult> getPriceSuggest(Long supplierId, String processName,
                                                                String materialCode) {
        if (supplierId == null) {
            return Result.fail(40001, "SUPPLIER_ID_REQUIRED");
        }
        String process = (processName != null && !processName.isBlank()) ? processName.trim() : null;
        String material = (materialCode != null && !materialCode.isBlank()) ? materialCode.trim() : null;
        if (process == null && material == null) {
            return Result.fail(40001, "PROCESS_OR_MATERIAL_REQUIRED");
        }
        List<BigDecimal> prices = process != null
                ? orderMapper.selectRecentPrices(supplierId, process)
                : orderMapper.selectRecentPricesByMaterial(supplierId, material);
        return Result.ok(buildHistoryPriceResult(supplierId, process, material, prices));
    }

    private OutsourceHistoryPriceResult buildHistoryPriceResult(Long vendorId, String processName,
                                                                 String materialCode,
                                                                 List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return OutsourceHistoryPriceResult.empty(vendorId, processName);
        }
        List<BigDecimal> sorted = new ArrayList<>(prices);
        sorted.sort(BigDecimal::compareTo);
        BigDecimal median = sorted.get(sorted.size() / 2);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal p : prices) {
            sum = sum.add(p);
        }
        OutsourceHistoryPriceResult result = new OutsourceHistoryPriceResult();
        result.setVendorId(vendorId);
        result.setProcessName(processName);
        result.setMaterialCode(materialCode);
        result.setHistoryPrices(prices);
        result.setSuggestedPrice(median);
        result.setAvgPrice(sum.divide(new BigDecimal(prices.size()), 2, RoundingMode.HALF_UP));
        result.setSampleCount(prices.size());
        result.setEmpty(false);
        return result;
    }

    private CrmOutsourceOrder findExisting(String workorderNo, Integer stepNo, Long supplierId) {
        if (workorderNo == null || supplierId == null) {
            return null;
        }
        List<CrmOutsourceOrder> all = orderMapper.selectList(null);
        for (CrmOutsourceOrder o : all) {
            if (workorderNo.equals(o.getWorkorderNo())
                    && supplierId.equals(o.getSupplierId())
                    && (stepNo == null || stepNo.equals(o.getStepNo()))) {
                return o;
            }
        }
        return null;
    }

    private void recordHistory(String outsourceNo, String operation, String fromStatus, String toStatus,
                               Long userId, String note) {
        CrmOutsourceHistory hist = new CrmOutsourceHistory();
        hist.setOutsourceNo(outsourceNo);
        hist.setOperation(operation);
        hist.setOperatorUserId(userId);
        hist.setOperatedAt(LocalDateTime.now());
        hist.setFromStatus(fromStatus);
        hist.setToStatus(toStatus);
        hist.setNote(note);
        historyMapper.insert(hist);
    }
}
