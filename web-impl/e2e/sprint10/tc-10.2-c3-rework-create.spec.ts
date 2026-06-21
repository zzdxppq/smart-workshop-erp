/**
 * Story 10.2 · TC-10.2.C3 发起返修
 * 测例 ID：e2e/sprint10/tc-10.2-c3-rework-create.spec.ts
 * 业务域：C. 委外 + 返修
 * 端点覆盖：POST /reworks
 * 角色：品管（qa01）
 * 验收：返修单创建 · 7 状态衍生态 NOTIFIED_REPAIR
 *
 * QA 商鞅设计 · 耗时预算 < 5s
 */
import { test, expect, resetDb, loginWithCredentials, getOutsourceStatus } from './helpers';

test.describe('Story 10.2 · TC-10.2.C3 发起返修', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 品管发起返修 · 委外单衍生态 NOTIFIED_REPAIR', async ({ page, request }) => {
    await loginWithCredentials(page, request, 'qa01', 'qa123', '品管 01', ['qa', 'rework:create']);
    // 假定 C2 已推进到 IN_PROGRESS · 用 mock WW-20260613-0001
    const outsourceNo = 'WW-20260613-0001';
    // 进质量检验页
    await page.goto('/quality/rework/new');
    await page.fill('[name=outsourceNo]', outsourceNo);
    await page.fill('[name=reason]', '尺寸超差');
    await page.click('button:has-text("发起返修")');
    // 断言：返修单号可见
    await expect(page.locator('[data-testid="rework-no"]')).toBeVisible();
    // 断言：委外单衍生态 NOTIFIED_REPAIR（V1.3.7 1.22 委外深化）
    const status = await getOutsourceStatus(request, outsourceNo);
    expect(status).toBe('NOTIFIED_REPAIR');
  });
});