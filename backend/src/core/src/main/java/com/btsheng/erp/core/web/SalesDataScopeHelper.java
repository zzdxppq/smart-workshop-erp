package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销售 CRM 数据权限（PRD）
 * <ul>
 *   <li>业务员 SALES · SELF · 仅 owner_user_id / owner_id 本人</li>
 *   <li>销售经理 SALES_MGR · DEPT · 本部门 dept_id</li>
 *   <li>总经理/财务/管理员 · ALL</li>
 * </ul>
 */
public final class SalesDataScopeHelper {

    public enum Scope {
        ALL, DEPT, SELF
    }

    private static final Set<String> APPROVER_ROLES = Set.of(
            "ROLE_GM", "ROLE_ADMIN", "ROLE_SYS_ADMIN",
            "ROLE_SALES_MGR", "ROLE_SALES_MANAGER"
    );

    private static final Set<String> GLOBAL_VIEW_ROLES = Set.of(
            "ROLE_GM", "ROLE_ADMIN", "ROLE_SYS_ADMIN", "ROLE_FINANCE"
    );

    private static final Set<String> DEPT_SALES_ROLES = Set.of(
            "ROLE_SALES_MGR", "ROLE_SALES_MANAGER"
    );

    private static final Set<String> SALES_REP_ROLES = Set.of(
            "ROLE_SALES", "ROLE_SALESPERSON"
    );

    private SalesDataScopeHelper() {
    }

    public static Scope effectiveScope() {
        DataScopeContext ctx = DataScopeContext.current();
        if (ctx != null && ctx.getDataScope() != null && !ctx.getDataScope().isBlank()) {
            return parseScope(ctx.getDataScope());
        }
        if (hasAnyRole(GLOBAL_VIEW_ROLES)) {
            return Scope.ALL;
        }
        if (hasAnyRole(DEPT_SALES_ROLES)) {
            return Scope.DEPT;
        }
        if (hasAnyRole(SALES_REP_ROLES)) {
            return Scope.SELF;
        }
        return Scope.SELF;
    }

    public static boolean canApproveQuotes() {
        return hasAnyRole(APPROVER_ROLES);
    }

    /** 业务员（SELF 范围） */
    public static boolean isSalesRepOnly() {
        return effectiveScope() == Scope.SELF && hasAnyRole(SALES_REP_ROLES);
    }

    public static Long resolveOwnerUserId(Long requestedOwner) {
        if (effectiveScope() == Scope.SELF) {
            Long uid = CurrentUserHelper.currentUserId();
            return uid != null ? uid : requestedOwner;
        }
        return requestedOwner;
    }

    public static Long resolveDeptId(Long requestedDept) {
        if (effectiveScope() == Scope.DEPT) {
            DataScopeContext ctx = DataScopeContext.current();
            if (ctx != null && ctx.getDeptId() != null && ctx.getDeptId() > 0) {
                return ctx.getDeptId();
            }
        }
        return requestedDept;
    }

    /** 列表/聚合 SQL：SELF → owner_user_id；DEPT → dept_id；ALL → null */
    public static Long resolveListOwnerUserId() {
        if (effectiveScope() == Scope.SELF) {
            Long uid = CurrentUserHelper.currentUserId();
            return uid != null && uid > 0 ? uid : null;
        }
        return null;
    }

    public static Long resolveListDeptId() {
        if (effectiveScope() == Scope.DEPT) {
            return resolveDeptId(null);
        }
        return null;
    }

    /** 列表查询用：返回 salesperson / dept_manager / gm */
    public static String resolveListRole(String defaultRole) {
        return switch (effectiveScope()) {
            case SELF -> "salesperson";
            case DEPT -> "dept_manager";
            case ALL -> defaultRole != null ? defaultRole : "gm";
        };
    }

    public static long requireOperatorUserId(long fallback) {
        Long uid = CurrentUserHelper.currentUserId();
        return uid != null && uid > 0 ? uid : fallback;
    }

    public static Result<Void> assertOwnerDept(Long ownerUserId, Long deptId) {
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
        Long userDept = resolveDeptId(null);
        if (userDept != null && deptId != null && !userDept.equals(deptId)) {
            return Result.fail(40302, "DATA_SCOPE");
        }
        return Result.ok(null);
    }

    public static Result<Void> assertCustomerOwner(Long ownerId) {
        Scope scope = effectiveScope();
        if (scope == Scope.ALL) {
            return Result.ok(null);
        }
        Long uid = CurrentUserHelper.currentUserId();
        if (scope == Scope.SELF) {
            if (uid != null && ownerId != null && !uid.equals(ownerId)) {
                return Result.fail(40302, "DATA_SCOPE");
            }
            return Result.ok(null);
        }
        return Result.ok(null);
    }

    public static boolean matchesOrderScope(Long ownerUserId, Long deptId) {
        Result<Void> r = assertOwnerDept(ownerUserId, deptId);
        return r.isSuccess();
    }

    private static Scope parseScope(String raw) {
        if (raw == null) {
            return Scope.SELF;
        }
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
