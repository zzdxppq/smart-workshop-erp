package com.btsheng.erp.production.outsub.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.util.ErpDocNoGenerator;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.production.allocation.entity.OutsubAllocation;
import com.btsheng.erp.production.allocation.entity.OutsubAllocationVendor;
import com.btsheng.erp.production.allocation.mapper.OutsubAllocationMapper;
import com.btsheng.erp.production.allocation.mapper.OutsubAllocationVendorMapper;
import com.btsheng.erp.production.allocation.service.OutsubAllocationService;
import com.btsheng.erp.production.integration.client.BusinessDrawingLinkClient;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceHistory;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceHistoryMapper;
import com.btsheng.erp.production.outsource.mapper.CrmOutsourceOrderMapper;
import com.btsheng.erp.production.outsource.service.OutsourceService;
import com.btsheng.erp.production.outsub.dto.OutsubOrderCreateRequest;
import com.btsheng.erp.production.workorder.entity.CrmWorkorder;
import com.btsheng.erp.production.workorder.entity.CrmWorkorderStep;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderMapper;
import com.btsheng.erp.production.workorder.mapper.CrmWorkorderStepMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * V1.3.7 · E6-Outsub · 采购选厂商创建 WW- 单（AD-1 职责分离）
 * V1.3.8 · 正式写入 drawingId + crm_drawing_link（biz_type=OUTSOURCE）
 */
@Service
public class OutsubOrderService {

    private static final Logger log = LoggerFactory.getLogger(OutsubOrderService.class);
    public static final String DRAWING_LINK_BIZ_TYPE_OUTSOURCE = "OUTSOURCE";

    private final OutsubAllocationMapper allocationMapper;
    private final OutsubAllocationVendorMapper vendorMapper;
    private final CrmWorkorderMapper workorderMapper;
    private final CrmWorkorderStepMapper stepMapper;
    private final CrmOutsourceOrderMapper orderMapper;
    private final CrmOutsourceHistoryMapper historyMapper;
    private final ErpDocNoGenerator docNoGenerator;
    private final BusinessDrawingLinkClient drawingLinkClient;

    @Autowired
    public OutsubOrderService(OutsubAllocationMapper allocationMapper,
                              OutsubAllocationVendorMapper vendorMapper,
                              CrmWorkorderMapper workorderMapper,
                              CrmWorkorderStepMapper stepMapper,
                              CrmOutsourceOrderMapper orderMapper,
                              CrmOutsourceHistoryMapper historyMapper,
                              ErpDocNoGenerator docNoGenerator,
                              BusinessDrawingLinkClient drawingLinkClient) {
        this.allocationMapper = allocationMapper;
        this.vendorMapper = vendorMapper;
        this.workorderMapper = workorderMapper;
        this.stepMapper = stepMapper;
        this.orderMapper = orderMapper;
        this.historyMapper = historyMapper;
        this.docNoGenerator = docNoGenerator;
        this.drawingLinkClient = drawingLinkClient;
    }

    @Transactional
    @AuditLog(module = "outsub", action = "outsub.create_order")
    public Result<CrmOutsourceOrder> createOrder(OutsubOrderCreateRequest req, Long userId) {
        if (req == null) {
            return Result.fail(40001, "OUTSUB_REQUEST_REQUIRED");
        }
        if (req.getAllocationId() == null) {
            return Result.fail(40001, "OUTSUB_ALLOCATION_REQUIRED");
        }
        if (req.getVendorId() == null) {
            return Result.fail(40001, "OUTSUB_VENDOR_REQUIRED");
        }
        if (req.getUnitPrice() == null || req.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "OUTSUB_UNIT_PRICE_INVALID");
        }
        if (req.getDeliveryDate() == null) {
            return Result.fail(40001, "OUTSUB_DELIVERY_DATE_REQUIRED");
        }
        if (req.getDrawingId() == null) {
            return Result.fail(40001, "OUTSUB_DRAWING_REQUIRED");
        }

        OutsubAllocation allocation = allocationMapper.selectById(req.getAllocationId());
        if (allocation == null) {
            return Result.fail(40401, "ALLOCATION_NOT_FOUND");
        }
        if (!OutsubAllocationService.DECISION_OUTSOURCE.equals(allocation.getDecision())) {
            return Result.fail(40304, "ALLOCATION_NOT_OUTSOURCE");
        }
        // V1.3.7 红线：工序归属由生管决定，采购不可修改
        // stepNo 强制从 allocation.processSeq 读取，忽略请求体中的任何值
        if (hasVendorAssigned(allocation.getId())) {
            return Result.fail(40902, "ALLOCATION_VENDOR_LOCKED");
        }

