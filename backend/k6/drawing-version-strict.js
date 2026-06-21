/**
 * k6 · Story 1.7 · P1 修补 2 · 版本号严格递增性能基线
 * 性能目标：P95 < 10ms / 1000 并发
 *
 * 验证 DrawingService.addVersion 端到端性能（含 v1→v2 步进 1 校验）
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const versionStrictLatency = new Trend('drawing_version_strict_latency');
const versionStrictSuccess = new Rate('drawing_version_strict_success');

export const options = {
  scenarios: {
    drawing_version_strict: {
      executor: 'constant-vus',
      vus: 1000,
      duration: '30s',
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'drawing_version_strict_latency': ['p(95)<10'],
    'drawing_version_strict_success': ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  // 1000 并发提交 v1→v2 addVersion 请求
  const url = `${BASE_URL}/drawings/1/versions?operatorUserId=1001`;
  const payload = JSON.stringify({
    version: 'v2',
    changeReason: 'k6 load test',
    pdfPath: '/data/pdf/dwg-0001-v2.pdf',
    signatureScanPath: '/data/sig/dwg-0001-v2-1001.enc'
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
  versionStrictLatency.add(elapsed);
  const success = res.status === 200 || res.status === 409;  // 409 = 业务校验失败但服务正常
  versionStrictSuccess.add(success);
  check(res, {
    'status 2xx or 409': (r) => r.status === 200 || r.status === 409,
  });
  sleep(0.05);
}
