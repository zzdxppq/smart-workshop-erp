// k6 性能基线 - 订单状态机推进接口 (Story 1.6 · FR-2-3 · 1.6-deploy)
// 性能要求：状态机 P95 < 30ms · 500 并发 / 30s
// 阈值：p(95) < 30；http_req_failed < 0.01
// 运行：k6 run backend/k6/order-state-machine.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const stateMachineLatency = new Trend('order_state_machine_latency_ms');
const errorRate = new Rate('order_state_machine_errors');

export const options = {
  scenarios: {
    state_burst: {
      executor: 'shared-iterations',
      vus: 500,             // 500 并发
      iterations: 15000,    // 总 30s 内 1.5 万次
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:order-state-machine}': ['p(95)<30'],  // 状态机 P95 < 30ms
    'http_req_failed': ['rate<0.01'],
    'order_state_machine_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.SALES_TOKEN || '';

// 7 状态机推进序列样本
const TRANSITIONS = [
  { from: 'DRAFT',         to: 'CONFIRMED',       endpoint: '/confirm' },
  { from: 'CONFIRMED',     to: 'PRODUCING',       endpoint: '/approve' },
  { from: 'PRODUCING',     to: 'PARTIAL_SHIPPED', endpoint: '/ship' },
  { from: 'PARTIAL_SHIPPED', to: 'SHIPPED',       endpoint: '/ship' },
  { from: 'SHIPPED',       to: 'SETTLED',         endpoint: '/settle' },
  { from: 'SETTLED',       to: 'CLOSED',          endpoint: '/close' },
];

export default function () {
  const trans = TRANSITIONS[Math.floor(Math.random() * TRANSITIONS.length)];
  // 假定有 1-10 编号的订单用于压测（部署环境用专门的压测数据 seed）
  const orderId = Math.floor(Math.random() * 10) + 1;
  const url = `${BASE_URL}/orders/${orderId}${trans.endpoint}`;
  const payload = JSON.stringify({
    approverUserId: 1001,
    comment: `k6 state machine ${trans.from} -> ${trans.to}`,
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'order-state-machine' },
  };
  const start = Date.now();
  const res = http.post(url, payload, params);
  stateMachineLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200/409 (合法/非法)': (r) => r.status === 200 || r.status === 409,
    'code 0 or 40904': (r) => {
      try {
        const c = r.json('code');
        return c === 0 || c === 40904;
      } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.05);
}
