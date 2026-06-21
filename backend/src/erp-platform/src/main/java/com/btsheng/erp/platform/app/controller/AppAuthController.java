package com.btsheng.erp.platform.app.controller;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.app.dto.AppLoginRequest;
import com.btsheng.erp.platform.app.dto.AppLoginResponse;
import com.btsheng.erp.platform.app.dto.AppSyncRequest;
import com.btsheng.erp.platform.app.dto.AppSyncResponse;
import com.btsheng.erp.platform.app.dto.AppMessageReadRequest;
import com.btsheng.erp.platform.app.dto.AppScanRouteRequest;
import com.btsheng.erp.platform.app.dto.AppScanRouteResponse;
import com.btsheng.erp.platform.app.service.AppService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** V1.3.7 Story 1.4 · AC-1.4.1 + AC-1.4.2 + AC-1.4.3 · APP 端 5 端点 */
@Tag(name = "E1-App", description = "APP 端（登录/消息/扫码）")
@RestController
@RequestMapping("/app")
public class AppAuthController {

    private final AppService appService;

    @Autowired
    public AppAuthController(AppService appService) { this.appService = appService; }

    @Operation(summary = "APP 登录（deviceId + platform + appVersion + loginType）")
    @PostMapping("/login")
    public Result<AppLoginResponse> login(@RequestBody AppLoginRequest req) {
        return appService.login(req);
    }

    @Operation(summary = "增量同步（lastSyncTime）")
    @PostMapping("/sync")
    public Result<AppSyncResponse> sync(@RequestBody AppSyncRequest req) {
        return appService.sync(req);
    }

    @Operation(summary = "消息列表（unreadOnly + 分页）")
    @GetMapping("/messages")
    public Result<List<AppSyncResponse.AppMessage>> listMessages(
            @RequestParam(value = "unreadOnly", required = false) Boolean unreadOnly,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return appService.listMessages(unreadOnly, page, size);
    }

    @Operation(summary = "标记消息已读 + 跳业务页")
    @PostMapping("/messages/{id}/read")
    public Result<Void> markRead(@PathVariable("id") Long id, @RequestBody AppMessageReadRequest req) {
        return appService.markRead(id, req.getUserId());
    }

    @Operation(summary = "扫码路由（5 类码 prefix 解析）")
    @GetMapping("/scan/route")
    public Result<AppScanRouteResponse> scanRoute(@RequestParam("code") String code) {
        return appService.scanRoute(code);
    }
}
