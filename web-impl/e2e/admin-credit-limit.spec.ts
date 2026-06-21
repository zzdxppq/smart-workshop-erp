/**
 * E2E · Story 1.6 · AC-2.3.3 · 信用额度校验 + 黑名单优先
 * 测例 ID：e2e/admin-credit-limit.spec.ts
 * 角色：业务员（salesperson01）+ admin（黑名单/信用额度管理）
 * 验收：AC-2.3.3 — 信用额度校验（40909 CREDIT_LIMIT_EXCEEDED）+ 黑名单优先（40902 优先于 40909）
 * V1.3.7 P2 修补 3：信用额度 hook
 * V1.3.7 红线 1：黑名单高亮
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.6 · 信用额度校验 + 黑名单优先 (AC-2.3.3)', () => {
  test('1. 业务创建订单 超信用额度 → 40909 CREDIT_LIMIT_EXCEEDED', async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    // 选客户 C0012（creditLimit=10000）
    await page.goto('http://localhost:8082/orders/new');
    await page.fill('[name=customerId]', '12');
    await page.fill('[name=deliveryDate]', '2026-07-15');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-LIMIT-001');
    await page.fill('[name=material]', 'Q235');
    await page.fill('[name=quantity]', '100');
    await page.fill('[name=unitPrice]', '200');  // 100 * 200 = 2万 > 1万
    await page.click('button:has-text("保存为草稿")');
    // 断言 40909 CREDIT_LIMIT_EXCEEDED
    await expect(page.locator('.error-credit')).toContainText('40909');
    await expect(page.locator('.error-credit')).toContainText('CREDIT_LIMIT_EXCEEDED');
    // 状态保持 DRAFT
    await expect(page.locator('.status')).toHaveText('DRAFT');
  });

  test('2. 黑名单优先（40902 优先于 40909）', async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    // 选黑名单客户 C0001-BL（同时信用额度 1万，但先命中黑名单）
    await page.goto('http://localhost:8082/orders/new');
    await page.fill('[name=customerId]', '1');
    await page.fill('[name=deliveryDate]', '2026-07-15');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-BL-001');
    await page.fill('[name=material]', 'Q235');
    await page.fill('[name=quantity]', '5');
    await page.fill('[name=unitPrice]', '100');  // 5 * 100 = 500 < 信用额度
    await page.click('button:has-text("保存为草稿")');
    // 断言 40902 CUSTOMER_BLACKLIST（优先于 40909）
    await expect(page.locator('.error-blacklist')).toContainText('40902');
    await expect(page.locator('.error-blacklist')).toContainText('CUSTOMER_BLACKLIST');
  });

  test('3. creditLimit=-1 无限制特殊值', async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    // 选 C0030（creditLimit=-1 无限制）
    await page.goto('http://localhost:8082/orders/new');
    await page.fill('[name=customerId]', '30');
    await page.fill('[name=deliveryDate]', '2026-07-15');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-NOLIMIT-001');
    await page.fill('[name=material]', 'Q235');
    await page.fill('[name=quantity]', '1000');
    await page.fill('[name=unitPrice]', '1000');  // 1000 * 1000 = 100万（应通过）
    await page.click('button:has-text("保存为草稿")');
    // 断言无信用额度错误
    await expect(page.locator('.order-no')).toContainText(/^XS\d{8}\d{4}$/);
  });

  test('4. 黑名单客户高亮（V1.3.7 红线 1）', async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    // 客户列表
    await page.goto('http://localhost:8082/customers?status=BLACKLIST');
    // 断言高亮 className
    const firstBl = page.locator('.customer-row').first();
    await expect(firstBl).toHaveClass(/blacklist-highlight/);
    // 颜色断言
    const bgColor = await firstBl.evaluate((el) => getComputedStyle(el).backgroundColor);
    expect(bgColor).toMatch(/rgb\(255,\s*235,\s*235\)|rgb\(254,\s*226,\s*226\)/);
  });
});
