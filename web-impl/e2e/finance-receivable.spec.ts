/**
 * E2E · Story 1.33 · 应收账款
 * 测例 ID：e2e/finance-receivable.spec.ts
 * 角色：财务（finance01）+ 业务员只读验证
 * 验收：AC-5.8.1 — 应收/应付 + 红线 5：作业人员自助只读金额
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.33 · 应收/应付 (AC-5.8.1)', () => {
  test('1. 财务查看应收列表 + 可编辑金额', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'finance01');
    await page.fill('[name=password]', 'fin123');
    await page.click('button[type=submit]');

    await page.goto('http://localhost:8082/finance/receivables');
    await expect(page.locator('text=应收账款')).toBeVisible();
    // 财务视角：可编辑金额（输入框存在）
    await expect(page.locator('input[type=number]').first()).toBeVisible();
  });

  test('2. 红线 5：作业人员自助只读金额', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'operator01');
    await page.fill('[name=password]', 'op123');
    await page.click('button[type=submit]');

    await page.goto('http://localhost:8082/finance/receivables');
    // 作业人员视角：金额只读（无 input 框）
    await expect(page.locator('.readonly-amount').first()).toBeVisible();
  });

  test('3. 应付列表', async ({ page }) => {
    await page.goto('http://localhost:8082/finance/payables');
    await expect(page.locator('text=应付账款')).toBeVisible();
  });

  test('4. 账龄分析', async ({ page }) => {
    await page.goto('http://localhost:8082/finance/aging');
    await expect(page.locator('text=账龄分析')).toBeVisible();
  });
});