/**
 * Story 10.2 · TC-10.2.B1 订单列表（3 角色权限矩阵）
 * 测例 ID：e2e/sprint10/tc-10.2-b1-order-list.spec.ts
 * 业务域：B. 订单 + 利润
 * 端点覆盖：GET /orders
 * 角色：业务员（salesperson01）+ 部门经理（dept_manager01）+ 总经理（gm01）
 * 验收：3 角色权限矩阵 · sales ≤ manager ≤ gm
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 */
import { test, expect, resetDb, loginAs } from './helpers';

test.describe('Story 10.2 · TC-10.2.B1 订单列表（3 角色权限矩阵）', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 业务员订单列表（owner 单）', async ({ page, request }) => {
    await loginAs(page, request, 'salesperson01');
    await page.goto('/orders');
    const rows = await page.locator('[data-testid="order-row"]').count();
    expect(rows).toBeGreaterThan(0);
    // 业务员仅看到 owner = 自己的订单 · 数据集小
    expect(rows).toBeLessThanOrEqual(10);
  });

  test('2. 部门经理订单列表（dept 单）', async ({ page, request }) => {
    await loginAs(page, request, 'dept_manager01');
    await page.goto('/orders');
    const rows = await page.locator('[data-testid="order-row"]').count();
    expect(rows).toBeGreaterThan(0);
    // 部门经理看到 dept 内所有订单 · 比业务员多
    expect(rows).toBeGreaterThanOrEqual(1);
  });

  test('3. 总经理订单列表（all 单）', async ({ page, request }) => {
    await loginAs(page, request, 'gm01');
    await page.goto('/orders');
    const rows = await page.locator('[data-testid="order-row"]').count();
    expect(rows).toBeGreaterThan(0);
    // 总经理看到全部订单 · 数量最多
    expect(rows).toBeGreaterThanOrEqual(1);
  });

  test('4. 权限矩阵 · sales ≤ manager ≤ gm', async ({ page, request }) => {
    // 业务员
    await loginAs(page, request, 'salesperson01');
    await page.goto('/orders');
    const salesRows = await page.locator('[data-testid="order-row"]').count();
    // 部门经理
    await loginAs(page, request, 'dept_manager01');
    await page.goto('/orders');
    const managerRows = await page.locator('[data-testid="order-row"]').count();
    // 总经理
    await loginAs(page, request, 'gm01');
    await page.goto('/orders');
    const gmRows = await page.locator('[data-testid="order-row"]').count();
    expect(salesRows).toBeLessThanOrEqual(managerRows);
    expect(managerRows).toBeLessThanOrEqual(gmRows);
  });
});