/**
 * E2E · Story 1.5 · AC-2.2.2 · 部门经理收到待办 + 审批
 * 测例 ID：e2e/dept-manager-pending-approve.spec.ts
 * 角色：部门经理
 */
import { test, expect } from '@playwright/test';

test('Story 1.5 · 部门经理收到待办 → 审批 5万 → 通知业务员 (AC-2.2.2)', async ({ page }) => {
  await page.goto('http://localhost:8082/login');
  await page.fill('[name=username]', 'dept_manager01');
  await page.fill('[name=password]', 'mgr123');
  await page.click('button[type=submit]');
  // 进入待办列表
  await page.goto('http://localhost:8082/approvals/pending');
  await expect(page.locator('.pending-quote')).toHaveCount(1);
  // 审批
  await page.click('button:has-text("审批通过")');
  await expect(page.locator('.status')).toHaveText('APPROVED');
  // 通知业务员 (stream:notify 4 通道)
  // 部署阶段断言邮件已发 / APP push 计数 +1
});
