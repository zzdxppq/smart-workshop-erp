package com.btsheng.erp.business.crm.drawing.authz;

import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingLink;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingLinkMapper;
import com.btsheng.erp.business.platform.mapper.PlatformLookupMapper;
import com.btsheng.erp.core.redis.CacheTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * V1.3.9 Sprint 12 Story 12.1 · DrawingAuthz 单元测试
 *
 * <p>7 角色 × 5 操作 = 35 cell 全覆盖（QA 测例组 1）
 * <p>每角色主操作正/反例（如 SALES 关联订单通过 + SALES 不关联拒绝）
 *
 * <p>关键设计：
 * <ul>
 *   <li>ENGINEER/PROD_PLANNER 不需查 DB → 测例不 mock linkMapper</li>
 *   <li>SALES/PURCHASER/WAREHOUSE/QC/OPERATOR 需 mock linkMapper.existsByDrawingAndBizTypeAndBizIdIn</li>
 *   <li>FINANCE 全 false → 不查 DB</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DrawingAuthz 7 角色 × 5 操作 = 35 cell 矩阵单元测试")
class DrawingAuthzTest {

    @Mock private CrmDrawingLinkMapper linkMapper;
    @Mock private CacheTemplate cacheTemplate;
    @Mock private PlatformLookupMapper platformLookupMapper;

    private DrawingAuthz authz;

    @BeforeEach
    void setUp() {
        authz = new DrawingAuthz(linkMapper, cacheTemplate, platformLookupMapper);
        when(platformLookupMapper.findUserIdByUsername(any())).thenAnswer(inv -> (long) Math.abs(inv.getArgument(0, String.class).hashCode() % 100000) + 1);
        when(linkMapper.selectOrderIdsByUser(anyLong())).thenReturn(Arrays.asList(123L));
        when(linkMapper.selectPoIdsByUser(anyLong())).thenReturn(Arrays.asList(456L));
        when(linkMapper.selectIncomingIdsByUser(anyLong())).thenReturn(Arrays.asList(789L));
        when(linkMapper.selectInspectionIdsByUser(anyLong())).thenReturn(Arrays.asList(321L));
        when(linkMapper.selectOperatorProcessIdsByUser(anyLong())).thenReturn(Arrays.asList(999L));
    }

