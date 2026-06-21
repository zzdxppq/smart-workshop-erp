import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.18 E2E · 生管委外下单（无厂商下拉 · V1.3.7 AD-1）
 */
test('production manager creates outsource (no vendor dropdown)', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'production')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/production/outsource-create')
  // 验证 AD-1：生管视图无厂商下拉框
  await expect(page.locator('text=V1.3.7 AD-1')).toBeVisible()
  await expect(page.locator('input[placeholder="选择供应商"]')).not.toBeVisible()

  await page.fill('input[placeholder=""] >> nth=0', 'GD20260612-0001')
  await page.click('button:has-text("创建")')

  await expect(page.locator('.el-message--success')).toBeVisible()
})
