package com.btsheng.erp.business.crm.workflowevent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.business.crm.workflowevent.entity.SysWorkflowEvent;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * V1.3.8 Sprint 8 Story 8.3 · sys_workflow_event Mapper
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */
@Mapper
public interface SysWorkflowEventMapper extends BaseMapper<SysWorkflowEvent> {

    /**
     * 按 workflow_code + 角色 + 时间范围统计事件数
     * <p>用于 Story 4.3 总经理报表 PROCUREMENT_MANAGER 工作量精确统计
     */
    @Select("""
            SELECT COUNT(*) AS event_count
            FROM sys_workflow_event
            WHERE workflow_code = #{workflowCode}
              AND approver_role = #{approverRole}
              AND event_type = 'APPROVED'
              AND created_at >= #{startDate}
              AND created_at &lt; #{endDate}
            """)
    Integer countApprovedEvents(@Param("workflowCode") String workflowCode,
                                @Param("approverRole") String approverRole,
                                @Param("startDate") LocalDateTime startDate,
                                @Param("endDate") LocalDateTime endDate);

    // V1.3.8 Sprint 10 Story 10.3：按 event_type 分组聚合
            @Select("""
            <script>
            SELECT event_type AS eventType, COUNT(*) AS cnt
            FROM sys_workflow_event
            WHERE workflow_code = #{workflowCode}
              AND created_at >= #{startDate} AND created_at &lt; #{endDate}
              <if test="approverRole != null">
              AND approver_role = #{approverRole}
              </if>
            GROUP BY event_type
            </script>
            """)
    List<Map<String, Object>> aggregateByEventType(@Param("workflowCode") String workflowCode,
                                                    @Param("approverRole") String approverRole,
                                                    @Param("startDate") LocalDateTime startDate,
                                                    @Param("endDate") LocalDateTime endDate);

    // V1.3.8 Sprint 10 Story 10.3：按 approver_role 分组聚合
            @Select("""
            <script>
            SELECT approver_role AS approverRole, COUNT(*) AS cnt
            FROM sys_workflow_event
            WHERE workflow_code = #{workflowCode}
              AND created_at >= #{startDate} AND created_at &lt; #{endDate}
              <if test="approverRole != null">
              AND approver_role = #{approverRole}
              </if>
            GROUP BY approver_role
            </script>
            """)
    List<Map<String, Object>> aggregateByApproverRole(@Param("workflowCode") String workflowCode,
                                                      @Param("approverRole") String approverRole,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    // V1.3.8 Sprint 10 Story 10.3：总事件数
            @Select("""
            <script>
            SELECT COUNT(*) AS total
            FROM sys_workflow_event
            WHERE workflow_code = #{workflowCode}
              AND created_at >= #{startDate} AND created_at &lt; #{endDate}
              <if test="approverRole != null">
              AND approver_role = #{approverRole}
              </if>
            </script>
            """)
    Long countByWorkflowCode(@Param("workflowCode") String workflowCode,
                             @Param("approverRole") String approverRole,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate);
}