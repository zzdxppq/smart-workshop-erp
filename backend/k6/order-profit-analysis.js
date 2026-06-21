// k6 性能基线 - 订单利润分析接口 (Story 1.6 · FR-2-3 · 1.6-deploy)
// 性能要求：利润分析 P95 < 3s · 30 并发 / 30s
// 阈值：p(95) < 3000；http_req_failed < 0.01
// 运行：k6 run backend/k6/order-profit-analysis.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const profitLatency = new Trend('order_profit_analysis_latency_ms');
const errorRate = new Rate('order_profit_analysis_errors');

export const options = {
  scenarios: {
    profit_burst: {
      executor: 'shared-iterations',
      vus: 30,              // 30 并发
      iterations: 900,      // 总 30s 内 900 次
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:order-profit}': ['p(95)<3000'],  // 利润分析 P95 < 3s
    'http_req_failed': ['rate<0.01'],
    'order_profit_analysis_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.FINANCE_TOKEN || '';  // 财务角色

export default function () {
  // 假定有 1-100 编号的订单用于压测
  const orderId = Math.floor(Math.random() * 100) + 1;
  const url = `${BASE_URL}/orders/${orderId}/profit`;
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'order-profit' },
  };
  const start = Date.now();
  const res = http.get(url, params);
  profitLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'has profit total': (r) => {
      try {
        return r.json('data.profitTotal') !== undefined;
      } catch (e) { return false; }
    },
    'has profit cost': (r) => {
      try {
        return r.json('data.profitCost') !== undefined;
      } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.3);
}
