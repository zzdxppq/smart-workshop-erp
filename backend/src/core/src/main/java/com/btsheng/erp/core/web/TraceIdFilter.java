package com.btsheng.erp.core.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * TraceId 注入过滤器（V1.3.7）
 *
 * <p>从 header {@code X-Trace-Id} 读取链路 ID；缺省生成 UUID。
 * 写入 SLF4J MDC（{@code trace_id}）以贯穿全链路日志，同时回写到 Response Header。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceIdFilter extends OncePerRequestFilter {

    public static final String HEADER_TRACE_ID = "X-Trace-Id";
    public static final String MDC_TRACE_ID = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String traceId = request.getHeader(HEADER_TRACE_ID);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        try {
            MDC.put(MDC_TRACE_ID, traceId);
            response.setHeader(HEADER_TRACE_ID, traceId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_TRACE_ID);
        }
    }
}
