package com.btsheng.erp.platform.auth.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.DekLoader;
import com.btsheng.erp.core.web.RedisBlacklist;
import com.btsheng.erp.core.web.AuthException;
import com.btsheng.erp.core.web.BizException;
import com.btsheng.erp.platform.auth.dto.LoginRequest;
import com.btsheng.erp.platform.auth.dto.LoginResponse;
import com.btsheng.erp.platform.auth.dto.UserDto;
import com.btsheng.erp.platform.auth.dto.UserMenuAccessDto;
import com.btsheng.erp.platform.auth.entity.SysUser;
import com.btsheng.erp.platform.auth.mapper.SysUserMapper;
import com.btsheng.erp.platform.auth.mapper.SysUserRoleMapper;
import com.btsheng.erp.platform.auth.security.JwtSigner;
import com.btsheng.erp.platform.auth.security.RoleCodeAliases;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 认证 Service（V1.3.7 · AC-1.1.1 BR-4/BR-5）
 *
 * <p>登录失败 5 次锁 30 分钟；登出写黑名单 TTL = token 剩余有效期。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Service
public class AuthService {

    public static final int MAX_LOGIN_FAIL = 5;
    public static final long LOCK_TTL_SECONDS = 30 * 60L;
    public static final String LOGIN_FAIL_KEY_PREFIX = "auth:login:fail:";

    private final SysUserMapper userMapper;
    private final SysUserRoleMapper userRoleMapper;
    private final MenuPermissionService menuPermissionService;
    private final JwtSigner jwtSigner;
    private final RedisBlacklist blacklist;
    private final StringRedisTemplate redis;

    @Value("${app.security.login-fail-window-seconds:1800}")
    private long loginFailWindow;

