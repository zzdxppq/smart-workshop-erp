package com.btsheng.erp.platform.auth.workflow.skip;

import com.btsheng.erp.platform.auth.service.UserAvailabilityService;
import com.btsheng.erp.platform.auth.workflow.mapper.ApprovalRecordMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkipOnLeaveRuleTest {

    @Mock private UserAvailabilityService availabilityService;
    @Mock private ApprovalRecordMapper approvalMapper;

    @Test void all_on_duty_no_skip() {
        when(availabilityService.findByUserId(10010L)).thenReturn(Optional.of(avail("ON_DUTY")));
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(avail("ON_DUTY")));
        SkipOnLeaveRule rule = new SkipOnLeaveRule(availabilityService, approvalMapper);
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
        assertEquals(2, r.getActiveCandidates().size());
    }

    @Test void partial_skip() {
        when(availabilityService.findByUserId(10010L)).thenReturn(Optional.of(avail("ON_DUTY")));
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(avail("ON_LEAVE")));
        SkipOnLeaveRule rule = new SkipOnLeaveRule(availabilityService, approvalMapper);
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
        assertEquals(1, r.getActiveCandidates().size());
    }

    @Test void all_skipped_advance() {
        when(availabilityService.findByUserId(10010L)).thenReturn(Optional.of(avail("ON_LEAVE")));
        when(availabilityService.findByUserId(10011L)).thenReturn(Optional.of(avail("ON_LEAVE")));
        SkipOnLeaveRule rule = new SkipOnLeaveRule(availabilityService, approvalMapper);
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertTrue(r.isNodeSkipped());
    }

    @Test void lookup_failure_degraded_no_skip() {
        when(availabilityService.findByUserId(anyLong())).thenThrow(new RuntimeException("DB error"));
        SkipOnLeaveRule rule = new SkipOnLeaveRule(availabilityService, approvalMapper);
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
    }

    @Test void missing_user_degraded_no_skip() {
        when(availabilityService.findByUserId(anyLong())).thenReturn(Optional.empty());
        SkipOnLeaveRule rule = new SkipOnLeaveRule(availabilityService, approvalMapper);
        SkipOnLeaveRule.SkipResult r = rule.evaluate(1L, List.of(10010L, 10011L));
        assertFalse(r.isNodeSkipped());
    }

    private UserAvailabilityService.Availability avail(String status) {
        UserAvailabilityService.Availability a = new UserAvailabilityService.Availability();
        a.setUserId(1L);
        a.setAccountStatus("ACTIVE");
        a.setAvailabilityStatus(status);
        return a;
    }
}
