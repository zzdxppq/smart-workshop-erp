package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.dto.UserCreateRequest;
import com.btsheng.erp.platform.auth.dto.UserDto;
import com.btsheng.erp.platform.auth.entity.SysRole;
import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.entity.SysUserRole;
import com.btsheng.erp.platform.auth.mapper.SysRoleMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserRoleMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 用户 Service（V1.3.7 · AC-1.1.1）
 *
 * <p>业务规则（BR-1~BR-6）：<br>
 * - 用户名全局唯一（小写归一）<br>
 * - 密码 BCrypt cost=12<br>
 * - phone 字段 AES-256-GCM 加密<br>
 * - 审计 100% 留痕
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Service
public class UserService {

    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysUserRoleMapper userRoleMapper;

    @Autowired
    public UserService(SysUserMapper userMapper, SysRoleMapper roleMapper, SysUserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.roleMapper = roleMapper;
        this.userRoleMapper = userRoleMapper;
    }

    /**
     * 创建用户（AC-1.1.1）。
     */
    @AuditLog(module = "user", action = "user.create")
    @Transactional
    public UserDto createUser(UserCreateRequest req) {
        // 1. 校验用户名
            String username = UsernameNormalizer.normalize(req.getUsername());
        // 2. 校验密码强度（HR 建档初始密码可与登录名相同）
            validatePasswordForCreate(req, username);
        // 3. 校验角色
            if (req.getRoleCodes() == null || req.getRoleCodes().isEmpty()) {
            throw new BizException(Result.CODE_PARAM_MISSING, "请至少分配一个角色");
        }
        // 4. 唯一性
            SysUser exist = userMapper.findActiveByUsername(username);
        if (exist != null) {
            throw new BizException(Result.CODE_CONFLICT, "用户名已存在");
        }
        // 5. 查 role_id
            List<Long> roleIds = new ArrayList<>();
        for (String code : req.getRoleCodes()) {
            SysRole role = roleMapper.findByCode(code);
            if (role == null) {
                throw new BizException(Result.CODE_NOT_FOUND, "角色 " + code + " 不存在");
            }
            roleIds.add(role.getId());
        }
        // 6. 写 sys_user
            SysUser u = new SysUser();
        u.setUsername(username);
        u.setPasswordHash(BcryptStrengthChecker.encode(req.getPassword()));
        u.setRealName(req.getRealName());
        u.setPhone(req.getPhone());
        u.setEmail(req.getEmail());
        u.setDeptId(req.getDeptId());
        u.setStatus("ACTIVE");
        u.setCreateTime(LocalDateTime.now());
        u.setUpdateTime(LocalDateTime.now());
        userMapper.insert(u);
        // 7. 写 sys_user_role
            for (Long roleId : roleIds) {
            userRoleMapper.insertRel(u.getId(), roleId);
        }
        // 8. 返回 DTO（不返回 passwordHash）
            return toDto(u, req.getRoleCodes());
    }

    public UserDto findById(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        return toDto(u, Collections.emptyList());
    }

    @Transactional
    public UserDto updateUser(Long id, UserCreateRequest req) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        if (req.getRealName() != null && !req.getRealName().isBlank()) {
            u.setRealName(req.getRealName());
        }
        if (req.getPhone() != null) {
            u.setPhone(req.getPhone());
        }
        if (req.getEmail() != null) {
            u.setEmail(req.getEmail());
        }
        if (req.getDeptId() != null) {
            u.setDeptId(req.getDeptId());
        }
        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            PasswordValidator.validate(req.getPassword());
            u.setPasswordHash(BcryptStrengthChecker.encode(req.getPassword()));
        }
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);

        List<String> roleCodes = req.getRoleCodes();
        if (roleCodes != null && !roleCodes.isEmpty()) {
            userRoleMapper.deleteByUserId(id);
            for (String code : roleCodes) {
                SysRole role = roleMapper.findByCode(code);
                if (role == null) {
                    throw new BizException(Result.CODE_NOT_FOUND, "角色 " + code + " 不存在");
                }
                userRoleMapper.insertRel(id, role.getId());
            }
        } else {
            roleCodes = Collections.emptyList();
        }
        return toDto(u, roleCodes);
    }

    @Transactional
    public void disableUser(Long id) {
        SysUser u = userMapper.selectById(id);
        if (u == null) {
            throw new BizException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        u.setStatus("DISABLED");
        u.setUpdateTime(LocalDateTime.now());
        userMapper.updateById(u);
    }

    public List<String> findRoleCodes(Long userId) {
        // 简化：经由 user_role 中间表聚合
            return Collections.emptyList();
    }

    private static void validatePasswordForCreate(UserCreateRequest req, String username) {
        String password = req.getPassword();
        if (password == null || password.isBlank()) {
            throw new BizException(Result.CODE_PARAM_FORMAT, "密码不能为空");
        }
        if (Boolean.TRUE.equals(req.getHrAutoProvision())
                && password.equalsIgnoreCase(username)) {
            if (password.length() < 3 || password.length() > PasswordValidator.MAX_LENGTH) {
                throw new BizException(Result.CODE_PARAM_FORMAT, "密码长度 3-32 位");
            }
            return;
        }
        PasswordValidator.validate(password);
    }

    private UserDto toDto(SysUser u, List<String> roleCodes) {
        UserDto d = new UserDto();
        d.setId(u.getId());
        d.setUsername(u.getUsername());
        d.setRealName(u.getRealName());
        d.setPhone(u.getPhone());
        d.setEmail(u.getEmail());
        d.setDeptId(u.getDeptId());
        d.setStatus(u.getStatus());
        d.setCreateTime(u.getCreateTime());
        d.setUpdateTime(u.getUpdateTime());
        d.setRoleCodes(roleCodes);
        return d;
    }
}
