package com.btsheng.erp.platform.auth.workflow.router;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.mapper.SysRoleMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserRoleMapper;
import com.btsheng.erp.platform.auth.entity.SysRole;
import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.entity.SysUserRole;
import com.btsheng.erp.platform.auth.dto.QuoteApprovalResult;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Story 1.1 路由兼容层（V1.3.7 · Story 1.2 · architect P2 反馈 ②）
 *
 * <p>复用 Story 1.1 的硬编码 chain（salesperson → dept_manager → gm）+ 候选人查询，
 * 同时暴露给 {@link WorkflowApprovalRouter} 作为底层能力（按 role_code 查 active users）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Service
public class Story11CompatRouter {

    private static final String[] CHAIN = {"salesperson", "dept_manager", "gm"};

    private final SysRoleMapper roleMapper;
    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Autowired
    public Story11CompatRouter(SysRoleMapper roleMapper,
                               SysUserMapper userMapper,
                               SysUserRoleMapper userRoleMapper) {
        this.roleMapper = roleMapper;
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 2 参重载（Story 1.1 兼容 · 35 测例不破）。
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
            BigDecimal threshold = role.getAmountThreshold();
            boolean withinRoleScope = (threshold == null) || amount.compareTo(threshold) <= 0;
            if (!withinRoleScope) {
                continue;
            }
            List<Long> users = findActiveUserIdsByRoleId(role.getId());
            if (users.isEmpty()) {
                if ("gm".equals(roleCode)) {
                    throw new BizException(Result.CODE_NOT_FOUND_ROUTING, "未找到总经理角色有效用户，请联系管理员");
                }
                continue;
            }
            Collections.sort(users);
            candidates = users;
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
        throw new BizException(Result.CODE_NOT_FOUND_ROUTING, "未找到合适的审批人，请联系管理员");
    }

    /**
     * 按 role_code 找有效 user_id 列表（V1.3.7 P1 修补：返回完整列表，不再 LIMIT 1）。
     */
    public List<Long> findActiveUserIdsByRoleCode(String roleCode) {
        SysRole role = roleMapper.findByCode(roleCode);
        if (role == null) {
            return Collections.emptyList();
        }
        return findActiveUserIdsByRoleId(role.getId());
    }

    private List<Long> findActiveUserIdsByRoleId(Long roleId) {
        List<SysUserRole> rels = userRoleMapper.selectList(
                new QueryWrapper<SysUserRole>().eq("role_id", roleId));
        List<Long> activeIds = new ArrayList<>();
        for (SysUserRole rel : rels) {
            SysUser u = userMapper.findStatusById(rel.getUserId());
            if (u != null && "ACTIVE".equals(u.getStatus())) {
                activeIds.add(u.getId());
            }
        }
        return activeIds;
    }
}
