// k6 性能基线 - 报价路由决策接口 (Story 1.5 · FR-2-2 · 1.5-deploy)
// 性能要求：路由 P95 < 50ms · 1000 并发单次 / 30s
// 阈值：p(95) < 50；http_req_failed < 0.01
// 运行：k6 run backend/k6/quote-router-load.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const routerLatency = new Trend('quote_router_latency_ms');
const errorRate = new Rate('quote_router_errors');

export const options = {
  scenarios: {
    router_burst: {
      executor: 'shared-iterations',
      vus: 1000,            // 1000 并发
      iterations: 30000,    // 总 30s 内 3 万次
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:quote-router}': ['p(95)<50'],  // 路由 P95 < 50ms
    'http_req_failed': ['rate<0.01'],
    'quote_router_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.SALES_TOKEN || ''; // 业务员 JWT，由登录脚本预生成

// 4 档金额样本（覆盖 4 个路由分支）
const SAMPLES = [
  { amount: 3000, expect: 'SELF' },                // < 5万 SELF
  { amount: 50000, expect: 'DEPT_MANAGER_OR_SIGN' }, // 5-20万
  { amount: 250000, expect: 'GM_FINANCE_DUAL_SIGN' },// > 20万
  { amount: 300000, expect: 'GM_FINANCE_DUAL_SIGN' },// 边界 30万
];

export default function () {
  const sample = SAMPLES[Math.floor(Math.random() * SAMPLES.length)];
  const url = `${BASE_URL}/quotes/_route-decision`;
  const payload = JSON.stringify({
    customerId: 11,
    totalAmount: sample.amount,
    currency: 'CNY',
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'quote-router' },
  };
  const start = Date.now();
  const res = http.post(url, payload, params);
  routerLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'code 0': (r) => {
      try { return r.json('code') === 0; } catch (e) { return false; }
    },
    'route matches': (r) => {
      try { return r.json('data.route') === sample.expect; } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.05);
}
