package com.btsheng.erp.business.crm.noorderpurchase.controller;

import com.btsheng.erp.business.crm.noorderpurchase.dto.NoOrderPurchaseRequest;
import com.btsheng.erp.business.crm.noorderpurchase.dto.NoOrderPurchaseResponse;
import com.btsheng.erp.business.crm.noorderpurchase.dto.PurchaseReasonDto;
import com.btsheng.erp.business.crm.noorderpurchase.service.NoOrderPurchaseService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * V1.3.8 · Story 4.1 · 无订单采购 Controller
 *
 * <p>2 端点（与 Story 4.1 端点契约一致）：
 * <ul>
 *   <li>POST /purchase/no-order  AC-4.1.1 创建</li>
 *   <li>GET  /purchase/reasons  AC-4.1.2 字典查询</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-13
 */
@RestController
@RequestMapping("/purchase")
@Tag(name = "V1.3.8-Story4.1-无订单采购")
public class NoOrderPurchaseController {

    private final NoOrderPurchaseService service;

    @Autowired
    public NoOrderPurchaseController(NoOrderPurchaseService service) {
        this.service = service;
    }

    @PostMapping("/no-order")
    @PreAuthorize("hasAnyRole('PURCHASER', 'PURCHASER_LEAD', 'ADMIN', 'SYS_ADMIN', 'BUYER')")
    @Operation(summary = "创建无订单采购 PO（Story 4.1 AC-4.1.1）")
    public Result<NoOrderPurchaseResponse> createNoOrder(@Valid @RequestBody NoOrderPurchaseRequest req) {
        Long createdBy = 1L; // V1.3.8 IMPL 阶段硬编码，TBD 从 JWT 上下文提取
            return service.createNoOrderPurchase(req, createdBy);
    }

    @GetMapping("/reasons")
    @Operation(summary = "采购理由字典查询（Story 4.1 AC-4.1.2）")
    public Result<List<PurchaseReasonDto>> listReasons() {
        return service.listPurchaseReasons();
    }
}