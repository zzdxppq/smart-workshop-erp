package com.btsheng.erp.production.outsource.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.outsource.dto.OutsourceArriveRequest;
import com.btsheng.erp.production.outsource.entity.CrmOutsourceOrder;
import com.btsheng.erp.production.outsource.service.OutsourceReceiveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * V1.3.5 · E12-S2 · 仓管委外到货扫码
 */
@RestController
@RequestMapping("/outsource")
@Tag(name = "E12-Receive", description = "仓管扫 WW- 委外到货")
public class OutsourceReceiveController {

    private final OutsourceReceiveService receiveService;

    @Autowired
    public OutsourceReceiveController(OutsourceReceiveService receiveService) {
        this.receiveService = receiveService;
    }

    @PostMapping("/orders/{id}/receive")
    @Operation(summary = "【仓管】委外到货扫码（按 ID）")
    public Result<CrmOutsourceOrder> receiveById(
            @PathVariable Long id,
            @RequestBody OutsourceArriveRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return receiveService.receive(id, req, userId);
    }

    @PostMapping("/by-no/{outsourceNo}/receive")
    @Operation(summary = "【仓管】委外到货扫码（按 WW- 单号）")
    public Result<CrmOutsourceOrder> receiveByNo(
            @PathVariable String outsourceNo,
            @RequestBody OutsourceArriveRequest req,
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return receiveService.receiveByNo(outsourceNo, req, userId);
    }
}
