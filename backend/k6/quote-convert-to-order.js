// k6 性能基线 - 报价转订单接口 (Story 1.5 · FR-2-2 · 1.5-deploy)
// 性能要求：转订单 P95 < 1s · 100 并发 / 30s
// 阈值：p(95) < 1000；http_req_failed < 0.01
// 运行：k6 run backend/k6/quote-convert-to-order.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const convertLatency = new Trend('quote_convert_latency_ms');
const errorRate = new Rate('quote_convert_errors');

export const options = {
  scenarios: {
    convert_steady: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 50 },   // ramp-up 到 50
        { duration: '20s', target: 100 }, // 100 并发持续 20s
        { duration: '5s', target: 0 },    // ramp-down
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'http_req_duration{name:quote-convert}': ['p(95)<1000'],  // 转订单 P95 < 1s
    'http_req_failed': ['rate<0.01'],
    'quote_convert_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.SALES_TOKEN || '';

/** 预生成 1000 个 APPROVED 状态报价（仅 sample set） */
const QUOTE_IDS = (__ENV.QUOTE_IDS || '')
  .split(',')
  .map((s) => Number(s.trim()))
  .filter((n) => Number.isFinite(n) && n > 0);

export default function () {
  if (QUOTE_IDS.length === 0) {
    console.error('QUOTE_IDS env not set; abort');
    return;
  }
  const quoteId = QUOTE_IDS[Math.floor(Math.random() * QUOTE_IDS.length)];
  const url = `${BASE_URL}/quotes/${quoteId}/convert-to-order`;
  const payload = JSON.stringify({
    quantityAdjustment: 0,    // 默认不调整
    remark: 'k6 perf test',
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'quote-convert' },
  };
  const start = Date.now();
  const res = http.post(url, payload, params);
  convertLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'code 0': (r) => {
      try { return r.json('code') === 0; } catch (e) { return false; }
    },
    'orderNo starts with XS': (r) => {
      try {
        const orderNo = r.json('data.orderNo') || '';
        return /^XS\d{8}-\d{4}$/.test(orderNo);
      } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.1);
}
