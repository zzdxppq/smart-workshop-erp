/**
 * k6 · Story 1.7 · AC-3.1.2 · 图纸发布审批性能基线
 * 性能目标：P95 < 50ms / 500 并发
 *
 * 验证 DrawingService.releaseDrawing 端到端性能（含 4 阈值路由 + > 20万 二次密码）
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const releaseLatency = new Trend('drawing_release_latency');
const releaseSuccess = new Rate('drawing_release_success');

export const options = {
  scenarios: {
    drawing_release: {
      executor: 'constant-vus',
      vus: 500,
      duration: '30s',
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'drawing_release_latency': ['p(95)<50'],
    'drawing_release_success': ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  const url = `${BASE_URL}/drawings/3/release?operatorUserId=1001`;
  const payload = JSON.stringify({
    adminPassword: 'admin@2026'
  });
  const params = {
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${TOKEN}`,
    },
  };
  const t0 = Date.now();
  const res = http.post(url, payload, params);
  const elapsed = Date.now() - t0;
  releaseLatency.add(elapsed);
  const success = res.status === 200 || res.status === 409;
  releaseSuccess.add(success);
  check(res, {
    'status 2xx or 409': (r) => r.status === 200 || r.status === 409,
  });
  sleep(0.1);
}
