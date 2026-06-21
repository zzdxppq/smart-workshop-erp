# C.1 DoD 验收清单

## 自动化命令

```bash
# Web 类型检查 + 构建
cd web-impl && npm run typecheck:ci && npm run build

# Storybook 构建（≥30 stories）
cd web-impl && npm run build-storybook

# Web 单元测试
cd web-impl && npm run test

# Android 单元测试
cd android-impl && ./gradlew :app:test
```

## 灵魂测试脚本

### 操作工奶奶测试（≤30s）

1. 打开 APP 首页 Tab
2. 输入 `GD-20260615-0001` → 下一步
3. 输入流转码 → 下一步 → 提交
4. 验收：全程 ≤3 步，无菜单导航

### 业务员 5 分钟测试

1. `/sales/quotes/new` → F8 图号 → 填明细
2. Ctrl+S 保存 → Ctrl+Enter 提交
3. `/sales/quotes/approval` → Enter 批量通过

### 生管 1 屏测试

1. 打开 `/dashboard/production`
2. 5 秒内可见 KPI + 逾期/异常列

## OpenAPI 对账

对照 [backend/spec/openapi.yaml](../backend/spec/openapi.yaml) 与 [spec-gap-matrix.md](./spec-gap-matrix.md)。

## 性能目标

| 指标 | 目标 | 验证 |
|------|------|------|
| LCP | ≤2.5s P75 | Lighthouse CI |
| 扫码响应 | ≤1s P95 | Android 手动 |
| 查询 P95 | ≤2s | API 监控 |

## WCAG AA

关键流（登录/报价/扫码）运行 axe-core：

```bash
npx playwright test --grep accessibility
```

（Playwright 用例可在 `tests/a11y/` 扩展）
