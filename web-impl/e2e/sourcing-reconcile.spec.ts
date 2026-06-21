/**
 * E2E · Story 1.25 · 月度对账 · V1.3.7 AD-2 红线
 * 测例 ID：e2e/sourcing-reconcile.spec.ts
 * 角色：采购员（purchaser01）+ 财务（finance01）+ 厂商（vendor 端）
 * 验收：AC-5.6.1 — 4 步流程（建单 → 厂商确认 → 财务审核 → 双方签），严格禁止"线下"按钮
 * V1.3.7 红线 AD-2：对账页面无"采购带纸去厂商处"/"线下签字"按钮
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.25 · 月度对账 · 4 步流程 · 不含"线下"按钮 (V1.3.7 AD-2)', () => {
  test('1. 采购员创建月度对账单（步骤 1/4 建单）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'purchaser01');
    await page.fill('[name=password]', 'purch123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    await page.goto('http://localhost:8082/sourcing/reconcile');
    await page.click('button:has-text("新建对账单")');
    await page.fill('[name=period]', '2026-06');
    await page.fill('[name=vendorName]', '苏州 CNC 外协厂');
    await page.click('button:has-text("提交建单")');
    await expect(page.locator('.reconcile-no')).toBeVisible();
  });

  test('2. 红线验证：对账页面无"线下"按钮（AD-2）', async ({ page }) => {
    await page.goto('http://localhost:8082/sourcing/reconcile');
    // 严格禁止以下按钮
    await expect(page.locator('button:has-text("线下签字")')).toHaveCount(0);
    await expect(page.locator('button:has-text("采购带纸")')).toHaveCount(0);
    await expect(page.locator('button:has-text("线下对账")')).toHaveCount(0);
    await expect(page.locator('button:has-text("去厂商处")')).toHaveCount(0);
    await expect(page.locator('button:has-text("带纸去厂商")')).toHaveCount(0);
    await expect(page.locator('button:has-text("手工打印")')).toHaveCount(0);
  });

  test('3. 厂商确认（步骤 2/4）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'vendor01');
    await page.fill('[name=password]', 'vendor123');
    await page.click('button[type=submit]');

    await page.goto('http://localhost:8082/sourcing/reconcile');
    await page.click('button:has-text("详情")');
    await page.click('button:has-text("确认对账")');
    await expect(page.locator('.step')).toContainText('VENDOR_CONFIRM');
  });

  test('4. 财务审核（步骤 3/4）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'finance01');
    await page.fill('[name=password]', 'fin123');
    await page.click('button[type=submit]');

    await page.goto('http://localhost:8082/sourcing/reconcile');
    await page.click('button:has-text("详情")');
    await page.click('button:has-text("财务审核")');
    await expect(page.locator('.step')).toContainText('FINANCE_AUDIT');
  });

  test('5. 双方电子签字（步骤 4/4，不允许"线下"）', async ({ page }) => {
    await page.goto('http://localhost:8082/sourcing/reconcile-signature/1');
    await page.click('button:has-text("厂商签字")');
    await page.click('button:has-text("财务签字")');
    await expect(page.locator('.step')).toContainText('SIGN');
    await expect(page.locator('.vendor-signed')).toContainText('已签');
    await expect(page.locator('.finance-signed')).toContainText('已签');
  });
});