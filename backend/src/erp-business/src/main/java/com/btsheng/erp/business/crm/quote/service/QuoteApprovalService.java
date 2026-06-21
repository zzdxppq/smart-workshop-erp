package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteItemMapper;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.AuditLog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 报价审批 Service（V1.3.7 + V2.1 报价流程 + 多级审批 Router）
 */
@Service
public class QuoteApprovalService {

    public static final String STATUS_PENDING_ENG = "PENDING_ENG";
    public static final String STATUS_PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String STATUS_SUBMITTED = "SUBMITTED";

    private static final int NODE_FINAL = 99;
    private static final int NODE_GM_FINANCE_SECOND = 2;

    private final CrmQuoteMapper quoteMapper;
    private final CrmQuoteItemMapper quoteItemMapper;
    private final QuoteService quoteService;
    private final QuoteApprovalRouter approvalRouter;
    private final CrmDrawingMapper drawingMapper;

    @Autowired
    public QuoteApprovalService(CrmQuoteMapper quoteMapper, CrmQuoteItemMapper quoteItemMapper,
                                QuoteService quoteService, QuoteApprovalRouter approvalRouter,
                                CrmDrawingMapper drawingMapper) {
        this.quoteMapper = quoteMapper;
        this.quoteItemMapper = quoteItemMapper;
        this.quoteService = quoteService;
        this.approvalRouter = approvalRouter;
        this.drawingMapper = drawingMapper;
    }

    /** 业务员提交给工程师（DRAFT → PENDING_ENG） */
    @AuditLog(module = "quote", action = "quote.submit_to_engineer")
    public Result<CrmQuote> submitToEngineer(Long id, Long operatorUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        if (!"DRAFT".equals(existing.getStatus())) {
            return Result.fail(40904, "QUOTE_STATE_INVALID");
        }
        CrmQuote before = existing;
        existing.setStatus(STATUS_PENDING_ENG);
        existing.setEngineerCompleted(0);
        existing.setCurrentNode(0);
        quoteMapper.updateById(existing);
        quoteService.recordHistory(id, "SUBMIT_TO_ENGINEER", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    /** 业务员确认后提交审批（PENDING_ENG → PENDING_APPROVAL） */
    @AuditLog(module = "quote", action = "quote.submit")
    public Result<CrmQuote> submit(Long id, Long operatorUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");

        CrmQuote before = existing;
        if (STATUS_PENDING_ENG.equals(existing.getStatus())) {
            if (existing.getEngineerCompleted() == null || existing.getEngineerCompleted() != 1) {
                return Result.fail(40904, "ENGINEER_NOT_COMPLETED");
            }
            existing.setStatus(STATUS_PENDING_APPROVAL);
            existing.setCurrentNode(1);
        } else if ("DRAFT".equals(existing.getStatus())) {
            return Result.fail(40904, "QUOTE_MUST_SUBMIT_TO_ENGINEER_FIRST");
        } else if (isPendingApproval(existing.getStatus())) {
            return Result.fail(40904, "QUOTE_ALREADY_SUBMITTED");
        } else {
            return Result.fail(40904, "QUOTE_STATE_INVALID");
        }

        quoteMapper.updateById(existing);
        quoteService.recordHistory(id, "SUBMIT", before, existing, operatorUserId);
        return Result.ok(existing);
    }

    @AuditLog(module = "quote", action = "quote.approve")
    public Result<CrmQuote> approve(Long id, Long approverUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        if (!isPendingApproval(existing.getStatus())) {
            return Result.fail(40904, "QUOTE_STATE_INVALID");
        }

        BigDecimal amount = existing.getTotalAmount() != null ? existing.getTotalAmount() : BigDecimal.ZERO;
        String route = approvalRouter.routeDecision(amount);
        int node = existing.getCurrentNode() != null ? existing.getCurrentNode() : 1;
        CrmQuote before = existing;

        if ("GM_FINANCE_DUAL_SIGN".equals(route) && node < NODE_GM_FINANCE_SECOND) {
            existing.setCurrentNode(NODE_GM_FINANCE_SECOND);
            quoteMapper.updateById(existing);
            quoteService.recordHistory(id, "APPROVE_NODE_" + node, before, existing, approverUserId);
            return Result.ok(existing);
        }

        existing.setStatus("APPROVED");
        existing.setCurrentNode(NODE_FINAL);
        quoteMapper.updateById(existing);

        // 报价单一经审批通过，所有关联图纸的 quote_approval_status 同步更新为 APPROVED
        for (Long drawingId : quoteItemMapper.selectDrawingIdsByQuoteId(id)) {
            CrmDrawing drawing = drawingMapper.selectById(drawingId);
            if (drawing != null && !"APPROVED".equals(drawing.getQuoteApprovalStatus())) {
                drawing.setQuoteApprovalStatus("APPROVED");
                drawingMapper.updateById(drawing);
            }
        }

        quoteService.recordHistory(id, "APPROVE", before, existing, approverUserId);
        return Result.ok(existing);
    }

    @AuditLog(module = "quote", action = "quote.reject")
    public Result<CrmQuote> reject(Long id, String reason, Long approverUserId) {
        CrmQuote existing = quoteMapper.selectById(id);
        if (existing == null) return Result.fail(40401, "QUOTE_NOT_FOUND");
        if (!isPendingApproval(existing.getStatus())) {
            return Result.fail(40904, "QUOTE_STATE_INVALID");
        }
        CrmQuote before = existing;
        existing.setStatus("REJECTED");
        existing.setComment((existing.getComment() == null ? "" : existing.getComment()) + " [REJECT:" + reason + "]");
        quoteMapper.updateById(existing);

        // 报价单驳回：恢复图纸待审批状态（允许重新报价）
        for (Long drawingId : quoteItemMapper.selectDrawingIdsByQuoteId(id)) {
            CrmDrawing drawing = drawingMapper.selectById(drawingId);
            if (drawing != null && "APPROVED".equals(drawing.getQuoteApprovalStatus())) {
                drawing.setQuoteApprovalStatus("PENDING");
                drawingMapper.updateById(drawing);
            }
        }

        quoteService.recordHistory(id, "REJECT", before, existing, approverUserId);
        return Result.ok(existing);
    }

    public static boolean isPendingApproval(String status) {
        return STATUS_PENDING_APPROVAL.equals(status) || STATUS_SUBMITTED.equals(status);
    }

    public String routeDecision(BigDecimal amount) {
        return approvalRouter.routeDecision(amount);
    }
}
