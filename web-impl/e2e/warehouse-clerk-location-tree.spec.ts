import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.13 E2E · 库位树查看
 */
test('warehouse clerk views location tree', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'warehouse')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/warehouse/locations')
  await expect(page.locator('text=WH-A')).toBeVisible()
  await expect(page.locator('.el-tree')).toBeVisible()
})
