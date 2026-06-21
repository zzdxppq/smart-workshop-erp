package com.btsheng.erp.platform.admin.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.admin.dto.EmailTemplateDto;
import com.btsheng.erp.platform.sysparam.entity.SysParam;
import com.btsheng.erp.platform.sysparam.mapper.SysParamMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PRD 邮件模板管理 · 5 套模板（委外/发货/质检/返修/对账）
 */
@Service
public class EmailTemplateService {

    private static final String PARAM_GROUP = "email_template";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final Map<String, EmailTemplateDto> DEFAULTS = new LinkedHashMap<>();

    static {
        putDefault("OUTSOURCE_ORDER", "委外下单通知", "【佰泰胜】委外订单 {{outsourceNo}} 已下达",
                "尊敬的 {{vendorName}}：\n\n委外订单 {{outsourceNo}} 已下达，请查收附件。\n交期：{{deliveryDate}}\n\n佰泰胜 ERP");
        putDefault("SHIPPING_REMIND", "发货提醒", "【佰泰胜】委外单 {{outsourceNo}} 待发货提醒",
                "厂商 {{vendorName}} 您好，委外单 {{outsourceNo}} 尚未发货，请尽快安排。");
        putDefault("QC_FAIL", "质检不合格通知", "【佰泰胜】委外质检不合格 {{outsourceNo}}",
                "委外单 {{outsourceNo}} 质检不合格，原因：{{reason}}。请安排返修。");
        putDefault("REWORK", "返修单通知", "【佰泰胜】返修单 {{reworkNo}}",
                "返修单 {{reworkNo}} 已生成，关联委外单 {{outsourceNo}}，请处理。");
        putDefault("MONTHLY_RECONCILE", "月度对账单", "【佰泰胜】{{period}} 月度对账单 {{reconcileNo}}",
                "尊敬的 {{vendorName}}：\n\n附件为 {{period}} 月度对账单，请核对后回传签字扫描件。\n\n佰泰胜 ERP");
    }

    private static void putDefault(String key, String name, String subject, String body) {
        EmailTemplateDto dto = new EmailTemplateDto();
        dto.setKey(key);
        dto.setName(name);
        dto.setSubject(subject);
        dto.setBody(body);
        dto.setDescription(name);
        DEFAULTS.put(key, dto);
    }

    private final SysParamMapper paramMapper;

    public EmailTemplateService(SysParamMapper paramMapper) {
        this.paramMapper = paramMapper;
    }

    public Result<List<EmailTemplateDto>> listTemplates() {
        List<EmailTemplateDto> list = new ArrayList<>();
        for (Map.Entry<String, EmailTemplateDto> e : DEFAULTS.entrySet()) {
            list.add(loadOrDefault(e.getKey()));
        }
        return Result.ok(list);
    }

    public Result<EmailTemplateDto> getTemplate(String key) {
        if (!DEFAULTS.containsKey(key)) {
            return Result.fail(40404, "TEMPLATE_NOT_FOUND");
        }
        return Result.ok(loadOrDefault(key));
    }

    public Result<EmailTemplateDto> updateTemplate(String key, EmailTemplateDto req) {
        if (!DEFAULTS.containsKey(key)) {
            return Result.fail(40404, "TEMPLATE_NOT_FOUND");
        }
        EmailTemplateDto merged = loadOrDefault(key);
        if (req.getSubject() != null) merged.setSubject(req.getSubject());
        if (req.getBody() != null) merged.setBody(req.getBody());
        if (req.getName() != null) merged.setName(req.getName());
        save(key, merged);
        return Result.ok(merged);
    }

    private EmailTemplateDto loadOrDefault(String key) {
        EmailTemplateDto base = clone(DEFAULTS.get(key));
        String paramKey = paramKey(key);
        SysParam p = paramMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysParam>().eq("param_key", paramKey));
        if (p == null || p.getParamValue() == null || p.getParamValue().isBlank()) {
            return base;
        }
        try {
            EmailTemplateDto stored = MAPPER.readValue(p.getParamValue(), EmailTemplateDto.class);
            if (stored.getSubject() != null) base.setSubject(stored.getSubject());
            if (stored.getBody() != null) base.setBody(stored.getBody());
            if (stored.getName() != null) base.setName(stored.getName());
        } catch (Exception ignored) {
            /* 使用默认 */
        }
        return base;
    }

    private void save(String key, EmailTemplateDto dto) {
        String paramKey = paramKey(key);
        SysParam p = paramMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysParam>().eq("param_key", paramKey));
        try {
            String json = MAPPER.writeValueAsString(Map.of(
                    "subject", dto.getSubject(),
                    "body", dto.getBody(),
                    "name", dto.getName()));
            if (p == null) {
                p = new SysParam();
                p.setParamKey(paramKey);
                p.setParamValue(json);
                p.setParamGroup(PARAM_GROUP);
                p.setDescription(DEFAULTS.get(key).getDescription());
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                paramMapper.insert(p);
            } else {
                p.setParamValue(json);
                p.setUpdatedAt(LocalDateTime.now());
                paramMapper.updateById(p);
            }
        } catch (Exception e) {
            throw new IllegalStateException("SAVE_TEMPLATE_FAILED", e);
        }
    }

    private static String paramKey(String templateKey) {
        return "email.template." + templateKey;
    }

    private static EmailTemplateDto clone(EmailTemplateDto src) {
        EmailTemplateDto d = new EmailTemplateDto();
        d.setKey(src.getKey());
        d.setName(src.getName());
        d.setSubject(src.getSubject());
        d.setBody(src.getBody());
        d.setDescription(src.getDescription());
        return d;
    }
}
