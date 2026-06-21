/**
 * Story 10.2 · TC-10.2.B4 利润分析
 * 测例 ID：e2e/sprint10/tc-10.2-b4-order-profit.spec.ts
 * 业务域：B. 订单 + 利润
 * 端点覆盖：GET /orders/{id}/profit
 * 角色：总经理（gm01）
 * 验收：收入/成本/毛利 · 委外占比
 *
 * QA 商鞅设计 · 耗时预算 < 5s
 */
import { test, expect, resetDb, loginAs } from './helpers';

test.describe('Story 10.2 · TC-10.2.B4 利润分析', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 总经理查看订单利润 · 收入/成本/毛利/委外占比', async ({ page, request }) => {
    await loginAs(page, request, 'gm01');
    // 进 B2 订单详情（id=100）· 切利润 tab
    await page.goto('/orders/100?tab=profit');
    // 断言：收入 > 0
    const revenue = await page.locator('[data-testid="revenue"]').textContent();
    expect(parseFloat(revenue ?? '0')).toBeGreaterThan(0);
    // 断言：成本 > 0
    const cost = await page.locator('[data-testid="cost"]').textContent();
    expect(parseFloat(cost ?? '0')).toBeGreaterThan(0);
    // 断言：毛利 ≈ 收入 - 成本（精度 0.01）
    const margin = await page.locator('[data-testid="margin"]').textContent();
    expect(parseFloat(margin ?? '0')).toBeCloseTo(
      parseFloat(revenue ?? '0') - parseFloat(cost ?? '0'),
      2,
    );
    // 断言：委外占比（N.NN% 格式）
    const outsourceRatio = await page.locator('[data-testid="outsource-ratio"]').textContent();
    expect(outsourceRatio).toMatch(/^\d+\.\d{2}%$/);
  });
});