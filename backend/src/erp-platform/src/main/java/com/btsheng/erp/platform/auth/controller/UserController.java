package com.btsheng.erp.platform.auth.controller;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.ApiLog;
import com.btsheng.erp.platform.auth.dto.UserAvailabilitySyncRequest;
import com.btsheng.erp.platform.auth.dto.UserCreateRequest;
import com.btsheng.erp.platform.auth.dto.UserDto;
import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import com.btsheng.erp.platform.auth.service.UserAvailabilityService;
import com.btsheng.erp.platform.auth.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 用户 Controller（V1.3.7 · users × 5）
 */
@Tag(name = "E1-Auth", description = "用户管理")
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final SysUserMapper userMapper;
    private final UserAvailabilityService userAvailabilityService;

    @Autowired
    public UserController(UserService userService, SysUserMapper userMapper,
                          UserAvailabilityService userAvailabilityService) {
        this.userService = userService;
        this.userMapper = userMapper;
        this.userAvailabilityService = userAvailabilityService;
    }

    @Operation(summary = "用户列表（分页 + 数据权限）")
    @GetMapping
    public Result<PageResponse<UserDto>> list(@RequestParam(defaultValue = "1") int pageNum,
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
        if (deptId != null) qw.eq("dept_id", deptId);
        if (status != null) qw.eq("status", status);
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

    @Operation(summary = "创建用户")
    @PostMapping
    @ApiLog("user.create")
    public Result<UserDto> create(@Valid @RequestBody UserCreateRequest req) {
        return Result.ok(userService.createUser(req));
    }

    @Operation(summary = "查询用户")
    @GetMapping("/{id}")
    public Result<UserDto> get(@PathVariable Long id) {
        return Result.ok(userService.findById(id));
    }

    @Operation(summary = "更新用户")
    @PutMapping("/{id}")
    @ApiLog("user.update")
    public Result<UserDto> update(@PathVariable Long id, @RequestBody UserCreateRequest req) {
        return Result.ok(userService.updateUser(id, req));
    }

    @Operation(summary = "同步用户可用性（erp-business HR Feign 调用）")
    @PutMapping("/{id}/availability")
    @ApiLog("user.availability.sync")
    public Result<Void> syncAvailability(@PathVariable Long id,
                                         @RequestBody UserAvailabilitySyncRequest req) {
        userAvailabilityService.syncAvailability(id, req.getAvailabilityStatus(), req.getLeaveNo());
        return Result.ok();
    }

    @Operation(summary = "软删 / 禁用用户")
    @DeleteMapping("/{id}")
    @ApiLog("user.disable")
    public Result<Void> delete(@PathVariable Long id) {
        userService.disableUser(id);
        return Result.ok();
    }
}
