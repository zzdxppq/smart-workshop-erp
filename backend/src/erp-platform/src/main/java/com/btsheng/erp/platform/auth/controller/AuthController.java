package com.btsheng.erp.platform.auth.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.ApiLog;
import com.btsheng.erp.platform.auth.dto.LoginRequest;
import com.btsheng.erp.platform.auth.dto.LoginResponse;
import com.btsheng.erp.platform.auth.dto.UserDto;
import com.btsheng.erp.platform.auth.dto.UserMenuAccessDto;
import com.btsheng.erp.platform.auth.security.JwtSigner;
import com.btsheng.erp.platform.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证 Controller（V1.3.7 · auth × 3）
 *
 * <p>13 端点 = auth×3 + users×5 + roles×5（users/roles 见 UserController/RoleController）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Tag(name = "E1-Auth", description = "认证 / 用户 / 角色（E1-S1）")
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtSigner jwtSigner;

    @Autowired
    public AuthController(AuthService authService, JwtSigner jwtSigner) {
        this.authService = authService;
        this.jwtSigner = jwtSigner;
    }

    @Operation(summary = "登录")
    @PostMapping("/login")
    @ApiLog("auth.login")
    public Result<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        return Result.ok(authService.login(req));
    }

    @Operation(summary = "登出（写黑名单，TTL = token 剩余有效期）")
    @PostMapping("/logout")
    @ApiLog(value = "auth.logout", logArgs = false)
    public Result<Void> logout(@RequestHeader(value = "Authorization", required = false) String authHeader,
                               HttpServletRequest request) {
        String token = extractToken(authHeader);
        Long userId = extractUserIdFromRequest(request, authHeader);
        authService.logout(token, userId);
        return Result.ok();
    }

    @Operation(summary = "刷新 access token")
    @PostMapping("/refresh")
    public Result<LoginResponse> refresh(@RequestBody String refreshToken) {
        return Result.ok(authService.refresh(refreshToken.replace("\"", "")));
    }

    @Operation(summary = "当前用户信息")
    @GetMapping("/me")
    public Result<UserDto> me(@RequestHeader(value = "Authorization", required = false) String authHeader,
                              HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request, authHeader);
        return Result.ok(authService.me(userId));
    }

    @Operation(summary = "当前用户菜单权限（sys_menu + sys_role_permission）")
    @GetMapping("/menus")
    public Result<UserMenuAccessDto> menus(@RequestHeader(value = "Authorization", required = false) String authHeader,
                                           HttpServletRequest request) {
        Long userId = extractUserIdFromRequest(request, authHeader);
        return Result.ok(authService.menus(userId));
    }

    private String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    private Long extractUserIdFromRequest(HttpServletRequest request, String authHeader) {
        String token = extractToken(authHeader);
        if (token != null) {
            try {
                Claims claims = jwtSigner.parse(token);
                Number uid = claims.get(JwtSigner.CLAIM_USER_ID, Number.class);
                if (uid != null) {
                    return uid.longValue();
                }
                return Long.parseLong(claims.getSubject());
            } catch (JwtException | NumberFormatException ignored) {
                // fall through to header
            }
        }
        String h = request.getHeader("X-User-Id");
        if (h == null) return null;
        try {
            return Long.parseLong(h);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
