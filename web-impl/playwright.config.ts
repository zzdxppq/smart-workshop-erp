/**
 * Playwright E2E 配置 · Story 1.6 · 订单管理
 *
 * - 在 Story 1.5 6 个 project 基础上追加 1 个 admin project（黑名单/信用额度管理）
 * - baseURL：http://localhost:8082（erp-business，对齐 V1.3.7 §3.1）
 * - 依赖 backend/deploy/docker-compose.yml 启动 mysql + nacos + erp-platform + erp-business
 * - 覆盖 Story 1.6 7 个 E2E spec：业务员订单 CRUD / 5万 OR 会签 / 转生产 GD / 转委外 WW /
 *   财务回款结算 / 财务 PDF 导出 / admin 信用额度校验
 *
 * 用法：
 *   1) docker compose -f backend/deploy/docker-compose.yml up -d
 *   2) cd web-impl && npx playwright test --project=salesperson
 */
import { defineConfig, devices, Project } from '@playwright/test';

const BASE_URL = process.env.E2E_BASE_URL ?? 'http://localhost:8082';

const sharedUse = {
  baseURL: BASE_URL,
  trace: 'on-first-retry' as const,
  screenshot: 'only-on-failure' as const,
  video: 'retain-on-failure' as const,
  actionTimeout: 15_000,
  navigationTimeout: 30_000,
};

/** Story 1.5 + 1.6 E2E 角色 · 与 V1.3.7 红线 1/2/5/6 + 1.6 红线 1/2/3/4/5 一致 */
const roleProject = (
  name: string,
  username: string,
  password: string,
  testMatch: RegExp,
): Project => ({
  name,
  use: {
    ...sharedUse,
    extraHTTPHeaders: {
      'X-E2E-User': username,
      'X-E2E-Pwd': password,
    },
  },
  testMatch,
});

export default defineConfig({
  testDir: './e2e',
  outputDir: './e2e/test-results',
  reporter: [
    ['list'],
    ['html', { outputFolder: 'e2e/playwright-report', open: 'never' }],
    ['json', { outputFile: 'e2e/playwright-report/results.json' }],
  ],
  timeout: 60_000,
  expect: { timeout: 10_000 },
  fullyParallel: false,
  workers: 1, // 共享同一份 docker mysql，避免并发种子竞态
  retries: process.env.CI ? 2 : 0,

  use: sharedUse,

  webServer: process.env.E2E_NO_WEBSERVER
    ? undefined
    : {
        command: 'npm run dev -- --port 5173 --host 0.0.0.0',
        url: 'http://localhost:5173',
        reuseExistingServer: !process.env.CI,
        timeout: 120_000,
        stdout: 'pipe',
        stderr: 'pipe',
      },

  projects: [
    // P0：业务员 - 订单 CRUD + 5万 OR 会签 + 转生产 GD + 转委外 WW
    roleProject(
      'salesperson',
      'salesperson01',
      'sales123',
      /(salesperson-quote-crud|salesperson-50k-approve|quote-convert-to-order|salesperson-order-crud|salesperson-50k-order-approve|salesperson-confirm-to-production|salesperson-transfer-outsource)\.spec\.ts/,
    ),
    // P0：部门经理 - 待办审批（OR 会签）
    roleProject(
      'dept-manager',
      'dept_manager01',
      'mgr123',
      /(dept-manager-pending-approve|salesperson-50k-order-approve)\.spec\.ts/,
    ),
    // P0：总经理 - 高额二次密码（> 20万 V1.3.7 红线 2）
    roleProject(
      'gm',
      'gm01',
      'gm123',
      /gm-250k-finance-dual-sign\.spec\.ts/,
    ),
    // P0：财务 - PDF/Excel 导出 + 1h 缓存 + 回款结算 + 利润分析
    roleProject(
      'finance',
      'finance01',
      'fin123',
      /(finance-pdf-export|finance-order-settle|finance-order-pdf-export|finance-quote-pdf-export)\.spec\.ts/,
    ),
    // P1：财务总监 - 双签
    roleProject(
      'finance-director',
      'finance_director01',
      'fin123',
      /gm-250k-finance-dual-sign\.spec\.ts/,
    ),
    // P1：admin - 黑名单管理 + 信用额度校验
    roleProject(
      'admin',
      'admin',
      'admin123',
      /.*\.spec\.ts/,
    ),
    // Story 10.2 · Sprint 10 E2E 14 端点 · 跨角色（sprint10/ 子目录）
    roleProject(
      'chromium',
      'admin',
      'admin123',
      /sprint10\/tc-10\.2-.*\.spec\.ts/,
    ),
    // P1：Story 1.6 新增 - admin 信用额度/黑名单专用（与 1.5 admin 区分）
    roleProject(
      'admin-credit',
      'admin',
      'admin123',
      /admin-credit-limit\.spec\.ts/,
    ),
    // Story 1.7 · E3-Drawing 7 projects（追加）
    roleProject(
      'engineering-drawing-crud',
      'engineer01',
      'eng123',
      /engineering-drawing-crud\.spec\.ts/,
    ),
    roleProject(
      'engineering-drawing-version',
      'engineer01',
      'eng123',
      /engineering-drawing-version\.spec\.ts/,
    ),
    roleProject(
      'engineering-drawing-release',
      'chief01',
      'chief123',
      /engineering-drawing-release\.spec\.ts/,
    ),
    roleProject(
      'engineering-drawing-archive',
      'engineer01',
      'eng123',
      /engineering-drawing-archive\.spec\.ts/,
    ),
    roleProject(
      'engineering-drawing-pdf-export',
      'engineer01',
      'eng123',
      /engineering-drawing-pdf-export\.spec\.ts/,
    ),
    roleProject(
      'admin-drawing-encryption',
      'admin',
      'admin123',
      /admin-drawing-encryption\.spec\.ts/,
    ),
    roleProject(
      'quality-drawing-fa',
      'qa01',
      'qa123',
      /quality-drawing-fa\.spec\.ts/,
    ),
  ],
});
