package com.btsheng.erp.platform.app.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.app.dto.*;
import com.btsheng.erp.platform.auth.dto.LoginRequest;
import com.btsheng.erp.platform.auth.dto.LoginResponse;
import com.btsheng.erp.platform.auth.security.JwtSigner;
import com.btsheng.erp.platform.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * APP 端 Service（V1.3.7 · Story 1.4 · 5 端点实现）
 *
 * <p>核心：5 类码 prefix 解析（GD-/LZ-/SB-/WL-/WW- · V1.3.5+）
 */
@Service
public class AppService {

    private static final Pattern GD = Pattern.compile("^GD-(\\d{8})-(\\d{4})$");
    private static final Pattern LZ = Pattern.compile("^LZ-([A-Z0-9-]+)$");
    private static final Pattern SB = Pattern.compile("^SB-([A-Z0-9-]+)$");
    private static final Pattern WL = Pattern.compile("^WL-([A-Z0-9-]+)$");
    private static final Pattern WW = Pattern.compile("^WW-(\\d{8})-(\\d{4})$");

    private final AuthService authService;
    private final JwtSigner jwtSigner;

    @Autowired
    public AppService(AuthService authService, JwtSigner jwtSigner) {
        this.authService = authService;
        this.jwtSigner = jwtSigner;
    }

    public Result<AppLoginResponse> login(AppLoginRequest req) {
        LoginRequest loginReq = new LoginRequest();
        loginReq.setUsername(req.getUsername());
        loginReq.setPassword(req.getPassword());
        LoginResponse loginResp = authService.login(loginReq);

        AppLoginResponse resp = new AppLoginResponse();
        resp.setAccessToken(loginResp.getAccessToken());
        resp.setRefreshToken(loginResp.getRefreshToken());
        resp.setUserId(loginResp.getUser() != null ? loginResp.getUser().getId() : null);
        resp.setRoles(loginResp.getRoles() != null ? loginResp.getRoles() : List.of());
        resp.setPermissions(loginResp.getPermissions() != null ? loginResp.getPermissions() : List.of());

        try {
            Claims claims = jwtSigner.parse(loginResp.getAccessToken());
            resp.setJti(claims.getId());
        } catch (Exception e) {
            resp.setJti("app-" + System.currentTimeMillis());
        }
        return Result.ok(resp);
    }

    public Result<AppSyncResponse> sync(AppSyncRequest req) {
        AppSyncResponse resp = new AppSyncResponse();
        resp.setSyncTime(LocalDateTime.now());
        resp.setMessages(new ArrayList<>());
        resp.setPendingScanResults(new ArrayList<>());
        if (req.getPendingScans() != null) {
            for (AppSyncRequest.PendingScan ps : req.getPendingScans()) {
                parseCode(ps.getCode());
                AppSyncResponse.PendingScanResult r = new AppSyncResponse.PendingScanResult();
                r.setCode(ps.getCode());
                r.setConflictType("NONE");
                r.setResolution("AUTO_OVERWRITE");
                resp.getPendingScanResults().add(r);
            }
        }
        return Result.ok(resp);
    }

    public Result<List<AppSyncResponse.AppMessage>> listMessages(Boolean unreadOnly, int page, int size) {
        List<AppSyncResponse.AppMessage> list = new ArrayList<>();
        if (unreadOnly == null || unreadOnly) {
            list.add(msg(1L, "APPROVAL_NOTIFY", "审批通知", "您的报价 BJ001 待审批", 100L, "approval/detail/100"));
            list.add(msg(2L, "OVERDUE_NOTIFY", "逾期提醒", "审批单 AP002 已逾期 24h", 200L, "approval/pending"));
        }
        int from = Math.max(0, (page - 1) * size);
        if (from >= list.size()) {
            return Result.ok(List.of());
        }
        int to = Math.min(from + size, list.size());
        return Result.ok(list.subList(from, to));
    }

    public Result<Void> markRead(Long id, Long userId) {
        return Result.ok();
    }

    public Result<AppScanRouteResponse> scanRoute(String code) {
        return Result.ok(parseCode(code));
    }

    private AppScanRouteResponse parseCode(String code) {
        AppScanRouteResponse r = new AppScanRouteResponse();
        if (code == null || code.isEmpty()) {
            r.setType("UNKNOWN");
            r.setRouteUrl("manual-input");
            return r;
        }
        Matcher m = GD.matcher(code);
        if (m.matches()) {
            r.setType("WORK_ORDER");
            r.setCode(code);
            r.setId(Long.parseLong(m.group(2)));
            r.setRouteUrl("workorder/detail/" + m.group(2));
            return r;
        }
        m = LZ.matcher(code);
        if (m.matches()) {
            r.setType("FLOW");
            r.setCode(code);
            r.setRouteUrl("flow/detail/" + m.group(1));
            return r;
        }
        m = SB.matcher(code);
        if (m.matches()) {
            r.setType("DEVICE");
            r.setCode(code);
            r.setRouteUrl("device/detail/" + m.group(1));
            return r;
        }
        m = WL.matcher(code);
        if (m.matches()) {
            r.setType("MATERIAL");
            r.setCode(code);
            r.setRouteUrl("material/detail/" + m.group(1));
            return r;
        }
        m = WW.matcher(code);
        if (m.matches()) {
            r.setType("OUTSOURCE_ORDER");
            r.setCode(code);
            r.setId(Long.parseLong(m.group(2)));
            r.setRouteUrl("outsource/detail/" + m.group(2));
            return r;
        }
        r.setType("UNKNOWN");
        r.setRouteUrl("manual-input");
        return r;
    }

    private AppSyncResponse.AppMessage msg(Long id, String type, String title, String content, Long bizId, String routeUrl) {
        AppSyncResponse.AppMessage m = new AppSyncResponse.AppMessage();
        m.setId(id);
        m.setType(type);
        m.setTitle(title);
        m.setContent(content);
        m.setBizId(bizId);
        m.setRouteUrl(routeUrl);
        m.setRead(false);
        m.setCreatedAt(LocalDateTime.now());
        return m;
    }
}
