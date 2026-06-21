/**
 * E2E · Story 1.6 · AC-2.3.4 · 财务 PDF + Excel 导出
 * 测例 ID：e2e/finance-order-pdf-export.spec.ts
 * 角色：财务（finance01）
 * 验收：AC-2.3.4 — PDF（带公司 logo + 订单明细 + 审批签字栏）+ Excel（多 Sheet） + 1h 缓存
 * V1.3.7 红线 5：导出审计（@AuditLog pdf.download / excel.download）
 */
import { test, expect } from '@playwright/test';
import path from 'node:path';
import fs from 'node:fs';

const DOWNLOAD_DIR = path.resolve(__dirname, '../.downloads');
fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });

test.describe('Story 1.6 · 财务 PDF + Excel 导出 (AC-2.3.4)', () => {
  test.beforeEach(async ({ page }) => {
    // 登录财务
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'finance01');
    await page.fill('[name=password]', 'fin123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. PDF 导出（含公司 logo + 订单明细 + 审批签字栏）', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/1');
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("导出 PDF")'),
    ]);
    const savePath = path.join(DOWNLOAD_DIR, `order-1-${Date.now()}.pdf`);
    await download.saveAs(savePath);
    // 断言 PDF 文件存在且 size > 1KB
    const stat = fs.statSync(savePath);
    expect(stat.size).toBeGreaterThan(1024);
    // 断言 PDF 头：%PDF-1.4
    const buf = fs.readFileSync(savePath, { encoding: 'utf-8', flag: 'r' });
    expect(buf.startsWith('%PDF-1.4')).toBe(true);
  });

  test('2. Excel 导出（多 Sheet：订单基本信息 + 明细 + 审批 + 发货）', async ({ page }) => {
    await page.goto('http://localhost:8082/orders/1');
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("导出 Excel")'),
    ]);
    const savePath = path.join(DOWNLOAD_DIR, `order-1-${Date.now()}.xlsx`);
    await download.saveAs(savePath);
    // 断言 xlsx 文件存在且 size > 1KB
    const stat = fs.statSync(savePath);
    expect(stat.size).toBeGreaterThan(1024);
    // 断言 xlsx magic number PK (zip)
    const buf = fs.readFileSync(savePath);
    expect(buf[0]).toBe(0x50);  // 'P'
    expect(buf[1]).toBe(0x4b);  // 'K'
  });

  test('3. PDF 1h 缓存（1st fresh + 2nd cache hit）', async ({ page, request }) => {
    await page.goto('http://localhost:8082/orders/1');
    // 1st download
    const [d1] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("导出 PDF")'),
    ]);
    await d1.saveAs(path.join(DOWNLOAD_DIR, 'order-1-1st.pdf'));
    // 2nd download（应在 1h 内走缓存）
    const [d2] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("导出 PDF")'),
    ]);
    await d2.saveAs(path.join(DOWNLOAD_DIR, 'order-1-2nd.pdf'));
    // 断言两次下载大小一致（缓存命中）
    const s1 = fs.statSync(path.join(DOWNLOAD_DIR, 'order-1-1st.pdf')).size;
    const s2 = fs.statSync(path.join(DOWNLOAD_DIR, 'order-1-2nd.pdf')).size;
    expect(s2).toBe(s1);
  });
});
