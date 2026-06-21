package com.btsheng.erp.core.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计日志注解（V1.3.7 · T4.3）
 *
 * <p>标注 Service 方法后由 {@link AuditAspect} 在 {@code AFTER_COMMIT} 阶段写
 * {@code sys_audit_log}。失败兜底到 {@code sys_audit_log_error}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditLog {

    /** 模块名（如 "auth" / "user" / "role"） */
    String module();

    /** 操作名（如 "user.create" / "role.update_permissions"） */
    String action();

    /** 是否采集前值（默认 true） */
    boolean captureBefore() default true;

    /** 是否采集后值（默认 true） */
    boolean captureAfter() default true;

    /** 敏感字段（写入审计时打码） */
    String[] sensitiveFields() default {"password", "passwordHash", "password_hash", "phone", "idCard"};
}
