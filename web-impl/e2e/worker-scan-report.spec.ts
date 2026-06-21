import { test, expect } from '@playwright/test'

/**
 * PRD 对齐：扫码三码/报工为 Android APP 功能，Web 旧 URL 应跳转提示页。
 */
test('production scan URL redirects to app-only hint', async ({ page }) => {
  await page.goto('/login')
  await page.fill('input[name="username"]', 'worker')
  await page.fill('input[name="password"]', 'pass')
  await page.click('button[type="submit"]')

  await page.goto('/production/scan-workorder')
  await expect(page.getByText('请在 Android APP 操作')).toBeVisible()
  await expect(page.getByText('扫码三码')).toBeVisible()
})
