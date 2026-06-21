// k6 性能基线 - 订单 PDF 导出接口 (Story 1.6 · FR-2-3 · 1.6-deploy)
// 性能要求：PDF 导出 P95 < 2s · 50 并发 / 30s
// 阈值：p(95) < 2000；http_req_failed < 0.01
// 运行：k6 run backend/k6/order-pdf-export.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const pdfLatency = new Trend('order_pdf_export_latency_ms');
const errorRate = new Rate('order_pdf_export_errors');

export const options = {
  scenarios: {
    pdf_burst: {
      executor: 'shared-iterations',
      vus: 50,              // 50 并发
      iterations: 1500,     // 总 30s 内 1.5 千次
      maxDuration: '30s',
    },
  },
  thresholds: {
    'http_req_duration{name:order-pdf-export}': ['p(95)<2000'],  // PDF 导出 P95 < 2s
    'http_req_failed': ['rate<0.01'],
    'order_pdf_export_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.FINANCE_TOKEN || '';  // 财务角色

export default function () {
  // 假定有 1-100 编号的订单用于压测
  const orderId = Math.floor(Math.random() * 100) + 1;
  const url = `${BASE_URL}/orders/export/${orderId}?format=pdf`;
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'order-pdf-export' },
  };
  const start = Date.now();
  const res = http.get(url, params);
  pdfLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'content-type pdf': (r) => {
      const ct = r.headers['Content-Type'] || '';
      return ct.includes('application/pdf') || ct.includes('octet-stream');
    },
    'size > 1KB': (r) => r.body && r.body.length > 1024,
  });
  if (!ok) errorRate.add(1);

  sleep(0.2);
}
