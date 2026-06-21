package com.btsheng.erp.platform.app.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.app.dto.AppLoginRequest;
import com.btsheng.erp.platform.app.dto.AppLoginResponse;
import com.btsheng.erp.platform.app.dto.AppScanRouteResponse;
import com.btsheng.erp.platform.app.dto.AppSyncRequest;
import com.btsheng.erp.platform.app.dto.AppSyncResponse;
import com.btsheng.erp.platform.auth.dto.LoginResponse;
import com.btsheng.erp.platform.auth.dto.UserDto;
import com.btsheng.erp.platform.auth.security.JwtSigner;
import com.btsheng.erp.platform.auth.service.AuthService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.4 · AppService 测例 */
@ExtendWith(MockitoExtension.class)
class AppServiceTest {

    @Mock private AuthService authService;
    @Mock private JwtSigner jwtSigner;

    private AppService appService;

    @BeforeEach
    void setUp() {
        appService = new AppService(authService, jwtSigner);
    }

    @Test void login_success() {
        LoginResponse loginResp = new LoginResponse();
        loginResp.setAccessToken("access-token");
        loginResp.setRefreshToken("refresh-token");
        UserDto user = new UserDto();
        user.setId(10086L);
        loginResp.setUser(user);
        when(authService.login(any())).thenReturn(loginResp);
        Claims claims = io.jsonwebtoken.Jwts.claims().id("jti-1").build();
        when(jwtSigner.parse("access-token")).thenReturn(claims);

        AppLoginRequest req = new AppLoginRequest();
        req.setUsername("zhangsan");
        req.setPassword("Pass1234");
        Result<AppLoginResponse> r = appService.login(req);
        assertEquals(0, r.getCode());
        assertEquals("access-token", r.getData().getAccessToken());
        assertEquals(10086L, r.getData().getUserId());
    }

    @Test void list_messages_unread_only() {
        Result<List<AppSyncResponse.AppMessage>> r = appService.listMessages(true, 1, 20);
        assertEquals(0, r.getCode());
        assertTrue(r.getData().size() >= 2);
    }

    @Test void scan_route_workorder_GD() {
        Result<AppScanRouteResponse> r = appService.scanRoute("GD-20260610-0001");
        assertEquals(0, r.getCode());
        assertEquals("WORK_ORDER", r.getData().getType());
        assertEquals("workorder/detail/0001", r.getData().getRouteUrl());
    }

    @Test void scan_route_material_WL() {
        Result<AppScanRouteResponse> r = appService.scanRoute("WL-STEEL-001");
        assertEquals(0, r.getCode());
        assertEquals("MATERIAL", r.getData().getType());
    }

    @Test void scan_route_flow_LZ() {
        Result<AppScanRouteResponse> r = appService.scanRoute("LZ-GD001-P03");
        assertEquals(0, r.getCode());
        assertEquals("FLOW", r.getData().getType());
    }

    @Test void scan_route_device_SB() {
        Result<AppScanRouteResponse> r = appService.scanRoute("SB-CNC-001");
        assertEquals(0, r.getCode());
        assertEquals("DEVICE", r.getData().getType());
    }

    @Test void scan_route_outsource_WW() {
        Result<AppScanRouteResponse> r = appService.scanRoute("WW-20260610-0001");
        assertEquals(0, r.getCode());
        assertEquals("OUTSOURCE_ORDER", r.getData().getType());
    }

    @Test void sync_with_pending_scans() {
        AppSyncRequest req = new AppSyncRequest();
        req.setUserId(10086L);
        AppSyncRequest.PendingScan ps = new AppSyncRequest.PendingScan();
        ps.setCode("GD-20260610-0001");
        req.setPendingScans(List.of(ps));
        Result<AppSyncResponse> r = appService.sync(req);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().getPendingScanResults().size());
    }
}
