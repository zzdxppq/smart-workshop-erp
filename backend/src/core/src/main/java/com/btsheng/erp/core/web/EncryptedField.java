package com.btsheng.erp.core.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 加密字段注解（V1.3.7 · T4.2）
 *
 * <p>标注 entity 字段（如 {@code @EncryptedField String phone}），由
 * {@link AesGcmTypeHandler} 在 mapper 层自动加解密。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface EncryptedField {

    /** 字段语义标识（写日志 / 审计用） */
    String value() default "";
}
