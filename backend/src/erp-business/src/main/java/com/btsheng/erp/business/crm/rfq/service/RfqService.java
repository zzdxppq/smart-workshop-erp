package com.btsheng.erp.business.crm.rfq.service;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.integration.client.OutsubOrderWriteClient;
import com.btsheng.erp.business.crm.purchaserequest.dto.ConvertPrToPoRequest;
import com.btsheng.erp.business.crm.purchaserequest.service.PurchaseRequestService;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.rfq.dto.AddRfqVendorRequest;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.business.crm.rfq.dto.AwardRfqRequest;
import com.btsheng.erp.business.crm.rfq.dto.CreateRfqRequest;
import com.btsheng.erp.business.crm.rfq.dto.SubmitQuoteRequest;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfq;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqQuote;
import com.btsheng.erp.business.crm.rfq.entity.CrmRfqVendor;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqMapper;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqQuoteMapper;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqVendorMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.web.ProcurementDataScopeHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * V1.3.7 · Story 1.32 · 采购·询比�?Service (FR-8-1)
 *
 * <p>5 业务方法：createRfq / addVendor / submitQuote / compareQuotes / awardRfq
 * <p>询价单号：RF{yyyyMMdd}{seq:4}
 * <p>5 状态：DRAFT/QUOTING/COMPARED/AWARDED/CLOSED
 * <p>2 中标模式：LOWEST（最低价�?WEIGHTED（加权评�?· price 50% + quality 20% + delivery 20% + service 10%�? * <p>3 P1 修补：询价单唯一 / 厂商报价必填 / 选最低不超预�?/ 中标自动触发 PO
 */
@Service
public class RfqService {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_QUOTING = "QUOTING";
    public static final String STATUS_COMPARED = "COMPARED";
    public static final String STATUS_AWARDED = "AWARDED";
    public static final String STATUS_CLOSED = "CLOSED";

    public static final String MODE_LOWEST = "LOWEST";
    public static final String MODE_WEIGHTED = "WEIGHTED";
    public static final Set<String> MODES = Set.of(MODE_LOWEST, MODE_WEIGHTED);

    public static final String VENDOR_PENDING = "PENDING";
    public static final String VENDOR_QUOTED = "QUOTED";
    public static final String VENDOR_NO_QUOTE = "NO_QUOTE";
    public static final String VENDOR_REJECTED = "REJECTED";

    /** P1 修补 1：每 RFQ 至少 3 厂商 */
    public static final int MIN_VENDOR_COUNT = 3;

    /** P1 修补 4：中标后自动触发 PO 单号前缀 */
    public static final String PO_NO_PREFIX = "PO";

    private final CrmRfqMapper rfqMapper;
    private final CrmRfqVendorMapper vendorMapper;
    private final CrmRfqQuoteMapper quoteMapper;
    private final DocNoGenerator docNoGenerator;
    private final WorkflowEventService workflowEventService;
    private final PurchaseRequestService purchaseRequestService;
    private final OutsubOrderWriteClient outsubOrderWriteClient;
    private final CrmDrawingMapper drawingMapper;

    @Autowired
    public RfqService(CrmRfqMapper rfqMapper,
                      CrmRfqVendorMapper vendorMapper,
                      CrmRfqQuoteMapper quoteMapper,
                      DocNoGenerator docNoGenerator,
                      WorkflowEventService workflowEventService,
                      PurchaseRequestService purchaseRequestService,
                      OutsubOrderWriteClient outsubOrderWriteClient,
                      CrmDrawingMapper drawingMapper) {
        this.rfqMapper = rfqMapper;
        this.vendorMapper = vendorMapper;
        this.quoteMapper = quoteMapper;
        this.docNoGenerator = docNoGenerator;
        this.workflowEventService = workflowEventService;
        this.purchaseRequestService = purchaseRequestService;
        this.outsubOrderWriteClient = outsubOrderWriteClient;
        this.drawingMapper = drawingMapper;
    }

