/**
 * Story 10.2 · TC-10.2.A1 登录
 * 测例 ID：e2e/sprint10/tc-10.2-a1-login.spec.ts
 * 业务域：A. 认证 + 审批工作流
 * 端点覆盖：POST /auth/login
 * 角色：业务员（salesperson01）
 * 验收：AC-10.2.1 14 测例全 PASS / AC-10.2.4 与 10.1 generated token 类型衔接
 *
 * QA 商鞅设计 · 耗时预算 < 3s
 */
import { test, expect, resetDb, loginAs } from './helpers';

test.describe('Story 10.2 · TC-10.2.A1 登录', () => {
  test.beforeAll(async ({ request }) => {
    await resetDb(request);
  });

  test('1. 登录 → 跳看板 · token 注入 localStorage', async ({ page, request }) => {
    // 打开登录页
    await page.goto('/login');
    // 填表 + 提交
    await page.fill('[name=username]', 'salesperson01');
    await page.fill('[name=password]', 'sales123');
    await page.click('button[type=submit]');
    // 断言：跳转到 /dashboard
    await expect(page).toHaveURL(/\/dashboard/);
    // 断言：localStorage 有 token（消费 10.1 generated LoginResponse 类型）
    const token = await page.evaluate(() => localStorage.getItem('access_token'));
    expect(token).toBeTruthy();
    expect(token?.length).toBeGreaterThan(50); // JWT 长度
    expect(token).toMatch(/^eyJ/); // JWT 前缀
  });

  test('2. 登录 API · 强类型 token 字段', async ({ page, request }) => {
    // 直接调 helper loginAs · 验证 token 强类型链路
    const loginResp = await loginAs(page, request, 'salesperson01');
    expect(loginResp.accessToken).toBeTruthy();
    expect(loginResp.refreshToken).toBeTruthy();
    expect(loginResp.user?.username).toBe('salesperson01');
  });
});