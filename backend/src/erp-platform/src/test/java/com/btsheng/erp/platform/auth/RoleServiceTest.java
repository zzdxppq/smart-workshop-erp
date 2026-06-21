package com.btsheng.erp.platform.auth;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * RoleService 单测（V1.3.7 · T2.1 · AC-1.1.2）
 */
@DisplayName("RoleService 角色权限管理")
class RoleServiceTest {

    private SysRoleMapper roleMapper;
    private SysRolePermissionMapper permMapper;
    private SysUserRoleMapper userRoleMapper;
    private SysMenuMapper menuMapper;
    private RoleService service;

    @BeforeEach
    void setUp() {
        roleMapper = Mockito.mock(SysRoleMapper.class);
        permMapper = Mockito.mock(SysRolePermissionMapper.class);
        userRoleMapper = Mockito.mock(SysUserRoleMapper.class);
        menuMapper = Mockito.mock(SysMenuMapper.class);
        service = new RoleService(roleMapper, permMapper, userRoleMapper, menuMapper);
    }

    @Test
    @DisplayName("assignPermissions_removesOld: DELETE 旧权限 + INSERT 新权限")
    void assignPermissions_removesOld() {
        SysRole role = new SysRole();
        role.setId(10L);
        role.setRoleCode("SALES");
        role.setRoleName("业务员");
        Mockito.when(roleMapper.selectById(10L)).thenReturn(role);

        SysMenu menu = new SysMenu();
        menu.setId(1001L);
        menu.setStatus("ACTIVE");
        Mockito.when(menuMapper.selectById(1001L)).thenReturn(menu);

        PermissionAssignRequest p = new PermissionAssignRequest();
        p.setMenuId(1001L);
        p.setActions(List.of("view", "add"));
        service.assignPermissions(10L, List.of(p));

        Mockito.verify(permMapper).deleteByRoleId(10L);
        Mockito.verify(permMapper, Mockito.times(2)).insertPerm(Mockito.eq(10L), Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    @DisplayName("assignPermissions_emptyList: 不抛异常 · 仅清空")
    void assignPermissions_emptyList() {
        SysRole role = new SysRole();
        role.setId(10L);
        Mockito.when(roleMapper.selectById(10L)).thenReturn(role);
        service.assignPermissions(10L, List.of());
        Mockito.verify(permMapper).deleteByRoleId(10L);
        Mockito.verify(permMapper, Mockito.never()).insertPerm(Mockito.anyLong(), Mockito.anyLong(), Mockito.anyString());
    }

    @Test
    @DisplayName("deleteRole_inUse_throws40902: 角色被引用 → 40902")
    void deleteRole_inUse_throws40902() {
        SysRole role = new SysRole();
        role.setId(20L);
        role.setRoleCode("CUSTOM_ROLE");
        Mockito.when(roleMapper.selectById(20L)).thenReturn(role);
        Mockito.when(userRoleMapper.countByRoleId(20L)).thenReturn(3);
        BizException ex = assertThrows(BizException.class, () -> service.deleteRole(20L));
        assertEquals(Result.CODE_CONFLICT_IN_USE, ex.getCode());
    }

    @Test
    @DisplayName("deleteRole_builtin_throws40903: SYS_ADMIN → 40903")
    void deleteRole_builtin_throws40903() {
        SysRole role = new SysRole();
        role.setId(1L);
        role.setRoleCode("SYS_ADMIN");
        Mockito.when(roleMapper.selectById(1L)).thenReturn(role);
        BizException ex = assertThrows(BizException.class, () -> service.deleteRole(1L));
        assertEquals(Result.CODE_CONFLICT_BUILTIN, ex.getCode());
    }

    @Test
    @DisplayName("deleteRole_notReferenced: 软删成功 · status=DELETED")
    void deleteRole_notReferenced() {
        SysRole role = new SysRole();
        role.setId(50L);
        role.setRoleCode("TEMP_ROLE");
        role.setStatus("ACTIVE");
        Mockito.when(roleMapper.selectById(50L)).thenReturn(role);
        Mockito.when(userRoleMapper.countByRoleId(50L)).thenReturn(0);
        service.deleteRole(50L);
        assertEquals("DELETED", role.getStatus());
        Mockito.verify(roleMapper).updateById(role);
    }
}
