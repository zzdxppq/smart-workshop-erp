/**
 * Story 10.2 · TC-10.2.D1 入库扫码
 * 测例 ID：e2e/sprint10/tc-10.2-d1-stock-in.spec.ts
 * 业务域：D. 库存 + 报表
 * 端点覆盖：POST /stock/in
 * 角色：仓管员（warehouse01）
 * 验收：物料码校验 · 库位/批次写入
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 */
import { test, expect, resetDb, loginWithCredentials, getStock } from './helpers';

test.describe('Story 10.2 · TC-10.2.D1 入库扫码', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 仓管员扫码入库 · 库位/批次写入', async ({ page, request }) => {
    await loginWithCredentials(page, request, 'warehouse01', 'wh123', '仓管员 01', ['warehouse', 'stock:in']);
    // 进入库页
    await page.goto('/warehouse/scan');
    // 扫码 + 填库位
    await page.locator('[data-testid="barcode-input"]').fill('MAT-20260613-0001');
    await page.locator('[data-testid="location-input"]').fill('A-01-03');
    await page.fill('[name=qty]', '100');
    await page.click('[data-testid="submit"]');
    // 断言：成功提示
    await expect(page.locator('.el-message--success')).toBeVisible();
    // 断言：库存写入 + 批次记录
    const stock = await getStock(request, 'MAT-20260613-0001');
    expect(stock.qty).toBeGreaterThan(0);
    expect(stock.batchNo).toBeTruthy();
  });
});