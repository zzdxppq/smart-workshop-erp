/**
 * k6 · Story 1.7 · AC-3.1.4 · 图纸 PDF 导出性能基线
 * 性能目标：P95 < 3s / 30 并发（含 AES-256-GCM 解密嵌入签字扫描件）
 *
 * 验证 DrawingPdfExportService.exportPdf 端到端性能
 * 1h 缓存命中后 P95 < 200ms
 */
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const pdfExportLatency = new Trend('drawing_pdf_export_latency');
const pdfExportSuccess = new Rate('drawing_pdf_export_success');

export const options = {
  scenarios: {
    drawing_pdf_export: {
      executor: 'constant-vus',
      vus: 30,
      duration: '60s',
      gracefulStop: '10s',
    },
  },
  thresholds: {
    'drawing_pdf_export_latency': ['p(95)<3000'],
    'drawing_pdf_export_success': ['rate>0.99'],
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const TOKEN = __ENV.TOKEN || '';

export default function () {
  const url = `${BASE_URL}/drawings/export/1?format=pdf`;
  const params = {
    headers: {
      'Authorization': `Bearer ${TOKEN}`,
    },
  };
  const t0 = Date.now();
  const res = http.get(url, params);
  const elapsed = Date.now() - t0;
  pdfExportLatency.add(elapsed);
  const success = res.status === 200;
  pdfExportSuccess.add(success);
  check(res, {
    'status 200': (r) => r.status === 200,
    'content-type pdf': (r) => r.headers['Content-Type'] && r.headers['Content-Type'].includes('application/pdf'),
  });
  sleep(1);
}
