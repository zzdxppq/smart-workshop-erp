// k6 性能基线 - 报价 PDF 导出接口 (Story 1.5 · FR-2-2 · 1.5-deploy)
// 性能要求：PDF 导出 P95 < 2s · 50 并发 / 30s
// 阈值：p(95) < 2000；http_req_failed < 0.01
// 运行：k6 run backend/k6/quote-pdf-export.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const pdfLatency = new Trend('quote_pdf_latency_ms');
const errorRate = new Rate('quote_pdf_errors');

export const options = {
  scenarios: {
    pdf_steady: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 20 },
        { duration: '20s', target: 50 },   // 50 并发持续 20s
        { duration: '5s', target: 0 },
      ],
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    'http_req_duration{name:quote-pdf}': ['p(95)<2000'],   // PDF P95 < 2s
    'http_req_failed': ['rate<0.01'],
    'quote_pdf_errors': ['rate<0.01'],
  },
  summaryTrendStats: ['avg', 'p(50)', 'p(90)', 'p(95)', 'p(99)'],
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.FINANCE_TOKEN || '';

/** 预生成 200 个不同报价 ID，跨 quote 命中不同 cache key */
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
  const url = `${BASE_URL}/quotes/export/${quoteId}?format=pdf`;
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
    tags: { name: 'quote-pdf' },
  };
  const start = Date.now();
  const res = http.get(url, params);
  pdfLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'content-type pdf': (r) => {
      const ct = r.headers['Content-Type'] || '';
      return ct.includes('application/pdf');
    },
    'body non-empty': (r) => r.body && r.body.length > 1024,
  });
  if (!ok) errorRate.add(1);

  sleep(0.2);
}
