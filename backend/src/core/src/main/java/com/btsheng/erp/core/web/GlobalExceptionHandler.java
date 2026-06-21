package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.Result;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理（V1.3.7）
 *
 * <p>统一包装 {@link Result} 返回，自动注入 {@code traceId}。HTTP 状态语义：
 * <ul>
 *   <li>{@link BizException} → 200 + 业务码</li>
 *   <li>{@link AuthException} → 401 + 401xx</li>
 *   <li>{@link PermException} → 403 + 403xx</li>
 *   <li>其他 → 500 + 50001</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BizException.class)
    public ResponseEntity<Result<Void>> handleBiz(BizException e, HttpServletResponse response) {
        log.warn("[BizException] code={} msg={}", e.getCode(), e.getMessage());
        return ResponseEntity.ok(enrich(Result.fail(e.getCode(), e.getMessage())));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<Result<Void>> handleAuth(AuthException e) {
        log.info("[AuthException] code={} msg={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(enrich(Result.fail(e.getCode(), e.getMessage())));
    }

    @ExceptionHandler(PermException.class)
    public ResponseEntity<Result<Void>> handlePerm(PermException e) {
        log.warn("[PermException] code={} msg={}", e.getCode(), e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(enrich(Result.fail(e.getCode(), e.getMessage())));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<Result<Void>> handleAuthentication(AuthenticationException e) {
        log.info("[AuthenticationException] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(enrich(Result.fail(Result.CODE_UNAUTHORIZED, "未登录或登录已过期，请重新登录")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Result<Void>> handleAccessDenied(AccessDeniedException e) {
        log.warn("[AccessDenied] {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(enrich(Result.fail(Result.CODE_FORBIDDEN, "无权限访问该功能，请联系管理员分配角色")));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<Void>> handleNotFound(NoResourceFoundException e) {
        log.warn("[NotFound] {}", e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(enrich(Result.fail(Result.CODE_NOT_FOUND, "接口不存在或未部署：" + e.getResourcePath())));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("[MethodNotSupported] {} {}", e.getMethod(), e.getMessage());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(enrich(Result.fail(Result.CODE_PARAM_FORMAT, "请求方法不支持：" + e.getMethod())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleValid(MethodArgumentNotValidException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数校验失败";
        log.warn("[ValidException] {}", msg);
        return ResponseEntity.ok(enrich(Result.fail(Result.CODE_PARAM_FORMAT, msg)));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBind(BindException e) {
        FieldError fe = e.getBindingResult().getFieldError();
        String msg = fe != null ? fe.getDefaultMessage() : "参数绑定失败";
        return ResponseEntity.ok(enrich(Result.fail(Result.CODE_PARAM_FORMAT, msg)));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraint(ConstraintViolationException e) {
        return ResponseEntity.ok(enrich(Result.fail(Result.CODE_PARAM_FORMAT, e.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleUnknown(Exception e) {
        log.error("[SystemException] unexpected", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(enrich(Result.fail(Result.CODE_SYSTEM, "系统异常，请稍后再试")));
    }

    private <T> Result<T> enrich(Result<T> r) {
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
