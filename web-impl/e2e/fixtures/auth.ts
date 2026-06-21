/**
 * E2E auth fixture · Story 1.5
 *
 * - 6 套预置账号，覆盖 Story 1.5 所有审批分支
 * - 登录走 backend POST /api/v1/auth/login（Story 1.1 JWT）
 * - 登录后 localStorage 注入 access_token + user_info，Vite SPA 自动带上
 * - 通过 Playwright `test.use({ storageState })` 跨用例复用，避免重复登录
 */
import { test as base, expect, type Page, type APIRequestContext } from '@playwright/test';
import path from 'node:path';
import fs from 'node:fs';

export type Role =
  | 'salesperson01'
  | 'dept_manager01'
  | 'gm01'
  | 'finance_director01'
  | 'finance01'
  | 'admin';

export interface UserAccount {
  username: Role;
  password: string;
  displayName: string;
  roles: string[];
  /** V1.3.7 红线 2：> 20万二次密码的 admin 密码（admin01 持有） */
  adminPassword?: string;
}

/** V1.3.7 红线 1/2 预置 6 套账号 · 与 backend/db/seed 30 客户种子一致 */
export const USERS: Record<Role, UserAccount> = {
  salesperson01: {
    username: 'salesperson01',
    password: 'sales123',
    displayName: '业务员 01',
    roles: ['salesperson', 'quote:read', 'quote:create', 'quote:update'],
  },
  dept_manager01: {
    username: 'dept_manager01',
    password: 'mgr123',
    displayName: '部门经理 01',
    roles: ['dept_manager', 'quote:read', 'quote:approve', 'quote:reject'],
  },
  gm01: {
    username: 'gm01',
    password: 'gm123',
    displayName: '总经理 01',
    roles: ['gm', 'quote:read', 'quote:approve', 'quote:reject'],
    adminPassword: 'admin123',
  },
  finance_director01: {
    username: 'finance_director01',
    password: 'fin123',
    displayName: '财务总监 01',
    roles: ['finance_director', 'quote:read', 'quote:approve'],
  },
  finance01: {
    username: 'finance01',
    password: 'fin123',
    displayName: '财务 01',
    roles: ['finance', 'quote:read', 'quote:export'],
  },
  admin: {
    username: 'admin',
    password: 'admin123',
    displayName: '系统管理员',
    roles: ['admin', 'quote:*', 'customer:read', 'customer:write'],
  },
};

/** storageState 目录：每个角色一份 JSON，Playwright 跨用例复用 */
const STORAGE_DIR = path.resolve(__dirname, '../.auth');
fs.mkdirSync(STORAGE_DIR, { recursive: true });

/** 调 backend 登录 API 拿 JWT · Story 1.1 复用 */
async function loginViaApi(
  request: APIRequestContext,
  account: UserAccount,
): Promise<{ accessToken: string; refreshToken: string; userId: number }> {
  const res = await request.post('/api/v1/auth/login', {
    data: { username: account.username, password: account.password },
  });
  expect(res.ok(), `login ${account.username} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return {
    accessToken: body.data.accessToken,
    refreshToken: body.data.refreshToken,
    userId: body.data.userId,
  };
}

/** 写一份 localStorage 模拟 SPA 登录态 */
async function writeStorageState(
  page: Page,
  account: UserAccount,
  tokens: { accessToken: string; refreshToken: string; userId: number },
): Promise<void> {
  await page.goto('/login');
  await page.evaluate(
    ({ tokens, account }) => {
      localStorage.setItem('access_token', tokens.accessToken);
      localStorage.setItem('refresh_token', tokens.refreshToken);
      localStorage.setItem('user_info', JSON.stringify({
        userId: tokens.userId,
        username: account.username,
        displayName: account.displayName,
        roles: account.roles,
      }));
    },
    { tokens, account },
  );
}

/** 主 fixture：提供 loginAs(role) → 已登录 Page + storageState 写入 */
type AuthFixtures = {
  loginAs: (role: Role) => Promise<Page>;
  freshLogin: (role: Role) => Promise<Page>;
  storageStatePath: (role: Role) => string;
};

export const test = base.extend<AuthFixtures>({
  storageStatePath: async ({}, use) => {
    await use((role: Role) => path.join(STORAGE_DIR, `${role}.json`));
  },

  loginAs: async ({ page, request, storageStatePath }, use) => {
    /** 默认登录 salesperson01，可被 test.describe.configure 覆盖 */
    let currentRole: Role = 'salesperson01';
    const login = async (role: Role = currentRole): Promise<Page> => {
      const statePath = storageStatePath(role);
      if (fs.existsSync(statePath)) {
        await page.context().addCookies(
          JSON.parse(fs.readFileSync(statePath, 'utf-8')).cookies ?? [],
        );
      }
      const account = USERS[role];
      const tokens = await loginViaApi(request, account);
      await writeStorageState(page, account, tokens);
      fs.writeFileSync(
        statePath,
        JSON.stringify(await page.context().storageState(), null, 2),
      );
      currentRole = role;
      return page;
    };
    await use(login as Parameters<typeof use>[0] extends (fn: infer F) => any ? F : never);
  },

  freshLogin: async ({ browser, request, storageStatePath }, use) => {
    await use(async (role: Role) => {
      const account = USERS[role];
      const tokens = await loginViaApi(request, account);
      const ctx = await browser.newContext();
      const page = await ctx.newPage();
      await writeStorageState(page, account, tokens);
      const statePath = storageStatePath(role);
      fs.writeFileSync(
        statePath,
        JSON.stringify(await ctx.storageState(), null, 2),
      );
      return page;
    });
  },
});

export { expect };
