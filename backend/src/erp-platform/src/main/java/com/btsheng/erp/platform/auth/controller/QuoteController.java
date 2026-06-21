package com.btsheng.erp.platform.auth.controller;

import com.btsheng.erp.core.web.AuditLog;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.ApiLog;
import com.btsheng.erp.platform.auth.dto.QuoteApprovalResult;
import com.btsheng.erp.platform.auth.service.QuoteApprovalRouter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * 报价提交接口（V1.3.7 · AC-1.1.3 · T3.3）
 *
 * <p>消费 {@link QuoteApprovalRouter} 决定审批人，返回路由结果。
 * 实际报价单持久化留给 Story 1.3 系统单据 + Story 2.x 销售报价。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Tag(name = "E1-Quote", description = "报价审批路由（消费 Router）")
@RestController
@RequestMapping("/quotes")
public class QuoteController {

    private final QuoteApprovalRouter router;

    @Autowired
    public QuoteController(QuoteApprovalRouter router) {
        this.router = router;
    }

    @Operation(summary = "提交报价（消费路由 · 返回审批人）")
    @PostMapping
    @ApiLog("quote.submit")
    @AuditLog(module = "quote", action = "quote.submit")
    public Result<Map<String, Object>> submit(@RequestBody @NotNull QuoteSubmitRequest req,
                                              HttpServletRequest request) {
        Long userId = extractUserId(request);
        QuoteApprovalResult routing = router.route(req.getAmount(), userId);
        Map<String, Object> data = new HashMap<>();
        data.put("approverUserId", routing.getApproverUserId());
        data.put("currentNode", routing.getCurrentNode());
        data.put("candidates", routing.getCandidates());
        data.put("reason", routing.getReason());
        // V1.3.7 简化：本 Story 不持久化报价单（留给 Story 1.3 单据 + Story 2.x 销售报价）
            return Result.ok(data);
    }

    private Long extractUserId(HttpServletRequest request) {
        String h = request.getHeader("X-User-Id");
        if (h == null) {
            // dev 兜底
            return 10086L;
        }
        try {
            return Long.parseLong(h);
        } catch (NumberFormatException e) {
            return 10086L;
        }
    }

    public static class QuoteSubmitRequest {
        private BigDecimal amount;
        private Long customerId;
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public Long getCustomerId() { return customerId; }
        public void setCustomerId(Long customerId) { this.customerId = customerId; }
    }
}
