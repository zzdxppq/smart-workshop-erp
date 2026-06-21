package com.btsheng.erp.platform.integration;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.dto.PermissionAssignRequest;
import com.btsheng.erp.platform.auth.entity.SysMenu;
import com.btsheng.erp.platform.auth.entity.SysRole;
import com.btsheng.erp.platform.auth.mapper.SysMenuMapper;
import com.btsheng.erp.platform.auth.mapper.SysRoleMapper;
import com.btsheng.erp.platform.auth.mapper.SysRolePermissionMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserRoleMapper;
import com.btsheng.erp.platform.auth.service.RoleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 角色权限 E2E 集成（V1.3.7 · T2.5）
 */
@DisplayName("E2E: Role Permission (T2.5)")
class RolePermissionE2ETest {

    @Test
    @DisplayName("e2e_role_permission: 分配权限 + 删除守卫 + 软删")
    void e2e_role_permission_full_flow() {
        SysRoleMapper roleMapper = Mockito.mock(SysRoleMapper.class);
        SysRolePermissionMapper permMapper = Mockito.mock(SysRolePermissionMapper.class);
        SysUserRoleMapper userRoleMapper = Mockito.mock(SysUserRoleMapper.class);
        SysMenuMapper menuMapper = Mockito.mock(SysMenuMapper.class);
        RoleService service = new RoleService(roleMapper, permMapper, userRoleMapper, menuMapper);

        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleCode("CUSTOM_TEST");
        role.setRoleName("测试角色");
        role.setStatus("ACTIVE");
        Mockito.when(roleMapper.selectById(10L)).thenReturn(role);
        Mockito.when(userRoleMapper.countByRoleId(10L)).thenReturn(0);

        // 1. 分配权限
            PermissionAssignRequest p = new PermissionAssignRequest();
        p.setMenuId(1001L);
        p.setActions(List.of("view", "add"));
        SysMenu menu = new SysMenu();
        menu.setId(1001L);
        menu.setStatus("ACTIVE");
        Mockito.when(menuMapper.selectById(1001L)).thenReturn(menu);
        service.assignPermissions(10L, List.of(p));
        Mockito.verify(permMapper).deleteByRoleId(10L);
        // Mockito 规则：用 matcher 必须全用 matcher（10L/1001L 改 anyLong 避免 InvalidUseOfMatchers）
            Mockito.verify(permMapper, Mockito.times(2)).insertPerm(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());

        // 2. 软删（无引用）
            service.deleteRole(10L);
        assertEquals("DELETED", role.getStatus());

        // 3. 引用计数 > 0 → 抛 40902
            SysRole role2 = new SysRole();
        role2.setId(11L);
        role2.setRoleCode("CUSTOM_TEST_2");
        role2.setStatus("ACTIVE");
        Mockito.when(roleMapper.selectById(11L)).thenReturn(role2);
        Mockito.when(userRoleMapper.countByRoleId(11L)).thenReturn(2);
        BizException ex = assertThrows(BizException.class, () -> service.deleteRole(11L));
        assertEquals(Result.CODE_CONFLICT_IN_USE, ex.getCode());
    }
}
