// k6 性能基线 - 报价转订单接口 (Story 1.6 · FR-2-3 · 1.6-deploy)
// 性能要求：转订单 P95 < 800ms · 200 并发 / 30s
// 阈值：p(95) < 800；http_req_failed < 0.01
// 运行：k6 run backend/k6/order-conversion-from-quote.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const convertLatency = new Trend('order_conversion_latency_ms');
const errorRate = new Rate('order_conversion_errors');

export const options = {
  scenarios: {
    convert_burst: {
      executor: 'shared-iterations',
      vus: 200,             // 200 并发
      iterations: 6000,     // 总 30s 内 6 千次
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:order-convert}': ['p(95)<800'],  // 转订单 P95 < 800ms
    'http_req_failed': ['rate<0.01'],
    'order_conversion_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.SALES_TOKEN || '';

export default function () {
  // 假定有 1-100 编号的 APPROVED 状态报价
  const quoteId = Math.floor(Math.random() * 100) + 1;
  const url = `${BASE_URL}/quotes/${quoteId}/convert-to-order`;
  const payload = JSON.stringify({
    quantityAdjustment: 0,
    deliveryDate: '2026-07-15',
    comment: `k6 convert quote ${quoteId}`,
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'order-convert' },
  };
  const start = Date.now();
  const res = http.post(url, payload, params);
  convertLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200/409 (成功/已转)': (r) => r.status === 200 || r.status === 409,
    'has orderNo': (r) => {
      try {
        return r.json('data.orderNo') !== undefined;
      } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.1);
}
