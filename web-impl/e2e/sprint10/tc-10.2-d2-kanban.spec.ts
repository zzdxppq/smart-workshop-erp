/**
 * Story 10.2 · TC-10.2.D2 多维度看板
 * 测例 ID：e2e/sprint10/tc-10.2-d2-kanban.spec.ts
 * 业务域：D. 库存 + 报表
 * 端点覆盖：GET /api/v1/reports/kanban
 * 角色：总经理（gm01）
 * 验收：9 维度 × 5 角色矩阵 · P95 < 500ms
 *
 * QA 商鞅设计 · 耗时预算 < 5s
 * architect review IMPL 注意事项 4：端到端含 UI 渲染 · responseTime < 5000ms
 */
import { test, expect, resetDb, loginAs } from './helpers';

test.describe('Story 10.2 · TC-10.2.D2 多维度看板', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 总经理查看看板 · 9 维度 + 端到端 < 5s', async ({ page, request }) => {
    await loginAs(page, request, 'gm01');
    // 进看板页
    const startTime = Date.now();
    await page.goto('/reports/kanban');
    // 等待第一张看板卡渲染
    await page.locator('[data-testid="kanban-card"]').first().waitFor();
    const elapsed = Date.now() - startTime;
    // 断言：端到端含 UI 渲染 < 5s（architect review IMPL 注意事项 4）
    expect(elapsed).toBeLessThan(5_000);
    // 断言：9 维度 tab 可见（gm 角色看全 9 维度）
    const tabs = await page.locator('[data-testid^="kanban-tab-"]').count();
    expect(tabs).toBe(9);
    // 切维度验证交互
    await page.locator('[data-testid="kanban-tab-time"]').click();
    await page.locator('[data-testid="kanban-card"]').first().waitFor();
  });
});