/**
 * Story 10.2 · TC-10.2.C1 委外下单
 * 测例 ID：e2e/sprint10/tc-10.2-c1-outsource-create.spec.ts
 * 业务域：C. 委外 + 返修
 * 端点覆盖：POST /orders/{id}/convert-to-outsource
 * 角色：采购员（purchaser01）
 * 验收：委外单创建 · 7 状态机起 PENDING · WW- 单号
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 */
import { test, expect, resetDb, loginWithCredentials, getOutsourceStatus } from './helpers';

test.describe('Story 10.2 · TC-10.2.C1 委外下单', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 采购员将订单转委外 · WW- 单号 + PENDING 初始状态', async ({ page, request }) => {
    await loginWithCredentials(page, request, 'purchaser01', 'pur123', '采购员 01', ['purchaser', 'order:convert-outsource']);
    // 进 B2 订单详情（id=100）
    await page.goto('/orders/100');
    // 点击"转委外"
    await page.click('button:has-text("转委外")');
    // 选择厂商
    await page.selectOption('[name=vendorId]', 'V001');
    // 确认
    await page.click('button:has-text("确认转委外")');
    // 断言：WW- 单号可见
    await expect(page.locator('[data-testid="outsource-no"]')).toBeVisible();
    const outsourceNo = await page.locator('[data-testid="outsource-no"]').textContent();
    expect(outsourceNo).toMatch(/^WW-\d{8}-\d{4}$/);
    // 断言：7 状态机起 PENDING
    const status = await getOutsourceStatus(request, outsourceNo ?? '');
    expect(status).toBe('PENDING');
  });
});