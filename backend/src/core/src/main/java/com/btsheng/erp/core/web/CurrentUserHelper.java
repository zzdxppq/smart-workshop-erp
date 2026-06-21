package com.btsheng.erp.core.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * 当前登录用户 ID 解析（V1.3.9 · 替代 username.hashCode 占位）
 */
public final class CurrentUserHelper {

    private CurrentUserHelper() {
    }

    public static Long currentUserId() {
        DataScopeContext ctx = DataScopeContext.current();
        if (ctx != null && ctx.getUserId() != null && ctx.getUserId() > 0) {
            return ctx.getUserId();
        }
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return null;
        }
        Object details = auth.getDetails();
        if (details instanceof Long uid && uid > 0) {
            return uid;
        }
        String name = auth.getName();
        if (name != null) {
            try {
                return Long.parseLong(name);
            } catch (NumberFormatException ignored) {
                // fall through
            }
        }
        return null;
    }

    public static Long resolveUserId(String username, java.util.function.Function<String, Long> usernameLookup) {
        Long fromCtx = currentUserId();
        if (fromCtx != null) {
            return fromCtx;
        }
        if (username != null && usernameLookup != null) {
            Long looked = usernameLookup.apply(username);
            if (looked != null) {
                return looked;
            }
        }
        if (username != null) {
            try {
                return Long.parseLong(username);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
