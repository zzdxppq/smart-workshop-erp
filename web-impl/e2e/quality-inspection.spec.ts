/**
 * E2E · Story 1.27 · 品质 IQC 来料检
 * 测例 ID：e2e/quality-inspection.spec.ts
 * 角色：品检员（inspector01）
 * 验收：AC-5.7.1 — IQC 来料检 + IPQC 过程检 + OQC 成品检 + 委外检
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.27 · 品质 3 检 (AC-5.7.1)', () => {
  test('1. IQC 来料检 - 创建检验单', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'inspector01');
    await page.fill('[name=password]', 'qc123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    await page.goto('http://localhost:8082/quality/inspection');
    await page.click('button:has-text("新建检验单")');
    await page.selectOption('[name=type]', 'IQC');
    await page.fill('[name=materialCode]', 'M001');
    await page.fill('[name=qty]', '100');
    await page.fill('[name=inspector]', 'inspector01');
    await page.click('button:has-text("提交")');
    await expect(page.locator('.inspection-no')).toBeVisible();
  });

  test('2. IPQC 过程检 - 切 Tab', async ({ page }) => {
    await page.goto('http://localhost:8082/quality/inspection');
    await page.click('text=IPQC 过程检');
    await expect(page.locator('.tab-active')).toContainText('IPQC');
  });

  test('3. OQC 成品检 - 切 Tab', async ({ page }) => {
    await page.goto('http://localhost:8082/quality/inspection');
    await page.click('text=OQC 成品检');
    await expect(page.locator('.tab-active')).toContainText('OQC');
  });

  test('4. 检验报告生成', async ({ page }) => {
    await page.goto('http://localhost:8082/quality/inspection-report/1');
    await expect(page.locator('.inspection-no')).toBeVisible();
    await page.click('button:has-text("导出 PDF")');
  });
});