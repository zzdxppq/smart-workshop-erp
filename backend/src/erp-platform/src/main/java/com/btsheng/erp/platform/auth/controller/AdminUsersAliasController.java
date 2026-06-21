package com.btsheng.erp.platform.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.dto.UserDto;
import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Web 管理端路径别名 {@code GET /admin/users} */
@Tag(name = "E1-Admin-Web", description = "管理端 Web 路径别名")
@RestController
@RequestMapping("/admin/users")
public class AdminUsersAliasController {

    private final SysUserMapper userMapper;

    @Autowired
    public AdminUsersAliasController(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Operation(summary = "用户列表（Web /admin/users 别名）")
    @GetMapping
    public Result<PageResponse<UserDto>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String status) {
        Page<SysUser> p = new Page<>(pageNum, pageSize);
        QueryWrapper<SysUser> qw = new QueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            qw.and(w -> w.like("username", kw).or().like("real_name", kw));
        }
        if (deptId != null) {
            qw.eq("dept_id", deptId);
        }
        if (status != null) {
            qw.eq("status", status);
        }
        qw.orderByDesc("updated_at");
        Page<SysUser> res = userMapper.selectPage(p, qw);
        return Result.ok(PageResponse.of(
                res.getRecords().stream().map(u -> {
                    UserDto d = new UserDto();
                    d.setId(u.getId());
                    d.setUsername(u.getUsername());
                    d.setRealName(u.getRealName());
                    d.setPhone(u.getPhone());
                    d.setEmail(u.getEmail());
                    d.setDeptId(u.getDeptId());
                    d.setStatus(u.getStatus());
                    return d;
                }).toList(),
                res.getTotal(), res.getCurrent(), res.getSize()));
    }
}
