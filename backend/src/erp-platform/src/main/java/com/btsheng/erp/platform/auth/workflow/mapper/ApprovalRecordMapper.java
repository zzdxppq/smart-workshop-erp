package com.btsheng.erp.platform.auth.workflow.mapper;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.btsheng.erp.platform.auth.workflow.entity.ApprovalRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 审批记录 Mapper（V1.3.7 · Story 1.2 · T0.3 · P1 修补 ④）
 *
 * <p>对应 {@code sys_approval_record} 表（V3__approval_record.sql）。
 * 含 {@code FOR UPDATE SKIP LOCKED} 分布式扫描（V1.3.7 P1-3 幂等）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Mapper
public interface ApprovalRecordMapper extends BaseMapper<ApprovalRecord> {

    /** 当前用户作为审批人的待办（排除 SKIPPED） */
    default List<ApprovalRecord> findPendingByApprover(Long approverUserId) {
        return selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getCurrentApproverUserId, approverUserId)
                .eq(ApprovalRecord::getStatus, "PENDING")
                .orderByDesc(ApprovalRecord::getCreateTime));
    }

    /** 当前用户作为申请人的待办（看自己提交的单子的进度） */
    default List<ApprovalRecord> findMyPendingByApplicant(Long applicantUserId) {
        return selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getCreateBy, applicantUserId)
                .eq(ApprovalRecord::getStatus, "PENDING")
                .orderByDesc(ApprovalRecord::getCreateTime));
    }

    /** 按业务单号查所有审批记录（同一业务单多轮审批单） */
    default List<ApprovalRecord> findByBizTypeAndBizId(String bizType, String bizId) {
        return selectList(new LambdaQueryWrapper<ApprovalRecord>()
                .eq(ApprovalRecord::getBizType, bizType)
                .eq(ApprovalRecord::getBizId, bizId)
                .orderByAsc(ApprovalRecord::getCurrentNodeIndex));
    }

    /**
     * 扫描超时记录（V1.3.7 P1-3 幂等 + 分布式锁）
     *
     * <p>使用 {@code FOR UPDATE SKIP LOCKED} 防止多实例并发扫描同一行。
     */
    @Select("SELECT * FROM sys_approval_record " +
            "WHERE status = 'PENDING' AND timeout_at < #{now} AND is_overdue = FALSE " +
            "ORDER BY timeout_at ASC LIMIT #{limit} FOR UPDATE SKIP LOCKED")
    List<ApprovalRecord> findOverdue(@Param("now") LocalDateTime now,
                                     @Param("limit") int limit);

    /** 批量标记超时 */
    @Update("UPDATE sys_approval_record SET is_overdue = TRUE, overdue_at = #{overdueAt} " +
            "WHERE id = #{id} AND is_overdue = FALSE")
    int markOverdue(@Param("id") Long id, @Param("overdueAt") LocalDateTime overdueAt);
}
