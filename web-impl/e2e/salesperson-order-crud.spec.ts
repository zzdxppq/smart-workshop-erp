/**
 * E2E · Story 1.6 · AC-2.3.1 · 业务员订单 CRUD
 * 测例 ID：e2e/salesperson-order-crud.spec.ts
 * 角色：业务员（salesperson01）
 * 验收：AC-2.3.1 — 订单 CRUD + 字段必填校验
 * V1.3.7 红线 1：业务自审 < 5万 列表不显示金额
 * V1.3.7 红线 5：所有写操作 @AuditLog AFTER_COMMIT
 *
 * 状态机：DRAFT → CONFIRMED → PRODUCING → PARTIAL_SHIPPED → SHIPPED → SETTLED → CLOSED
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.6 · 业务员订单 CRUD (AC-2.3.1)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录业务员（Story 1.1 JWT）
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. 创建订单 → XS 单号 → 修改 → 列表 (AC-2.3.1)', async ({ page }) => {
    // 创建订单
    await page.goto('http://localhost:8082/orders/new');
    await page.fill('[name=customerId]', '11');  // 客户 C0011-NORMAL（普通客户）
    await page.selectOption('[name=currency]', 'CNY');
    await page.fill('[name=deliveryDate]', '2026-07-15');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-ORD-001');
    await page.fill('[name=material]', 'Q235');
    await page.fill('[name=spec]', 'M16x50');
    await page.fill('[name=quantity]', '20');
    await page.fill('[name=unitPrice]', '500');  // 20 * 500 = 1万 < 5万 业务自审
    await page.click('button:has-text("保存为草稿")');
    // 断言订单号 XS+YYYYMMDD+4位流水（继承 Story 1.5 DocNoGenerator）
    await expect(page.locator('.order-no')).toContainText(/^XS\d{8}\d{4}$/);
    // 断言 status=DRAFT
    await expect(page.locator('.status')).toHaveText('DRAFT');
    // 修改（DRAFT 状态可改）
    await page.click('button:has-text("编辑")');
    await page.fill('[name=comment]', 'test update 1.6');
    await page.click('button:has-text("保存")');
    await expect(page.locator('.status')).toHaveText('DRAFT');
    // 列表查询
    await page.goto('http://localhost:8082/orders?status=DRAFT');
    await expect(page.locator('.order-row').first()).toContainText(/^XS\d{8}\d{4}$/);
    // V1.3.7 红线 1：业务自审 < 5万 列表不显示金额
    const amountCell = page.locator('.order-row .amount').first();
    await expect(amountCell).toHaveText(/--|\*\*\*/);  // 隐藏金额
  });

  test('2. 字段必填校验失败 (AC-2.3.1)', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/new');
    // 不填 customerId 直接保存
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('.error-customerId')).toBeVisible();
    // items 为空
    await page.fill('[name=customerId]', '11');
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('.error-items')).toContainText('items ≥ 1');
  });

  test('3. 非 DRAFT 状态不可编辑 (AC-2.3.1 40903)', async ({ page }) => {
    // 假定有 CONFIRMED 状态订单（id=1，外部准备）
    await page.goto('http://localhost:8082/orders/1/edit');
    await expect(page.locator('.status')).toHaveText('CONFIRMED');
    // 编辑按钮不可见
    await expect(page.locator('button:has-text("编辑")')).toBeHidden();
  });
});
