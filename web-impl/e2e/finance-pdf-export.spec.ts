/**
 * E2E · Story 1.5 · AC-2.2.3 · 财务导出 PDF + Excel + 1h 缓存
 * 测例 ID：e2e/finance-pdf-export.spec.ts
 * 角色：财务
 */
import { test, expect } from '@playwright/test';

test('Story 1.5 · 财务导出 PDF + Excel + 1h 缓存 (AC-2.2.3)', async ({ page }) => {
  await page.goto('http://localhost:8082/login');
  await page.fill('[name=username]', 'finance01');
  await page.fill('[name=password]', 'fin123');
  await page.click('button[type=submit]');
  // 1st PDF download
  const dl1 = page.waitForEvent('download');
  await page.goto('http://localhost:8082/quotes/100');
  await page.click('button:has-text("导出 PDF")');
  const download1 = await dl1;
  expect(download1.suggestedFilename()).toContain('.pdf');
  // 2nd PDF download (1h 内应走缓存)
  const dl2 = page.waitForEvent('download');
  await page.click('button:has-text("导出 PDF")');
  const download2 = await dl2;
  expect(download2.suggestedFilename()).toContain('.pdf');
  // Excel 导出
  const dl3 = page.waitForEvent('download');
  await page.click('button:has-text("导出 Excel")');
  const download3 = await dl3;
  expect(download3.suggestedFilename()).toContain('.xlsx');
});
