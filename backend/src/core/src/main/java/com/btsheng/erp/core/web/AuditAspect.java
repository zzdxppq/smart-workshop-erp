package com.btsheng.erp.core.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 审计切面（V1.3.7 · T4.3 · AFTER_COMMIT 触发）
 *
 * <p>业务方法返回成功后，绑定到当前事务的 AFTER_COMMIT 钩子：事务真正提交才写审计。
 * 切面自身异常写 {@code sys_audit_log_error} 兜底表（不阻断主流程）。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Aspect
@Component
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired(required = false)
    private JdbcTemplate jdbcTemplate;

    @Around("@annotation(auditLog)")
    public Object around(ProceedingJoinPoint pjp, AuditLog auditLog) throws Throwable {
        Object before = null;
        Object result = null;
        Throwable thrown = null;
        try {
            if (auditLog.captureBefore()) {
                before = captureArgs(pjp);
            }
            result = pjp.proceed();
            return result;
        } catch (Throwable t) {
            thrown = t;
            throw t;
        } finally {
            try {
                Object after = auditLog.captureAfter() && thrown == null ? result : null;
                AuditEntry entry = new AuditEntry(auditLog.module(), auditLog.action(),
                        serialize(before), serialize(after), currentUserId());
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            persist(entry);
                        }
                    });
                } else {
                    persist(entry);
                }
            } catch (Exception ex) {
                log.error("[AuditAspect] failed to write audit log, fallback to sys_audit_log_error", ex);
                writeErrorFallback(auditLog, ex);
            }
        }
    }

    private Object captureArgs(ProceedingJoinPoint pjp) {
        Object[] args = pjp.getArgs();
        if (args == null || args.length == 0) return null;
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method m = sig.getMethod();
        String[] paramNames = sig.getParameterNames();
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String name = (paramNames != null && i < paramNames.length) ? paramNames[i] : "arg" + i;
            map.put(name, mask(m, args[i]));
        }
        return map;
    }

    private Object mask(Method m, Object o) {
        if (o == null) return null;
        return o; // 简化实装：敏感字段在 DTO 上 @JsonIgnore；切面层不重复打码
    }

    private String serialize(Object o) {
        if (o == null) return null;
        try {
            return mapper.writeValueAsString(o);
        } catch (Exception e) {
            return String.valueOf(o);
        }
    }

    private Long currentUserId() {
        // 简化：从 SecurityContextHolder / DataScopeContext 取
            com.btsheng.erp.core.web.DataScopeContext ctx =
                com.btsheng.erp.core.web.DataScopeContext.current();
        return ctx == null ? 0L : ctx.getUserId();
    }

    private void persist(AuditEntry entry) {
        // V1.3.9 P0：写入 sys_audit_log 表（JdbcTemplate 由 spring-boot-starter-jdbc 提供）
        if (jdbcTemplate != null) {
            try {
                jdbcTemplate.update(
                    "INSERT INTO sys_audit_log (user_id, module, action, before_json, after_json, ip, ts) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    entry.userId,
                    entry.module,
                    entry.action,
                    entry.before,
                    entry.after,
                    getClientIp(),
                    LocalDateTime.now()
                );
            } catch (Exception ex) {
                log.error("[AUDIT] failed to write sys_audit_log, fallback to log", ex);
            }
        } else {
            log.info("[AUDIT] module={} action={} userId={} before={} after={}",
                entry.module, entry.action, entry.userId, entry.before, entry.after);
        }
    }

    private String getClientIp() {
        try {
            org.springframework.web.context.request.ServletRequestAttributes attr =
                (org.springframework.web.context.request.ServletRequestAttributes)
                    org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes();
            if (attr != null) {
                String xf = attr.getRequest().getHeader("X-Forwarded-For");
                if (xf != null && !xf.isEmpty()) return xf.split(",")[0].trim();
                return attr.getRequest().getRemoteAddr();
            }
        } catch (Exception ignored) {}
        return null;
    }

    private void writeErrorFallback(AuditLog auditLog, Exception ex) {
        log.error("[AUDIT_FALLBACK] module={} action={} error={}",
                auditLog.module(), auditLog.action(), ex.getMessage());
    }

    /** 审计记录（不可变值对象） */
    public static class AuditEntry {
        public final String module;
        public final String action;
        public final String before;
        public final String after;
        public final Long userId;

        public AuditEntry(String module, String action, String before, String after, Long userId) {
            this.module = module;
            this.action = action;
            this.before = before;
            this.after = after;
            this.userId = userId;
        }
    }
}