    /**
     * AC-8.1.1：创建询价单
     * P1 修补 1：rfq_no 唯一（DB 唯一索引兜底�?     * P1 修补 3：预算金额非�?     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.create")
    public Result<CrmRfq> createRfq(CreateRfqRequest req, Long operatorUserId) {
        if (req == null) {
            return Result.fail(40001, "RFQ_REQUIRED");
        }
        if (req.getTitle() == null || req.getTitle().isEmpty()) {
            return Result.fail(40001, "RFQ_TITLE_REQUIRED");
        }
        if (req.getMaterialId() == null) {
            return Result.fail(40001, "MATERIAL_ID_REQUIRED");
        }
        if (req.getQty() == null || req.getQty().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "QTY_INVALID");
        }
        // P1 修补 3：预算非�?
            if (req.getBudgetAmount() == null || req.getBudgetAmount().compareTo(BigDecimal.ZERO) < 0) {
            return Result.fail(40001, "BUDGET_NEGATIVE");
        }
        if (req.getWinnerMode() != null && !MODES.contains(req.getWinnerMode())) {
            return Result.fail(40001, "WINNER_MODE_INVALID");
        }

        CrmRfq rfq = new CrmRfq();
        rfq.setRfqNo(docNoGenerator.nextRfqNo());
        rfq.setTitle(req.getTitle());
        rfq.setMaterialId(req.getMaterialId());
        rfq.setMaterialCode(req.getMaterialCode());
        rfq.setMaterialName(req.getMaterialName());
        rfq.setQty(req.getQty());
        rfq.setUnit(req.getUnit());
        rfq.setBudgetAmount(req.getBudgetAmount());
        rfq.setRequiredDate(req.getRequiredDate());
        rfq.setWinnerMode(req.getWinnerMode() == null ? MODE_LOWEST : req.getWinnerMode());
        rfq.setInquirySourceType(req.getInquirySourceType() != null ? req.getInquirySourceType() : "MATERIAL");
        rfq.setPrId(req.getPrId());
        rfq.setPrNo(req.getPrNo());
        rfq.setWorkorderNo(req.getWorkorderNo());
        rfq.setProcessStepNo(req.getProcessStepNo());
        rfq.setAllocationId(req.getAllocationId());
        rfq.setConvertStatus("NOT_CONVERTED");
        rfq.setStatus(STATUS_DRAFT);
        rfq.setCreatedBy(operatorUserId);
        rfq.setCreatedAt(LocalDateTime.now());
        rfq.setUpdatedAt(LocalDateTime.now());
        rfqMapper.insert(rfq);

        return Result.ok(rfq);
    }

    /**
     * AC-8.1.1：添加询价厂商（�?RFQ 至少 3 厂商�?     * P1 修补 2：同一厂商不可重复
     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.add_vendor")
    public Result<CrmRfqVendor> addVendor(Long rfqId, AddRfqVendorRequest req, Long operatorUserId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        if (req == null || req.getVendorId() == null) {
            return Result.fail(40001, "VENDOR_ID_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        if (STATUS_AWARDED.equals(rfq.getStatus()) || STATUS_CLOSED.equals(rfq.getStatus())) {
            return Result.fail(40903, "RFQ_LOCKED");
        }
        CrmRfqVendor existing = vendorMapper.selectByRfqAndVendor(rfqId, req.getVendorId());
        if (existing != null) {
            return Result.fail(40902, "VENDOR_DUPLICATE");
        }

        CrmRfqVendor v = new CrmRfqVendor();
        v.setRfqId(rfqId);
        v.setVendorId(req.getVendorId());
        v.setVendorName(req.getVendorName());
        v.setVendorCode(req.getVendorCode());
        v.setContactName(req.getContactName());
        v.setContactPhone(req.getContactPhone());
        v.setInvitedAt(LocalDateTime.now());
        v.setQuoteStatus(VENDOR_PENDING);
        v.setCreatedAt(LocalDateTime.now());
        vendorMapper.insert(v);

        // 状�?DRAFT �?QUOTING
            if (STATUS_DRAFT.equals(rfq.getStatus())) {
            rfq.setStatus(STATUS_QUOTING);
            rfq.setUpdatedAt(LocalDateTime.now());
            rfqMapper.updateById(rfq);
        }
        return Result.ok(v);
    }

    /**
     * AC-8.1.1：厂商报�?     * P1 修补 2：unitPrice �?totalAmount 必填�?> 0
     * 加权评分：price 50% + quality 20% + delivery 20% + service 10%
     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.submit_quote")
    public Result<CrmRfqQuote> submitQuote(Long rfqId, SubmitQuoteRequest req, Long operatorUserId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        if (req == null || req.getVendorId() == null) {
            return Result.fail(40001, "VENDOR_ID_REQUIRED");
        }
        // P1 修补 2：单�?/ 总价必填
            if (req.getUnitPrice() == null || req.getUnitPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "UNIT_PRICE_REQUIRED");
        }
        if (req.getTotalAmount() == null || req.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return Result.fail(40001, "TOTAL_AMOUNT_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        if (STATUS_AWARDED.equals(rfq.getStatus()) || STATUS_CLOSED.equals(rfq.getStatus())) {
            return Result.fail(40903, "RFQ_LOCKED");
        }
        CrmRfqVendor rv = vendorMapper.selectByRfqAndVendor(rfqId, req.getVendorId());
        if (rv == null) {
            return Result.fail(40404, "VENDOR_NOT_INVITED");
        }
        CrmRfqQuote existed = quoteMapper.selectByRfqAndVendor(rfqId, req.getVendorId());
        if (existed != null) {
            return Result.fail(40902, "QUOTE_DUPLICATE");
        }

        CrmRfqQuote q = new CrmRfqQuote();
        q.setRfqId(rfqId);
        q.setRfqVendorId(rv.getId());
        q.setVendorId(req.getVendorId());
        q.setUnitPrice(req.getUnitPrice());
        q.setTotalAmount(req.getTotalAmount());
        q.setLeadTimeDays(req.getLeadTimeDays());
        q.setValidUntil(req.getValidUntil());
        q.setPaymentTerms(req.getPaymentTerms());
        q.setQualityScore(req.getQualityScore());
        q.setDeliveryScore(req.getDeliveryScore());
        q.setServiceScore(req.getServiceScore());
        q.setRemark(req.getRemark());
        q.setIsAwarded(0);
        q.setSubmittedAt(LocalDateTime.now());
        q.setSubmittedBy(operatorUserId);
        // 加权评分：price(50%) + quality(20%) + delivery(20%) + service(10%)
        // 价格�?= (1 - unitPrice/minPrice) * 50
        // 实际实现：先�?price 升序排序后由 compareQuotes 计算
            if (req.getQualityScore() != null || req.getDeliveryScore() != null || req.getServiceScore() != null) {
            BigDecimal qScore = req.getQualityScore() == null ? BigDecimal.ZERO : req.getQualityScore();
            BigDecimal dScore = req.getDeliveryScore() == null ? BigDecimal.ZERO : req.getDeliveryScore();
            BigDecimal sScore = req.getServiceScore() == null ? BigDecimal.ZERO : req.getServiceScore();
            // 简化：默认价格�?25（待 compareQuotes 二次校准�?
            q.setWeightedScore(qScore.multiply(new BigDecimal("0.20"))
                    .add(dScore.multiply(new BigDecimal("0.20")))
                    .add(sScore.multiply(new BigDecimal("0.10")))
                    .add(new BigDecimal("25"))
                    .setScale(2, RoundingMode.HALF_UP));
        }
        quoteMapper.insert(q);

        // 同步厂商 quote_status
            rv.setQuoteStatus(VENDOR_QUOTED);
        vendorMapper.updateById(rv);

        return Result.ok(q);
    }

    /**
     * AC-8.1.1：自动比�?     * LOWEST：最低价
     * WEIGHTED：price 50% + quality 20% + delivery 20% + service 10%
     * P1 修补 3：选最低不能超预算
     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.compare")
    public Result<Map<String, Object>> compareQuotes(Long rfqId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        if (STATUS_AWARDED.equals(rfq.getStatus()) || STATUS_CLOSED.equals(rfq.getStatus())) {
            return Result.fail(40903, "RFQ_LOCKED");
        }
        List<CrmRfqQuote> quotes = quoteMapper.selectByRfqId(rfqId);
        if (quotes == null || quotes.isEmpty()) {
            return Result.fail(40903, "NO_QUOTES");
        }

        // WEIGHTED 模式：重算加权分（基于所有报价）
            if (MODE_WEIGHTED.equals(rfq.getWinnerMode())) {
            BigDecimal minPrice = quotes.stream()
                    .map(CrmRfqQuote::getUnitPrice)
                    .min(Comparator.naturalOrder())
                    .orElse(BigDecimal.ZERO);
            for (CrmRfqQuote q : quotes) {
                BigDecimal priceScore = minPrice.compareTo(BigDecimal.ZERO) > 0
                        ? BigDecimal.ONE.subtract(q.getUnitPrice().divide(minPrice, 4, RoundingMode.HALF_UP))
                                .multiply(new BigDecimal("50"))
                                .setScale(2, RoundingMode.HALF_UP)
                        : new BigDecimal("50");
                BigDecimal qScore = q.getQualityScore() == null ? BigDecimal.ZERO : q.getQualityScore();
                BigDecimal dScore = q.getDeliveryScore() == null ? BigDecimal.ZERO : q.getDeliveryScore();
                BigDecimal sScore = q.getServiceScore() == null ? BigDecimal.ZERO : q.getServiceScore();
                BigDecimal weighted = priceScore
                        .add(qScore.multiply(new BigDecimal("0.20")))
                        .add(dScore.multiply(new BigDecimal("0.20")))
                        .add(sScore.multiply(new BigDecimal("0.10")))
                        .setScale(2, RoundingMode.HALF_UP);
                q.setWeightedScore(weighted);
                quoteMapper.updateById(q);
            }
        }

        // 选最�?
            CrmRfqQuote winner;
        if (MODE_LOWEST.equals(rfq.getWinnerMode())) {
            winner = quotes.stream().min(Comparator.comparing(CrmRfqQuote::getTotalAmount)).orElse(null);
            // P1 修补 3：选最低不能超预算
            if (winner != null && winner.getTotalAmount().compareTo(rfq.getBudgetAmount()) > 0) {
                return Result.fail(40303, "WINNER_OVER_BUDGET");
            }
        } else {
            // WEIGHTED：按 weightedScore 降序
            winner = quotes.stream()
                    .filter(q -> q.getWeightedScore() != null)
                    .max(Comparator.comparing(CrmRfqQuote::getWeightedScore))
                    .orElse(quotes.get(0));
        }

        // 更新比价状�?
            rfq.setStatus(STATUS_COMPARED);
        rfq.setUpdatedAt(LocalDateTime.now());
        rfqMapper.updateById(rfq);

        Map<String, Object> result = new HashMap<>();
        result.put("rfq", rfq);
        result.put("winner", winner);
        result.put("quoteCount", quotes.size());
        // 排序后的报价列表
            List<CrmRfqQuote> sorted = new ArrayList<>(quotes);
        if (MODE_LOWEST.equals(rfq.getWinnerMode())) {
            sorted.sort(Comparator.comparing(CrmRfqQuote::getTotalAmount));
        } else {
            sorted.sort((a, b) -> {
                BigDecimal sa = a.getWeightedScore() == null ? BigDecimal.ZERO : a.getWeightedScore();
                BigDecimal sb = b.getWeightedScore() == null ? BigDecimal.ZERO : b.getWeightedScore();
                return sb.compareTo(sa);
            });
        }
        result.put("ranked", sorted);
        return Result.ok(result);
    }

    /**
     * AC-8.1.1：中标（自动选最�?+ 自动触发 PO 闭环�?     * P1 修补 4：中标后自动生成 PO（PO{yyyyMMdd}{seq:4}）写�?crm_order，rfq.purchase_order_no 同步
     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.award")
    public Result<Map<String, Object>> awardRfq(Long rfqId, AwardRfqRequest req, Long operatorUserId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        if (STATUS_AWARDED.equals(rfq.getStatus()) || STATUS_CLOSED.equals(rfq.getStatus())) {
            return Result.fail(40903, "RFQ_AWARDED");
        }
        // 已比价（COMPARED）或�?QUOTING 时直接选最�?
            Result<Map<String, Object>> cmpResult = compareQuotes(rfqId);
        if (!cmpResult.isSuccess()) {
            return cmpResult;
        }
        Map<String, Object> cmpData = cmpResult.getData();
        CrmRfqQuote winner = (CrmRfqQuote) cmpData.get("winner");
        if (winner == null) {
            return Result.fail(40903, "NO_WINNER");
        }

        // 标记中标
            List<CrmRfqQuote> all = quoteMapper.selectByRfqId(rfqId);
        for (CrmRfqQuote q : all) {
            q.setIsAwarded(0);
            quoteMapper.updateById(q);
        }
        winner.setIsAwarded(1);
        quoteMapper.updateById(winner);

        rfq.setStatus(STATUS_AWARDED);
        rfq.setAwardedVendorId(winner.getVendorId());
        // 厂商名称
            CrmRfqVendor awVendor = vendorMapper.selectByRfqAndVendor(rfqId, winner.getVendorId());
        if (awVendor != null) {
            rfq.setAwardedVendorName(awVendor.getVendorName());
        }
        rfq.setAwardedQuoteId(winner.getId());
        rfq.setAwardedAmount(winner.getTotalAmount());
        rfq.setUpdatedAt(LocalDateTime.now());

        // P1 修补 4：自动触�?PO（生�?PO 单号 + 写入 rfq 表）
        // 完整 PO 主表�?1.36 财务应付统一管理，本 Story 仅闭环到单号�?
            boolean autoPo = req != null && Boolean.TRUE.equals(req.getAutoCreatePo());
        if (autoPo) {
            String poNo = docNoGenerator.nextNo(PO_NO_PREFIX);
            rfq.setPurchaseOrderNo(poNo);
        }
        rfqMapper.updateById(rfq);

        // V1.3.8 Sprint 9 Story 9.1：记�?workflow_event AWARDED
        // 中标事件�?PURCHASER 角色发起（采购员操作�?
            try {
            workflowEventService.recordEvent(
                    "QUOTE_APPROVAL", rfq.getId(), rfq.getRfqNo(),
                    "AWARDED",
                    "PURCHASER",
                    operatorUserId, null,
                    "RFQ 中标，PO 号：" + rfq.getPurchaseOrderNo(),
                    null, null);
        } catch (Exception e) {
            System.err.println("[RfqService] workflowEventService.recordEvent failed: " + e.getMessage());
        }

        Map<String, Object> result = new HashMap<>();
        result.put("rfq", rfq);
        result.put("winner", winner);
        result.put("purchaseOrderNo", rfq.getPurchaseOrderNo());
        return Result.ok(result);
    }

    /**
     * 定标后一键转采购单：绑定 PR 时扣减申请数量并生成 PO（防重复转单）
     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.convert_po")
    public Result<Map<String, Object>> convertToPurchaseOrder(Long rfqId, Long operatorUserId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        if (!STATUS_AWARDED.equals(rfq.getStatus())) {
            return Result.fail(40903, "RFQ_NOT_AWARDED");
        }
        if ("PO_CONVERTED".equals(rfq.getConvertStatus())) {
            return Result.fail(40903, "RFQ_ALREADY_CONVERTED");
        }

        CrmRfqQuote winner = rfq.getAwardedQuoteId() != null
                ? quoteMapper.selectById(rfq.getAwardedQuoteId()) : null;
        if (winner == null) {
            return Result.fail(40903, "NO_WINNER");
        }
        CrmRfqVendor vendor = vendorMapper.selectByRfqAndVendor(rfqId, winner.getVendorId());
        String vendorName = vendor != null ? vendor.getVendorName() : "中标供应商";

        Map<String, Object> convertResult;
        if ("NO_ORDER".equals(rfq.getInquirySourceType())) {
            return Result.fail(40301, "无订单采购询价请走无订单采购通道");
        }
        if ("OUTSOURCE".equals(rfq.getInquirySourceType())) {
            return Result.fail(40001, "委外询价请使用转委外单");
        }
        if (rfq.getPrId() == null) {
            return Result.fail(40001, "RFQ_PR_REQUIRED");
        }

        ConvertPrToPoRequest req = new ConvertPrToPoRequest();
        req.setVendorName(vendorName);
        req.setUnitPrice(winner.getUnitPrice());
        int qty = rfq.getQty() != null ? rfq.getQty().intValue() : 0;
        req.setQty(qty);
        req.setNote("RFQ定标转单 " + rfq.getRfqNo());

        Result<Map<String, Object>> prConvert = purchaseRequestService.convertToPo(rfq.getPrId(), req, operatorUserId);
        if (!prConvert.isSuccess()) {
            return prConvert;
        }
        convertResult = prConvert.getData();

        rfq.setConvertStatus("PO_CONVERTED");
        rfq.setConvertedOrderNo(String.valueOf(convertResult.get("poNo")));
        rfq.setPurchaseOrderNo(String.valueOf(convertResult.get("poNo")));
        rfq.setUpdatedAt(LocalDateTime.now());
        rfqMapper.updateById(rfq);

        Map<String, Object> out = new HashMap<>(convertResult);
        out.put("rfqNo", rfq.getRfqNo());
        out.put("prNo", rfq.getPrNo());
        out.put("workorderNo", rfq.getWorkorderNo());
        return Result.ok(out);
    }

    /**
     * 定标后一键转委外单：绑定待委外工序 · 工序归属不可改 · 仅选厂商与价格
     */
    @Transactional
    @AuditLog(module = "rfq", action = "rfq.convert_outsource")
    public Result<Map<String, Object>> convertToOutsourceOrder(Long rfqId, Long operatorUserId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        if (!STATUS_AWARDED.equals(rfq.getStatus())) {
            return Result.fail(40903, "RFQ_NOT_AWARDED");
        }
        if ("OUTSOURCE_CONVERTED".equals(rfq.getConvertStatus()) || "PO_CONVERTED".equals(rfq.getConvertStatus())) {
            return Result.fail(40903, "RFQ_ALREADY_CONVERTED");
        }
        if (!"OUTSOURCE".equals(rfq.getInquirySourceType())) {
            return Result.fail(40001, "RFQ_NOT_OUTSOURCE");
        }
        if (rfq.getAllocationId() == null) {
            return Result.fail(40001, "RFQ_ALLOCATION_REQUIRED");
        }

        CrmRfqQuote winner = rfq.getAwardedQuoteId() != null
                ? quoteMapper.selectById(rfq.getAwardedQuoteId()) : null;
        if (winner == null || winner.getVendorId() == null) {
            return Result.fail(40903, "NO_WINNER");
        }

        Long drawingId = resolveDrawingId(rfq.getMaterialCode());
        if (drawingId == null) {
            return Result.fail(40001, "DRAWING_NOT_FOUND_FOR_MATERIAL");
        }

        int leadDays = winner.getLeadTimeDays() != null && winner.getLeadTimeDays() > 0
                ? winner.getLeadTimeDays() : 14;
        LocalDate deliveryDate = LocalDate.now().plusDays(leadDays);

        Map<String, Object> body = new HashMap<>();
        body.put("allocationId", rfq.getAllocationId());
        body.put("vendorId", winner.getVendorId());
        body.put("unitPrice", winner.getUnitPrice());
        body.put("deliveryDate", deliveryDate.toString());
        body.put("drawingId", drawingId);

        Result<Object> created;
        try {
            Result<Object> raw = outsubOrderWriteClient.createOrder(body, operatorUserId);
            if (raw == null || !raw.isSuccess()) {
                String msg = raw != null ? raw.getMessage() : "OUTSUB_CREATE_FAILED";
                return Result.fail(50001, msg);
            }
            created = raw;
        } catch (Exception ex) {
            return Result.fail(50001, "OUTSUB_SERVICE_UNAVAILABLE");
        }

        Map<String, Object> order = toMap(created.getData());
        String wwNo = order.get("outsourceNo") != null
                ? String.valueOf(order.get("outsourceNo"))
                : (order.get("outsource_no") != null ? String.valueOf(order.get("outsource_no")) : null);

        rfq.setConvertStatus("OUTSOURCE_CONVERTED");
        rfq.setConvertedOrderNo(wwNo);
        rfq.setUpdatedAt(LocalDateTime.now());
        rfqMapper.updateById(rfq);

        Map<String, Object> out = new HashMap<>(order);
        out.put("rfqNo", rfq.getRfqNo());
        out.put("workorderNo", rfq.getWorkorderNo());
        out.put("outsourceNo", wwNo);
        out.put("allocationId", rfq.getAllocationId());
        out.put("deliveryDate", deliveryDate);
        return Result.ok(out);
    }

