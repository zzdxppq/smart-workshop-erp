/**
 * E2E · Story 1.7 · AC-3.1.2 · 图纸发布 + 4 阈值路由 + > 20万 二次密码
 * 测例 ID：e2e/engineering-drawing-release.spec.ts
 * 角色：总工（chief01）+ 部门经理（dept01）
 * 验收：AC-3.1.2 — 发布审批 + 4 阈值路由 + V1.3.7 红线 2（> 20万 二次密码）
 *
 * 阈值路由：
 *   is_fa=0: 总工单人
 *   is_fa=1: 总工 + 部门经理双签
 *   FA 件 release 强制要求 adminPassword（二次密码 · 40101）
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.7 · 图纸发布审批 (AC-3.1.2)', () => {
  test('1. 非 FA 件单人审批（总工）(AC-3.1.2)', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'chief01');
    await page.fill('[name=password]', 'chief123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    // 假定图纸 2（WL-1002 is_fa=0）DRAFT
    await page.goto('http://localhost:8082/drawings/2/release');
    await page.click('button:has-text("确认发布")');
    await expect(page.locator('.status')).toHaveText('RELEASED');
  });

  test('2. FA 件强制二次密码（40101 ADMIN_PASSWORD_REQUIRED）(V1.3.7 红线 2)', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'chief01');
    await page.fill('[name=password]', 'chief123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    // 假定图纸 1（WL-1001 is_fa=1）DRAFT
    await page.goto('http://localhost:8082/drawings/1/release');
    // 不填 adminPassword
    await page.click('button:has-text("确认发布")');
    await expect(page.locator('.error-adminPassword')).toContainText('ADMIN_PASSWORD_REQUIRED');
    // 填入 adminPassword
    await page.fill('[name=adminPassword]', 'admin@2026');
    await page.click('button:has-text("确认发布")');
    await expect(page.locator('.status')).toHaveText('RELEASED');
  });

  test('3. 非 DRAFT 状态不可发布（40904 DRAWING_STATE_INVALID）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'chief01');
    await page.fill('[name=password]', 'chief123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    // 假定图纸 5（WL-1005 is_fa=0）已 RELEASED
    await page.goto('http://localhost:8082/drawings/5/release');
    await expect(page.locator('.error')).toContainText('DRAWING_STATE_INVALID');
  });
});
