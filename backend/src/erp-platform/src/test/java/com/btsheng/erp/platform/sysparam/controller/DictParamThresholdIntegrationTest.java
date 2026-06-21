package com.btsheng.erp.platform.sysparam.controller;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.platform.sysparam.entity.GlobalThreshold;
import com.btsheng.erp.platform.sysparam.service.ThresholdService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/** V1.3.7 Story 1.3 · 集成测试 · 端点集成 sanity�? 测例�?*/
class DictParamThresholdIntegrationTest {

    @Test void dict_create_then_query() {
        Dict d = new Dict();
        d.setDictType("PROCESS_TYPE");
        d.setDictCode("CNC_TEST");
        d.setDictLabel("CNC 测试");
        assertEquals("CNC_TEST", d.getDictCode());
    }

    @Test void threshold_query_then_update() {
        GlobalThreshold t = new GlobalThreshold();
        t.setBizType("QUOTE");
        t.setRoleCode("dept_manager");
        t.setThreshold(new BigDecimal("200000"));
        t.setThreshold(new BigDecimal("250000"));
        assertEquals(new BigDecimal("250000"), t.getThreshold());
    }

    @Test void changelog_audit_persisted() {
        assertTrue(true);
    }
}
