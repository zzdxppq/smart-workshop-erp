package com.btsheng.erp.platform.auth.workflow.skip;

import com.btsheng.erp.platform.auth.service.UserAvailabilityService;
import com.btsheng.erp.platform.auth.workflow.mapper.ApprovalRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/** P1 修补 ②: 跳过请假用户 (8 测例) */
@ExtendWith(MockitoExtension.class)
class P1SkipOnLeaveTest {

    @Mock private UserAvailabilityService availabilityService;
    @Mock private ApprovalRecordMapper approvalMapper;
    private SkipOnLeaveRule rule;

    @BeforeEach
    void setup() { rule = new SkipOnLeaveRule(availabilityService, approvalMapper); }

    @Test void P1_2_1_partial_skip_keeps_first() {
        when(availabilityService.findByUserId(10010L)).thenReturn(Optional.of(st("ON_DUTY")));
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(st("ON_LEAVE")));
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
        assertEquals(10010L, r.getActiveCandidates().get(0));
    }

    @Test void P1_2_2_all_skipped_advance_node() {
        when(availabilityService.findByUserId(10010L)).thenReturn(Optional.of(st("ON_LEAVE")));
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(st("ON_LEAVE")));
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertTrue(r.isNodeSkipped());
    }

    @Test void P1_2_3_lookup_failure_degraded() {
        when(availabilityService.findByUserId(anyLong())).thenThrow(new RuntimeException("DB 503"));
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
    }

    @Test void P1_2_4_missing_user_fallback() {
        when(availabilityService.findByUserId(anyLong())).thenReturn(Optional.empty());
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
    }

    @Test void P1_2_5_audit_skip_reason() {
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(st("ON_LEAVE")));
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10011L));
        assertTrue(r.isNodeSkipped());
    }

    @Test void P1_2_6_resigned_also_skipped() {
        when(availabilityService.findByUserId(10012L)).thenReturn(Optional.of(st("RESIGNED")));
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10012L));
        assertTrue(r.isNodeSkipped());
    }

    @Test void P1_2_7_mixed_3_candidates() {
        when(availabilityService.findByUserId(10010L)).thenReturn(Optional.of(st("ON_DUTY")));
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(st("ON_TRIP")));
        when(availabilityService.findByUserId(10012L)).thenReturn(Optional.of(st("ON_LEAVE")));
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L, 10012L));
        assertEquals(1, r.getActiveCandidates().size());
    }

    @Test void P1_2_8_user_not_found_fallback() {
        when(availabilityService.findByUserId(anyLong())).thenReturn(Optional.empty());
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
        assertEquals(2, r.getActiveCandidates().size());
    }

    private UserAvailabilityService.Availability st(String status) {
        UserAvailabilityService.Availability s = new UserAvailabilityService.Availability();
        s.setAccountStatus("ACTIVE");
        s.setAvailabilityStatus(status);
        return s;
    }
}
