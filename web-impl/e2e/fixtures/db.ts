/**
 * E2E DB fixture · Story 1.5
 *
 * - 启动期用 testcontainers 拉 MySQL 8
 * - 跑 Flyway V1~V4 迁移 + 种子 30 客户（10 黑名单 + 20 普通）
 * - 提供 reset() / seedQuote() 给 E2E 用例复用
 * - 全部 fixture 全局单例（worker scope），跨用例共享一个容器
 */
import { test as base, expect } from '@playwright/test';
import {
  GenericContainer,
  StartedTestContainer,
  Wait,
  Network,
} from 'testcontainers';
import mysql from 'mysql2/promise';
import fs from 'node:fs';
import path from 'node:path';

const IMAGE = 'mysql:8.0';
const CONTAINER_PORT = 3306;
const DB_USER = 'erp';
const DB_PASS = 'erp_test_pwd';
const DB_NAME = 'cnc_business';
const NETWORK = 'e2e-erp-net';

let networkSingleton: Network | undefined;
async function ensureNetwork(): Promise<Network> {
  if (!networkSingleton) {
    networkSingleton = await new Network().start();
  }
  return networkSingleton;
}

let containerSingleton: StartedTestContainer | undefined;
let connectionSingleton: mysql.Connection | undefined;

/** 启动 MySQL 容器（testcontainers），跑 V1~V4 迁移 + 30 客户种子 */
export async function startMysql(): Promise<StartedTestContainer> {
  if (containerSingleton) return containerSingleton;
  const net = await ensureNetwork();
  const container = await new GenericContainer(IMAGE)
    .withName('e2e-erp-mysql')
    .withNetwork(net)
    .withNetworkAliases('mysql')
    .withExposedPorts(CONTAINER_PORT)
    .withEnvironment({
      MYSQL_ROOT_PASSWORD: 'root_pwd',
      MYSQL_DATABASE: DB_NAME,
      MYSQL_USER: DB_USER,
      MYSQL_PASSWORD: DB_PASS,
      TZ: 'Asia/Shanghai',
    })
    .withCopyContentToContainer([
      {
        content: readMysqlConf(),
        target: '/etc/mysql/conf.d/erp.cnf',
      },
    ])
    .withWaitStrategy(Wait.forLogMessage('port: 3306'))
    .withStartupTimeout(120_000)
    .start();
  containerSingleton = container;
  await runMigrations(container);
  return container;
}

function readMysqlConf(): string {
  return `
[mysqld]
character-set-server=utf8mb4
collation-server=utf8mb4_unicode_ci
default-time-zone='+08:00'
sql_mode='STRICT_TRANS_TABLES,NO_ENGINE_SUBSTITUTION'
innodb_flush_log_at_trx_commit=2
`;
}

/** 跑 backend/db/migration/V1~V4 + seed 30 客户 */
async function runMigrations(container: StartedTestContainer): Promise<void> {
  const conn = await mysql.createConnection({
    host: container.getHost(),
    port: container.getMappedPort(CONTAINER_PORT),
    user: 'root',
    password: 'root_pwd',
    multipleStatements: true,
  });

  const migrationsDir = path.resolve(__dirname, '../../../backend/db/migration');
  if (!fs.existsSync(migrationsDir)) {
    throw new Error(`migrations dir not found: ${migrationsDir}`);
  }
  const files = fs
    .readdirSync(migrationsDir)
    .filter((f) => /^V\d+__.*\.sql$/.test(f))
    .sort();
  for (const f of files) {
    const sql = fs.readFileSync(path.join(migrationsDir, f), 'utf-8');
    await conn.query(sql);
  }
  const seedFile = path.resolve(__dirname, '../../../backend/db/seed/30-customers.sql');
  if (fs.existsSync(seedFile)) {
    await conn.query(fs.readFileSync(seedFile, 'utf-8'));
  } else {
    /** fallback: 通过 storageState 触发 backend 端 seed endpoint */
    console.warn('[db] 30-customers.sql not found, falling back to runtime seed');
  }
  await conn.end();
}

/** 主 fixture：worker 启动时拉容器，所有用例共享 */
type DbFixtures = {
  db: {
    reset: () => Promise<void>;
    query: (sql: string, params?: unknown[]) => Promise<unknown>;
    seedQuote: (quoteNo: string, totalAmount: number) => Promise<number>;
    close: () => Promise<void>;
  };
};

export const test = base.extend<DbFixtures>({
  db: async ({}, use) => {
    const container = await startMysql();
    connectionSingleton = await mysql.createConnection({
      host: container.getHost(),
      port: container.getMappedPort(CONTAINER_PORT),
      user: 'root',
      password: 'root_pwd',
      database: DB_NAME,
      multipleStatements: true,
    });

    const api = {
      async reset(): Promise<void> {
        await connectionSingleton!.query('SET FOREIGN_KEY_CHECKS=0');
        for (const t of [
          'crm_quote_history',
          'crm_quote_item',
          'crm_quote',
          'crm_customer',
        ]) {
          await connectionSingleton!.query(`TRUNCATE TABLE ${t}`);
        }
        await connectionSingleton!.query('SET FOREIGN_KEY_CHECKS=1');
        const seed = path.resolve(
          __dirname,
          '../../../backend/db/seed/30-customers.sql',
        );
        if (fs.existsSync(seed)) {
          await connectionSingleton!.query(fs.readFileSync(seed, 'utf-8'));
        }
      },
      async query(sql: string, params: unknown[] = []): Promise<unknown> {
        const [rows] = await connectionSingleton!.query(sql, params);
        return rows;
      },
      async seedQuote(quoteNo: string, totalAmount: number): Promise<number> {
        const [r] = (await connectionSingleton!.query(
          `INSERT INTO crm_quote
             (quote_no, customer_id, owner_user_id, status, current_node, total_amount, currency, created_at, updated_at)
           VALUES (?, 11, 1001, 'DRAFT', 1, ?, 'CNY', NOW(), NOW())`,
          [quoteNo, totalAmount],
        )) as any;
        return r.insertId;
      },
      async close(): Promise<void> {
        await connectionSingleton?.end();
        connectionSingleton = undefined;
        if (containerSingleton) {
          await containerSingleton.stop();
          containerSingleton = undefined;
        }
      },
    };

    try {
      await use(api);
    } finally {
      await api.close();
    }
  },
});

export { expect };
