import { test, expect } from '@playwright/test'

/**
 * V1.3.7 Story 1.15 E2E · 生管创建工单
 */
test('production manager creates workorder', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'production')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/production/workorder-create')
  await page.fill('input[placeholder="CP-0001"]', 'CP-0001')
  await page.fill('input[placeholder=""]', '齿轮减速机 BWD4')
  await page.click('button:has-text("创建")')

  await expect(page.locator('.el-message--success')).toBeVisible()
})
