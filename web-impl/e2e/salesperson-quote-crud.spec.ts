/**
 * E2E · Story 1.5 · AC-2.2.1 · 业务员报价 CRUD
 * 测例 ID：e2e/salesperson-quote-crud.spec.ts
 * 角色：业务员
 * 验收：AC-2.2.1 — 报价单 CRUD + 字段必填校验
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.5 · 业务员报价 CRUD (AC-2.2.1)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录业务员（Story 1.1 JWT）
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. 创建报价 → 修改 → 提交 (AC-2.2.1)', async ({ page }) => {
    // 创建报价
    await page.goto('http://localhost:8082/quotes/new');
    await page.fill('[name=customerId]', '11');
    await page.selectOption('[name=currency]', 'CNY');
    await page.fill('[name=deliveryDate]', '2026-07-01');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-001');
    await page.fill('[name=quantity]', '10');
    await page.fill('[name=unitPrice]', '5000');
    await page.click('button:has-text("保存为草稿")');
    // 断言报价号 BJ 开头
    await expect(page.locator('.quote-no')).toContainText(/^BJ\d{8}-\d{4}$/);
    // 修改
    await page.click('button:has-text("编辑")');
    await page.fill('[name=comment]', 'test update');
    await page.click('button:has-text("保存")');
    await expect(page.locator('.status')).toHaveText('DRAFT');
    // 提交
    await page.click('button:has-text("提交审批")');
    await expect(page.locator('.status')).toHaveText('SUBMITTED');
  });
});
