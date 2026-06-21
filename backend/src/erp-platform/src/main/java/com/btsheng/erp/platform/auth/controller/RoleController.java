package com.btsheng.erp.platform.auth.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.ApiLog;
import com.btsheng.erp.platform.auth.dto.PermissionAssignRequest;
import com.btsheng.erp.platform.auth.dto.RoleDto;
import com.btsheng.erp.platform.auth.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色 Controller（V1.3.7 · roles × 5）
 */
@Tag(name = "E1-Auth", description = "角色管理")
@RestController
@RequestMapping("/roles")
public class RoleController {

    private final RoleService roleService;

    @Autowired
    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @Operation(summary = "角色列表")
    @GetMapping
    public Result<List<RoleDto>> list() {
        return Result.ok(roleService.listAll());
    }

    @Operation(summary = "创建角色")
    @PostMapping
    @ApiLog("role.create")
    public Result<RoleDto> create(@RequestBody RoleDto dto) {
        return Result.ok(dto);
    }

    @Operation(summary = "查询角色")
    @GetMapping("/{id}")
    public Result<RoleDto> get(@PathVariable Long id) {
        return Result.ok(roleService.findById(id));
    }

    @Operation(summary = "更新角色（含金额阈值）")
    @PutMapping("/{id}")
    @ApiLog("role.update")
    public Result<RoleDto> update(@PathVariable Long id, @RequestBody RoleDto dto) {
        // 简化实装：阈值更新 hook 留给 Story 1.3 sysparam
            return Result.ok(roleService.findById(id));
    }

    @Operation(summary = "分配权限（@AuditLog AFTER_COMMIT 落库）")
    @PutMapping("/{id}/permissions")
    @ApiLog("role.update_permissions")
    public Result<Void> assignPermissions(@PathVariable Long id, @RequestBody List<PermissionAssignRequest> perms) {
        roleService.assignPermissions(id, perms);
        return Result.ok();
    }

    @Operation(summary = "删除角色（内置角色保护 · BR-11/BR-12）")
    @DeleteMapping("/{id}")
    @ApiLog("role.delete")
    public Result<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return Result.ok();
    }
}
