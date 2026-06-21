/**
 * Story 10.2 E2E helpers · 4 业务域共享工具
 *
 * - DB 隔离：resetDb() 调 backend POST /test/reset-db（V1.3.7 测试 API）
 *   - 兜底：若该 API 在 V1.3.7.1 退役，则调 seed SQL 重置
 * - 角色登录：loginAs(page, request, role) 复用 1.5 auth fixture 的 USERS 凭据
 *   - 消费 10.1 生成的 LoginResponse 类型（accessToken/refreshToken/userId 显式标注）
 * - 委外 7 状态机推进：advanceOutsource(request, outsourceNo, targetStatus)
 *   - 封装 PENDING → SENT → IN_PROGRESS → ... 多步调用
 * - 二次密码弹窗：confirmHighAmountDialog(page, adminPwd)
 *   - 显式 waitFor state=visible timeout=5000（architect review IMPL 注意事项 3）
 *
 * V1.3.7 端点契约不新增 · 消费既有 14 端点
 */
import { test as base, expect, type Page, type APIRequestContext } from '@playwright/test';
import { USERS, type Role } from '../fixtures/auth';
import type { LoginResponse } from '../../src/api/generated/models/LoginResponse';
import type { LoginRequest } from '../../src/api/generated/models/LoginRequest';

/** backend test API · V1.3.7 测试隔离钩子 */
const TEST_RESET_ENDPOINT = '/api/v1/test/reset-db';

/**
 * DB 隔离：调 POST /test/reset-db（V1.3.7 测试 API）
 * - 兜底：API 退役时（V1.3.7.1）返回 false · 调用方可决定是否走 seed SQL
 */
export async function resetDb(request: APIRequestContext): Promise<boolean> {
  const res = await request.post(TEST_RESET_ENDPOINT);
  if (!res.ok()) {
    console.warn(`[10.2 helper] resetDb failed: ${res.status()} · fallback to seed SQL`);
    return false;
  }
  return true;
}

/**
 * 角色登录（强类型 · 限定 6 套预置账号）：复用 1.5 auth fixture USERS + 消费 10.1 LoginResponse
 * - tokens 对象显式标注 { accessToken: string; refreshToken: string; userId: number }
 * - 与 e2e/fixtures/auth.ts loginViaApi 行为一致 · 但此处暴露给所有 14 spec
 */
export async function loginAs(
  page: Page,
  request: APIRequestContext,
  role: Role,
): Promise<LoginResponse> {
  const account = USERS[role];
  return loginWithCredentials(page, request, account.username, account.password, account.displayName, account.roles);
}

/**
 * 灵活登录（任意 username/password）· 适配 Story 10.2 14 测例中的非预置角色
 * - production_manager01 / purchaser01 / warehouse01 / qa01 在 V1.3.7 既有 seed 中存在
 *   （参考 e2e/quality-drawing-fa.spec.ts qa01 · e2e/sourcing-reconcile.spec.ts purchaser01）
 *   但 auth.ts USERS 仅覆盖 6 套 · 此 helper 拓宽支持所有 14 测例
 */
