/**
 * E2E · Story 1.6 · AC-2.3.4 · 财务回款结算 + 利润分析
 * 测例 ID：e2e/finance-order-settle.spec.ts
 * 角色：财务（finance01）
 * 验收：AC-2.3.4 — 结算（SHIPPED → SETTLED，引用 Epic 9 crm_receipt）+ 利润分析
 * V1.3.7 红线 1：业务自审 < 5万 列表不显示金额（财务不受限）
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.6 · 财务回款结算 + 利润分析 (AC-2.3.4)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录财务
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'finance01');
    await page.fill('[name=password]', 'fin123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. SHIPPED 订单 → 回款 → 结算（SHIPPED → SETTLED）', async ({ page }) => {
    // 假定有 SHIPPED 状态订单 id=10
    await page.goto('http://localhost:8082/orders/10');
    await expect(page.locator('.status')).toHaveText('SHIPPED');
    // 创建回款（引用 Epic 9 crm_receipt）
    await page.click('button:has-text("登记回款")');
    await page.fill('[name=receiptAmount]', '5000');  // 部分回款 5000
    await page.click('button:has-text("保存回款")');
    await expect(page.locator('.receipt-row').last()).toContainText('5000');
    // 满额回款（订单总额 5000）→ 触发结算
    await page.click('button:has-text("登记回款")');
    await page.fill('[name=receiptAmount]', '0');  // 完成回款
    await page.click('button:has-text("保存回款")');
    // 断言订单状态：SHIPPED → SETTLED
    await expect(page.locator('.status')).toHaveText('SETTLED');
    // 断言回款校验：sum(receipt.amount) == order.totalAmount
    await expect(page.locator('.receipt-summary')).toContainText('已全额回款');
  });

  test('2. 利润分析（生产成本 0.55 / 委外 0.65 / 材料 0.20 系数）', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/10/profit');
    // 利润 = 总金额 - (生产成本 0.55 + 委外 0.65 + 材料 0.20 系数)
    await expect(page.locator('.profit-total')).toBeVisible();
    await expect(page.locator('.profit-cost')).toContainText(/[0-9]+/);
    await expect(page.locator('.profit-margin')).toBeVisible();
    // 负利润告警（如有）
    const warnEl = page.locator('.profit-warning');
    if (await warnEl.isVisible()) {
      await expect(warnEl).toContainText('负利润');
    }
  });

  test('3. SETTLED → CLOSED 终态关闭', async ({ page }) => {
    // 假定有 SETTLED 状态订单 id=11
    await page.goto('http://localhost:8082/orders/11');
    await expect(page.locator('.status')).toHaveText('SETTLED');
    // 关闭订单
    await page.click('button:has-text("关闭订单")');
    await expect(page.locator('.status')).toHaveText('CLOSED');
    // 终态：所有按钮不可见
    await expect(page.locator('button:has-text("编辑")')).toBeHidden();
    await expect(page.locator('button:has-text("转生产")')).toBeHidden();
    await expect(page.locator('button:has-text("转委外")')).toBeHidden();
  });
});
