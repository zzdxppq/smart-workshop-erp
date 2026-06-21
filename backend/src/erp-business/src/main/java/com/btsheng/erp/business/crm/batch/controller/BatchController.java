package com.btsheng.erp.business.crm.batch.controller;

import com.btsheng.erp.business.crm.batch.dto.BatchCreateRequest;
import com.btsheng.erp.business.crm.batch.dto.BatchCreateResponse;
import com.btsheng.erp.business.crm.batch.dto.PoStatusResponse;
import com.btsheng.erp.business.crm.batch.service.BatchService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

/**
 * V1.3.8 · Story 3.1 · 批次 Controller（按物料粒度）
 *
 * <p>2 端点（与 Story 3.1 端点契约一致）：
 * <ul>
 *   <li>POST /incoming/batch-create  AC-3.1.1</li>
 *   <li>GET  /incoming/po-status/{poId}  AC-3.1.2</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@RestController
@RequestMapping("/incoming")
@Tag(name = "V1.3.8-Story3.1-批次管理")
public class BatchController {

    private final BatchService batchService;

    @Autowired
    public BatchController(BatchService batchService) {
        this.batchService = batchService;
    }

    @PostMapping("/batch-create")
    @PreAuthorize("hasAnyRole('WAREHOUSE', 'WAREHOUSE_LEAD', 'ADMIN', 'SYS_ADMIN', 'BUYER')")
    @Operation(summary = "按物料粒度创建批次（Story 3.1 AC-3.1.1）")
    public Result<BatchCreateResponse> batchCreate(@Valid @RequestBody BatchCreateRequest req) {
        Long createdBy = 1L; // V1.3.8 IMPL 阶段硬编码，TBD 从 JWT 上下文提取
            return batchService.createBatch(req, createdBy);
    }

    @GetMapping("/po-status/{poId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "按物料粒度查询 PO 状态（Story 3.1 AC-3.1.2）")
    public Result<PoStatusResponse> poStatus(@PathVariable Long poId) {
        return batchService.getPoStatus(poId);
    }
}