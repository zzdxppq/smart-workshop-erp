/**
 * E2E · Story 1.7 · AC-3.1.1 · 工程师图纸 CRUD
 * 测例 ID：e2e/engineering-drawing-crud.spec.ts
 * 角色：工程师（engineer01）
 * 验收：AC-3.1.1 — 图纸 CRUD + 字段必填校验
 * V1.3.7 红线 5：所有写操作 @AuditLog AFTER_COMMIT
 *
 * 状态机：DRAFT → RELEASED → ARCHIVED + OBSOLETE
 */
import { test, expect } from '@playwright/test';

test.describe('Story 1.7 · 工程师图纸 CRUD (AC-3.1.1)', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('http://localhost:8082/login');
    await page.fill('[name=username]', 'engineer01');
    await page.fill('[name=password]', 'eng123');
    await page.click('button[type=submit]');
    await page.waitForURL('**/dashboard');
  });

  test('1. 创建图纸 → DWG 单号 → 修改 → 列表 (AC-3.1.1)', async ({ page }) => {
    await page.goto('http://localhost:8082/drawings/new');
    await page.fill('[name=title]', '航空精密连接器外壳');
    await page.fill('[name=materialCode]', 'WL-1001');
    await page.fill('[name=processRoute]', '[{"step":1,"name":"车削","cost":120.50},{"step":2,"name":"铣削","cost":80.00},{"step":3,"name":"热处理","cost":200.00},{"step":4,"name":"精磨","cost":150.00},{"step":5,"name":"表面处理","cost":90.00}]');
    await page.check('[name=isFa]');
    await page.check('[name=isNew]');
    await page.fill('[name=comment]', 'test 1.7');
    await page.click('button:has-text("保存为草稿")');
    // 断言图号 DWG-YYYYMMDD-NNNN
    await expect(page.locator('.drawing-no')).toContainText(/^DWG-\d{8}-\d{4}$/);
    // 断言 status=DRAFT
    await expect(page.locator('.status')).toHaveText('DRAFT');
    // 修改（DRAFT 状态可改）
    await page.click('button:has-text("编辑")');
    await page.fill('[name=comment]', 'updated 1.7');
    await page.click('button:has-text("保存")');
    await expect(page.locator('.status')).toHaveText('DRAFT');
    // 列表查询
    await page.goto('http://localhost:8082/drawings?status=DRAFT');
    await expect(page.locator('.drawing-row').first()).toContainText(/^DWG-\d{8}-\d{4}$/);
  });

  test('2. 字段必填校验失败 (AC-3.1.1)', async ({ page }) => {
    await page.goto('http://localhost:8082/drawings/new');
    // 不填 materialCode 直接保存
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('.error-materialCode')).toBeVisible();
    // materialCode 格式错误
    await page.fill('[name=materialCode]', 'XX-1234');
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('.error-materialCode')).toContainText('WL-\\d{4}');
  });

  test('3. 非 DRAFT 状态不可编辑 (AC-3.1.1 40903)', async ({ page }) => {
    await page.goto('http://localhost:8082/drawings/1/edit');
    await expect(page.locator('.status')).toHaveText('RELEASED');
    await expect(page.locator('button:has-text("编辑")')).toBeHidden();
  });

  test('4. 物料编码唯一性 40905 (P1 修补 1)', async ({ page }) => {
    // 创建图纸 A 物料编码 WL-9999
    await page.goto('http://localhost:8082/drawings/new');
    await page.fill('[name=title]', '图纸 A');
    await page.fill('[name=materialCode]', 'WL-9999');
    await page.fill('[name=processRoute]', '[{"step":1,"name":"车削","cost":50}]');
    await page.click('button:has-text("保存为草稿")');
    // 重新创建同物料编码
    await page.goto('http://localhost:8082/drawings/new');
    await page.fill('[name=title]', '图纸 B');
    await page.fill('[name=materialCode]', 'WL-9999');
    await page.fill('[name=processRoute]', '[{"step":1,"name":"车削","cost":50}]');
    await page.click('button:has-text("保存为草稿")');
    await expect(page.locator('.error-materialCode')).toContainText('MATERIAL_CODE_DUPLICATE');
  });
});
