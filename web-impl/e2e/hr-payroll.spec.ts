/**
 * E2E · Story 1.42 · 薪酬核算
 * 测例 ID：e2e/hr-payroll.spec.ts
 * 角色：HR（hr01）+ 作业人员只读验证
 * 验收：AC-5.9.1 — 薪酬核算 + 红线 5：作业人员自助只读金额
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.42 · 薪酬核算 (AC-5.9.1)', () => {
  test('1. HR 运行薪酬核算', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'hr01');
    await page.fill('[name=password]', 'hr123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    await page.goto('http://localhost:8082/admin/payroll');
    await page.fill('[name=period]', '2026-06');
    await page.click('button:has-text("运行核算")');
    await expect(page.locator('.payroll-list')).toBeVisible();
  });

  test('2. 红线 5：作业人员自助金额只读', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'operator01');
    await page.fill('[name=password]', 'op123');
    await page.click('button[type=submit]');

    await page.goto('http://localhost:8082/admin/payroll');
    await expect(page.locator('.readonly-amount').first()).toBeVisible();
  });

  test('3. 薪酬审批', async ({ page }) => {
    await page.goto('http://localhost:8082/admin/payroll-detail/1');
    await page.click('button:has-text("审批")');
    await expect(page.locator('.status')).toContainText('APPROVED');
  });
});