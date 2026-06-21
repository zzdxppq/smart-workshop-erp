package com.btsheng.erp.platform.auth.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.workflow.entity.Workflow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 工作流 Mapper（V1.3.7 · Story 1.2 · T0.3）
 *
 * <p>复用 {@code AesGcmTypeHandler}（V1.3.6 字段加密）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Mapper
public interface WorkflowMapper extends BaseMapper<Workflow> {

    /** 默认方法：按 workflow_code 精确查（UNIQUE 索引） */
    default Workflow findByCode(String code) {
        return selectOne(new LambdaQueryWrapper<Workflow>()
                .eq(Workflow::getWorkflowCode, code)
                .last("LIMIT 1"));
    }

    /** 查 status=ACTIVE 的工作流（cache-friendly） */
    default Workflow findActiveByCode(String code) {
        return selectOne(new LambdaQueryWrapper<Workflow>()
                .eq(Workflow::getWorkflowCode, code)
                .eq(Workflow::getStatus, "ACTIVE")
                .last("LIMIT 1"));
    }

    /** 按 status 列表（分页由 Service 层用 Page） */
    default List<Workflow> listByStatus(String status, String keyword) {
        LambdaQueryWrapper<Workflow> q = new LambdaQueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            q.eq(Workflow::getStatus, status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            q.like(Workflow::getWorkflowCode, keyword);
        }
        q.orderByDesc(Workflow::getCreateTime);
        return selectList(q);
    }
}
