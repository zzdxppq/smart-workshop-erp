/**
 * Story 10.2 · TC-10.2.B2 订单审批
 * 测例 ID：e2e/sprint10/tc-10.2-b2-order-approve.spec.ts
 * 业务域：B. 订单 + 利润
 * 端点覆盖：POST /orders/{id}/approve
 * 角色：部门经理（dept_manager01）
 * 验收：CONFIRMED → PRODUCING · 4 阈值路由审计
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 */
import { test, expect, resetDb, loginAs, getApprovalRoute } from './helpers';

test.describe('Story 10.2 · TC-10.2.B2 订单审批', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 部门经理审批 5-20万 订单 · 4 阈值路由', async ({ page, request }) => {
    await loginAs(page, request, 'dept_manager01');
    // 进 A4 创建的订单详情（id=100）
    await page.goto('/orders/100');
    // 审批通过
    await page.click('[data-testid="approve-btn"]');
    // 断言：CONFIRMED → PRODUCING
    await expect(page.locator('[data-testid="order-status"]')).toHaveText('PRODUCING');
    // 断言：4 阈值路由审计（5-20万 = DEPT_MANAGER）
    const route = await getApprovalRoute(request, 100);
    expect(route).toContain('DEPT_MANAGER');
  });
});