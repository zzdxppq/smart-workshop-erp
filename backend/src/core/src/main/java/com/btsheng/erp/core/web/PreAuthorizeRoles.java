package com.btsheng.erp.core.web;

/**
 * 统一 {@code @PreAuthorize} SpEL 表达式（与 {@link com.btsheng.erp.platform.auth.security.RoleCodeAliases} 对齐）。
 */
public final class PreAuthorizeRoles {

    public static final String ADMIN = "hasAnyRole('ADMIN', 'SYS_ADMIN')";
    public static final String FINANCE = "hasAnyRole('FINANCE', 'GM', 'ADMIN', 'SYS_ADMIN')";
    /** 销售侧回款计划维护（FR-2-4-1 / AC-2.4.1） */
    public static final String SALES = "hasAnyRole('SALES', 'SALES_MGR', 'SALES_MANAGER', 'GM', 'ADMIN', 'SYS_ADMIN')";
    public static final String REPORTS = "hasAnyRole('GM', 'SALES', 'SALES_MGR', 'SALES_MANAGER', 'FINANCE', 'ADMIN', 'SYS_ADMIN', 'HR')";

    private PreAuthorizeRoles() {
    }
}
