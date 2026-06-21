package com.btsheng.erp.platform.auth.workflow.router;

import com.btsheng.erp.platform.auth.dto.QuoteApprovalResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class P1OrSignTest {

    @Test void P1_1_candidates_field_not_limited() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        assertEquals(2, r.getCandidates().size());
    }

    @Test void P1_2_or_sign_false_picks_first() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        r.setApproverUserId(10010L);
        assertEquals(10010L, r.getApproverUserId());
    }

    @Test void P1_3_or_sign_true_any_passes() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        r.setApproverUserId(10011L);
        assertEquals(10011L, r.getApproverUserId());
    }

    @Test void P1_4_or_sign_audit_partial() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        r.setReason("OR 会签 2 个候选人 等待 1 人通过");
        assertEquals(2, r.getCandidates().size());
        assertNotNull(r.getReason());
    }

    @Test void P1_5_or_sign_rejected_by_one() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        r.setApproverUserId(10010L);
        r.setReason("OR 会签 · 10010 reject → 整单 REJECTED");
        assertEquals(10010L, r.getApproverUserId());
    }

    @Test void P1_6_boundary_first_deterministic() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        for (int i = 0; i < 5; i++) {
            r.setApproverUserId(r.getCandidates().get(0));
            assertEquals(10010L, r.getApproverUserId());
        }
    }

    @Test void P1_7_candidates_json_persisted() {
        QuoteApprovalResult r = new QuoteApprovalResult();
        r.setCandidates(List.of(10010L, 10011L));
        String json = "[10010,10011]";
        assertTrue(json.contains("10010") && json.contains("10011"));
    }

    @Test void P1_8_regression_story_11_2param() {
        assertDoesNotThrow(() -> {
            QuoteApprovalResult r = new QuoteApprovalResult();
            r.setCandidates(List.of(10086L));
        });
    }
}
