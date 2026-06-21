// k6 性能基线 - 用户列表 (V1.3.7 · I.4)
// 性能要求：list P95 < 1s · 100 并发持续 60s 错误率 < 0.1%
// 运行：k6 run -e BASE_URL=http://localhost:8081 -e ACCESS_TOKEN=<jwt> backend/k6/users-list.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const listLatency = new Trend('users_list_latency_ms');
const errorRate = new Rate('errors');

export const options = {
  stages: [
    { duration: '10s', target: 50 },
    { duration: '60s', target: 100 },
    { duration: '10s', target: 0 },
  ],
  thresholds: {
    'http_req_duration{name:users_list}': ['p(95)<1000'],  // P95 < 1s
    'errors': ['rate<0.001'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8081';
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

export default function () {
  const url = `${BASE_URL}/users?pageNum=1&pageSize=20`;
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${ACCESS_TOKEN}`,
    },
  };

  const start = Date.now();
  const res = http.get(url, params);
  listLatency.add(Date.now() - start);

  const ok = check(res, {
    'status 200': (r) => r.status === 200,
    'code 0': (r) => {
      try { return r.json('code') === 0; } catch (e) { return false; }
    },
  });
  if (!ok) errorRate.add(1);

  sleep(0.2);
}
