package com.btsheng.erp.business.crm.quote;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
import com.btsheng.erp.business.crm.quote.service.QuoteApprovalRouter;
import com.btsheng.erp.business.crm.quote.service.QuoteApprovalService;
import com.btsheng.erp.business.crm.quote.service.QuoteService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** V2.1 报价审批集成测例（Mock · 无 DB） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteApprovalIntegrationTest {

    @Mock private CrmQuoteMapper quoteMapper;

    private QuoteApprovalService svc() {
        return new QuoteApprovalService(quoteMapper, mock(QuoteService.class), new QuoteApprovalRouter());
    }

    @Test void full_flow_draft_to_approved_under_50k() {
        CrmQuote q = new CrmQuote();
        q.setId(1L);
        q.setStatus("DRAFT");
        q.setTotalAmount(new BigDecimal("30000"));
        when(quoteMapper.selectById(1L)).thenReturn(q);

        Result<CrmQuote> eng = svc().submitToEngineer(1L, 1L);
        assertEquals(0, eng.getCode());
        q.setStatus(QuoteApprovalService.STATUS_PENDING_ENG);
        q.setEngineerCompleted(1);

        Result<CrmQuote> sub = svc().submit(1L, 1L);
        assertEquals(0, sub.getCode());
        q.setStatus(QuoteApprovalService.STATUS_PENDING_APPROVAL);
        q.setCurrentNode(1);

        Result<CrmQuote> app = svc().approve(1L, 100L);
        assertEquals(0, app.getCode());
        assertEquals("APPROVED", app.getData().getStatus());
    }

    @Test void approve_self_threshold_single_node() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus(QuoteApprovalService.STATUS_PENDING_APPROVAL);
        existing.setCurrentNode(1);
        existing.setTotalAmount(new BigDecimal("10000"));
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        Result<CrmQuote> r = svc().approve(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals("APPROVED", r.getData().getStatus());
    }

    @Test void reject_pending_approval() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus(QuoteApprovalService.STATUS_PENDING_APPROVAL);
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        Result<CrmQuote> r = svc().reject(1L, "价格过高", 100L);
        assertEquals(0, r.getCode());
        assertEquals("REJECTED", r.getData().getStatus());
        assertTrue(r.getData().getComment().contains("REJECT"));
    }

    @Test void route_50k_dept_manager_or_sign() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus(QuoteApprovalService.STATUS_PENDING_APPROVAL);
        existing.setTotalAmount(new BigDecimal("50000"));
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        assertEquals("DEPT_MANAGER_OR_SIGN", svc().routeDecision(existing.getTotalAmount()));
        Result<CrmQuote> r = svc().approve(1L, 100L);
        assertEquals(0, r.getCode());
        assertEquals("APPROVED", r.getData().getStatus());
    }
}
