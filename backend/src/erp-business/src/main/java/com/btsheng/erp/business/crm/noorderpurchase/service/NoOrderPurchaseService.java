package com.btsheng.erp.business.crm.noorderpurchase.service;

import com.btsheng.erp.business.crm.gmsummary.service.GmSummaryService;
import com.btsheng.erp.business.crm.workflowevent.service.WorkflowEventService;
import com.btsheng.erp.business.crm.noorderpurchase.dto.NoOrderPurchaseRequest;
import com.btsheng.erp.business.crm.noorderpurchase.dto.NoOrderPurchaseResponse;
import com.btsheng.erp.business.crm.noorderpurchase.dto.PurchaseReasonDto;
import com.btsheng.erp.business.crm.noorderpurchase.enums.PurchaseReason;
import com.btsheng.erp.business.crm.noorderpurchase.enums.PurchaseSourceType;
import com.btsheng.erp.business.crm.purchaseorder.entity.CrmPurchaseOrder;
import com.btsheng.erp.business.crm.purchaseorder.mapper.CrmPurchaseOrderMapper;
import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.rfq.mapper.CrmRfqVendorMapper;
import com.btsheng.erp.core.model.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * V1.3.8 · Story 4.1 · 无订单采购 Service
 *
 * <p>核心方法：
 * <ul>
 *   <li>{@link #createNoOrderPurchase} AC-4.1.1 创建 NO_ORDER PO</li>
 *   <li>{@link #listPurchaseReasons} AC-4.1.2 字典查询</li>
 *   <li>{@link #determineApprovalRoute} 金额阈值路由（Story 4.2 联动）</li>
 * </ul>
 *
 * <p>AC-4.1.3：复用 1.45 看板 query 参数（purchase_reason / source_type）—— 本期仅服务层预留接口，
 * 看板 query 实装在 Sprint 7 集成阶段。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Service
public class NoOrderPurchaseService {

    private static final Logger log = LoggerFactory.getLogger(NoOrderPurchaseService.class);

    /** 金额阈值（与 Story 4.2 sys_workflow_node threshold 对齐） */
    public static final BigDecimal AMOUNT_PM_THRESHOLD = new BigDecimal("10000");
    public static final BigDecimal AMOUNT_GM_THRESHOLD = new BigDecimal("50000");

    /** 路由常量 */
    public static final String ROUTE_PM = "PROCUREMENT_MANAGER";
    public static final String ROUTE_GM = "GM";
    public static final String ROUTE_GM_PM = "GM+PROCUREMENT_MANAGER";

    private final CrmPurchaseOrderMapper purchaseOrderMapper;
    private final DocNoGenerator docNoGenerator;
    private final GmSummaryService gmSummaryService;
    private final WorkflowEventService workflowEventService;
    private final CrmRfqVendorMapper rfqVendorMapper;

    @Autowired
    public NoOrderPurchaseService(CrmPurchaseOrderMapper purchaseOrderMapper,
                                   DocNoGenerator docNoGenerator,
                                   GmSummaryService gmSummaryService,
                                   WorkflowEventService workflowEventService,
                                   CrmRfqVendorMapper rfqVendorMapper) {
        this.purchaseOrderMapper = purchaseOrderMapper;
        this.docNoGenerator = docNoGenerator;
        this.gmSummaryService = gmSummaryService;
        this.workflowEventService = workflowEventService;
        this.rfqVendorMapper = rfqVendorMapper;
    }

    /**
     * AC-4.1.1：创建无订单采购 PO
     * <p>Bean Validation 在 Controller 层 @Valid 已拦截 purchaseReason 必填，
     * 此处 service 层做业务校验 + 金额聚合 + 路由决策。
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<NoOrderPurchaseResponse> createNoOrderPurchase(NoOrderPurchaseRequest req, Long createdBy) {
        if (req == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "request 必填");
        }

        // 1. purchaseReason 校验（Story 4.1 §1.1：4 枚举）
            PurchaseReason reason = PurchaseReason.fromCode(req.getPurchaseReason());
        if (reason == null) {
            return Result.fail(Result.CODE_PARAM_FORMAT,
                    "invalid purchase_reason: " + req.getPurchaseReason()
                            + " (expected URGENT_REPLENISH / CUSTOMER_ADD / STOCK_SWAP / OTHER)");
        }

        // 2. items 非空已在 Controller @NotEmpty 拦截
        // 3. 聚合金额
            BigDecimal estimatedTotal = BigDecimal.ZERO;
        for (NoOrderPurchaseRequest.Item item : req.getItems()) {
            estimatedTotal = estimatedTotal.add(
                    item.getEstimatedPrice().multiply(new BigDecimal(item.getQuantity())));
        }

        // 4. 路由决策（金额 > 1 万 → PROCUREMENT_MANAGER，> 5 万 → GM+PM）
            String approvalRoute = determineApprovalRoute(estimatedTotal);

        // 5. V1.3.8 Sprint 7 集成 C：真实 INSERT crm_purchase_order
        //    字段：po_no (DocNoGenerator.nextOrderNo) + source_type=NO_ORDER + purchase_reason
        //          + approval_route + approval_status=PENDING + total_amount + supplier_id
            CrmPurchaseOrder po = new CrmPurchaseOrder();
        po.setPoNo(docNoGenerator.nextOrderNo());
        po.setRfqId(null);  // 无订单采购，rfq_id 必为 null
            po.setSupplierId(req.getSupplierId());
        po.setSupplierName(resolveSupplierName(req.getSupplierId()));
        po.setTotalAmount(estimatedTotal);
        po.setStatus("PENDING_SHIP");  // V1.3.8 新枚举
            po.setSourceType(PurchaseSourceType.NO_ORDER.getCode());
        po.setPurchaseReason(req.getPurchaseReason());
        po.setApprovalRoute(approvalRoute);
        po.setApprovalStatus("PENDING");
        po.setRemark(req.getRemark());
        po.setCreatedBy(createdBy);
        po.setCreatedAt(LocalDateTime.now());
        po.setUpdatedAt(LocalDateTime.now());
        purchaseOrderMapper.insert(po);

        // 6. AFTER_COMMIT 失效 gm:summary 缓存（Story 4.3 AC-4.3.2）
            registerGmSummaryEvict();

        NoOrderPurchaseResponse resp = new NoOrderPurchaseResponse();
        resp.setPoId(po.getId());
        resp.setPoNo(po.getPoNo());
        resp.setSourceType(po.getSourceType());
        resp.setPurchaseReason(po.getPurchaseReason());
        resp.setApprovalRoute(approvalRoute);
        resp.setEstimatedTotal(estimatedTotal);

        log.info("[NoOrderPurchaseService] createNoOrderPurchase ok: reason={} total={} route={} poNo={}",
                req.getPurchaseReason(), estimatedTotal, approvalRoute, po.getPoNo());

        // V1.3.8 Sprint 9 Story 9.1：记录 workflow_event CREATED
        // 注意：recordEvent 在事务内调用（不在 AFTER_COMMIT），保证事件与 PO 原子提交
            try {
            workflowEventService.recordEvent(
                    "PO_APPROVAL", po.getId(), po.getPoNo(),
                    WorkflowEventService.EVENT_CREATED,
                    po.getApprovalRoute(),
                    null, null, "创建无订单 PO（" + po.getPurchaseReason() + "）",
                    null, null);
        } catch (Exception e) {
            log.warn("[NoOrderPurchaseService] workflowEventService.recordEvent failed: {}", e.getMessage());
        }

        return Result.ok(resp);
    }

    /**
     * V1.3.8 Sprint 7 集成 A：注册 AFTER_COMMIT 缓存失效
     * <p>保证 DB 事务提交后再清空 gm:summary 缓存，避免幻读
     */
    private void registerGmSummaryEvict() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        gmSummaryService.evictCache();
                    } catch (Exception e) {
                        log.warn("[NoOrderPurchaseService] gmSummaryService.evictCache failed: {}", e.getMessage());
                    }
                }
            });
        } else {
            // 非事务场景直接清缓存
            gmSummaryService.evictCache();
        }
    }

    /**
     * AC-4.1.2：字典查询（4 项 PURCHASE_REASON）
     */
    public Result<List<PurchaseReasonDto>> listPurchaseReasons() {
        List<PurchaseReasonDto> list = new ArrayList<>();
        for (PurchaseReason r : PurchaseReason.values()) {
            list.add(new PurchaseReasonDto(r.getCode(), r.getName(), r.getColor()));
        }
        return Result.ok(list);
    }

    /**
     * 金额阈值路由决策（与 Story 4.2 sys_workflow_node 联动）
     * <p>≤ 1 万：跳过（默认业务自审）
     * <p>1-5 万：PROCUREMENT_MANAGER
     * <p>&gt; 5 万：GM + PROCUREMENT_MANAGER 双签
     */
    public String determineApprovalRoute(BigDecimal amount) {
        if (amount == null) return ROUTE_PM;
        if (amount.compareTo(AMOUNT_PM_THRESHOLD) < 0) {
            return "SELF"; // ≤ 1 万 业务自审
        }
        if (amount.compareTo(AMOUNT_GM_THRESHOLD) < 0) {
            return ROUTE_PM;
        }
        return ROUTE_GM_PM;
    }

    private String resolveSupplierName(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        try {
            String name = rfqVendorMapper.findVendorNameByVendorId(supplierId);
            return name != null && !name.isBlank() ? name : "供应商#" + supplierId;
        } catch (Exception e) {
            log.warn("[NoOrderPurchaseService] supplier lookup failed id={}", supplierId, e);
            return "供应商#" + supplierId;
        }
    }
}