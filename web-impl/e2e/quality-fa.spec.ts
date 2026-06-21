/**
 * E2E · Story 1.28 · FA 首件检
 * 测例 ID：e2e/quality-fa.spec.ts
 * 角色：品检员（inspector01）
 * 验收：AC-5.7.2 — FA 首件检验 + 报告
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.28 · FA 首件检 (AC-5.7.2)', () => {
  test('1. 查看 FA 首件列表', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'inspector01');
    await page.fill('[name=password]', 'qc123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    await page.goto('http://localhost:8082/quality/fa');
    await expect(page.locator('text=FA 首件检')).toBeVisible();
  });

  test('2. FA 首件详情', async ({ page }) => {
    await page.goto('http://localhost:8082/quality/fa-detail/1');
    await expect(page.locator('.fa-no')).toBeVisible();
  });

  test('3. FA 首件报告', async ({ page }) => {
    await page.goto('http://localhost:8082/quality/fa-report/1');
    await expect(page.locator('.fa-no')).toBeVisible();
    await page.click('button:has-text("导出 PDF")');
  });
});