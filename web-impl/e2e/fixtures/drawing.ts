/**
 * Story 1.7 · 图纸 fixture
 * 5 图纸 + 3 版本迭代 + 3 签字扫描件
 */
import { test as base } from '@playwright/test';

export type DrawingFixture = {
  drawings: any[];
  versions: any[];
  signatures: any[];
};

export const test = base.extend<DrawingFixture>({
  drawings: async ({}, use) => {
    await use([
      { id: 1, drawingNo: 'DWG-20260612-0001', version: 'v2', title: 'FA 件 - 航空精密连接器外壳', materialCode: 'WL-1001', status: 'RELEASED', isFa: 1, isNew: 1, ownerUserId: 1001, deptId: 10 },
      { id: 2, drawingNo: 'DWG-20260612-0002', version: 'v1', title: '通用机械齿轮组件', materialCode: 'WL-1002', status: 'RELEASED', isFa: 0, isNew: 0, ownerUserId: 1002, deptId: 10 },
      { id: 3, drawingNo: 'DWG-20260612-0003', version: 'v1', title: '液压阀体', materialCode: 'WL-1003', status: 'DRAFT', isFa: 0, isNew: 1, ownerUserId: 1001, deptId: 10 },
      { id: 4, drawingNo: 'DWG-20260612-0004', version: 'v1', title: '高精度轴承座', materialCode: 'WL-1004', status: 'DRAFT', isFa: 0, isNew: 0, ownerUserId: 1003, deptId: 20 },
      { id: 5, drawingNo: 'DWG-20260612-0005', version: 'v1', title: '汽车变速箱壳体', materialCode: 'WL-1005', status: 'RELEASED', isFa: 0, isNew: 0, ownerUserId: 1002, deptId: 10 }
    ]);
  },
  versions: async ({}, use) => {
    await use([
      { id: 1, drawingId: 1, version: 'v1', changeReason: '首版发布', changedBy: 1001 },
      { id: 2, drawingId: 1, version: 'v2', changeReason: '客户反馈修改法兰尺寸', changedBy: 1001 },
      { id: 3, drawingId: 2, version: 'v1', changeReason: '首版发布', changedBy: 1002 }
    ]);
  },
  signatures: async ({}, use) => {
    await use([
      { id: 1, drawingId: 1, version: 'v1', signerUserId: 1001, signatureImagePath: 'AES-256-GCM-encrypted-base64-001', iv: 'iv-base64-001' },
      { id: 2, drawingId: 1, version: 'v2', signerUserId: 1001, signatureImagePath: 'AES-256-GCM-encrypted-base64-002', iv: 'iv-base64-002' },
      { id: 3, drawingId: 2, version: 'v1', signerUserId: 1002, signatureImagePath: 'AES-256-GCM-encrypted-base64-003', iv: 'iv-base64-003' }
    ]);
  }
});
