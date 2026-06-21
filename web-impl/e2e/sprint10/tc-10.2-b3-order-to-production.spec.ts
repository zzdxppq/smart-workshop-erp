/**
 * Story 10.2 · TC-10.2.B3 转生产
 * 测例 ID：e2e/sprint10/tc-10.2-b3-order-to-production.spec.ts
 * 业务域：B. 订单 + 利润
 * 端点覆盖：POST /orders/{id}/convert-to-production
 * 角色：生产经理（production_manager01）
 * 验收：工单生成 · 库存预扣
 *
 * QA 商鞅设计 · 耗时预算 < 5s
 */
import { test, expect, resetDb, loginWithCredentials, getWorkorderNo, getStock } from './helpers';

test.describe('Story 10.2 · TC-10.2.B3 转生产', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 生产经理将订单转生产 · 工单生成 + 库存预扣', async ({ page, request }) => {
    await loginWithCredentials(page, request, 'production_manager01', 'prod123', '生产经理 01', ['production_manager', 'order:approve', 'order:convert-production']);
    // 进 B2 订单详情（id=100）
    await page.goto('/orders/100');
    // 点击"转生产"
    await page.click('[data-testid="convert-production"]');
    // 断言：工单号可见
    await expect(page.locator('[data-testid="workorder-no"]')).toBeVisible();
    const workorderNo = await page.locator('[data-testid="workorder-no"]').textContent();
    expect(workorderNo).toMatch(/^GD\d{8}\d{4}$/);
    // 断言：工单生成 · backend 查询
    const wo = await getWorkorderNo(request, 100);
    expect(wo).toBeTruthy();
    // 断言：库存预扣
    const stock = await getStock(request, 'MAT-20260613-0001');
    expect(stock.reservedQty).toBeGreaterThan(0);
  });
});