        CrmWorkorder workorder = workorderMapper.selectById(allocation.getWorkorderId());
        if (workorder == null) {
            return Result.fail(40401, "WORKORDER_NOT_FOUND");
        }

        QueryWrapper<CrmWorkorderStep> stepQ = new QueryWrapper<>();
        stepQ.eq("workorder_id", allocation.getWorkorderId()).eq("step_no", allocation.getProcessSeq());
        CrmWorkorderStep step = stepMapper.selectOne(stepQ);

        String outsourceNo = docNoGenerator.nextOutsourceOrderNo();
        CrmOutsourceOrder order = new CrmOutsourceOrder();
        order.setOutsourceNo(outsourceNo);
        order.setWorkorderNo(workorder.getWorkorderNo());
        order.setStepNo(allocation.getProcessSeq());
        order.setSupplierId(req.getVendorId());
        order.setProcessName(step != null ? step.getStepName() : null);
        order.setMaterialCode(workorder.getMaterialCode());
        order.setDrawingId(req.getDrawingId());
        order.setQty(workorder.getQty() != null ? workorder.getQty() : 1);
        order.setUnitPrice(req.getUnitPrice());
        order.setTotalAmount(req.getUnitPrice().multiply(new BigDecimal(order.getQty())));
        order.setDeliveryDate(req.getDeliveryDate());
        order.setStatus(OutsourceService.STATUS_SENT);
        order.setReworkCount(0);
        order.setCreatorUserId(userId);
        order.setSubmittedAt(LocalDateTime.now());
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        orderMapper.insert(order);

        OutsubAllocationVendor vendor = new OutsubAllocationVendor();
        vendor.setAllocationId(allocation.getId());
        vendor.setVendorId(req.getVendorId());
        vendor.setUnitPrice(req.getUnitPrice());
        vendor.setDeliveryDate(req.getDeliveryDate());
        vendor.setSelectedByUserId(userId);
        vendor.setSelectedAt(LocalDateTime.now());
        vendor.setStatus("CONFIRMED");
        vendorMapper.insert(vendor);

        recordHistory(outsourceNo, "CREATE", null, OutsourceService.STATUS_SENT, userId, "采购委外下单");

        Result<Void> linkResult = linkDrawing(req.getDrawingId(), order.getId(), userId);
        if (!linkResult.isSuccess()) {
            log.warn("委外单 {} 图纸关联失败: {}", outsourceNo, linkResult.getMessage());
            return Result.fail(linkResult.getCode(), linkResult.getMessage());
        }

        return Result.ok(order);
    }

    private Result<Void> linkDrawing(Long drawingId, Long outsourceOrderId, Long userId) {
        Map<String, Object> body = new HashMap<>();
        body.put("drawingId", drawingId);
        body.put("bizType", DRAWING_LINK_BIZ_TYPE_OUTSOURCE);
        body.put("bizId", outsourceOrderId);
        body.put("createdBy", userId);
        try {
            Result<Void> r = drawingLinkClient.createLink(body);
            if (r == null) {
                return Result.fail(50001, "DRAWING_LINK_SERVICE_UNAVAILABLE");
            }
            return r;
        } catch (Exception e) {
            log.error("Feign drawing link failed", e);
            return Result.fail(50001, "DRAWING_LINK_SERVICE_UNAVAILABLE");
        }
    }

    private boolean hasVendorAssigned(Long allocationId) {
        QueryWrapper<OutsubAllocationVendor> q = new QueryWrapper<>();
        q.eq("allocation_id", allocationId);
        return vendorMapper.selectCount(q) > 0;
    }

    private void recordHistory(String outsourceNo, String operation, String fromStatus, String toStatus,
                               Long userId, String note) {
        CrmOutsourceHistory h = new CrmOutsourceHistory();
        h.setOutsourceNo(outsourceNo);
        h.setOperation(operation);
        h.setOperatorUserId(userId);
        h.setFromStatus(fromStatus);
        h.setToStatus(toStatus);
        h.setNote(note);
        h.setOperatedAt(LocalDateTime.now());
        historyMapper.insert(h);
    }
}