    @Autowired
    public AuthService(SysUserMapper userMapper, SysUserRoleMapper userRoleMapper,
                       MenuPermissionService menuPermissionService,
                       JwtSigner jwtSigner,
                       RedisBlacklist blacklist, StringRedisTemplate redis) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
        this.menuPermissionService = menuPermissionService;
        this.jwtSigner = jwtSigner;
        this.blacklist = blacklist;
        this.redis = redis;
    }

    public LoginResponse login(LoginRequest req) {
        String username = req.getUsername() == null ? "" : req.getUsername().toLowerCase();
        // 1. 失败计数
            String failKey = LOGIN_FAIL_KEY_PREFIX + username;
        String cur = redis.opsForValue().get(failKey);
        int fails = cur == null ? 0 : Integer.parseInt(cur);
        if (fails >= MAX_LOGIN_FAIL) {
            throw new AuthException(40104, "账号已锁定 30 分钟，请稍后再试");
        }
        // 2. 查用户
            SysUser u = userMapper.findActiveByUsername(username);
        if (u == null || !"ACTIVE".equals(u.getStatus())) {
            incrFail(failKey);
            throw new AuthException(40101, "用户名或密码错误");
        }
        // 3. 校验密码
            if (!BcryptStrengthChecker.verify(req.getPassword(), u.getPasswordHash())) {
            incrFail(failKey);
            throw new AuthException(40101, "用户名或密码错误");
        }
        // 4. 清失败计数
            redis.delete(failKey);
        // 5. 签发 token（角色来自 sys_user_role，含 Spring Security 别名）
        List<String> roleCodes = userRoleMapper.findRoleCodesByUserId(u.getId());
        List<String> expandedRoles = RoleCodeAliases.expand(roleCodes);
        String rolesJwt = RoleCodeAliases.toJwtRoles(roleCodes);
        String access = jwtSigner.signAccessToken(u.getId(), u.getUsername(), rolesJwt, u.getDeptId(),
                resolveDataScope(u.getId(), roleCodes));
        String refresh = jwtSigner.signRefreshToken(u.getId(), u.getUsername());
        // 6. 写 last_login_time（仅更新登录时间字段）
        SysUser patch = new SysUser();
        patch.setId(u.getId());
        patch.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(patch);
        // 7. 响应
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(access);
        resp.setRefreshToken(refresh);
        resp.setExpiresIn(jwtSigner.getAccessTtlSeconds());
        resp.setUser(toDto(u));
        resp.setRoles(expandedRoles);
        applyMenuAccess(resp, u.getId(), roleCodes);
        return resp;
    }

    public void logout(String accessToken, Long userId) {
        if (accessToken == null || userId == null) return;
        try {
            Claims c = jwtSigner.parse(accessToken);
            long remaining = jwtSigner.remainingSeconds(accessToken);
            blacklist.blacklist(userId, c.getId(), remaining);
        } catch (Exception e) {
            // 解析失败视作已过期，无需黑名单
        }
    }

    public LoginResponse refresh(String refreshToken) {
        Claims c = jwtSigner.parse(refreshToken);
        Long uid = c.get(JwtSigner.CLAIM_USER_ID, Number.class).longValue();
        String usr = c.get(JwtSigner.CLAIM_USERNAME, String.class);
        SysUser u = userMapper.selectById(uid);
        if (u == null || !"ACTIVE".equals(u.getStatus())) {
            throw new AuthException(40101, "用户不存在或已禁用");
        }
        List<String> roleCodes = userRoleMapper.findRoleCodesByUserId(u.getId());
        String rolesJwt = RoleCodeAliases.toJwtRoles(roleCodes);
        String access = jwtSigner.signAccessToken(u.getId(), u.getUsername(), rolesJwt, u.getDeptId(),
                resolveDataScope(u.getId(), roleCodes));
        LoginResponse resp = new LoginResponse();
        resp.setAccessToken(access);
        resp.setRefreshToken(refreshToken);
        resp.setExpiresIn(jwtSigner.getAccessTtlSeconds());
        resp.setUser(toDto(u));
        resp.setRoles(RoleCodeAliases.expand(roleCodes));
        applyMenuAccess(resp, u.getId(), roleCodes);
        return resp;
    }

    public UserMenuAccessDto menus(Long userId) {
        if (userId == null) {
            return new UserMenuAccessDto();
        }
        List<String> roleCodes = userRoleMapper.findRoleCodesByUserId(userId);
        return menuPermissionService.resolveForUser(userId, roleCodes);
    }

    private void applyMenuAccess(LoginResponse resp, Long userId, List<String> roleCodes) {
        UserMenuAccessDto access = menuPermissionService.resolveForUser(userId, roleCodes);
        resp.setMenuPaths(access.getMenuPaths());
        resp.setPermissions(access.getPermissions());
    }

    public UserDto me(Long userId) {
        SysUser u = userMapper.selectById(userId);
        if (u == null) {
            throw new BizException(Result.CODE_NOT_FOUND, "用户不存在");
        }
        return toDto(u);
    }

    public boolean isDekReady() {
        return DekLoader.isReady();
    }

    private void incrFail(String key) {
        Long v = redis.opsForValue().increment(key);
        if (v != null && v == 1L) {
            redis.expire(key, Duration.ofSeconds(loginFailWindow));
        }
    }

    private String resolveDataScope(Long userId, List<String> roleCodes) {
        if (roleCodes != null && roleCodes.stream().anyMatch(c -> "SYS_ADMIN".equals(c) || "GM".equals(c))) {
            return "ALL";
        }
        List<String> scopes = userRoleMapper.findDataScopesByUserId(userId);
        if (scopes == null || scopes.isEmpty()) {
            return "SELF";
        }
        if (scopes.stream().anyMatch(s -> "ALL".equalsIgnoreCase(s))) {
            return "ALL";
        }
        if (scopes.stream().anyMatch(s -> "DEPT".equalsIgnoreCase(s))) {
            return "DEPT";
        }
        return "SELF";
    }

    private UserDto toDto(SysUser u) {
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
        return d;
    }
}
