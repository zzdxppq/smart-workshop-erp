package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.dto.PermissionAssignRequest;
import com.btsheng.erp.platform.auth.dto.RoleDto;
import com.btsheng.erp.platform.auth.entity.SysMenu;
import com.btsheng.erp.platform.auth.entity.SysRole;
import com.btsheng.erp.platform.auth.enums.BuiltinRoleEnum;
import com.btsheng.erp.platform.auth.mapper.SysMenuMapper;
import com.btsheng.erp.platform.auth.mapper.SysRoleMapper;
import com.btsheng.erp.platform.auth.mapper.SysRolePermissionMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 角色 Service（V1.3.7 · AC-1.1.2）
 *
 * <p>BR-11 角色被引用不可删；BR-12 内置角色不可删；金额阈值可调（实时生效）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Service
public class RoleService {

    private final SysRoleMapper roleMapper;
    private final SysRolePermissionMapper rolePermissionMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final SysMenuMapper menuMapper;

    @Autowired
    public RoleService(SysRoleMapper roleMapper,
                       SysRolePermissionMapper rolePermissionMapper,
                       SysUserRoleMapper userRoleMapper,
                       SysMenuMapper menuMapper) {
        this.roleMapper = roleMapper;
        this.rolePermissionMapper = rolePermissionMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuMapper = menuMapper;
    }

    @AuditLog(module = "role", action = "role.update_permissions")
    @Transactional
    public void assignPermissions(Long roleId, List<PermissionAssignRequest> perms) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(Result.CODE_NOT_FOUND, "角色不存在");
        }
        // DELETE 旧权限
            rolePermissionMapper.deleteByRoleId(roleId);
        // INSERT 新权限
            if (perms != null) {
            for (PermissionAssignRequest p : perms) {
                if (p.getMenuId() == null || p.getActions() == null) {
                    throw new BizException(Result.CODE_PARAM_MISSING, "menu_id 和 actions 必填");
                }
                SysMenu menu = menuMapper.selectById(p.getMenuId());
                if (menu == null || !"ACTIVE".equals(menu.getStatus())) {
                    throw new BizException(Result.CODE_NOT_FOUND, "菜单不存在或已禁用: " + p.getMenuId());
                }
                for (String action : p.getActions()) {
                    rolePermissionMapper.insertPerm(roleId, p.getMenuId(), action);
                }
            }
        }
    }

    /**
     * 软删角色（BR-11 / BR-12）。
     */
    @AuditLog(module = "role", action = "role.delete")
    @Transactional
    public void deleteRole(Long roleId) {
        SysRole role = roleMapper.selectById(roleId);
        if (role == null) {
            throw new BizException(Result.CODE_NOT_FOUND, "角色不存在");
        }
        if (BuiltinRoleEnum.isBuiltin(role.getRoleCode())) {
            throw new BizException(Result.CODE_CONFLICT_BUILTIN, "内置角色不可删除");
        }
        int refs = userRoleMapper.countByRoleId(roleId);
        if (refs > 0) {
            throw new BizException(Result.CODE_CONFLICT_IN_USE, "角色已被 " + refs + " 个用户引用，禁止删除");
        }
        // 软删
            role.setStatus("DELETED");
        roleMapper.updateById(role);
    }

    public RoleDto findById(Long id) {
        SysRole r = roleMapper.selectById(id);
        if (r == null) throw new BizException(Result.CODE_NOT_FOUND, "角色不存在");
        return toDto(r);
    }

    public List<RoleDto> listAll() {
        return roleMapper.selectList(null).stream().map(this::toDto).collect(Collectors.toList());
    }

    private RoleDto toDto(SysRole r) {
        RoleDto d = new RoleDto();
        d.setId(r.getId());
        d.setRoleCode(r.getRoleCode());
        d.setRoleName(r.getRoleName());
        d.setDataScope(r.getDataScope());
        d.setAmountThreshold(r.getAmountThreshold());
        d.setStatus(r.getStatus());
        return d;
    }
}
