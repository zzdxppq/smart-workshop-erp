/**
 * E2E · Story 1.7 · 跨模块 · FA 首件质检引用图纸
 * 测例 ID：e2e/quality-drawing-fa.spec.ts
 * 角色：品质（qa01）
 * 验收：Epic 7 品质 · FA 首件质检引用图纸
 *
 * 跨模块：Story 1.7 图纸 → Epic 7 品质 FA 首件
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.7 · 跨模块 FA 首件质检引用图纸 (Epic 7)', () => {
  test('1. FA 首件质检可引用图纸 DWG-20260612-0001', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'qa01');
    await page.fill('[name=password]', 'qa123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    // 创建 FA 首件质检
    await page.goto('http://localhost:8082/quality/fa/new');
    // 通过图号搜索引用图纸
    await page.fill('[name=drawingNoSearch]', 'DWG-20260612-0001');
    await page.click('button:has-text("搜索")');
    await page.click('.drawing-option:has-text("DWG-20260612-0001")');
    // 断言图纸信息自动带出
    await expect(page.locator('.drawing-title')).toHaveText('FA 件 - 航空精密连接器外壳');
    await expect(page.locator('.material-code')).toHaveText('WL-1001');
    // 提交
    await page.fill('[name=quantity]', '5');
    await page.click('button:has-text("提交")');
    await expect(page.locator('.fa-status')).toHaveText('PENDING_INSPECTION');
  });

  test('2. FA 首件引用 v2 版本（最新 RELEASED）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'qa01');
    await page.fill('[name=password]', 'qa123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    await page.goto('http://localhost:8082/quality/fa/new');
    await page.fill('[name=drawingNoSearch]', 'DWG-20260612-0001');
    await page.click('button:has-text("搜索")');
    // 断言显示 v2（最新）
    await expect(page.locator('.drawing-option').first()).toContainText('v2');
  });
});
