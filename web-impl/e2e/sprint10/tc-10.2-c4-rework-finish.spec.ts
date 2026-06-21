/**
 * Story 10.2 · TC-10.2.C4 完成返修
 * 测例 ID：e2e/sprint10/tc-10.2-c4-rework-finish.spec.ts
 * 业务域：C. 委外 + 返修
 * 端点覆盖：POST /reworks/{id}/finish
 * 角色：供应商门户（vendor_portal mock）
 * 验收：状态回 IN_PROGRESS · 委外单正常流
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 */
import { test, expect, resetDb, loginAs, getOutsourceStatus } from './helpers';

test.describe('Story 10.2 · TC-10.2.C4 完成返修', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 供应商门户完成返修 · 委外单状态回 IN_PROGRESS', async ({ page, request }) => {
    // 用 admin mock 供应商门户角色（V1.3.7 暂无独立 vendor_portal 角色）
    await loginAs(page, request, 'admin');
    // 进返修单详情（mock id=200）
    await page.goto('/reworks/200');
    // 点击"完成返修"
    await page.click('[data-testid="finish-rework"]');
    // 断言：委外单状态回 IN_PROGRESS（正常流回归）
    const status = await getOutsourceStatus(request, 'WW-20260613-0001');
    expect(status).toBe('IN_PROGRESS');
  });
});