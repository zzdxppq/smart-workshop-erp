package com.btsheng.erp.core.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API 日志注解（V1.3.7）
 *
 * <p>标注 Controller 方法后由 {@code ApiLogAspect} 自动记录入参/出参/耗时（脱敏）。
 * 严禁在日志中输出 {@code password} / {@code password_hash} / {@code phone}。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiLog {

    /** 操作描述 */
    String value() default "";

    /** 是否记录入参（默认 true，敏感接口可关闭） */
    boolean logArgs() default true;

    /** 是否记录出参（默认 false，写操作可能数据量较大） */
    boolean logResult() default false;
}