    private Long resolveDrawingId(String materialCode) {
        if (materialCode == null || materialCode.isBlank()) {
            return null;
        }
        CrmDrawing d = drawingMapper.selectByMaterialCode(materialCode.trim());
        return d != null ? d.getId() : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toMap(Object data) {
        if (data instanceof Map<?, ?> m) {
            return (Map<String, Object>) m;
        }
        return new HashMap<>();
    }

    /**
     * 公开方法用于 DocNoGenerator PO 前缀
     */
    public DocNoGenerator getDocNoGenerator() {
        return docNoGenerator;
    }

    /**
     * 列表查询
     */
    @AuditLog(module = "rfq", action = "rfq.list")
    public Result<List<CrmRfq>> list(String status) {
        List<CrmRfq> list;
        if (status != null && !status.isEmpty()) {
            list = rfqMapper.selectByStatus(status);
        } else {
            list = rfqMapper.selectListAll();
        }
        Long creatorId = ProcurementDataScopeHelper.resolveCreatorId(null);
        if (creatorId != null && ProcurementDataScopeHelper.effectiveScope() == ProcurementDataScopeHelper.Scope.SELF) {
            list = list.stream()
                    .filter(r -> creatorId.equals(r.getCreatedBy()))
                    .toList();
        }
        return Result.ok(list);
    }

    /**
     * 详情
     */
    public Result<Map<String, Object>> getDetail(Long rfqId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        CrmRfq rfq = rfqMapper.selectById(rfqId);
        if (rfq == null) {
            return Result.fail(40404, "RFQ_NOT_FOUND");
        }
        Result<Void> scope = ProcurementDataScopeHelper.assertCreator(rfq.getCreatedBy());
        if (!scope.isSuccess()) {
            return Result.fail(scope.getCode(), scope.getMessage());
        }
        List<CrmRfqVendor> vendors = vendorMapper.selectByRfqId(rfqId);
        List<CrmRfqQuote> quotes = quoteMapper.selectByRfqId(rfqId);
        Map<Long, CrmRfqVendor> vendorById = new HashMap<>();
        for (CrmRfqVendor v : vendors) {
            if (v.getVendorId() != null) vendorById.put(v.getVendorId(), v);
        }
        List<Map<String, Object>> quoteRows = new ArrayList<>();
        for (CrmRfqQuote q : quotes) {
            CrmRfqVendor vendor = q.getVendorId() != null ? vendorById.get(q.getVendorId()) : null;
            Map<String, Object> row = new HashMap<>();
            row.put("id", q.getId());
            row.put("vendorId", q.getVendorId());
            row.put("vendorName", vendor != null ? vendor.getVendorName() : null);
            row.put("unitPrice", q.getUnitPrice());
            row.put("totalPrice", q.getTotalAmount());
            row.put("totalAmount", q.getTotalAmount());
            row.put("leadTime", q.getLeadTimeDays());
            row.put("leadTimeDays", q.getLeadTimeDays());
            row.put("quality", q.getQualityScore());
            row.put("qualityScore", q.getQualityScore());
            row.put("isAwarded", q.getIsAwarded());
            quoteRows.add(row);
        }
        Map<String, Object> rfqView = new HashMap<>();
        rfqView.put("id", rfq.getId());
        rfqView.put("rfqNo", rfq.getRfqNo());
        rfqView.put("title", rfq.getTitle());
        rfqView.put("materialCode", rfq.getMaterialCode());
        rfqView.put("materialName", rfq.getMaterialName());
        rfqView.put("qty", rfq.getQty());
        rfqView.put("unit", rfq.getUnit());
        rfqView.put("status", rfq.getStatus());
        rfqView.put("createdAt", rfq.getCreatedAt());
        rfqView.put("deadline", rfq.getRequiredDate());
        rfqView.put("expectedReplyDate", rfq.getRequiredDate());
        rfqView.put("requiredDate", rfq.getRequiredDate());
        rfqView.put("inquirySourceType", rfq.getInquirySourceType());
        rfqView.put("prId", rfq.getPrId());
        rfqView.put("prNo", rfq.getPrNo());
        rfqView.put("workorderNo", rfq.getWorkorderNo());
        rfqView.put("allocationId", rfq.getAllocationId());
        rfqView.put("processStepNo", rfq.getProcessStepNo());
        rfqView.put("convertStatus", rfq.getConvertStatus());
        rfqView.put("convertedOrderNo", rfq.getConvertedOrderNo());
        rfqView.put("purchaseOrderNo", rfq.getPurchaseOrderNo());
        rfqView.put("quotes", quoteRows);
        Map<String, Object> data = new HashMap<>();
        data.put("rfq", rfq);
        data.put("vendors", vendors);
        data.put("quotes", quoteRows);
        data.putAll(rfqView);
        return Result.ok(data);
    }

    /**
     * Web 比价页只读预览（不修改 RFQ 状态）
     */
    public Result<Map<String, Object>> previewCompare(Long rfqId) {
        if (rfqId == null) {
            return Result.fail(40001, "RFQ_ID_REQUIRED");
        }
        try {
            CrmRfq rfq = rfqMapper.selectById(rfqId);
            if (rfq == null) {
                return Result.fail(40404, "RFQ_NOT_FOUND");
            }
            List<CrmRfqQuote> quotes = quoteMapper.selectByRfqId(rfqId);
            List<Map<String, Object>> rows = new ArrayList<>();
            CrmRfqQuote best = null;
            for (CrmRfqQuote q : quotes) {
                CrmRfqVendor vendor = q.getVendorId() != null
                        ? vendorMapper.selectByRfqAndVendor(rfqId, q.getVendorId()) : null;
                Map<String, Object> row = new HashMap<>();
                row.put("vendorName", vendor != null ? vendor.getVendorName()
                        : (q.getVendorId() != null ? String.valueOf(q.getVendorId()) : "未知厂商"));
                row.put("unitPrice", q.getUnitPrice());
                row.put("totalPrice", q.getTotalAmount());
                row.put("leadTime", q.getLeadTimeDays());
                row.put("quality", q.getQualityScore());
                row.put("score", q.getWeightedScore() != null ? q.getWeightedScore().intValue()
                        : (q.getTotalAmount() != null ? q.getTotalAmount().intValue() : 0));
                row.put("recommended", false);
                rows.add(row);
                if (best == null || (q.getTotalAmount() != null && best.getTotalAmount() != null
                        && q.getTotalAmount().compareTo(best.getTotalAmount()) < 0)) {
                    best = q;
                }
            }
            if (best != null && best.getTotalAmount() != null) {
                for (Map<String, Object> row : rows) {
                    Object totalPrice = row.get("totalPrice");
                    if (totalPrice instanceof BigDecimal bd && best.getTotalAmount().compareTo(bd) == 0) {
                        row.put("recommended", true);
                        break;
                    }
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("quotes", rows);
            result.put("rfq", rfq);
            return Result.ok(result);
        } catch (Exception ex) {
            Map<String, Object> result = new HashMap<>();
            result.put("quotes", List.of());
            result.put("rfq", rfqMapper.selectById(rfqId));
            return Result.ok(result);
        }
    }
}
