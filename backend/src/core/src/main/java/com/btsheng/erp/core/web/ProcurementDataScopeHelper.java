package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.Result;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 采购模块数据权限（PRD：采购员 SELF · 采购主管/经理 DEPT · GM/ADMIN ALL）
 */
public final class ProcurementDataScopeHelper {

    public enum Scope {
        ALL, DEPT, SELF
    }

    private static final Set<String> GLOBAL_ROLES = Set.of(
            "ROLE_GM", "ROLE_ADMIN", "ROLE_SYS_ADMIN"
    );

    private static final Set<String> DEPT_ROLES = Set.of(
            "ROLE_PROCUREMENT_MANAGER", "ROLE_PURCHASER_LEAD", "ROLE_BUYER"
    );

    private ProcurementDataScopeHelper() {
    }

    public static Scope effectiveScope() {
        DataScopeContext ctx = DataScopeContext.current();
        if (ctx != null && ctx.getDataScope() != null && !ctx.getDataScope().isBlank()) {
            return parseScope(ctx.getDataScope());
        }
        if (hasAnyRole(GLOBAL_ROLES)) {
            return Scope.ALL;
        }
        if (hasAnyRole(Set.of("ROLE_PROCUREMENT_MANAGER"))) {
            return Scope.DEPT;
        }
        if (hasAnyRole(DEPT_ROLES)) {
            return Scope.SELF;
        }
        return Scope.SELF;
    }

    public static Long resolveCreatorId(Long requested) {
        if (effectiveScope() == Scope.SELF) {
            Long uid = CurrentUserHelper.currentUserId();
            return uid != null ? uid : requested;
        }
        return requested;
    }

    public static Result<Void> assertCreator(Long createdBy) {
        if (effectiveScope() != Scope.SELF) {
            return Result.ok(null);
        }
        Long uid = CurrentUserHelper.currentUserId();
        if (uid != null && createdBy != null && !uid.equals(createdBy)) {
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
