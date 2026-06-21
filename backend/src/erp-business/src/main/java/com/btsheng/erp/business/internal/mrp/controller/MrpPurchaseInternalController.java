package com.btsheng.erp.business.internal.mrp.controller;

import com.btsheng.erp.business.crm.purchaserequest.service.PurchaseRequestService;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 内部 API · MRP 缺料一键转采购申请（PR） */
@RestController
@RequestMapping("/internal/mrp-purchase")
public class MrpPurchaseInternalController {

    private final PurchaseRequestService purchaseRequestService;

    @Autowired
    public MrpPurchaseInternalController(PurchaseRequestService purchaseRequestService) {
        this.purchaseRequestService = purchaseRequestService;
    }

    @PostMapping("/create-from-shortages")
    public Result<Map<String, Object>> createFromShortages(
            @RequestBody Map<String, Object> body,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return purchaseRequestService.createFromShortages(body, userId);
    }
}
