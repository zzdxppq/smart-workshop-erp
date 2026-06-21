package com.btsheng.erp.core.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.Serializable;

/**
 * 统一 API 返回结构（V1.3.7）
 *
 * <p>与 OpenAPI {@code Result} schema 100% 对齐：code / message / data / traceId。
 * 错误码语义：0=成功 · 4xxxx=业务错误 · 5xxxx=系统错误。
 *
 * @param <T> 业务数据类型
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Schema(description = "统一返回结构")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final int CODE_SUCCESS = 0;
    public static final int CODE_PARAM_MISSING = 40001;
    public static final int CODE_PARAM_FORMAT = 40002;
    public static final int CODE_PARAM_BOUND = 40003;
    public static final int CODE_UNAUTHORIZED = 40101;
    public static final int CODE_TOKEN_EXPIRED = 40102;
    public static final int CODE_TOKEN_BLACKLISTED = 40103;
    public static final int CODE_ACCOUNT_LOCKED = 40104;
    public static final int CODE_FORBIDDEN = 40301;
    public static final int CODE_DATA_SCOPE = 40302;
    public static final int CODE_AMOUNT_LIMIT = 40303;
    public static final int CODE_NOT_FOUND = 40401;
    public static final int CODE_NOT_FOUND_ROUTING = 40402;
    public static final int CODE_CONFLICT = 40901;
    public static final int CODE_CONFLICT_IN_USE = 40902;
    public static final int CODE_CONFLICT_BUILTIN = 40903;
    public static final int CODE_SYSTEM = 50001;
    public static final int CODE_DB = 50002;
    public static final int CODE_REDIS = 50003;

    @Schema(description = "业务码，0=成功", example = "0")
    private int code;

    @Schema(description = "消息", example = "ok")
    private String message;

    @Schema(description = "业务数据")
    private T data;

    @Schema(description = "链路追踪 ID", example = "abc123def456")
    private String traceId;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "ok", null);
    }

    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "ok", data);
    }

    public static <T> Result<T> ok(String message, T data) {
        return new Result<>(CODE_SUCCESS, message, data);
    }

    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    public boolean isSuccess() {
        return code == CODE_SUCCESS;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
}
