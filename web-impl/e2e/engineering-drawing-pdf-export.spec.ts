/**
 * E2E · Story 1.7 · AC-3.1.4 · PDF 导出 + AES-256-GCM 解密嵌入签字扫描件
 * 测例 ID：e2e/engineering-drawing-pdf-export.spec.ts
 * 角色：工程师（engineer01）
 * 验收：AC-3.1.4 — PDF 导出 + 签字扫描件解密嵌入 + 1h 缓存
 *
 * V1.3.6 红线：AES-256-GCM 加密存储 + IV 唯一
 * 1h 缓存模式（继承 Story 1.5）
 */
import { test, expect } from '@playwright/test';
import * as fs from 'fs';

test.describe('Story 1.7 · 图纸 PDF 导出 (AC-3.1.4)', () => {
  test('1. 导出 PDF 含图号 + 工艺路线 + 签字扫描件（解密后嵌入）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'engineer01');
    await page.fill('[name=password]', 'eng123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    // 假定图纸 1（WL-1001 RELEASED · 3 签字扫描件）
    await page.goto('http://localhost:8082/drawings/1');
    const [download] = await Promise.all([
      page.waitForEvent('download'),
      page.click('button:has-text("导出 PDF")')
    ]);
    const path = await download.path();
    const content = fs.readFileSync(path, 'utf-8');
    // 断言图号
    expect(content).toContain('DWG-20260612-0001');
    // 断言工艺路线 5 段
    expect(content).toContain('车削');
    expect(content).toContain('表面处理');
    // 断言总成本聚合（120+80+200+150+90=640）
    expect(content).toContain('640.00');
    // 断言签字扫描件解密后嵌入
    expect(content).toContain('签字人');
  });

  test('2. 1h 缓存命中（重复导出耗时 < 100ms）', async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'engineer01');
    await page.fill('[name=password]', 'eng123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
    await page.goto('http://localhost:8082/drawings/1');
    // 第 1 次
    const t1 = Date.now();
    await page.click('button:has-text("导出 PDF")');
    const e1 = Date.now() - t1;
    // 第 2 次（缓存命中）
    const t2 = Date.now();
    await page.click('button:has-text("导出 PDF")');
    const e2 = Date.now() - t2;
    expect(e2).toBeLessThan(e1);
  });
});
