package com.btsheng.erp.business.crm.materialdetail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * V1.3.8 · Story 2.1 · 历史变更条目 DTO（基于 sys_change_log）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@Data
@Schema(description = "料号历史变更条目（Story 2.1 · 基于 sys_change_log + AuditLog）")
public class ChangeLogEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private String entityType;
    private Long entityId;
    private String action;
    private String fieldName;
    private String oldValue;
    private String newValue;
    private Long changedBy;
    private LocalDateTime changedAt;
}