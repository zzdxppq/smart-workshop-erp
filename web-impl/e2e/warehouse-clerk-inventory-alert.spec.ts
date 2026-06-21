import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.14 E2E · 库存预警
 */
test('warehouse clerk views inventory alert', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'warehouse')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/warehouse/inventory-alert')
  await expect(page.locator('text=CRITICAL')).toBeVisible()
  await expect(page.locator('text=库存预警中心')).toBeVisible()
})
