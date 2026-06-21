/**
 * E2E · Story 1.5 · AC-2.2.2 · 总经理 25万 + 财务总监双签
 * 测例 ID：e2e/gm-250k-finance-dual-sign.spec.ts
 * 角色：总经理
 */
import { test, expect } from '@playwright/test';

test('Story 1.5 · 总经理审批 25万 → 财务总监双签 (AC-2.2.2)', async ({ page }) => {
  await page.goto('http://localhost:8082/login');
  await page.fill('[name=username]', 'gm01');
  await page.fill('[name=password]', 'gm123');
  await page.click('button[type=submit]');
  await page.goto('http://localhost:8082/quotes/100');
  await expect(page.locator('.approval-route')).toHaveText('GM_FINANCE_DUAL_SIGN');
  // 二次密码确认 (V1.3.7 红线 2: > 20万)
  await page.click('button:has-text("审批通过")');
  await page.fill('[name=adminPassword]', 'admin123');
  await page.click('button:has-text("确认")');
  await expect(page.locator('.status')).toHaveText('SUBMITTED');
  // 财务总监双签
  await page.goto('http://localhost:8082/logout');
  await page.fill('[name=username]', 'finance_director01');
  await page.fill('[name=password]', 'fin123');
  await page.click('button[type=submit]');
  await page.goto('http://localhost:8082/quotes/100');
  await page.click('button:has-text("审批通过")');
  await expect(page.locator('.status')).toHaveText('APPROVED');
});
