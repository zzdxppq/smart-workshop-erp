package com.btsheng.erp.platform.admin.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.admin.dto.EmailTemplateDto;
import com.btsheng.erp.platform.admin.dto.FieldEncryptionConfigDto;
import com.btsheng.erp.platform.admin.service.EmailTemplateService;
import com.btsheng.erp.platform.admin.service.FieldEncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "E1-Admin", description = "系统管理 · 邮件模板 / 字段加密")
@RestController
@RequestMapping("/admin")
public class AdminSystemController {

    private final EmailTemplateService emailTemplateService;
    private final FieldEncryptionService fieldEncryptionService;

    public AdminSystemController(EmailTemplateService emailTemplateService,
                                   FieldEncryptionService fieldEncryptionService) {
        this.emailTemplateService = emailTemplateService;
        this.fieldEncryptionService = fieldEncryptionService;
    }

    @GetMapping("/email-templates")
    @Operation(summary = "邮件模板列表（5 套）")
    public Result<List<EmailTemplateDto>> listEmailTemplates() {
        return emailTemplateService.listTemplates();
    }

    @GetMapping("/email-templates/{key}")
    @Operation(summary = "单个邮件模板")
    public Result<EmailTemplateDto> getEmailTemplate(@PathVariable String key) {
        return emailTemplateService.getTemplate(key);
    }

    @PutMapping("/email-templates/{key}")
    @Operation(summary = "更新邮件模板")
    public Result<EmailTemplateDto> updateEmailTemplate(@PathVariable String key, @RequestBody EmailTemplateDto body) {
        return emailTemplateService.updateTemplate(key, body);
    }

    @GetMapping("/field-encryption")
    @Operation(summary = "字段加密配置")
    public Result<FieldEncryptionConfigDto> getFieldEncryption() {
        return fieldEncryptionService.getConfig();
    }

    @PutMapping("/field-encryption")
    @Operation(summary = "更新字段加密白名单")
    public Result<FieldEncryptionConfigDto> updateFieldEncryption(@RequestBody FieldEncryptionConfigDto body) {
        return fieldEncryptionService.updateConfig(body);
    }
}
