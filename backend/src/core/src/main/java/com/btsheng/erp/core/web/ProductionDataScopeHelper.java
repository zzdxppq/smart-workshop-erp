package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 生产模块数据权限（PRD FR-1-1-2）
 * <ul>
 *   <li>操作工 OPERATOR · SELF · owner_user_id 本人</li>
 *   <li>生产经理/工程师/品质 PROD_MGR/ENGINEER/QC · DEPT · dept_id</li>
 *   <li>总经理/管理员 · ALL</li>
 * </ul>
 */
public final class ProductionDataScopeHelper {

    public enum Scope {
        ALL, DEPT, SELF
    }

    private static final Set<String> GLOBAL_ROLES = Set.of(
            "ROLE_GM", "ROLE_ADMIN", "ROLE_SYS_ADMIN"
    );

    private static final Set<String> DEPT_ROLES = Set.of(
            "ROLE_PROD_MGR", "ROLE_PRODUCTION_MANAGER", "ROLE_ENGINEER", "ROLE_QC"
    );

    private static final Set<String> OPERATOR_ROLES = Set.of(
            "ROLE_OPERATOR"
    );

    private ProductionDataScopeHelper() {
    }

    public static Scope effectiveScope() {
        DataScopeContext ctx = DataScopeContext.current();
        if (ctx != null && ctx.getDataScope() != null && !ctx.getDataScope().isBlank()) {
            return parseScope(ctx.getDataScope());
        }
        if (hasAnyRole(GLOBAL_ROLES)) {
            return Scope.ALL;
        }
        if (hasAnyRole(DEPT_ROLES)) {
            return Scope.DEPT;
        }
        if (hasAnyRole(OPERATOR_ROLES)) {
            return Scope.SELF;
        }
        return Scope.DEPT;
    }

    /** 列表 SQL：SELF → owner_user_id；DEPT → dept_id；ALL → 均 null */
    public static Long resolveScopeOwnerId() {
        if (effectiveScope() == Scope.SELF) {
            Long uid = CurrentUserHelper.currentUserId();
            return uid != null && uid > 0 ? uid : null;
        }
        return null;
    }

    public static Long resolveScopeDeptId() {
        if (effectiveScope() == Scope.DEPT) {
            DataScopeContext ctx = DataScopeContext.current();
            if (ctx != null && ctx.getDeptId() != null && ctx.getDeptId() > 0) {
                return ctx.getDeptId();
            }
        }
        return null;
    }

    public static Result<Void> assertWorkorderScope(Long ownerUserId, Long deptId) {
        Scope scope = effectiveScope();
        if (scope == Scope.ALL) {
            return Result.ok(null);
        }
        Long uid = CurrentUserHelper.currentUserId();
        if (scope == Scope.SELF) {
            if (uid != null && ownerUserId != null && !uid.equals(ownerUserId)) {
                return Result.fail(40302, "DATA_SCOPE");
            }
            return Result.ok(null);
        }
        Long userDept = resolveScopeDeptId();
        if (userDept != null && deptId != null && !userDept.equals(deptId)) {
            return Result.fail(40302, "DATA_SCOPE");
        }
        return Result.ok(null);
    }

    private static Scope parseScope(String raw) {
        return switch (raw.trim().toUpperCase()) {
            case "ALL", "CUSTOM" -> Scope.ALL;
            case "DEPT" -> Scope.DEPT;
            default -> Scope.SELF;
        };
    }

    private static boolean hasAnyRole(Set<String> allowed) {
        return currentRoles().stream().anyMatch(allowed::contains);
    }

    private static Set<String> currentRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return Set.of();
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }
}
