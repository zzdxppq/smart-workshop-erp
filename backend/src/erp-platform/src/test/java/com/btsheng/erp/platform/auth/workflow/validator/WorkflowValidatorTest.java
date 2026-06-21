package com.btsheng.erp.platform.auth.workflow.validator;

import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowValidatorTest {

    private WorkflowNode n(int idx, String type, BigDecimal th, String role) {
        WorkflowNode wn = new WorkflowNode();
        wn.setNodeIndex(idx);
        wn.setNodeType(type);
        wn.setThreshold(th);
        wn.setRoleCode(role);
        return wn;
    }

    @Test void success_3_nodes_valid() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "START", null, null),
                n(2, "APPROVAL", new BigDecimal("50000"), "salesperson"),
                n(3, "END", null, null));
        assertDoesNotThrow(() -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void success_with_cc_node() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "START", null, null),
                n(2, "APPROVAL", new BigDecimal("50000"), "salesperson"),
                n(3, "CC", null, "gm"),
                n(4, "END", null, null));
        assertDoesNotThrow(() -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void success_null_threshold_top() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "START", null, null),
                n(2, "APPROVAL", new BigDecimal("10000"), "a"),
                n(3, "APPROVAL", new BigDecimal("50000"), "b"),
                n(4, "APPROVAL", null, "gm"),
                n(5, "END", null, null));
        assertDoesNotThrow(() -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void fail_threshold_not_monotonic() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "START", null, null),
                n(2, "APPROVAL", new BigDecimal("200000"), "a"),
                n(3, "APPROVAL", new BigDecimal("50000"), "b"),
                n(4, "END", null, null));
        assertThrows(BizException.class, () -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void fail_first_node_not_start() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "APPROVAL", new BigDecimal("50000"), "a"),
                n(2, "END", null, null));
        assertThrows(BizException.class, () -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void fail_last_node_not_end() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "START", null, null),
                n(2, "APPROVAL", new BigDecimal("50000"), "a"));
        assertThrows(BizException.class, () -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void fail_node_count_1() {
        List<WorkflowNode> nodes = new ArrayList<>();
        nodes.add(n(1, "START", null, null));
        assertThrows(BizException.class, () -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void fail_node_count_21() {
        List<WorkflowNode> nodes = new ArrayList<>();
        nodes.add(n(1, "START", null, null));
        for (int i = 2; i <= 21; i++) nodes.add(n(i, "APPROVAL", new BigDecimal(String.valueOf(10000 * i)), "r" + i));
        assertThrows(BizException.class, () -> WorkflowValidator.validate(nodes, "amount"));
    }

    @Test void fail_amount_field_missing() {
        List<WorkflowNode> nodes = Arrays.asList(
                n(1, "START", null, null),
                n(2, "APPROVAL", new BigDecimal("50000"), "a"),
                n(3, "END", null, null));
        assertThrows(BizException.class, () -> WorkflowValidator.validate(nodes, null));
    }
}
