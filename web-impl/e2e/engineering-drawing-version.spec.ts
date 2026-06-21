/**
 * E2E · Story 1.7 · AC-3.1.2 · 图纸版本 v1→v2→v3 严格递增
 * 测例 ID：e2e/engineering-drawing-version.spec.ts
 * 角色：工程师（engineer01）
 * 验收：AC-3.1.2 — 版本管理 + V1.3.7 P1 修补 2（严格递增步进 1）
 *
 * P1 修补 2：版本号严格递增 v1 < v2 < v3（步进 1，禁止跳跃 v1→v3）
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.7 · 图纸版本严格递增 (AC-3.1.2 · P1 修补 2)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'engineer01');
    await page.fill('[name=password]', 'eng123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. 正常递增 v1 → v2 → v3 (AC-3.1.2)', async ({ page }) => {
    // 创建图纸 v1
    await page.goto('http://localhost:8082/drawings/1');
    await page.click('button:has-text("新增版本")');
    await page.fill('[name=version]', 'v2');
    await page.fill('[name=changeReason]', '客户反馈修改法兰尺寸');
    await page.fill('[name=pdfPath]', '/data/pdf/dwg-0001-v2.pdf');
    await page.click('button:has-text("保存")');
    await expect(page.locator('.current-version')).toHaveText('v2');
    // 继续 v3
    await page.click('button:has-text("新增版本")');
    await page.fill('[name=version]', 'v3');
    await page.fill('[name=changeReason]', '公差调整');
    await page.click('button:has-text("保存")');
    await expect(page.locator('.current-version')).toHaveText('v3');
    // 版本列表 3 条
    await page.goto('http://localhost:8082/drawings/1/versions');
    await expect(page.locator('.version-row')).toHaveCount(3);
  });

  test('2. 禁止跳跃 v1 → v3 (P1 修补 2 · 40904 VERSION_NOT_STRICTLY_INCREASING)', async ({ page }) => {
    // 假定图纸 1 当前版本 v1
    await page.goto('http://localhost:8082/drawings/1');
    await page.click('button:has-text("新增版本")');
    await page.fill('[name=version]', 'v3');  // 跳跃 v1→v3
    await page.fill('[name=changeReason]', '跳跃测试');
    await page.click('button:has-text("保存")');
    await expect(page.locator('.error-version')).toContainText('VERSION_NOT_STRICTLY_INCREASING');
  });

  test('3. 旧版本自动 OBSOLETE (AC-3.1.2)', async ({ page }) => {
    // v1 → v2 发布后，v1 应自动标记 OBSOLETE
    await page.goto('http://localhost:8082/drawings/1/versions');
    await expect(page.locator('.version-row:has-text("v1") .status')).toHaveText('OBSOLETE');
    await expect(page.locator('.version-row:has-text("v2") .status')).toHaveText('ACTIVE');
  });
});
