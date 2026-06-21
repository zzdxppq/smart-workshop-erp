/**
 * E2E · Story 1.6 · AC-2.3.4 · 业务员确认 → 转生产（GD 单号）
 * 测例 ID：e2e/salesperson-confirm-to-production.spec.ts
 * 角色：业务员（salesperson01）
 * 验收：AC-2.3.4 — 转生产（CONFIRMED → PRODUCING） + GD 单号
 * V1.3.7 PRD FR-5-1-1：工单号 GD{yyyyMMdd}{seq:4}
 * V1.3.7 红线 4（1.6 新增）：转生产预览
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.6 · 业务员确认 → 转生产 (AC-2.3.4)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. CONFIRMED 订单 → 转生产预览 → 触发 Epic 5 生成 GD 单号', async ({ page }) => {
    // 假定有 CONFIRMED 状态订单 id=2
    await page.goto('http://localhost:8082/orders/2');
    await expect(page.locator('.status')).toHaveText('CONFIRMED');
    // 点击转生产按钮（V1.3.7 红线 4：转生产预览对话框）
    await page.click('button:has-text("转生产")');
    await expect(page.locator('.preview-dialog')).toBeVisible();
    // 预览内容：订单明细 + 自动带出的图纸/材质/尺寸/数量
    await expect(page.locator('.preview-items')).toContainText('DWG');
    await expect(page.locator('.preview-items')).toContainText('Q235');
    await expect(page.locator('.preview-qty')).toBeVisible();
    // 确认转生产
    await page.click('button:has-text("确认转生产")');
    // 断言 GD 单号（V1.3.7 PRD FR-5-1-1）
    await expect(page.locator('.production-no')).toContainText(/^GD\d{8}\d{4}$/);
    // 断言订单状态：CONFIRMED → PRODUCING
    await expect(page.locator('.status')).toHaveText('PRODUCING');
    // 断言 crm_order_history 留痕：operation=CONVERT_PROD
    await page.click('button:has-text("历史记录")');
    await expect(page.locator('.history-row').last()).toContainText('CONVERT_PROD');
  });

  test('2. 转生产幂等：重复点击应拒绝 (40904 ORDER_STATE_INVALID)', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/2');
    await expect(page.locator('.status')).toHaveText('PRODUCING');
    // 已 PRODUCING 状态不应再有"转生产"按钮
    await expect(page.locator('button:has-text("转生产")')).toBeHidden();
  });
});
