// k6 性能基线 - 登录接口 (V1.3.7 · I.4)
// 性能要求：login P95 < 200ms · 100 并发持续 60s 错误率 < 0.1%
// 运行：k6 run -e BASE_URL=http://localhost:9080 backend/k6/login.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const loginLatency = new Trend('login_latency_ms');
const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '10s', target: 50 },   // ramp-up
    { duration: '60s', target: 100 },  // 100 并发持续 60s
    { duration: '10s', target: 0 },    // ramp-down
  ],
  thresholds: {
    'http_req_duration{name:login}': ['p(95)<200'],  // P95 < 200ms
    'errors': ['rate<0.001'],                        // 错误率 < 0.1%
    'http_req_failed': ['rate<0.001'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9080';

export default function () {
  const url = `${BASE_URL}/erp-platform/auth/login`;
  const payload = JSON.stringify({
    username: 'admin',
    password: 'admin123',
  });
  const params = { headers: { 'Content-Type': 'application/json' } };

  const start = Date.now();
  const res = http.post(url, payload, params);
  loginLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'code 0': (r) => {
      try { return r.json('code') === 0; } catch (e) { return false; }
    },
    'has accessToken': (r) => {
      try { return r.json('data.accessToken') !== undefined; } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.1);
}
