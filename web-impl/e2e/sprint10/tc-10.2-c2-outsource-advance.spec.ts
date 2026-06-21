/**
 * Story 10.2 · TC-10.2.C2 委外状态推进
 * 测例 ID：e2e/sprint10/tc-10.2-c2-outsource-advance.spec.ts
 * 业务域：C. 委外 + 返修
 * 端点覆盖：POST /outsource-states/advance
 * 角色：仓管员（warehouse01）
 * 验收：PENDING → SENT → IN_PROGRESS · helper advanceOutsource 封装
 *
 * QA 商鞅设计 · 耗时预算 < 5s
 * architect review IMPL 注意事项：状态机跨多步推进封装
 */
import { test, expect, resetDb, loginWithCredentials, advanceOutsource, getOutsourceStatus } from './helpers';

test.describe('Story 10.2 · TC-10.2.C2 委外状态推进', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 仓管员推进委外状态 PENDING → SENT → IN_PROGRESS', async ({ page, request }) => {
    await loginWithCredentials(page, request, 'warehouse01', 'wh123', '仓管员 01', ['warehouse', 'outsource:advance']);
    // 假定已有 WW- 单号（从 C1 接力 · 此处用 mock WW-20260613-0001）
    const outsourceNo = 'WW-20260613-0001';
    // PENDING → SENT（helper 封装）
    await advanceOutsource(request, outsourceNo, 'SENT');
    expect(await getOutsourceStatus(request, outsourceNo)).toBe('SENT');
    // SENT → IN_PROGRESS
    await advanceOutsource(request, outsourceNo, 'IN_PROGRESS');
    expect(await getOutsourceStatus(request, outsourceNo)).toBe('IN_PROGRESS');
  });
});