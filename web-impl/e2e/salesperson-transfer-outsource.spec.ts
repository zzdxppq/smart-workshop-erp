/**
 * E2E · Story 1.6 · AC-2.3.4 · 业务员转委外（WW 单号）
 * 测例 ID：e2e/salesperson-transfer-outsource.spec.ts
 * 角色：业务员（salesperson01）
 * 验收：AC-2.3.4 — 转委外（CONFIRMED → PARTIAL_SHIPPED 或 PRODUCING） + WW 单号
 * V1.3.7 PRD FR-6-1-1：委外单号 WW{yyyyMMdd}{seq:4}
 * V1.3.7 红线 5（1.6 新增）：转委外预览
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.6 · 业务员转委外 (AC-2.3.4)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. CONFIRMED 订单 → 转委外预览 → 触发 Epic 6 生成 WW 单号', async ({ page }) => {
    // 假定有 CONFIRMED 状态订单 id=3
    await page.goto('http://localhost:8082/orders/3');
    await expect(page.locator('.status')).toHaveText('CONFIRMED');
    // 点击转委外按钮（V1.3.7 红线 5：转委外预览对话框）
    await page.click('button:has-text("转委外")');
    await expect(page.locator('.preview-dialog')).toBeVisible();
    // 选择委外厂商
    await page.selectOption('[name=vendorId]', 'V001');
    // 预览内容：订单明细 + 委外数量
    await expect(page.locator('.preview-items')).toContainText('DWG');
    await page.fill('[name=outsourceQty]', '50');  // 部分委外 50/100
    // 确认转委外
    await page.click('button:has-text("确认转委外")');
    // 断言 WW 单号（V1.3.7 PRD FR-6-1-1）
    await expect(page.locator('.outsource-no')).toContainText(/^WW\d{8}\d{4}$/);
    // 断言订单状态：CONFIRMED → PARTIAL_SHIPPED（部分委外）
    await expect(page.locator('.status')).toHaveText('PARTIAL_SHIPPED');
    // 断言 crm_order_history 留痕：operation=CONVERT_OUTSUB
    await page.click('button:has-text("历史记录")');
    await expect(page.locator('.history-row').last()).toContainText('CONVERT_OUTSUB');
  });

  test('2. 部分生产 + 部分委外：状态保持 PRODUCING', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/4');
    await expect(page.locator('.status')).toHaveText('CONFIRMED');
    // 先转生产 30%
    await page.click('button:has-text("转生产")');
    await page.fill('[name=productionQty]', '30');
    await page.click('button:has-text("确认转生产")');
    // 再转委外 70%
    await page.click('button:has-text("转委外")');
    await page.fill('[name=outsourceQty]', '70');
    await page.click('button:has-text("确认转委外")');
    // 断言状态：PRODUCING（部分生产 + 部分委外）
    await expect(page.locator('.status')).toHaveText('PRODUCING');
  });
});
