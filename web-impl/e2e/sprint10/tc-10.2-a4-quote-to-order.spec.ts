/**
 * Story 10.2 · TC-10.2.A4 报价转订单
 * 测例 ID：e2e/sprint10/tc-10.2-a4-quote-to-order.spec.ts
 * 业务域：A. 认证 + 审批工作流
 * 端点覆盖：POST /quotes/{id}/convert-to-order
 * 角色：业务员（salesperson01）
 * 验收：APPROVED 报价转订单 · 跳订单详情 · 订单号 SO- 开头
 *
 * QA 商鞅设计 · 耗时预算 < 3s
 */
import { test, expect, resetDb, loginAs } from './helpers';

test.describe('Story 10.2 · TC-10.2.A4 报价转订单', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 业务员将 APPROVED 报价转化为订单', async ({ page, request }) => {
    await loginAs(page, request, 'salesperson01');
    // 进报价详情（用 mock id=100 · 与 Sprint 1.5 gm-250k-finance-dual-sign 一致）
    await page.goto('/quotes/100');
    // 点击"转化为订单"
    await page.click('[data-testid="convert-btn"]');
    // 断言：跳订单详情
    await expect(page).toHaveURL(/\/orders\/\d+/);
    // 断言：订单号 SO- 开头
    const orderNo = await page.locator('[data-testid="order-no"]').textContent();
    expect(orderNo).toMatch(/^SO-\d{8}-\d{4}$/);
  });
});