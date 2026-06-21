package com.btsheng.erp.platform.admin.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.admin.dto.FieldEncryptionConfigDto;
import com.btsheng.erp.platform.sysparam.entity.SysParam;
import com.btsheng.erp.platform.sysparam.mapper.SysParamMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * PRD 字段加密管理 · AES-256-GCM + DEK 路径 + 白名单
 */
@Service
public class FieldEncryptionService {

    private static final String PARAM_KEY = "field.encryption.config";
    private static final String PARAM_GROUP = "field_encryption";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final SysParamMapper paramMapper;

    public FieldEncryptionService(SysParamMapper paramMapper) {
        this.paramMapper = paramMapper;
    }

    public Result<FieldEncryptionConfigDto> getConfig() {
        return Result.ok(load());
    }

    public Result<FieldEncryptionConfigDto> updateConfig(FieldEncryptionConfigDto req) {
        FieldEncryptionConfigDto current = load();
        if (req.getDekPath() != null) current.setDekPath(req.getDekPath());
        if (req.getWhitelistFields() != null) current.setWhitelistFields(req.getWhitelistFields());
        if (req.getNote() != null) current.setNote(req.getNote());
        save(current);
        return Result.ok(current);
    }

    private FieldEncryptionConfigDto load() {
        FieldEncryptionConfigDto dto = defaults();
        SysParam p = paramMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysParam>().eq("param_key", PARAM_KEY));
        if (p == null || p.getParamValue() == null || p.getParamValue().isBlank()) {
            return dto;
        }
        try {
            FieldEncryptionConfigDto stored = MAPPER.readValue(p.getParamValue(), FieldEncryptionConfigDto.class);
            if (stored.getDekPath() != null) dto.setDekPath(stored.getDekPath());
            if (stored.getWhitelistFields() != null) dto.setWhitelistFields(stored.getWhitelistFields());
            if (stored.getNote() != null) dto.setNote(stored.getNote());
        } catch (Exception ignored) {
            /* 默认 */
        }
        return dto;
    }

    private void save(FieldEncryptionConfigDto dto) {
        try {
            String json = MAPPER.writeValueAsString(dto);
            SysParam p = paramMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SysParam>().eq("param_key", PARAM_KEY));
            if (p == null) {
                p = new SysParam();
                p.setParamKey(PARAM_KEY);
                p.setParamValue(json);
                p.setParamGroup(PARAM_GROUP);
                p.setDescription("字段加密白名单配置");
                p.setCreatedAt(LocalDateTime.now());
                p.setUpdatedAt(LocalDateTime.now());
                paramMapper.insert(p);
            } else {
                p.setParamValue(json);
                p.setUpdatedAt(LocalDateTime.now());
                paramMapper.updateById(p);
            }
        } catch (Exception e) {
            throw new IllegalStateException("SAVE_ENCRYPTION_CONFIG_FAILED", e);
        }
    }

    private static FieldEncryptionConfigDto defaults() {
        FieldEncryptionConfigDto dto = new FieldEncryptionConfigDto();
        dto.setAlgorithm("AES-256-GCM");
        dto.setDekPath("/etc/erp/dek.key");
        dto.setWhitelistFields(Arrays.asList("mobile", "idCard", "bankCard"));
        dto.setNote("签字扫描件、对账附件使用 AES-256-GCM 加密存储；下载需 GM/财务权限并写入 sys_download_log。");
        return dto;
    }
}