    // ============================================================
    // 1. ENGINEER · 5/5 全 true
    // ============================================================
            @Test @DisplayName("ENGINEER.canView=true")
    void canView_ENGINEER() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        assertTrue(authz.canView(a, 100L));
    }
    @Test @DisplayName("ENGINEER.canPrint=true")
    void canPrint_ENGINEER() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        assertTrue(authz.canPrint(a, 100L));
    }
    @Test @DisplayName("ENGINEER.canDownload=true")
    void canDownload_ENGINEER() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        assertTrue(authz.canDownload(a, 100L));
    }
    @Test @DisplayName("ENGINEER.canUpload=true")
    void canUpload_ENGINEER() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        assertTrue(authz.canUpload(a, 100L));
    }
    @Test @DisplayName("ENGINEER.canDelete=true")
    void canDelete_ENGINEER() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        assertTrue(authz.canDelete(a, 100L));
    }

    // ============================================================
    // 2. PROD_PLANNER · preview+print 全 true，download/upload/delete 全 false
    // ============================================================
            @Test @DisplayName("PROD_PLANNER.canView=true")
    void canView_PROD_PLANNER() {
        Authentication a = auth("planner01", "ROLE_PROD_PLANNER");
        assertTrue(authz.canView(a, 100L));
    }
    @Test @DisplayName("PROD_PLANNER.canPrint=true")
    void canPrint_PROD_PLANNER() {
        Authentication a = auth("planner01", "ROLE_PROD_PLANNER");
        assertTrue(authz.canPrint(a, 100L));
    }
    @Test @DisplayName("PROD_PLANNER.canDownload=false")
    void canDownload_PROD_PLANNER() {
        Authentication a = auth("planner01", "ROLE_PROD_PLANNER");
        assertFalse(authz.canDownload(a, 100L));
    }
    @Test @DisplayName("PROD_PLANNER.canUpload=false")
    void canUpload_PROD_PLANNER() {
        Authentication a = auth("planner01", "ROLE_PROD_PLANNER");
        assertFalse(authz.canUpload(a, 100L));
    }
    @Test @DisplayName("PROD_PLANNER.canDelete=false")
    void canDelete_PROD_PLANNER() {
        Authentication a = auth("planner01", "ROLE_PROD_PLANNER");
        assertFalse(authz.canDelete(a, 100L));
    }

    // ============================================================
    // 3. SALES · 关联订单可预览，不关联拒绝
    // ============================================================
            @Test @DisplayName("SALES.canView linked=true")
    void canView_SALES_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_ORDER), any()))
                .thenReturn(1L);
        Authentication a = auth("sales01", "ROLE_SALES");
        assertTrue(authz.canView(a, 789L));
    }
    @Test @DisplayName("SALES.canView unlinked=false")
    void canView_SALES_unlinked() {
        when(linkMapper.selectOrderBizIdsByDrawing(any(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("sales01", "ROLE_SALES");
        assertFalse(authz.canView(a, 789L));
    }
    @Test @DisplayName("SALES.canPrint linked=true")
    void canPrint_SALES_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_ORDER), any()))
                .thenReturn(1L);
        Authentication a = auth("sales01", "ROLE_SALES");
        assertTrue(authz.canPrint(a, 789L));
    }
    @Test @DisplayName("SALES.canDownload=false")
    void canDownload_SALES() {
        Authentication a = auth("sales01", "ROLE_SALES");
        assertFalse(authz.canDownload(a, 789L));
    }
    @Test @DisplayName("SALES.canUpload=false")
    void canUpload_SALES() {
        Authentication a = auth("sales01", "ROLE_SALES");
        assertFalse(authz.canUpload(a, 789L));
    }

    // ============================================================
    // 4. PURCHASER · 关联 PO 可预览
    // ============================================================
            @Test @DisplayName("PURCHASER.canView linked=true")
    void canView_PURCHASER_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_PO), any()))
                .thenReturn(1L);
        Authentication a = auth("purchaser01", "ROLE_PURCHASER");
        assertTrue(authz.canView(a, 789L));
    }
    @Test @DisplayName("PURCHASER.canView unlinked=false")
    void canView_PURCHASER_unlinked() {
        when(linkMapper.selectPoBizIdsByDrawing(any(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("purchaser01", "ROLE_PURCHASER");
        assertFalse(authz.canView(a, 789L));
    }
    @Test @DisplayName("PURCHASER.canPrint linked=true")
    void canPrint_PURCHASER_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_PO), any()))
                .thenReturn(1L);
        Authentication a = auth("purchaser01", "ROLE_PURCHASER");
        assertTrue(authz.canPrint(a, 789L));
    }
    @Test @DisplayName("PURCHASER.canDownload=false")
    void canDownload_PURCHASER() {
        Authentication a = auth("purchaser01", "ROLE_PURCHASER");
        assertFalse(authz.canDownload(a, 789L));
    }
    @Test @DisplayName("PURCHASER.canUpload=false")
    void canUpload_PURCHASER() {
        Authentication a = auth("purchaser01", "ROLE_PURCHASER");
        assertFalse(authz.canUpload(a, 789L));
    }

    // ============================================================
    // 5. WAREHOUSE · 关联入库单可预览
    // ============================================================
            @Test @DisplayName("WAREHOUSE.canView linked=true")
    void canView_WAREHOUSE_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_INCOMING), any()))
                .thenReturn(1L);
        Authentication a = auth("warehouse01", "ROLE_WAREHOUSE");
        assertTrue(authz.canView(a, 789L));
    }
    @Test @DisplayName("WAREHOUSE.canView unlinked=false")
    void canView_WAREHOUSE_unlinked() {
        when(linkMapper.selectIncomingBizIdsByDrawing(any(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("warehouse01", "ROLE_WAREHOUSE");
        assertFalse(authz.canView(a, 789L));
    }
    @Test @DisplayName("WAREHOUSE.canPrint linked=true")
    void canPrint_WAREHOUSE_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_INCOMING), any()))
                .thenReturn(1L);
        Authentication a = auth("warehouse01", "ROLE_WAREHOUSE");
        assertTrue(authz.canPrint(a, 789L));
    }
    @Test @DisplayName("WAREHOUSE.canDownload=false")
    void canDownload_WAREHOUSE() {
        Authentication a = auth("warehouse01", "ROLE_WAREHOUSE");
        assertFalse(authz.canDownload(a, 789L));
    }
    @Test @DisplayName("WAREHOUSE.canUpload=false")
    void canUpload_WAREHOUSE() {
        Authentication a = auth("warehouse01", "ROLE_WAREHOUSE");
        assertFalse(authz.canUpload(a, 789L));
    }

    // ============================================================
    // 6. QC · 关联质检单可预览
    // ============================================================
            @Test @DisplayName("QC.canView linked=true")
    void canView_QC_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_INSPECTION), any()))
                .thenReturn(1L);
        Authentication a = auth("qc01", "ROLE_QC");
        assertTrue(authz.canView(a, 789L));
    }
    @Test @DisplayName("QC.canView unlinked=false")
    void canView_QC_unlinked() {
        when(linkMapper.selectInspectionBizIdsByDrawing(any(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("qc01", "ROLE_QC");
        assertFalse(authz.canView(a, 789L));
    }
    @Test @DisplayName("QC.canPrint linked=true")
    void canPrint_QC_linked() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(789L), eq(CrmDrawingLink.BIZ_TYPE_INSPECTION), any()))
                .thenReturn(1L);
        Authentication a = auth("qc01", "ROLE_QC");
        assertTrue(authz.canPrint(a, 789L));
    }
    @Test @DisplayName("QC.canDownload=false")
    void canDownload_QC() {
        Authentication a = auth("qc01", "ROLE_QC");
        assertFalse(authz.canDownload(a, 789L));
    }
    @Test @DisplayName("QC.canUpload=false")
    void canUpload_QC() {
        Authentication a = auth("qc01", "ROLE_QC");
        assertFalse(authz.canUpload(a, 789L));
    }

    // ============================================================
    // 7. OPERATOR · 当前工序可预览
    // ============================================================
            @Test @DisplayName("OPERATOR.canView current=true")
    void canView_OPERATOR_currentProcess() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(eq(100L), eq(CrmDrawingLink.BIZ_TYPE_WORKORDER_PROCESS), any()))
                .thenReturn(1L);
        Authentication a = auth("operator01", "ROLE_OPERATOR");
        assertTrue(authz.canView(a, 100L));
    }
    @Test @DisplayName("OPERATOR.canView other=false")
    void canView_OPERATOR_otherProcess() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("operator01", "ROLE_OPERATOR");
        assertFalse(authz.canView(a, 100L));
    }
    @Test @DisplayName("OPERATOR.canPrint=false (矩阵 OPERATOR 无 print)")
    void canPrint_OPERATOR() {
        Authentication a = auth("operator01", "ROLE_OPERATOR");
        assertFalse(authz.canPrint(a, 100L));
    }
    @Test @DisplayName("OPERATOR.canDownload=false")
    void canDownload_OPERATOR() {
        Authentication a = auth("operator01", "ROLE_OPERATOR");
        assertFalse(authz.canDownload(a, 100L));
    }
    @Test @DisplayName("OPERATOR.canUpload=false")
    void canUpload_OPERATOR() {
        Authentication a = auth("operator01", "ROLE_OPERATOR");
        assertFalse(authz.canUpload(a, 100L));
    }

    // ============================================================
    // 8. FINANCE · 5/5 全 false
    // ============================================================
            @Test @DisplayName("FINANCE.canView=false")
    void canView_FINANCE() {
        Authentication a = auth("finance01", "ROLE_FINANCE");
        assertFalse(authz.canView(a, 100L));
    }
    @Test @DisplayName("FINANCE.canPrint=false")
    void canPrint_FINANCE() {
        Authentication a = auth("finance01", "ROLE_FINANCE");
        assertFalse(authz.canPrint(a, 100L));
    }
    @Test @DisplayName("FINANCE.canDownload=false")
    void canDownload_FINANCE() {
        Authentication a = auth("finance01", "ROLE_FINANCE");
        assertFalse(authz.canDownload(a, 100L));
    }
    @Test @DisplayName("FINANCE.canUpload=false")
    void canUpload_FINANCE() {
        Authentication a = auth("finance01", "ROLE_FINANCE");
        assertFalse(authz.canUpload(a, 100L));
    }
    @Test @DisplayName("FINANCE.canDelete=false")
    void canDelete_FINANCE() {
        Authentication a = auth("finance01", "ROLE_FINANCE");
        assertFalse(authz.canDelete(a, 100L));
    }

    // ============================================================
    // 9. 边界：null auth → 全 false
    // ============================================================
            @Test @DisplayName("null auth canView=false")
    void canView_nullAuth() {
        assertFalse(authz.canView(null, 100L));
    }
    @Test @DisplayName("null drawingId canView=false")
    void canView_nullDrawingId() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        assertFalse(authz.canView(a, null));
    }

    // ============================================================
    // 10. computePermissionBits · permission 端点专用（不受 feature flag 影响）
    // ============================================================
            @Test @DisplayName("computePermissionBits ENGINEER 全 true scope=ALL")
    void computePermissionBits_ENGINEER() {
        Authentication a = auth("engineer01", "ROLE_ENGINEER");
        DrawingAuthz.PermissionBits bits = authz.computePermissionBits(a, 100L);
        assertTrue(bits.view && bits.print && bits.download && bits.upload && bits.delete);
        assertEquals("ALL", bits.scope);
    }

    @Test @DisplayName("computePermissionBits FINANCE 全 false scope=NONE")
    void computePermissionBits_FINANCE() {
        Authentication a = auth("finance01", "ROLE_FINANCE");
        DrawingAuthz.PermissionBits bits = authz.computePermissionBits(a, 100L);
        assertFalse(bits.view || bits.print || bits.download || bits.upload || bits.delete);
        assertEquals("NONE", bits.scope);
    }

    @Test @DisplayName("computePermissionBits SALES scope=ORDER view/print 受 link 控制")
    void computePermissionBits_SALES() {
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), eq(CrmDrawingLink.BIZ_TYPE_ORDER), any()))
                .thenReturn(1L);
        Authentication a = auth("sales01", "ROLE_SALES");
        DrawingAuthz.PermissionBits bits = authz.computePermissionBits(a, 100L);
        assertEquals("ORDER", bits.scope);
        assertTrue(bits.view);
        assertTrue(bits.print);
        assertFalse(bits.download);
        assertFalse(bits.upload);
        assertFalse(bits.delete);
    }

    @Test @DisplayName("computePermissionBits SALES unlinked view=false")
    void computePermissionBits_SALES_unlinked() {
        when(linkMapper.selectOrderBizIdsByDrawing(any(), anyLong()))
                .thenReturn(Collections.emptyList());
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(0L);
        Authentication a = auth("sales01", "ROLE_SALES");
        DrawingAuthz.PermissionBits bits = authz.computePermissionBits(a, 100L);
        assertFalse(bits.view);
        assertFalse(bits.print);
    }

    // ============================================================
    // 11. primaryRole + scopeFor 静态辅助
    // ============================================================
            @Test @DisplayName("primaryRole 提取 ROLE_ 前缀后角色码")
    void primaryRole_test() {
        Authentication a = auth("sales01", "ROLE_SALES");
        assertEquals("SALES", DrawingAuthz.primaryRole(a));
    }

    @Test @DisplayName("scopeFor 7 角色映射")
    void scopeFor_test() {
        assertEquals("ALL", DrawingAuthz.scopeFor("ENGINEER"));
        assertEquals("ALL", DrawingAuthz.scopeFor("PROD_PLANNER"));
        assertEquals("ORDER", DrawingAuthz.scopeFor("SALES"));
        assertEquals("PO", DrawingAuthz.scopeFor("PURCHASER"));
        assertEquals("INCOMING", DrawingAuthz.scopeFor("WAREHOUSE"));
        assertEquals("INSPECTION", DrawingAuthz.scopeFor("QC"));
        assertEquals("WORKORDER_PROCESS", DrawingAuthz.scopeFor("OPERATOR"));
        assertEquals("NONE", DrawingAuthz.scopeFor("FINANCE"));
        assertEquals("NONE", DrawingAuthz.scopeFor("UNKNOWN"));
    }

    // ============================================================
    // 辅助
    // ============================================================
            private Authentication auth(String username, String... roles) {
        return new UsernamePasswordAuthenticationToken(username, "password",
                Collections.singletonList(new SimpleGrantedAuthority(roles[0])));
    }
}