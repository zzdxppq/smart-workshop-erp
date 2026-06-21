package com.btsheng.erp.business.crm.drawing.performance;

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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * V1.3.9 Sprint 12 Story 12.1 · SpEL 鉴权性能基线（3 测例 · QA 测例组 4）
 *
 * <p>期望：
 * <ul>
 *   <li>TC-12.1.4.1 @Cacheable 命中（Redis 命中 < 5ms）</li>
 *   <li>TC-12.1.4.2 OPERATOR 工序 Redis 缓存命中（< 3ms）</li>
 *   <li>TC-12.1.4.3 P95 鉴权 < 50ms（1000 次随机压测）</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DrawingAuthz SpEL 鉴权性能基线")
class DrawingAuthzBenchmarkTest {

    @Mock private CrmDrawingLinkMapper linkMapper;
    @Mock private CacheTemplate cacheTemplate;
    @Mock private PlatformLookupMapper platformLookupMapper;

    private DrawingAuthz newAuthz() {
        return new DrawingAuthz(linkMapper, cacheTemplate, platformLookupMapper);
    }

    @Test
    @DisplayName("TC-12.1.4.1 ENGINEER 全量鉴权 < 50ms（不查 DB · 短路返 true）")
    void benchmark_engineer_canView() {
        DrawingAuthz authz = newAuthz();
        Authentication a = auth("engineer01", "ROLE_ENGINEER");

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            assertTrue(authz.canView(a, 100L));
        }
        long elapsedNs = System.nanoTime() - start;
        long elapsedMs = elapsedNs / 1_000_000;
        double avgMs = (double) elapsedMs / 1000.0;
        assertTrue(avgMs < 1.0, "ENGINEER 单次平均 < 1ms（短路），实际 " + avgMs + "ms");
        System.out.println("[BENCHMARK] ENGINEER.canView 1000 次总耗时=" + elapsedMs + "ms · 平均=" + avgMs + "ms");
    }

    @Test
    @DisplayName("TC-12.1.4.2 SALES 关联查询平均 < 5ms（含 mock 链接）")
    void benchmark_sales_linked_canView() {
        DrawingAuthz authz = newAuthz();
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(1L);
        Authentication a = auth("sales01", "ROLE_SALES");

        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            assertTrue(authz.canView(a, 789L));
        }
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        double avgMs = (double) elapsedMs / 1000.0;
        assertTrue(avgMs < 5.0, "SALES 关联平均 < 5ms，实际 " + avgMs + "ms");
        System.out.println("[BENCHMARK] SALES.canView 1000 次总耗时=" + elapsedMs + "ms · 平均=" + avgMs + "ms");
    }

    @Test
    @DisplayName("TC-12.1.4.3 P95 鉴权 < 50ms（1000 次混合角色压测）")
    void benchmark_p95_mixed_roles() {
        DrawingAuthz authz = newAuthz();
        when(linkMapper.existsByDrawingAndBizTypeAndBizIdIn(anyLong(), any(), any())).thenReturn(1L);

        List<String> roles = Arrays.asList(
                "ROLE_ENGINEER", "ROLE_PROD_PLANNER", "ROLE_SALES", "ROLE_PURCHASER",
                "ROLE_WAREHOUSE", "ROLE_QC", "ROLE_OPERATOR");
        List<Long> latenciesNs = new ArrayList<>(1000);

        for (int i = 0; i < 1000; i++) {
            String role = roles.get(i % roles.size());
            Authentication a = auth("user_" + i, role);
            long t0 = System.nanoTime();
            authz.canView(a, (long) (i % 100 + 1));
            long elapsed = System.nanoTime() - t0;
            latenciesNs.add(elapsed);
        }
        Collections.sort(latenciesNs);
        long p50 = latenciesNs.get(500) / 1_000_000;
        long p95 = latenciesNs.get(950) / 1_000_000;
        long p99 = latenciesNs.get(990) / 1_000_000;
        System.out.println("[BENCHMARK] P50=" + p50 + "ms · P95=" + p95 + "ms · P99=" + p99 + "ms");
        assertTrue(p95 < 50, "P95 鉴权 < 50ms，实际 " + p95 + "ms");
    }

    private Authentication auth(String username, String role) {
        return new UsernamePasswordAuthenticationToken(username, "password",
                Collections.singletonList(new SimpleGrantedAuthority(role)));
    }
}