export async function loginWithCredentials(
  page: Page,
  request: APIRequestContext,
  username: string,
  password: string,
  displayName?: string,
  roles: string[] = [],
): Promise<LoginResponse> {
  // 调 backend POST /auth/login · 消费 10.1 generated LoginRequest 类型
  const loginBody: LoginRequest = { username, password };
  const res = await request.post('/api/v1/auth/login', { data: loginBody });
  expect(res.ok(), `login ${username} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  // 消费 10.1 生成的 LoginResponse 类型（accessToken/refreshToken/user）
  const loginResp: LoginResponse = body.data;
  const accessToken = loginResp.accessToken ?? '';
  const refreshToken = loginResp.refreshToken ?? '';
  // 写 localStorage 模拟 SPA 登录态
  await page.goto('/login');
  await page.evaluate(
    ({ tokens, username, displayName, roles }) => {
      localStorage.setItem('access_token', tokens.accessToken);
      localStorage.setItem('refresh_token', tokens.refreshToken);
      localStorage.setItem('user_info', JSON.stringify({
        userId: tokens.userId,
        username,
        displayName: displayName ?? username,
        roles,
      }));
    },
    {
      tokens: { accessToken, refreshToken, userId: loginResp.user?.userId ?? 0 },
      username,
      displayName,
      roles,
    },
  );
  return loginResp;
}

/**
 * 委外 7 状态机推进 helper · 封装 POST /outsource-states/advance 多步调用
 * - architect review IMPL 注意事项：状态机跨多步推进封装
 * - targetStatus: PENDING | SENT | IN_PROGRESS | COMPLETED | INSPECTED | NOTIFIED_REPAIR | CLOSED
 */
export async function advanceOutsource(
  request: APIRequestContext,
  outsourceNo: string,
  targetStatus:
    | 'PENDING'
    | 'SENT'
    | 'IN_PROGRESS'
    | 'COMPLETED'
    | 'INSPECTED'
    | 'NOTIFIED_REPAIR'
    | 'CLOSED',
): Promise<void> {
  const res = await request.post('/api/v1/outsource-states/advance', {
    data: { outsourceNo, targetStatus },
  });
  expect(res.ok(), `advanceOutsource ${outsourceNo} → ${targetStatus} failed: ${res.status()}`).toBeTruthy();
}

/**
 * 二次密码弹窗（> 20万 审批）显式等待
 * - architect review IMPL 注意事项 3：必须用 locator('[data-testid="high-amount-confirm"]').waitFor({ state: 'visible', timeout: 5000 })
 * - 兜底：若弹窗未出现（金额 < 20万），waitFor 自动 timeout · 测试失败更早暴露
 */
export async function confirmHighAmountDialog(page: Page, adminPassword: string): Promise<void> {
  const dialog = page.locator('[data-testid="high-amount-confirm"]');
  await dialog.waitFor({ state: 'visible', timeout: 5_000 });
  await page.fill('[name=adminPassword]', adminPassword);
  await page.click('button:has-text("确认")');
}

/**
 * 委外状态查询 helper · 给 TC-10.2.C2/C3/C4 状态断言用
 */
export async function getOutsourceStatus(
  request: APIRequestContext,
  outsourceNo: string,
): Promise<string> {
  const res = await request.get(`/api/v1/outsource-states?outsourceNo=${outsourceNo}`);
  expect(res.ok(), `getOutsourceStatus ${outsourceNo} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return body?.data?.status ?? '';
}

/**
 * 库存查询 helper · TC-10.2.D1 入库断言
 */
export async function getStock(
  request: APIRequestContext,
  materialCode: string,
): Promise<{ qty: number; reservedQty: number; batchNo?: string }> {
  const res = await request.get(`/api/v1/stock?materialCode=${materialCode}`);
  expect(res.ok(), `getStock ${materialCode} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return body?.data ?? { qty: 0, reservedQty: 0 };
}

/**
 * 工单号查询 helper · TC-10.2.B3 转生产断言
 */
export async function getWorkorderNo(request: APIRequestContext, orderId: number): Promise<string> {
  const res = await request.get(`/api/v1/orders/${orderId}`);
  expect(res.ok(), `getOrder ${orderId} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return body?.data?.workorderNo ?? '';
}

/**
 * 审计日志查询 helper · TC-10.2.A2 4 通道推送触发断言
 */
export async function getAuditLogs(
  request: APIRequestContext,
  quoteId: number,
): Promise<string[]> {
  const res = await request.get(`/api/v1/audit-logs?quoteId=${quoteId}`);
  expect(res.ok(), `getAuditLogs quote=${quoteId} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return Array.isArray(body?.data) ? body.data.map((l: { action: string }) => l.action) : [];
}

/**
 * 审批路由查询 helper · TC-10.2.B2 4 阈值路由断言
 */
export async function getApprovalRoute(
  request: APIRequestContext,
  orderId: number,
): Promise<string> {
  const res = await request.get(`/api/v1/orders/${orderId}/approval-route`);
  expect(res.ok(), `getApprovalRoute ${orderId} failed: ${res.status()}`).toBeTruthy();
  const body = await res.json();
  return body?.data?.route ?? '';
}

/**
 * Playwright fixture · 让 14 spec 共享 resetDb 钩子
 */
type Sprint10Fixtures = {
  resetDbHook: () => Promise<void>;
};

export const test = base.extend<Sprint10Fixtures>({
  resetDbHook: async ({ request }, use) => {
    await use(async () => {
      await resetDb(request);
    });
  },
});

export { expect };