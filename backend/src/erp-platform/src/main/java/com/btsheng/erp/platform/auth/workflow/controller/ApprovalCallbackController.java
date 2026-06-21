package com.btsheng.erp.platform.auth.workflow.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.auth.workflow.config.WorkflowConfig;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 业务侧审批回调 Controller（V1.3.7 · Story 1.2 · T3.3 · architect P2 反馈 ⑤）
 *
 * <p>内部端点，Service Token 校验（{@code X-Service-Token}）— 复用
 * {@link WorkflowConfig#serviceTokens}（Nacos 配置，避免硬编码）。
 *
 * <p>提供：
 * <ul>
 *   <li>POST /approvals/callback/quote-to-order - 报价转订单触发自动审批</li>
 *   <li>POST /approvals/callback/order-cancel - 订单取消反向同步审批</li>
 * </ul>
 */
@Tag(name = "E1-Workflow", description = "业务回调（Service Token 守）")
@RestController
@RequestMapping("/approvals/callback")
public class ApprovalCallbackController {

    private static final Logger log = LoggerFactory.getLogger(ApprovalCallbackController.class);

    private final WorkflowConfig config;

    @Autowired
    public ApprovalCallbackController(WorkflowConfig config) {
        this.config = config;
    }

    @Operation(summary = "报价转订单触发自动审批（Service Token 守）")
    @PostMapping("/quote-to-order")
    public Result<Void> quoteToOrder(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!verifyServiceToken(request, config.getServiceTokens().getErpBusiness())) {
            return Result.fail(40101, "Invalid Service Token");
        }
        log.info("[ApprovalCallback] 报价转订单：body={}", body);
        // 业务侧实装在 Story 2.x，本 Story 仅占位
            return Result.ok("已受理", null);
    }

    @Operation(summary = "订单取消反向同步审批（Service Token 守）")
    @PostMapping("/order-cancel")
    public Result<Void> orderCancel(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (!verifyServiceToken(request, config.getServiceTokens().getErpBusiness())) {
            return Result.fail(40101, "Invalid Service Token");
        }
        log.info("[ApprovalCallback] 订单取消：body={}", body);
        return Result.ok("已受理", null);
    }

    /**
     * Service Token 校验（V1.3.7 §16 · architect P2 反馈 ⑤：Nacos 配置）。
     * 简化实装：直接字符串比较。生产实装应加 HMAC + 时间戳。
     */
    private boolean verifyServiceToken(HttpServletRequest request, String expected) {
        String token = request.getHeader("X-Service-Token");
        return token != null && token.equals(expected);
    }
}
