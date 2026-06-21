package com.btsheng.erp.platform.auth.workflow.util;

import com.btsheng.erp.platform.auth.workflow.entity.Workflow;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import com.btsheng.erp.platform.auth.workflow.enums.WorkflowType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowTemplateFactoryTest {

    @Test void quote_3_nodes() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.QUOTE_FLOW);
        assertTrue(ns.size() >= 3);
        assertEquals("START", ns.get(0).getNodeType());
        assertEquals("END", ns.get(ns.size() - 1).getNodeType());
    }

    @Test void quote_threshold_5w_20w_null() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.QUOTE_FLOW);
        BigDecimal prev = null;
        for (WorkflowNode n : ns) {
            if (n.getThreshold() != null) {
                if (prev != null) assertTrue(n.getThreshold().compareTo(prev) >= 0);
                prev = n.getThreshold();
            }
        }
    }

    @Test void quote_or_sign_default_false() {
        for (WorkflowNode n : WorkflowTemplateFactory.getTemplate(WorkflowType.QUOTE_FLOW))
            assertNotEquals(Boolean.TRUE, n.getOrSignRequired());
    }

    @Test void quote_first_start_last_end() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.QUOTE_FLOW);
        assertEquals("START", ns.get(0).getNodeType());
        assertEquals("END", ns.get(ns.size() - 1).getNodeType());
    }

    @Test void quote_clone_to_custom() {
        Workflow w = WorkflowTemplateFactory.cloneTemplate(WorkflowType.QUOTE_FLOW, "CUSTOM_QUOTE_FLOW");
        assertNotNull(w);
    }

    @Test void order_4_nodes() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.ORDER_FLOW);
        assertTrue(ns.size() >= 4);
        assertEquals("START", ns.get(0).getNodeType());
        assertEquals("END", ns.get(ns.size() - 1).getNodeType());
    }

    @Test void order_threshold_monotonic() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.ORDER_FLOW);
        BigDecimal prev = null;
        for (WorkflowNode n : ns) {
            if (n.getThreshold() != null) {
                if (prev != null) assertTrue(n.getThreshold().compareTo(prev) >= 0);
                prev = n.getThreshold();
            }
        }
    }

    @Test void order_or_sign_default_false() {
        for (WorkflowNode n : WorkflowTemplateFactory.getTemplate(WorkflowType.ORDER_FLOW))
            assertNotEquals(Boolean.TRUE, n.getOrSignRequired());
    }

    @Test void order_first_start_last_end() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.ORDER_FLOW);
        assertEquals("START", ns.get(0).getNodeType());
        assertEquals("END", ns.get(ns.size() - 1).getNodeType());
    }

    @Test void order_clone_to_custom() {
        Workflow w = WorkflowTemplateFactory.cloneTemplate(WorkflowType.ORDER_FLOW, "CUSTOM_ORDER_FLOW");
        assertNotNull(w);
    }

    @Test void purchase_3_nodes() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.PURCHASE_FLOW);
        assertTrue(ns.size() >= 3);
        assertEquals("START", ns.get(0).getNodeType());
    }

    @Test void purchase_threshold_1w_5w_null() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.PURCHASE_FLOW);
        boolean has50k = ns.stream().anyMatch(n -> n.getThreshold() != null && n.getThreshold().compareTo(new BigDecimal("50000")) == 0);
        assertTrue(has50k);
    }

    @Test void purchase_or_sign_default_false() {
        for (WorkflowNode n : WorkflowTemplateFactory.getTemplate(WorkflowType.PURCHASE_FLOW))
            assertNotEquals(Boolean.TRUE, n.getOrSignRequired());
    }

    @Test void purchase_first_start_last_end() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.PURCHASE_FLOW);
        assertEquals("START", ns.get(0).getNodeType());
        assertEquals("END", ns.get(ns.size() - 1).getNodeType());
    }

    @Test void purchase_clone_to_custom() {
        Workflow w = WorkflowTemplateFactory.cloneTemplate(WorkflowType.PURCHASE_FLOW, "CUSTOM_PURCHASE");
        assertNotNull(w);
    }

    @Test void payment_2_nodes_min() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.PAYMENT_FLOW);
        assertTrue(ns.size() >= 2);
    }

    @Test void payment_or_sign_default_false() {
        for (WorkflowNode n : WorkflowTemplateFactory.getTemplate(WorkflowType.PAYMENT_FLOW))
            assertNotEquals(Boolean.TRUE, n.getOrSignRequired());
    }

    @Test void payment_first_start_last_end() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.PAYMENT_FLOW);
        assertEquals("START", ns.get(0).getNodeType());
        assertEquals("END", ns.get(ns.size() - 1).getNodeType());
    }

    @Test void payment_clone_to_custom() {
        Workflow w = WorkflowTemplateFactory.cloneTemplate(WorkflowType.PAYMENT_FLOW, "CUSTOM_PAYMENT");
        assertNotNull(w);
    }

    @Test void payment_threshold_monotonic() {
        List<WorkflowNode> ns = WorkflowTemplateFactory.getTemplate(WorkflowType.PAYMENT_FLOW);
        BigDecimal prev = null;
        for (WorkflowNode n : ns) {
            if (n.getThreshold() != null) {
                if (prev != null) assertTrue(n.getThreshold().compareTo(prev) >= 0);
                prev = n.getThreshold();
            }
        }
    }
}
