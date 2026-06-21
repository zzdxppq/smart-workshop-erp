package com.btsheng.erp.platform.email.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.email.dto.EmailConfigDto;
import com.btsheng.erp.platform.email.dto.EmailSendRequest;
import com.btsheng.erp.platform.email.dto.EmailTestRequest;
import com.btsheng.erp.platform.email.entity.EmailConfig;
import com.btsheng.erp.platform.email.entity.EmailSendLog;
import com.btsheng.erp.platform.email.mapper.EmailConfigMapper;
import com.btsheng.erp.platform.email.mapper.EmailSendLogMapper;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final int SINGLETON_ID = 1;

    private final EmailConfigMapper configMapper;
    private final EmailSendLogMapper logMapper;

    public EmailService(EmailConfigMapper configMapper, EmailSendLogMapper logMapper) {
        this.configMapper = configMapper;
        this.logMapper = logMapper;
    }

    @Transactional(readOnly = true)
    public Result<EmailConfigDto> getConfig() {
        EmailConfig cfg = loadOrDefault();
        return Result.ok(toDto(cfg, false));
    }

    @Transactional
    public Result<EmailConfigDto> updateConfig(EmailConfigDto dto) {
        EmailConfig cfg = loadOrDefault();
        if (dto.getSmtpHost() != null) cfg.setSmtpHost(dto.getSmtpHost());
        if (dto.getSmtpPort() != null) cfg.setSmtpPort(dto.getSmtpPort());
        if (dto.getUseSsl() != null) cfg.setUseSsl(dto.getUseSsl());
        if (dto.getFromAddress() != null) cfg.setFromAddress(dto.getFromAddress());
        if (dto.getAuthCode() != null && !dto.getAuthCode().isBlank()) {
            cfg.setAuthCodeKek(dto.getAuthCode().trim());
        }
        if (dto.getRetryPolicy() != null && !dto.getRetryPolicy().isEmpty()) {
            cfg.setRetryPolicy(String.join(",", dto.getRetryPolicy()));
        }
        if (dto.getDailyQuota() != null) cfg.setDailyQuota(dto.getDailyQuota());
        if (dto.getQuotaWarnThreshold() != null) cfg.setWarnThreshold(dto.getQuotaWarnThreshold());
        if (dto.getLogRetentionDays() != null) cfg.setLogRetentionDays(dto.getLogRetentionDays());
        if (dto.getAttachmentMaxSizeMb() != null) cfg.setAttachmentMaxSizeMb(dto.getAttachmentMaxSizeMb());
        cfg.setUpdatedAt(LocalDateTime.now());
        if (configMapper.selectById(SINGLETON_ID) == null) {
            cfg.setId(SINGLETON_ID);
            configMapper.insert(cfg);
        } else {
            configMapper.updateById(cfg);
        }
        return Result.ok(toDto(cfg, false));
    }

    @Transactional(readOnly = true)
    public Result<Map<String, Object>> listLogs(String status, int pageNum, int pageSize) {
        if (pageNum < 1) pageNum = 1;
        if (pageSize < 1 || pageSize > 200) pageSize = 20;
        int offset = (pageNum - 1) * pageSize;
        List<EmailSendLog> rows = logMapper.selectPage(status, pageSize, offset);
        long total = logMapper.countByStatus(status);
        Map<String, Object> page = new HashMap<>();
        page.put("items", rows);
        page.put("list", rows);
        page.put("records", rows);
        page.put("total", total);
        page.put("pageNum", pageNum);
        page.put("pageSize", pageSize);
        return Result.ok(page);
    }

    @Transactional
    public Result<Map<String, Object>> testSend(EmailTestRequest req) {
        if (req == null || req.getToAddress() == null || req.getToAddress().isBlank()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "toAddress 必填");
        }
        EmailSendRequest send = new EmailSendRequest();
        send.setToAddress(req.getToAddress());
        send.setSubject(req.getSubject());
        send.setBody(req.getBody());
        send.setAuthCode(req.getAuthCode());
        return sendBusinessEmail(send);
    }

    @Transactional
    public Result<Map<String, Object>> sendBusinessEmail(EmailSendRequest req) {
        if (req == null || req.getToAddress() == null || req.getToAddress().isBlank()) {
            return Result.fail(Result.CODE_PARAM_MISSING, "toAddress 必填");
        }
        EmailConfig cfg = loadOrDefault();
        String authCode = resolveAuthCode(cfg, req.getAuthCode());
        if (authCode == null || authCode.isBlank() || authCode.startsWith("PLACEHOLDER")) {
            return Result.fail(50203, "SMTP 授权码未配置，请在邮件配置页保存 authCode");
        }

        String subject = req.getSubject() == null || req.getSubject().isBlank()
                ? "ERP 通知" : req.getSubject();
        String body = req.getBody() == null || req.getBody().isBlank()
                ? "请查收附件。" : req.getBody();

        byte[] attachment = null;
        if (req.getAttachmentBase64() != null && !req.getAttachmentBase64().isBlank()) {
            try {
                attachment = Base64.getDecoder().decode(req.getAttachmentBase64().trim());
            } catch (IllegalArgumentException ex) {
                return Result.fail(Result.CODE_PARAM_FORMAT, "attachmentBase64 无效");
            }
            int maxMb = cfg.getAttachmentMaxSizeMb() == null ? 10 : cfg.getAttachmentMaxSizeMb();
            if (attachment.length > maxMb * 1024L * 1024L) {
                return Result.fail(Result.CODE_PARAM_BOUND, "附件超过大小限制 " + maxMb + "MB");
            }
        }

        EmailSendLog row = new EmailSendLog();
        row.setToAddress(req.getToAddress().trim());
        row.setSubject(subject);
        row.setStatus("PENDING");
        row.setRetryCount(0);
        row.setCreatedAt(LocalDateTime.now());
        logMapper.insert(row);

        try {
            JavaMailSenderImpl sender = buildMailSender(cfg, authCode);
            MimeMessage message = sender.createMimeMessage();
            boolean multipart = attachment != null && attachment.length > 0;
            MimeMessageHelper helper = new MimeMessageHelper(message, multipart, StandardCharsets.UTF_8.name());
            helper.setFrom(cfg.getFromAddress());
            helper.setTo(row.getToAddress());
            helper.setSubject(subject);
            helper.setText(body, false);
            if (multipart) {
                String filename = req.getAttachmentFilename() == null || req.getAttachmentFilename().isBlank()
                        ? "attachment.pdf" : req.getAttachmentFilename().trim();
                helper.addAttachment(filename, new ByteArrayResource(attachment), "application/pdf");
            }
            sender.send(message);

            row.setStatus("SENT");
            row.setSmtpResponse("250 OK");
            row.setSentAt(LocalDateTime.now());
            logMapper.updateById(row);

            Map<String, Object> data = new HashMap<>();
            data.put("logId", row.getId());
            data.put("status", row.getStatus());
            return Result.ok(data);
        } catch (Exception ex) {
            log.warn("[EmailService] send failed: {}", ex.getMessage());
            row.setStatus("FAILED");
            row.setSmtpResponse(truncate(ex.getMessage(), 480));
            logMapper.updateById(row);
            return Result.fail(50203, "SMTP 发送失败：" + ex.getMessage());
        }
    }

    private EmailConfig loadOrDefault() {
        EmailConfig cfg = configMapper.selectSingleton();
        if (cfg != null) {
            return cfg;
        }
        EmailConfig d = new EmailConfig();
        d.setId(SINGLETON_ID);
        d.setSmtpHost("smtp.163.com");
        d.setSmtpPort(465);
        d.setUseSsl(true);
        d.setFromAddress("noreply@yourcompany.local");
        d.setAuthCodeKek("PLACEHOLDER_KMS_INJECT");
        d.setRetryPolicy("1h,6h,24h");
        d.setDailyQuota(5000);
        d.setWarnThreshold(new BigDecimal("0.80"));
        d.setLogRetentionDays(90);
        d.setAttachmentMaxSizeMb(10);
        return d;
    }

    private static EmailConfigDto toDto(EmailConfig cfg, boolean includeSecret) {
        EmailConfigDto dto = new EmailConfigDto();
        dto.setSmtpHost(cfg.getSmtpHost());
        dto.setSmtpPort(cfg.getSmtpPort());
        dto.setUseSsl(cfg.getUseSsl());
        dto.setFromAddress(cfg.getFromAddress());
        if (includeSecret) {
            dto.setAuthCode(cfg.getAuthCodeKek());
        }
        if (cfg.getRetryPolicy() != null) {
            dto.setRetryPolicy(Arrays.stream(cfg.getRetryPolicy().split(","))
                    .map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList()));
        }
        dto.setDailyQuota(cfg.getDailyQuota());
        dto.setQuotaWarnThreshold(cfg.getWarnThreshold());
        dto.setLogRetentionDays(cfg.getLogRetentionDays());
        dto.setAttachmentMaxSizeMb(cfg.getAttachmentMaxSizeMb());
        return dto;
    }

    private static String resolveAuthCode(EmailConfig cfg, String override) {
        if (override != null && !override.isBlank()) {
            return override.trim();
        }
        return cfg.getAuthCodeKek();
    }

    private static JavaMailSenderImpl buildMailSender(EmailConfig cfg, String authCode) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(cfg.getSmtpHost());
        sender.setPort(cfg.getSmtpPort() == null ? 465 : cfg.getSmtpPort());
        sender.setUsername(cfg.getFromAddress());
        sender.setPassword(authCode);
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        boolean ssl = cfg.getUseSsl() == null || cfg.getUseSsl();
        if (ssl) {
            props.put("mail.smtp.ssl.enable", "true");
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", String.valueOf(sender.getPort()));
        } else {
            props.put("mail.smtp.starttls.enable", "true");
        }
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "10000");
        return sender;
    }

    private static String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
