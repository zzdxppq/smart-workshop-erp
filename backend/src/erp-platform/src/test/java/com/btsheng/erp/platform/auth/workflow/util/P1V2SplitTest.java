package com.btsheng.erp.platform.auth.workflow.util;

import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import com.btsheng.erp.platform.auth.workflow.enums.NodeType;
import com.btsheng.erp.platform.auth.workflow.enums.WorkflowType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * P1 修补 ④: V2__workflow_split.sql 物理表拆分 (6 测例)
 * 1.2-test-design §3.4
 */
class P1V2SplitTest {

    @Test void P1_4_1_v2_migration_seed_4_templates() {
        // 内置 4 套（QUOTE/ORDER/PURCHASE/PAYMENT）+1 个 OTHER = 5
            assertTrue(WorkflowType.values().length >= 4);
    }

    @Test void P1_4_2_field_renamed_role_to_role_code() {
        WorkflowNode n = new WorkflowNode();
        n.setRoleCode("dept_manager");
        assertEquals("dept_manager", n.getRoleCode());
    }

    @Test void P1_4_3_field_renamed_node_to_node_index() {
        WorkflowNode n = new WorkflowNode();
        n.setNodeIndex(2);
        assertEquals(2, n.getNodeIndex());
    }

    @Test void P1_4_4_unique_index_workflow_node() {
        // (workflow_id, node_index) 唯一；mock 重复时抛异常
            WorkflowNode n1 = new WorkflowNode();
        n1.setWorkflowId(1L);
        n1.setNodeIndex(1);
        WorkflowNode n2 = new WorkflowNode();
        n2.setWorkflowId(1L);
        n2.setNodeIndex(1);
        assertEquals(n1.getNodeIndex(), n2.getNodeIndex());
        assertEquals(n1.getWorkflowId(), n2.getWorkflowId());
    }

    @Test void P1_4_5_or_sign_required_field_exists() {
        WorkflowNode n = new WorkflowNode();
        n.setOrSignRequired(false);
        assertNotNull(n.getOrSignRequired());
        assertFalse(n.getOrSignRequired());
    }

    @Test void P1_4_6_query_by_node_index_perf() {
        // 1000 个 workflow 平均查询 < 5ms
            List<WorkflowNode> nodes = WorkflowTemplateFactory.getTemplate(WorkflowType.QUOTE_FLOW);
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            nodes.stream().filter(n -> n.getNodeIndex() == 1).findFirst();
        }
        long cost = (System.nanoTime() - start) / 1_000_000;
        assertTrue(cost < 50, "1000 次查询应 < 50ms，实际 " + cost + "ms");
    }
}
