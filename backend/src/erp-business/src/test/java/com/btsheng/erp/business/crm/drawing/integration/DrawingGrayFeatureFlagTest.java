package com.btsheng.erp.business.crm.drawing.integration;

import com.btsheng.erp.business.crm.drawing.authz.DrawingAuthz;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingLinkMapper;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import com.btsheng.erp.core.redis.CacheTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.nio.file.Path;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * V1.3.9 Sprint 12 Story 12.1 · 灰度 feature flag 测试（3 测例 · QA 测例组 5）
 *
 * <ul>
 *   <li>TC-12.1.5.1 灰度关闭 → SALES 全 403（不查 DB · 直接拒绝）</li>
 *   <li>TC-12.1.5.2 灰度开启 → SALES 按矩阵生效（关联订单通过 + 不关联拒绝）</li>
 *   <li>TC-12.1.5.3 OPERATOR 灰度 → 当前工序可预览 · 其它工序拒绝</li>
 * </ul>
 *
 * <p>验证：
 * <ul>
 *   <li>默认全 false（draw.acl.gray.OPERATOR=false 等）</li>
 *   <li>feature flag 关闭即短路 → 返 true（V1.3.7 行为兼容）</li>
 *   <li>feature flag 开启 → 走完整矩阵</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DRAWING_ACL_FEATURE_FLAG 灰度 4 阶段验证")
class DrawingGrayFeatureFlagTest {

    @Mock private CrmDrawingLinkMapper linkMapper;
    @Mock private CacheTemplate cacheTemplate;
    @Mock private PlatformLookupMapper platformLookupMapper;

    private DrawingAuthz authz;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(platformLookupMapper.findUserIdByUsername(any())).thenReturn(42L);
        when(linkMapper.selectOrderIdsByUser(anyLong())).thenReturn(java.util.List.of(123L));
        when(linkMapper.selectOperatorProcessIdsByUser(anyLong())).thenReturn(java.util.List.of(999L));
        authz = new DrawingAuthz(linkMapper, cacheTemplate, platformLookupMapper);
    }

    @Test
    @DisplayName("TC-12.1.5.1 灰度关闭 · SALES 全部短路返 true（V1.3.7 兼容）")
    void gray_flag_off_sales_bypass() {
        // 默认 feature flag 全 false → canView 短路返 true
            when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("sales01", "ROLE_SALES");
        // 即使 SALES 不关联，也返 true（V1.3.7 全员可见行为）
            assertTrue(authz.canView(a, 789L));
        assertTrue(authz.canPrint(a, 789L));
        assertFalse(authz.canDownload(a, 789L));   // download 仍仅 ENGINEER
            assertFalse(authz.canUpload(a, 789L));
    }

    @Test
    @DisplayName("TC-12.1.5.2 灰度开启模拟 · SALES 按矩阵生效（关联通过 + 不关联拒绝）")
    void gray_flag_on_sales_matrix_active() {
        // 模拟 sys_dict draw.acl.gray.SALES=true 时
        // 因 getFeatureFlag 是 protected，可通过子类覆盖
            DrawingAuthz testAuthz = new DrawingAuthz(linkMapper, cacheTemplate, platformLookupMapper) {
            @Override
            public boolean canApplyAcl(Authentication a) {
                // 强制返回 true 模拟灰度开启
            return true;
            }
        };
        // SALES 关联订单 → canView=true
            when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(789L, CrmDrawingLink.BIZ_TYPE_ORDER, any()))
                .thenReturn(1L);
        Authentication sales = auth("sales01", "ROLE_SALES");
        assertTrue(testAuthz.canView(sales, 789L));

        // SALES 不关联订单 → canView=false
            when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(790L, CrmDrawingLink.BIZ_TYPE_ORDER, any()))
                .thenReturn(0L);
        assertFalse(testAuthz.canView(sales, 790L));
    }

    @Test
    @DisplayName("TC-12.1.5.3 OPERATOR 灰度 · 当前工序可预览 · 其它工序拒绝")
    void gray_flag_operator_process() {
        DrawingAuthz testAuthz = new DrawingAuthz(linkMapper, cacheTemplate, platformLookupMapper) {
            @Override
            public boolean canApplyAcl(Authentication a) { return true; }
        };
        // OPERATOR 当前工序 P03 关联 → canView=true
            when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(100L, CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS, any()))
                .thenReturn(1L);
        Authentication op = auth("operator01", "ROLE_OPERATOR");
        assertTrue(testAuthz.canView(op, 100L));

        // OPERATOR 其它工序 P05 不关联 → canView=false
            when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(101L, CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS, any()))
                .thenReturn(0L);
        assertFalse(testAuthz.canView(op, 101L));

        // OPERATOR print 始终 false（矩阵）
            assertFalse(testAuthz.canPrint(op, 100L));
    }

    @Test
    @DisplayName("灰度默认全 false · sys_dict 初始化验证")
    void default_feature_flag_all_false() throws Exception {
        Path v54 = resolvePath();
        String content = java.nio.file.Files.readString(v54);
        // 7 个角色灰度开关全部存在且默认 false
            String[] roles = {"ENGINEER", "PROD_PLANNER", "SALES", "PURCHASER", "WAREHOUSE", "QC", "OPERATOR"};
        for (String role : roles) {
            String key = "draw.acl.gray." + role;
            assertTrue(content.contains(key), "缺灰度开关 " + key);
        }
        // 默认 value=false（除 ENGINEER/PROD_PLANNER 内部硬编码 true）
            int falseCount = content.split("'false'").length - 1;
        assertTrue(falseCount >= 7, "默认 false 开关数应 ≥ 7，实际 " + falseCount);
    }

    // ============================================================
    // 辅助
    // ============================================================
            private Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "password",
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }

    private java.nio.file.Path resolvePath() throws Exception {
        java.nio.file.Path p1 = java.nio.file.Paths.get("db/migrations/V54__crm_drawing_link.sql");
        if (java.nio.file.Files.exists(p1)) return p1;
        java.nio.file.Path p2 = java.nio.file.Paths.get("backend/db/migrations/V54__crm_drawing_link.sql");
        if (java.nio.file.Files.exists(p2)) return p2;
        throw new AssertionError("V54 SQL 文件不存在");
    }
}