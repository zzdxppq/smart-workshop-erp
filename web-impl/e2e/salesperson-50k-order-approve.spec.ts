/**
 * E2E · Story 1.6 · AC-2.3.2 · 5万订单部门经理 OR 会签
 * 测例 ID：e2e/salesperson-50k-order-approve.spec.ts
 * 角色：业务员（salesperson01）+ 部门经理（dept_manager01）
 * 验收：AC-2.3.2 — 4 阈值路由 5-20万 部门经理 OR 会签
 * V1.3.7 红线 6：OR 会签 候选人列表完整返回
 *
 * 路由分支：5-20万 DEPT_MANAGER_OR_SIGN（复用 Story 1.5 QuoteApprovalRouter）
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.6 · 5万订单部门经理 OR 会签 (AC-2.3.2)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录业务员
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. 业务员创建 5万订单（5-20万 区间）', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/new');
    await page.fill('[name=customerId]', '11');
    await page.fill('[name=deliveryDate]', '2026-07-15');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-50K-001');
    await page.fill('[name=material]', 'Q235');
    await page.fill('[name=spec]', 'M20x80');
    await page.fill('[name=quantity]', '100');
    await page.fill('[name=unitPrice]', '500');  // 100 * 500 = 5万（边界）
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('.order-no')).toContainText(/^XS\d{8}\d{4}$/);
    // 确认订单（DRAFT → CONFIRMED）
    await page.click('button:has-text("确认订单")');
    await expect(page.locator('.status')).toHaveText('CONFIRMED');
    // 断言路由：5-20万 部门经理 OR 会签
    await expect(page.locator('.approval-route')).toContainText('DEPT_MANAGER_OR_SIGN');
  });

  test('2. 部门经理 OR 会签审批（候选人列表 ≥ 2）', async ({ page, context }) => {
    // 切换到部门经理账号
    await context.clearCookies();
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'dept_manager01');
    await page.fill('[name=password]', 'mgr123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    // 进入待办列表
    await page.goto('http://localhost:8082/orders/pending?route=DEPT_MANAGER_OR_SIGN');
    await expect(page.locator('.order-row').first()).toBeVisible();
    // V1.3.7 红线 6：OR 会签候选人列表 ≥ 2
    await page.locator('.order-row').first().locator('button:has-text("审批")').click();
    const candidates = await page.locator('.candidate-list li').allTextContents();
    expect(candidates.length).toBeGreaterThanOrEqual(2);  // OR 会签 多人
    // 审批通过
    await page.click('button:has-text("审批通过")');
    await expect(page.locator('.status')).toHaveText('PRODUCING');
  });
});
