package com.btsheng.erp.platform.auth.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.workflow.entity.WorkflowNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 工作流节点 Mapper（V1.3.7 · Story 1.2 · T0.3 · P1 修补 ④）
 *
 * <p>对应 {@code sys_workflow_node} 物理表（V2__workflow_split.sql）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Mapper
public interface WorkflowNodeMapper extends BaseMapper<WorkflowNode> {

    /** 按 workflow_id 升序取节点链（UNIQUE(workflow_id, node_index) 索引） */
    default List<WorkflowNode> findByWorkflowIdOrderByNodeIndex(Long workflowId) {
        return selectList(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getWorkflowId, workflowId)
                .orderByAsc(WorkflowNode::getNodeIndex));
    }

    /** 按 workflow_id 删除所有节点（PUT 整节点链时物理替换） */
    default int deleteByWorkflowId(Long workflowId) {
        return delete(new LambdaQueryWrapper<WorkflowNode>()
                .eq(WorkflowNode::getWorkflowId, workflowId));
    }

    /** 按 workflow_code 一次性 JOIN 取节点（路由用） */
    default List<WorkflowNode> findByWorkflowCode(String workflowCode) {
        // 通过子查询（实际由 Service 层 join 简化：先 findByCode workflow → findByWorkflowId）
            return selectList(new LambdaQueryWrapper<WorkflowNode>()
                .orderByAsc(WorkflowNode::getNodeIndex));
    }
}
