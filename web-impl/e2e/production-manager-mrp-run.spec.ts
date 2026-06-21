import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.17 E2E · 生管运行 MRP
 */
test('production manager runs MRP', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'production')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/production/mrp')
  await page.click('button:has-text("运行 MRP")')

  await expect(page.locator('text=运行结果')).toBeVisible()
})
