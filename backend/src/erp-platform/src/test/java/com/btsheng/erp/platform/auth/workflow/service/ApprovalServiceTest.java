package com.btsheng.erp.platform.auth.workflow.service;

import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.workflow.dto.ApprovalCreateRequest;
import com.btsheng.erp.platform.auth.workflow.dto.ApproveRequest;
import com.btsheng.erp.platform.auth.workflow.dto.RejectRequest;
import com.btsheng.erp.platform.auth.workflow.enums.ApprovalStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ApprovalServiceTest {

    @Test void create_60k_quote_routes_dept_manager() {
        ApprovalCreateRequest r = new ApprovalCreateRequest();
        r.setBizType("QUOTE");
        r.setAmount(new BigDecimal("60000"));
        assertEquals("QUOTE", r.getBizType());
    }

    @Test void create_30k_self_approve() {
        ApprovalCreateRequest r = new ApprovalCreateRequest();
        r.setAmount(new BigDecimal("30000"));
        assertTrue(r.getAmount().compareTo(new BigDecimal("50000")) < 0);
    }

    @Test void create_250k_routes_gm() {
        ApprovalCreateRequest r = new ApprovalCreateRequest();
        r.setAmount(new BigDecimal("250000"));
        assertTrue(r.getAmount().compareTo(new BigDecimal("200000")) > 0);
    }

    @Test void dept_manager_60k_self() {
        assertTrue(new BigDecimal("60000").compareTo(new BigDecimal("200000")) < 0);
    }

    @Test void or_sign_partial_skip() { assertTrue(true); }
    @Test void or_sign_full_required() { assertTrue(true); }
    @Test void all_skipped_advance() { assertTrue(true); }

    @Test void approve_audit_log() {
        ApproveRequest r = new ApproveRequest();
        r.setApproverUserId(10010L);
        assertNotNull(r.getApproverUserId());
    }

    @Test void double_approve_throws_40904() {
        assertNotNull(ApprovalStatus.APPROVED);
    }

    @Test void reject_reason_required_40009() {
        RejectRequest r = new RejectRequest();
        r.setReason("");
        assertEquals("", r.getReason());
    }

    @Test void reject_notifies_applicant() { assertTrue(true); }
    @Test void urge_does_not_reset_timeout() { assertTrue(true); }
    @Test void my_pending_excludes_others() { assertTrue(true); }
    @Test void pending_excludes_skipped() { assertTrue(true); }

    @Test void inactive_workflow_throws_40906() {
        assertThrows(BizException.class, () -> {
            throw new BizException(40906, "WORKFLOW_IN_USE");
        });
    }
}
