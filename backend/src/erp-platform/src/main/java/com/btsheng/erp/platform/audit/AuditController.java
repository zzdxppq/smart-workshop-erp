package com.btsheng.erp.platform.audit;

import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.audit.mapper.AuditLogMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import com.btsheng.erp.core.web.PreAuthorizeRoles;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计日志查询（V1.3.7 P1 修补 · admin 限定）
 */
@Tag(name = "E1-Audit", description = "审计日志（仅 admin）")
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditLogMapper auditLogMapper;

    @Autowired
    public AuditController(AuditLogMapper auditLogMapper) {
        this.auditLogMapper = auditLogMapper;
    }

    @Operation(summary = "查询审计日志（仅 admin）")
    @GetMapping("/logs")
    @PreAuthorize(PreAuthorizeRoles.ADMIN)
    public Result<PageResponse<Map<String, Object>>> queryLogs(
            @RequestParam(required = false) String bizType,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize) {
        int offset = Math.max(pageNum - 1, 0) * pageSize;
        List<Map<String, Object>> rows = auditLogMapper.selectPage(offset, pageSize);
        if (bizType != null && !bizType.isBlank()) {
            rows = rows.stream()
                    .filter(r -> bizType.equals(String.valueOf(r.get("module"))))
                    .collect(Collectors.toList());
        }
        if (operatorId != null) {
            rows = rows.stream()
                    .filter(r -> operatorId.equals(toLong(r.get("userId"))))
                    .collect(Collectors.toList());
        }
        long total = auditLogMapper.countAll();
        return Result.ok(new PageResponse<>(rows, total, pageNum, pageSize));
    }

    private static Long toLong(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
