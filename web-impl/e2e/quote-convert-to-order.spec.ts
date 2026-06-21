/**
 * E2E · Story 1.5 · AC-2.2.3 · 业务员审批通过后一键转订单
 * 测例 ID：e2e/quote-convert-to-order.spec.ts
 * 角色：业务员
 */
import { test, expect } from '@playwright/test';

test('Story 1.5 · 业务员一键转订单 → XS 单号 (AC-2.2.3)', async ({ page }) => {
  await page.goto('http://localhost:8082/login');
  await page.fill('[name=username]', 'salesperson01');
  await page.fill('[name=password]', 'sales123');
  await page.click('button[type=submit]');
  // 进入已审批的报价
  await page.goto('http://localhost:8082/quotes/100');
  await expect(page.locator('.status')).toHaveText('APPROVED');
  // 点击转订单 → 预览对话框（V1.3.7 红线 4）
  await page.click('button:has-text("转订单")');
  await expect(page.locator('.order-preview-modal')).toBeVisible();
  // 确认
  await page.click('button:has-text("确认转订单")');
  // 断言订单号 XS 开头
  await expect(page.locator('.order-no')).toContainText(/^XS\d{8}-\d{4}$/);
  // 状态变为 CONVERTED
  await expect(page.locator('.status')).toHaveText('CONVERTED');
});
