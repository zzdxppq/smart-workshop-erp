package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.platform.auth.dto.UserMenuAccessDto;
import com.btsheng.erp.platform.auth.mapper.SysMenuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 从 sys_menu + sys_role_permission 解析用户可访问菜单（DB 驱动 RBAC）。
 */
@Service
public class MenuPermissionService {

    private final SysMenuMapper menuMapper;

    @Autowired
    public MenuPermissionService(SysMenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }

    @Transactional(readOnly = true)
    public UserMenuAccessDto resolveForUser(Long userId, List<String> roleCodes) {
        UserMenuAccessDto dto = new UserMenuAccessDto();
        if (userId == null) {
            return dto;
        }
        boolean admin = roleCodes != null && roleCodes.stream()
                .anyMatch(c -> "SYS_ADMIN".equals(c) || "ADMIN".equals(c));
        List<String> paths;
        List<String> perms;
        List<Map<String, Object>> menus;
        if (admin) {
            menus = menuMapper.selectAllActiveMenus();
            paths = menuMapper.selectAllActivePaths();
            perms = menuMapper.selectPermissionCodesByUserId(userId);
            if (perms == null || perms.isEmpty()) {
                perms = viewPermsFromMenus(menus);
            }
        } else {
            paths = menuMapper.selectPathsByUserId(userId);
            perms = menuMapper.selectPermissionCodesByUserId(userId);
            menus = menuMapper.selectMenuTreeByUserId(userId);
        }
        dto.setMenuPaths(dedupe(paths));
        dto.setPermissions(dedupe(perms));
        dto.setMenus(menus == null ? List.of() : menus);
        return dto;
    }

    private static List<String> dedupe(List<String> raw) {
        if (raw == null || raw.isEmpty()) {
            return List.of();
        }
        Set<String> set = new LinkedHashSet<>(raw);
        return new ArrayList<>(set);
    }

    private static List<String> viewPermsFromMenus(List<Map<String, Object>> menus) {
        List<String> out = new ArrayList<>();
        if (menus == null) {
            return out;
        }
        for (Map<String, Object> row : menus) {
            Object code = row.get("menuCode");
            if (code != null) {
                out.add(code + ":view");
            }
        }
        return out;
    }
}
