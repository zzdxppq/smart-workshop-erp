import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.12 E2E · 仓管员扫码入库
 */
test('warehouse clerk scan inbound', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'warehouse')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/warehouse/scan')
  await page.fill('input[placeholder*="BC"]', 'BC20260612-0001')
  await page.click('input[placeholder="选择库位"]')
  await page.click('text=LOC-A01-01-01')

  await page.click('button:has-text("扫码入库")')

  await expect(page.locator('.el-message--success')).toBeVisible()
})
