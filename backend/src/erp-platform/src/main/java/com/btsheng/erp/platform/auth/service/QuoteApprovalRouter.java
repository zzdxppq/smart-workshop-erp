package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.model.Money;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.dto.QuoteApprovalResult;
import com.btsheng.erp.platform.auth.entity.SysRole;
import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.entity.SysUserRole;
import com.btsheng.erp.platform.auth.mapper.SysRoleMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 报价审批路由（V1.3.7 · AC-1.1.3 · P1 修补）
 *
 * <p>审批链：salesperson → dept_manager → gm。<br>
 * 阈值：&lt; 5万 / 5-20万 / &gt; 20万。同一节点多人时按 {@code user_id ASC} 取第一人。
 *
 * <p><b>P1 修补</b>：返回值增加 {@code candidates: List<Long>} 字段（V1.3.7 取 candidates[0]）。
 * Story 1.2 升级为 OR 会签时直接消费 candidates 全列表。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Service
public class QuoteApprovalRouter {

    private static final String[] CHAIN = {"salesperson", "dept_manager", "gm"};

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Autowired
    public QuoteApprovalRouter(SysRoleMapper roleMapper, SysUserMapper userMapper, SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 路由决策。
     *
     * @param amount    报价金额
     * @param applicantUserId 申请人 user_id
     * @return {@link QuoteApprovalResult}，含 candidates 字段
     */
    public QuoteApprovalResult route(BigDecimal amount, Long applicantUserId) {
        if (amount == null) {
            throw new BizException(Result.CODE_PARAM_MISSING, "金额必填");
        }
        QuoteApprovalResult res = new QuoteApprovalResult();
        List<Long> candidates = new ArrayList<>();

        for (String roleCode : CHAIN) {
            SysRole role = roleMapper.findByCode(roleCode);
            if (role == null) {
                continue;
            }
            // 阈值校验：role.amount_threshold == null 表示无限额；否则用 compareTo
            BigDecimal threshold = role.getAmountThreshold();
            boolean withinRoleScope = (threshold == null) || !Money.gt(amount, threshold);
            if (!withinRoleScope) {
                continue;
            }
            // 该角色下所有有效用户
            List<Long> users = findActiveUserIdsByRoleId(role.getId());
            if (users.isEmpty()) {
                // 没有有效审批人：抛出 404（必须人工干预）
            if (roleCode.equals("gm")) {
                    throw new BizException(Result.CODE_NOT_FOUND_ROUTING, "未找到总经理角色有效用户，请联系管理员");
                }
                continue;
            }
            Collections.sort(users);
            candidates = users;
            // 业务员：自审
            if ("salesperson".equals(roleCode)) {
                if (users.contains(applicantUserId)) {
                    res.setApproverUserId(applicantUserId);
                } else {
                    res.setApproverUserId(users.get(0));
                }
            } else {
                res.setApproverUserId(users.get(0));
            }
            res.setCurrentNode(roleCode);
            res.setReason("金额 " + amount + " → " + roleCode
                    + (threshold == null ? "（无限额）" : " 阈值 " + threshold));
            res.setCandidates(candidates);
            return res;
        }
        // 兜底：路由到 gm（应已覆盖）
            throw new BizException(Result.CODE_NOT_FOUND_ROUTING, "未找到合适的审批人，请联系管理员");
    }

    /**
     * 根据 role_id 找有效 user_id 列表（按 status=ACTIVE 过滤）。
     * 简化实装：先 sys_user_role → sys_user。
     */
    private List<Long> findActiveUserIdsByRoleId(Long roleId) {
        // 简化：直接 select from sys_user_role
            List<com.btsheng.erp.platform.auth.entity.SysUserRole> rels =
                userRoleMapper.selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysUserRole>()
                        .eq("role_id", roleId));
        List<Long> activeIds = new ArrayList<>();
        for (var rel : rels) {
            SysUser u = userMapper.findStatusById(rel.getUserId());
            if (u != null && "ACTIVE".equals(u.getStatus())) {
                activeIds.add(u.getId());
            }
        }
        return activeIds;
    }
}
