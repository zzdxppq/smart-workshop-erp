/**
 * E2E · Story 1.7 · AC-3.1.2 · 图纸归档 + OBSOLETE 旧版本
 * 测例 ID：e2e/engineering-drawing-archive.spec.ts
 * 角色：工程师（engineer01）
 * 验收：AC-3.1.2 — 归档（RELEASED → ARCHIVED）+ 旧版本 OBSOLETE
 *
 * 4 状态机：DRAFT → RELEASED → ARCHIVED + OBSOLETE
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.7 · 图纸归档 (AC-3.1.2)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'engineer01');
    await page.fill('[name=password]', 'eng123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. 归档 RELEASED → ARCHIVED (AC-3.1.2)', async ({ page }) => {
    // 假定图纸 2（WL-1002）RELEASED
    await page.goto('http://localhost:8082/drawings/2');
    await page.click('button:has-text("归档")');
    await page.click('button:has-text("确认")');
    await expect(page.locator('.status')).toHaveText('ARCHIVED');
    // ARCHIVED 状态所有字段只读
    await expect(page.locator('button:has-text("编辑")')).toBeHidden();
    await expect(page.locator('button:has-text("归档")')).toBeHidden();
  });

  test('2. DRAFT 状态不可归档（40904 DRAWING_STATE_INVALID）', async ({ page }) => {
    // 假定图纸 3（WL-1003）DRAFT
    await page.goto('http://localhost:8082/drawings/3');
    await expect(page.locator('button:has-text("归档")')).toBeHidden();
  });

  test('3. ARCHIVED 状态可查但不可改（40903 DRAWING_NOT_EDITABLE）', async ({ page }) => {
    await page.goto('http://localhost:8082/drawings/2');
    await expect(page.locator('.status')).toHaveText('ARCHIVED');
    await expect(page.locator('button:has-text("编辑")')).toBeHidden();
  });
});
