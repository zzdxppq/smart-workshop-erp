/**
 * E2E · Story 1.5 · AC-2.2.2 · 业务员提交 5万报价 + 部门经理 OR 会签
 * 测例 ID：e2e/salesperson-50k-approve.spec.ts
 * 角色：业务员
 */
import { test, expect } from '@playwright/test';

test('Story 1.5 · 业务员提交 5万报价触发部门经理 OR 会签 (AC-2.2.2)', async ({ page }) => {
  await page.goto('http://localhost:8082/login');
  await page.fill('[name=username]', 'salesperson01');
  await page.fill('[name=password]', 'sales123');
  await page.click('button[type=submit]');
  await page.goto('http://localhost:8082/quotes/new');
  await page.fill('[name=customerId]', '12');
  await page.fill('[name=deliveryDate]', '2026-07-15');
  await page.click('button:has-text("添加明细")');
  await page.fill('[name=quantity]', '50');
  await page.fill('[name=unitPrice]', '1000');  // 5万
  await page.click('button:has-text("保存为草稿")');
  await page.click('button:has-text("提交审批")');
  // 断言路由到 DEPT_MANAGER_OR_SIGN
  await expect(page.locator('.approval-route')).toHaveText('DEPT_MANAGER_OR_SIGN');
  // 候选人为多个部门经理
  await expect(page.locator('.candidate')).toHaveCount(2);
});
