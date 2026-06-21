package com.btsheng.erp.business.crm.qualityinspection.service;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 检验方案模板角色校验（X-User-Roles 逗号分隔，与前端 roleAccess 对齐）
 */
final class QualityTemplateRoleGuard {

    private static final Set<String> EDIT_ROLES = Set.of(
            "ENGINEER", "PROD_MGR", "PRODUCTION_MANAGER", "GM", "ADMIN", "SYS_ADMIN");
    private static final Set<String> PUBLISH_ROLES = Set.of(
            "PROD_MGR", "PRODUCTION_MANAGER", "GM", "ADMIN", "SYS_ADMIN");

    private QualityTemplateRoleGuard() {}

    static boolean canEdit(String userRolesHeader) {
        return hasAny(parseRoles(userRolesHeader), EDIT_ROLES);
    }

    static boolean canPublish(String userRolesHeader) {
        return hasAny(parseRoles(userRolesHeader), PUBLISH_ROLES);
    }

    private static Set<String> parseRoles(String header) {
        if (header == null || header.isBlank()) {
            return Set.of();
        }
        Set<String> roles = new HashSet<>();
        Arrays.stream(header.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(r -> {
                    roles.add(r);
                    if ("PROD_MGR".equals(r)) {
                        roles.add("PRODUCTION_MANAGER");
                    } else if ("PRODUCTION_MANAGER".equals(r)) {
                        roles.add("PROD_MGR");
                    }
                });
        return roles;
    }

    private static boolean hasAny(Set<String> userRoles, Set<String> allowed) {
        for (String r : userRoles) {
            if (allowed.contains(r)) {
                return true;
            }
        }
        return false;
    }
}
