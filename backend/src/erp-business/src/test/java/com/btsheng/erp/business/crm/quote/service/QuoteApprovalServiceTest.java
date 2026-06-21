package com.btsheng.erp.business.crm.quote.service;

import com.btsheng.erp.business.crm.quote.entity.CrmQuote;
import com.btsheng.erp.business.crm.quote.mapper.CrmQuoteMapper;
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

/** V2.1 报价审批 · QuoteApprovalService 测例 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class QuoteApprovalServiceTest {

    @Mock private CrmQuoteMapper quoteMapper;

    private QuoteApprovalService service() {
        return new QuoteApprovalService(quoteMapper, mock(QuoteService.class), new QuoteApprovalRouter());
    }

    @Test void submit_draft_must_go_engineer_first() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus("DRAFT");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        Result<CrmQuote> r = service().submit(1L, 1L);
        assertNotEquals(0, r.getCode());
    }

    @Test void submit_to_engineer_from_draft() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus("DRAFT");
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        Result<CrmQuote> r = service().submitToEngineer(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals(QuoteApprovalService.STATUS_PENDING_ENG, r.getData().getStatus());
    }

    @Test void submit_after_engineer_completed() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus(QuoteApprovalService.STATUS_PENDING_ENG);
        existing.setEngineerCompleted(1);
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        Result<CrmQuote> r = service().submit(1L, 1L);
        assertEquals(0, r.getCode());
        assertEquals(QuoteApprovalService.STATUS_PENDING_APPROVAL, r.getData().getStatus());
        assertEquals(1, r.getData().getCurrentNode());
    }

    @Test void route_30k_self() {
        assertEquals("SELF", service().routeDecision(new BigDecimal("30000")));
    }

    @Test void route_50k_dept_manager() {
        assertEquals("DEPT_MANAGER_OR_SIGN", service().routeDecision(new BigDecimal("50000")));
    }

    @Test void route_250k_gm_finance_dual_sign() {
        assertEquals("GM_FINANCE_DUAL_SIGN", service().routeDecision(new BigDecimal("250000")));
    }

    @Test void approve_gm_finance_requires_two_nodes() {
        CrmQuote existing = new CrmQuote();
        existing.setId(1L);
        existing.setStatus(QuoteApprovalService.STATUS_PENDING_APPROVAL);
        existing.setCurrentNode(1);
        existing.setTotalAmount(new BigDecimal("250000"));
        when(quoteMapper.selectById(1L)).thenReturn(existing);

        Result<CrmQuote> r1 = service().approve(1L, 100L);
        assertEquals(0, r1.getCode());
        assertEquals(QuoteApprovalService.STATUS_PENDING_APPROVAL, r1.getData().getStatus());
        assertEquals(2, r1.getData().getCurrentNode());

        existing.setCurrentNode(2);
        when(quoteMapper.selectById(1L)).thenReturn(existing);
        Result<CrmQuote> r2 = service().approve(1L, 200L);
        assertEquals(0, r2.getCode());
        assertEquals("APPROVED", r2.getData().getStatus());
        assertEquals(99, r2.getData().getCurrentNode());
    }
}
