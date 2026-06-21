/**
 * E2E 订单 fixture · Story 1.6
 *
 * - 提供 5 个客户订单关联 + 信用额度 seed
 * - 客户分布：
 *   - C0001-BL：黑名单（V1.3.7 红线 1 验证 40902 优先）
 *   - C0012-NORMAL-LIMIT：信用额度 1万（验证 40909 信用额度）
 *   - C0011-NORMAL：普通（业务自审 < 5万 验证）
 *   - C0015-NORMAL-50K：5万边界（验证 OR 会签）
 *   - C0030-NORMAL-NOLIMIT：creditLimit=-1 无限制
 *
 * - 5 客户订单关联（4 状态机覆盖）：
 *   - order #1：DRAFT（CRUD 测试用）
 *   - order #2：CONFIRMED（转生产 GD 用）
 *   - order #3：CONFIRMED（转委外 WW 用）
 *   - order #4：PRODUCING（部分生产 + 部分委外）
 *   - order #10：SHIPPED（财务回款 + 利润分析）
 *
 * 用法：
 *   import { test, expect, seedOrderData } from '../fixtures/order';
 *   test.beforeEach(async ({ page, db }) => { await seedOrderData(db); });
 */
import { test as base, expect } from '@playwright/test';
import type mysql from 'mysql2/promise';

const SEED_ORDERS = [
  {
    id: 1,
    order_no: 'XS202606110001',
    customer_id: 11,    // C0011-NORMAL
    status: 'DRAFT',
    total_amount: 10000,
    delivery_date: '2026-07-15',
    items: [
      { drawing_no: 'DWG-ORD-001', material: 'Q235', spec: 'M16x50', quantity: 20, unit_price: 500, amount: 10000 },
    ],
  },
  {
    id: 2,
    order_no: 'XS202606110002',
    customer_id: 15,    // C0015-NORMAL-50K
    status: 'CONFIRMED',
    total_amount: 50000,
    delivery_date: '2026-07-20',
    items: [
      { drawing_no: 'DWG-50K-001', material: 'Q235', spec: 'M20x80', quantity: 100, unit_price: 500, amount: 50000 },
    ],
  },
  {
    id: 3,
    order_no: 'XS202606110003',
    customer_id: 15,
    status: 'CONFIRMED',
    total_amount: 30000,
    delivery_date: '2026-07-20',
    items: [
      { drawing_no: 'DWG-WW-001', material: 'Q235', spec: 'M16x40', quantity: 100, unit_price: 300, amount: 30000 },
    ],
  },
  {
    id: 4,
    order_no: 'XS202606110004',
    customer_id: 15,
    status: 'PRODUCING',
    total_amount: 80000,
    delivery_date: '2026-07-25',
    items: [
      { drawing_no: 'DWG-MIX-001', material: 'Q235', spec: 'M24x100', quantity: 100, unit_price: 800, amount: 80000 },
    ],
  },
  {
    id: 10,
    order_no: 'XS202606110010',
    customer_id: 11,
    status: 'SHIPPED',
    total_amount: 5000,
    delivery_date: '2026-06-30',
    items: [
      { drawing_no: 'DWG-SHIP-001', material: 'Q235', spec: 'M12x30', quantity: 10, unit_price: 500, amount: 5000 },
    ],
  },
];

const SEED_CREDIT_LIMITS = [
  { customer_id: 1,  credit_limit: 10000, status: 'BLACKLIST' },  // C0001-BL
  { customer_id: 11, credit_limit: 100000, status: 'NORMAL' },     // C0011-NORMAL
  { customer_id: 12, credit_limit: 10000, status: 'NORMAL' },      // C0012-NORMAL-LIMIT
  { customer_id: 15, credit_limit: 200000, status: 'NORMAL' },     // C0015-NORMAL-50K
  { customer_id: 30, credit_limit: -1, status: 'NORMAL' },         // C0030-NORMAL-NOLIMIT
];

type OrderFixtures = {
  seedOrderData: (db: mysql.Connection) => Promise<void>;
  cleanupOrderData: (db: mysql.Connection) => Promise<void>;
};

export const test = base.extend<OrderFixtures>({
  seedOrderData: async ({}, use) => {
    await use(async (db: mysql.Connection) => {
      // 1. seed 信用额度（更新 sys_dict + sys_global_threshold 或直接 INSERT crm_customer.credit_limit）
      for (const c of SEED_CREDIT_LIMITS) {
        await db.execute(
          `UPDATE crm_customer
           SET credit_limit = ?, status = ?
           WHERE id = ?`,
          [c.credit_limit, c.status, c.customer_id],
        );
      }
      // 2. seed 5 订单
      for (const o of SEED_ORDERS) {
        await db.execute(
          `INSERT INTO crm_order
             (id, order_no, customer_id, owner_user_id, dept_id, status, current_node,
              total_amount, currency, delivery_date, created_at, updated_at)
           VALUES (?, ?, ?, 1001, 1, ?, 1, ?, 'CNY', ?, NOW(), NOW())
           ON DUPLICATE KEY UPDATE
             status = VALUES(status),
             total_amount = VALUES(total_amount)`,
          [o.id, o.order_no, o.customer_id, o.status, o.total_amount, o.delivery_date],
        );
        // 3. seed 订单明细
        for (let i = 0; i < o.items.length; i++) {
          const it = o.items[i];
          await db.execute(
            `INSERT INTO crm_order_item
               (order_id, drawing_no, material, spec, quantity, quantity_actual, quantity_shipped,
                unit_price, amount, is_fa, is_new, sort)
             VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, 0, 0, ?)
             ON DUPLICATE KEY UPDATE quantity = VALUES(quantity)`,
            [o.id, it.drawing_no, it.material, it.spec, it.quantity, it.quantity,
             it.unit_price, it.amount, i + 1],
          );
        }
      }
    });
  },
  cleanupOrderData: async ({}, use) => {
    await use(async (db: mysql.Connection) => {
      await db.execute('SET FOREIGN_KEY_CHECKS=0');
      for (const t of [
        'crm_order_shipment',
        'crm_order_history',
        'crm_order_payment',
        'crm_order_item',
        'crm_order',
      ]) {
        await db.execute(`TRUNCATE TABLE ${t}`);
      }
      await db.execute('SET FOREIGN_KEY_CHECKS=1');
    });
  },
});

export { expect };
