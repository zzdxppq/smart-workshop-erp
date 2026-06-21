import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.11 E2E · 物料条码生成
 */
test('salesperson generates material barcode', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'salesperson')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/material/barcode-generate')
  await page.click('input[placeholder="选择物料"]')
  await page.click('text=WL-0001')

  await page.click('button:has-text("生成条码")')

  await expect(page.locator('.el-message--success')).toBeVisible()
})
