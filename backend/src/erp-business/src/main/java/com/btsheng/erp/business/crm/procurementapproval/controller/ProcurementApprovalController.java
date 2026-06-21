package com.btsheng.erp.business.crm.procurementapproval.controller;

import com.btsheng.erp.business.crm.procurementapproval.dto.ApprovalRouteRequest;
import com.btsheng.erp.business.crm.procurementapproval.dto.ApprovalRouteResponse;
import com.btsheng.erp.business.crm.procurementapproval.service.ProcurementApprovalRouter;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V1.3.8 · Story 4.2 · 采购审批 Controller
 *
 * <p>2 端点（与 Story 4.2 端点契约一致）：
 * <ul>
 *   <li>POST /approval/route-preview  AC-4.2.2 路由预览（无副作用）</li>
 *   <li>GET  /roles/procurement-manager-perms  AC-4.2.1 权限查询</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@RestController
@Tag(name = "V1.3.8-Story4.2-采购审批")
public class ProcurementApprovalController {

    private final ProcurementApprovalRouter router;

    @Autowired
    public ProcurementApprovalController(ProcurementApprovalRouter router) {
        this.router = router;
    }

    @PostMapping("/approval/route-preview")
    @Operation(summary = "审批路由预览（Story 4.2 AC-4.2.2 · 无副作用）")
    public Result<ApprovalRouteResponse> previewRoute(@Valid @RequestBody ApprovalRouteRequest req) {
        return router.previewRoute(req);
    }

    @GetMapping("/roles/procurement-manager-perms")
    @Operation(summary = "采购主管权限查询（Story 4.2 AC-4.2.1）")
    public Result<List<String>> getProcurementManagerPerms() {
        return router.getProcurementManagerPermissions();
    }
}