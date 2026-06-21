package com.btsheng.erp.core.web;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * API 日志切面（V1.3.7）
 *
 * <p>环绕通知：记录方法耗时 + 脱敏入参。敏感字段（password / password_hash / phone）一律 mask。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Aspect
@Component
public class ApiLogAspect {

    private static final Logger log = LoggerFactory.getLogger(ApiLogAspect.class);

    private static final Set<String> SENSITIVE = new HashSet<>(Arrays.asList(
            "password", "passwordHash", "password_hash", "oldPassword", "newPassword",
            "phone", "mobile", "idCard", "id_card", "authCode", "auth_code"));

    private final ObjectMapper mapper = new ObjectMapper();

    @Around("@annotation(apiLog)")
    public Object around(ProceedingJoinPoint pjp, ApiLog apiLog) throws Throwable {
        long start = System.currentTimeMillis();
        MethodSignature sig = (MethodSignature) pjp.getSignature();
        Method method = sig.getMethod();
        String desc = apiLog.value().isEmpty() ? method.getName() : apiLog.value();
        try {
            Object result = pjp.proceed();
            long cost = System.currentTimeMillis() - start;
            if (log.isInfoEnabled()) {
                String args = apiLog.logArgs() ? mask(pjp.getArgs()) : "[hidden]";
                log.info("[API] {} {} cost={}ms args={}", method.getDeclaringClass().getSimpleName(),
                        desc, cost, args);
            }
            return result;
        } catch (Throwable t) {
            long cost = System.currentTimeMillis() - start;
            log.warn("[API] {} {} cost={}ms failed: {}", method.getDeclaringClass().getSimpleName(),
                    desc, cost, t.getMessage());
            throw t;
        }
    }

    private String mask(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(maskValue(args[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    private String maskValue(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        for (String key : SENSITIVE) {
            if (s.toLowerCase().contains(key.toLowerCase())) {
                return "***MASKED***";
            }
        }
        try {
            String json = mapper.writeValueAsString(o);
            if (json.length() > 256) {
                return json.substring(0, 256) + "...";
            }
            return json;
        } catch (JsonProcessingException e) {
            return s.length() > 128 ? s.substring(0, 128) + "..." : s;
        }
    }
}
