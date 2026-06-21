/**
 * E2E · Story 1.46 · 生产工作台
 * 测例 ID：e2e/dashboard-production.spec.ts
 * 角色：生管（pm01）
 * 验收：AC-5.10.1 — 生产看板：实时统计 + 工单 + 告警
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.46 · 生产工作台 (AC-5.10.1)', () => {
  test('1. 生管查看生产工作台', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'pm01');
    await page.fill('[name=password]', 'pm123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');

    await page.goto('http://localhost:8082/dashboard/production');
    await expect(page.locator('text=生产工作台')).toBeVisible();
    // 4 个 KPI 卡片
    await expect(page.locator('.kpi-card')).toHaveCount(4);
  });

  test('2. 工单详情', async ({ page }) => {
    await page.goto('http://localhost:8082/dashboard/workorder-detail/1');
    await expect(page.locator('.workorder-no')).toBeVisible();
  });

  test('3. 告警中心', async ({ page }) => {
    await page.goto('http://localhost:8082/dashboard/alerts');
    await expect(page.locator('text=生产告警中心')).toBeVisible();
  });

  test('4. 销售看板', async ({ page }) => {
    await page.goto('http://localhost:8082/dashboard/sales');
    await expect(page.locator('text=销售看板')).toBeVisible();
  });

  test('5. 财务看板', async ({ page }) => {
    await page.goto('http://localhost:8082/dashboard/finance');
    await expect(page.locator('text=财务看板')).toBeVisible();
  });

  test('6. 品质看板', async ({ page }) => {
    await page.goto('http://localhost:8082/dashboard/quality');
    await expect(page.locator('text=品质看板')).toBeVisible();
  });
});