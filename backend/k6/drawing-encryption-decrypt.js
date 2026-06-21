/**
 * k6 · Story 1.7 · P1 修补 3 · AES-256-GCM 解密性能基线
 * 性能目标：P95 < 100ms / 200 并发
 *
 * 验证 DrawingEncryptionService.decrypt 端到端性能
 * 256-bit key + 12 字节 IV + 128-bit GCM 标签
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const decryptLatency = new Trend('drawing_aes_decrypt_latency');
const decryptSuccess = new Rate('drawing_aes_decrypt_success');

export const options = {
  scenarios: {
    drawing_aes_decrypt: {
      executor: 'constant-vus',
      vus: 200,
      duration: '30s',
      gracefulStop: '5s',
    },
  },
  thresholds: {
    'drawing_aes_decrypt_latency': ['p(95)<100'],
    'drawing_aes_decrypt_success': ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  // 200 并发调用 PDF 导出（含 AES 解密嵌入签字扫描件）
  const url = `${BASE_URL}/drawings/export/1?format=pdf`;
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
  };
  const t0 = Date.now();
  const res = http.get(url, params);
  const elapsed = Date.now() - t0;
  decryptLatency.add(elapsed);
  const success = res.status === 200;
  decryptSuccess.add(success);
  check(res, {
    'status 200': (r) => r.status === 200,
  });
  sleep(0.2);
}
