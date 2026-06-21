package com.btsheng.erp.platform.auth.security;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 将 DB role_code 展开为 Spring Security @PreAuthorize 可识别的角色别名。
 */
public final class RoleCodeAliases {

    /** SYS_ADMIN 在 JWT 中附带全部业务角色，避免 @PreAuthorize 漏配导致 403 */
    private static final List<String> SYS_ADMIN_GRANTS = List.of(
            "ADMIN",
            "WAREHOUSE", "WAREHOUSE_LEAD",
            "BUYER", "PURCHASER", "PURCHASER_LEAD", "PROCUREMENT_MANAGER",
            "GM", "FINANCE", "PROD_MGR", "QC", "ENGINEER", "OPERATOR",
            "SALES", "SALES_MGR", "SALES_MANAGER", "HR"
    );

    private static final Map<String, List<String>> ALIASES = Map.of(
            "SYS_ADMIN", SYS_ADMIN_GRANTS,
            "BUYER", List.of("PURCHASER", "PROCUREMENT_MANAGER"),
            "SALES_MGR", List.of("SALES_MANAGER"),
            "PROD_MGR", List.of("PRODUCTION_MANAGER"),
            "WAREHOUSE", List.of("WAREHOUSE_LEAD"),
            "QC", List.of("QUALITY")
    );

    private RoleCodeAliases() {
    }

    public static List<String> expand(List<String> codes) {
        Set<String> out = new LinkedHashSet<>();
        if (codes != null) {
            for (String code : codes) {
                if (code == null || code.isBlank()) {
                    continue;
                }
                String trimmed = code.trim();
                out.add(trimmed);
                List<String> aliases = ALIASES.get(trimmed);
                if (aliases != null) {
                    out.addAll(aliases);
                }
            }
        }
        return new ArrayList<>(out);
    }

    public static String toJwtRoles(List<String> codes) {
        List<String> expanded = expand(codes);
        return expanded.isEmpty() ? "USER" : String.join(",", expanded);
    }
}
