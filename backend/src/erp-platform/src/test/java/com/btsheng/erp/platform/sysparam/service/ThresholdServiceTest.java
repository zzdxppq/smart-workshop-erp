package com.btsheng.erp.platform.sysparam.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.sysparam.entity.GlobalThreshold;
import com.btsheng.erp.platform.sysparam.mapper.ChangeLogMapper;
import com.btsheng.erp.platform.sysparam.mapper.GlobalThresholdMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.3 · AC-1.3.3 · ThresholdService 双轨（5 测例） */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ThresholdServiceTest {

    @Mock private GlobalThresholdMapper thresholdMapper;
    @Mock private ChangeLogMapper changeLogMapper;

    @Test void get_threshold_nacos_priority() {
        GlobalThreshold t = new GlobalThreshold();
        t.setBizType("QUOTE");
        t.setRoleCode("dept_manager");
        t.setThreshold(new BigDecimal("250000"));
        when(thresholdMapper.selectByBizTypeAndRole("QUOTE", "dept_manager")).thenReturn(t);
        ThresholdService svc = new ThresholdService(thresholdMapper, changeLogMapper);
        Result<GlobalThreshold> r = svc.getThreshold("QUOTE", "dept_manager");
        assertEquals(0, r.getCode());
        assertEquals(new BigDecimal("250000"), r.getData().getThreshold());
    }

    @Test void get_threshold_not_found_40404() {
        when(thresholdMapper.selectByBizTypeAndRole("QUOTE", "xxx")).thenReturn(null);
        ThresholdService svc = new ThresholdService(thresholdMapper, changeLogMapper);
        Result<GlobalThreshold> r = svc.getThreshold("QUOTE", "xxx");
        assertEquals(40404, r.getCode());
    }

    @Test void update_threshold_dual_write() {
        GlobalThreshold t = new GlobalThreshold();
        t.setId(1L);
        t.setBizType("QUOTE");
        t.setRoleCode("dept_manager");
        t.setThreshold(new BigDecimal("200000"));
        when(thresholdMapper.selectByBizTypeAndRole("QUOTE", "dept_manager")).thenReturn(t);
        ThresholdService svc = new ThresholdService(thresholdMapper, changeLogMapper);
        Result<GlobalThreshold> r = svc.updateThreshold("QUOTE", "dept_manager", new BigDecimal("250000"), 1L);
        assertEquals(0, r.getCode());
        assertEquals(new BigDecimal("250000"), r.getData().getThreshold());
    }

    @Test void update_threshold_not_found_40404() {
        when(thresholdMapper.selectByBizTypeAndRole("QUOTE", "xxx")).thenReturn(null);
        ThresholdService svc = new ThresholdService(thresholdMapper, changeLogMapper);
        Result<GlobalThreshold> r = svc.updateThreshold("QUOTE", "xxx", new BigDecimal("250000"), 1L);
        assertEquals(40404, r.getCode());
    }

    @Test void update_threshold_change_log() {
        GlobalThreshold t = new GlobalThreshold();
        t.setId(2L);
        t.setBizType("QUOTE");
        t.setRoleCode("dept_manager");
        t.setThreshold(new BigDecimal("200000"));
        when(thresholdMapper.selectByBizTypeAndRole("QUOTE", "dept_manager")).thenReturn(t);
        ThresholdService svc = new ThresholdService(thresholdMapper, changeLogMapper);
        svc.updateThreshold("QUOTE", "dept_manager", new BigDecimal("250000"), 10010L);
        // changeLogMapper.insert 被调过
            org.mockito.Mockito.verify(changeLogMapper).insert((com.btsheng.erp.platform.sysparam.entity.ChangeLog) org.mockito.ArgumentMatchers.any());
    }
}
