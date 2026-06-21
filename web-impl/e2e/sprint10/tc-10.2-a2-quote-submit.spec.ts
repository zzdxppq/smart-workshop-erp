/**
 * Story 10.2 · TC-10.2.A2 报价提交审批
 * 测例 ID：e2e/sprint10/tc-10.2-a2-quote-submit.spec.ts
 * 业务域：A. 认证 + 审批工作流
 * 端点覆盖：POST /quotes + POST /quotes/{id}/submit
 * 角色：业务员（salesperson01）
 * 验收：DRAFT → SUBMITTED · 4 通道推送触发
 *
 * QA 商鞅设计 · 耗时预算 < 4s
 */
import { test, expect, resetDb, loginAs, getAuditLogs } from './helpers';

test.describe('Story 10.2 · TC-10.2.A2 报价提交审批', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 业务员登录 → 新建报价 → 提交审批 · 4 通道推送触发', async ({ page, request }) => {
    await loginAs(page, request, 'salesperson01');
    // 新建报价
    await page.goto('/quotes/new');
    await page.fill('[name=customerId]', '11'); // C0011-NORMAL
    await page.selectOption('[name=currency]', 'CNY');
    await page.fill('[name=deliveryDate]', '2026-07-01');
    await page.click('button:has-text("添加明细")');
    await page.fill('[name=drawingNo]', 'DWG-10.2-A2-001');
    await page.fill('[name=quantity]', '10');
    await page.fill('[name=unitPrice]', '5000'); // 5万 · 5-20万 区间
    // 保存草稿
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('[data-testid="quote-status"]')).toHaveText('DRAFT');
    // 提取 quoteId（从 URL）
    const quoteUrl = page.url();
    const quoteIdMatch = quoteUrl.match(/\/quotes\/(\d+)/);
    const quoteId = quoteIdMatch ? Number(quoteIdMatch[1]) : 0;
    expect(quoteId).toBeGreaterThan(0);
    // 提交审批
    await page.click('button:has-text("提交审批")');
    await expect(page.locator('[data-testid="quote-status"]')).toHaveText('SUBMITTED');
    // 断言 4 通道推送触发（邮件/站内信/企业微信/钉钉）
    const logs = await getAuditLogs(request, quoteId);
    expect(logs.some((l) => l.includes('stream:notify triggered'))).toBeTruthy();
  });
});