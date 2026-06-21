package com.btsheng.erp.business.crm.purchaseinspection.controller;

import com.btsheng.erp.business.crm.purchaseinspection.dto.AddItemRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.CreateInspectionRequest;
import com.btsheng.erp.business.crm.purchaseinspection.dto.SubmitResultRequest;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingInspection;
import com.btsheng.erp.business.crm.purchaseinspection.entity.CrmPurchaseIncomingItem;
import com.btsheng.erp.business.crm.purchaseinspection.service.PurchaseIncomingInspectionService;
import com.btsheng.erp.core.model.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * V1.3.7 · Story 1.35 · 采购·来料质检 Controller (FR-8-4)
 *
 * <p>4 端点：
 * <ul>
 *   <li>POST /purchase-incoming-inspection          创建质检单</li>
 *   <li>POST /purchase-incoming-inspection/{id}/item 添加检验项</li>
 *   <li>POST /purchase-incoming-inspection/{id}/pass PASS 结论</li>
 *   <li>POST /purchase-incoming-inspection/{id}/reject REJECT 结论</li>
 *   <li>POST /purchase-incoming-inspection/list     列表</li>
 * </ul>
 */
@RestController
@RequestMapping("/purchase-incoming-inspection")
@Tag(name = "E8-Purchase-Incoming-Inspection", description = "采购·来料质检（Story 1.35 FR-8-4）")
public class PurchaseIncomingInspectionController {

    private final PurchaseIncomingInspectionService service;

    @Autowired
    public PurchaseIncomingInspectionController(PurchaseIncomingInspectionService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "创建来料质检单（AC-8.4.1）")
    public Result<CrmPurchaseIncomingInspection> create(
            @RequestBody CreateInspectionRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "601") Long userId) {
        return service.create(req, userId);
    }

    @PostMapping("/{id}/item")
    @Operation(summary = "添加检验项（含抽样 AQL · 关键项 1 票否决）")
    public Result<CrmPurchaseIncomingItem> addItem(@PathVariable Long id, @RequestBody AddItemRequest req) {
        return service.addItem(id, req);
    }

    @PostMapping("/{id}/pass")
    @Operation(summary = "PASS · P1 修补 3：不良率 > 10% 阻断")
    public Result<CrmPurchaseIncomingInspection> pass(@PathVariable Long id, @RequestBody(required = false) SubmitResultRequest req) {
        return service.pass(id, req);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "REJECT · 拒收")
    public Result<CrmPurchaseIncomingInspection> reject(@PathVariable Long id, @RequestBody(required = false) SubmitResultRequest req) {
        return service.reject(id, req);
    }

    @PostMapping("/list")
    @Operation(summary = "质检单 + 检验项 列表")
    public Result<List<Map<String, Object>>> list() {
        return service.list();
    }
}
