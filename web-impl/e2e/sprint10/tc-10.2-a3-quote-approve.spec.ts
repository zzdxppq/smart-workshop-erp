/**
 * Story 10.2 · TC-10.2.A3 报价审批通过
 * 测例 ID：e2e/sprint10/tc-10.2-a3-quote-approve.spec.ts
 * 业务域：A. 认证 + 审批工作流
 * 端点覆盖：GET /approvals/pending + POST /quotes/{id}/approve
 * 角色：部门经理（dept_manager01）
 * 验收：SUBMITTED → APPROVED · 金额 < 20万 无二次密码弹窗
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 * architect review IMPL 注意事项 3：二次密码弹窗显式等待 5s
 */
import { test, expect, resetDb, loginAs } from './helpers';

test.describe('Story 10.2 · TC-10.2.A3 报价审批通过', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 部门经理审批 < 20万 报价 · 无二次密码弹窗', async ({ page, request }) => {
    await loginAs(page, request, 'dept_manager01');
    // 进待办列表
    await page.goto('/approvals/pending');
    // 找到一条 SUBMITTED 报价（金额 < 20万）
    const firstPending = page.locator('[data-testid="approval-row"]').first();
    await expect(firstPending).toBeVisible();
    // 进入审批
    await firstPending.locator('button:has-text("审批")').click();
    // 审批通过
    await page.click('[data-testid="approve-btn"]');
    // 断言：SUBMITTED → APPROVED
    await expect(page.locator('[data-testid="quote-status"]')).toHaveText('APPROVED');
    // 断言：二次密码弹窗不出现（金额 < 20万）
    await expect(page.locator('[data-testid="high-amount-confirm"]')).not.toBeVisible();
  });
});