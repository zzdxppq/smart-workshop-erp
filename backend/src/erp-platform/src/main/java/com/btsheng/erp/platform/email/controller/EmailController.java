package com.btsheng.erp.platform.email.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import com.btsheng.erp.platform.email.dto.EmailConfigDto;
import com.btsheng.erp.platform.email.dto.EmailSendRequest;
import com.btsheng.erp.platform.email.dto.EmailTestRequest;
import com.btsheng.erp.platform.email.service.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Tag(name = "E1-Email", description = "邮件配置 · 163 SMTP · 发送日志")
@RestController
@RequestMapping("/email")
public class EmailController {

    private final EmailService emailService;

    public EmailController(EmailService emailService) {
        this.emailService = emailService;
    }

    @GetMapping("/config")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    @Operation(summary = "获取邮件配置")
    public Result<EmailConfigDto> getConfig() {
        return emailService.getConfig();
    }

    @PutMapping("/config")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    @Operation(summary = "更新邮件配置")
    public Result<EmailConfigDto> updateConfig(@RequestBody EmailConfigDto body) {
        return emailService.updateConfig(body);
    }

    @PostMapping("/test")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    @Operation(summary = "测试发送")
    public Result<Map<String, Object>> testSend(@RequestBody EmailTestRequest body) {
        return emailService.testSend(body);
    }

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('SALES', 'SALES_MGR', 'SALES_MANAGER', 'GM', 'ADMIN', 'SYS_ADMIN')")
    @Operation(summary = "业务邮件发送（可带附件）")
    public Result<Map<String, Object>> send(@RequestBody EmailSendRequest body) {
        return emailService.sendBusinessEmail(body);
    }

    @GetMapping("/logs")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    @Operation(summary = "发送日志查询")
    public Result<Map<String, Object>> logs(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String status) {
        return emailService.listLogs(status, pageNum, pageSize);
    }
}
