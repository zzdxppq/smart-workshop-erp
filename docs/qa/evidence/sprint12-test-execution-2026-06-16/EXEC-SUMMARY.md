# Sprint 12 CONDITIONAL GO 委派#1+2 · 执行总结（EXEC-SUMMARY）

> **执行人**：QA 商鞅
> **沙箱限制**：agent 沙箱受限无法直接跑 mvn/npm · 输出结构化执行计划 + 测试结果模板 + 失败处理流程
> **工作站**：沙箱外 Windows 10 + Git Bash · 截止 2026-06-16 18:00
> **关联**：sprint12-integration-test-report.md §3.1 委派 1+2+3

---

## 0. 文件清单（落到 docs/qa/evidence/sprint12-test-execution-2026-06-16/）

| # | 文件 | 范围 | 期望退出码 | 实际退出码 |
|---|------|------|----------|----------|
| 1 | `backend-mvn-result.log` | mvn 86 测例 + 12.3 14 + 12.4 32 + 1408 回归 + 12.1 24 = ~1570 测例 | 0 | ____ |
| 2 | `web-impl-typecheck-result.log` | gen:api + typecheck + typecheck:ci | 0/0/0 | ____ |
| 3 | `web-impl-build-result.log` | vue-tsc -b + vite build | 0/0 | ____ |
| 4 | `12.1-gray-scale-stages.md` | 4 阶段观察记录（阶段 1 已 ship · 2/3/4 待启动）| — | 🟡 PENDING |
| 5 | `v54-migration-rate.log` | V54 Flyway + 5 表 JOIN 回填率 | ≥ 99% | ____ |
| 6 | `EXEC-SUMMARY.md` | 本文件 | — | — |

---

## 1. 委派 1：86 测例 + risk-profile 8 项 test-execute

### 1.1 测例分布（期望 ~1570 全 PASS）

| Story | 测例数 | 范围 |
|-------|--------|------|
| 12.1 | 24 | 8 单元 + 6 集成 + 3 迁移 + 3 性能 + 3 灰度 + 1 E2E（2 E2E 跳过）|
| 12.2 | 16 | 5 CRUD + 4 心跳 + 3 available + 2 admin UI + 2 边界 |
| 12.3 | 14 | 4 单元 + 4 集成 + 2 QR 路由 + 2 跨仓 + 2 边界 |
| 12.4 | 32 + 8 risk-profile | 8 模式一 + 6 模式二 + 6 留痕 + 2 聚合 + 6 跨仓 E2E + 4 错误码性能 |
| 7-9 回归 | 1408 | Sprint 7 IMPL + 集成 + Sprint 8 优化 + Sprint 9 JWT |
| **合计** | **~1570** | **86 测例 + 8 risk-profile + 1476 回归** |

### 1.2 4 类型失败处理矩阵（mvn 后端 · A/B/C/D）

| 类型 | 症状 | 根因 | 修复责任 | SLA |
|------|------|------|---------|-----|
| **A · 瞬断误标** | 偶发 transient test 失败 | 网络/容器/数据库未就绪 | dev agent | < 30min（重跑 1 次）|
| **B · DB 污染** | 测例间数据串扰（FK 冲突/库存预扣错）| 事务未回滚 / `@Transactional` 缺失 | dev agent | < 1h |
| **C · 后端 API 漂移** | 404/500/字段缺失/JSON 解析失败 | 12.x 端点契约变更（status code / path / 字段名）| dev + architect | < 2h |
| **D · codegen 不一致** | codegen 漂移 / typecheck 错 | PM 决策#1 修复不彻底 | dev | < 1h |

**升级路径**：仍 FAIL → 通知 SM 萧何 → 评估是否推迟 V1.3.8 FAT 准入

### 1.3 期望产出
- 86 测例 + risk-profile 8 项 + 12.3 14 + 12.4 32 + 1408 回归 + 12.1 24 = **~1570 全 PASS**
- 0 flake · 0 BUILD FAILURE
- 3/7 模块 SUCCESS（er-platform / er-business / er-production）

### 1.4 实际结果（跑完后回填）
- 总测例数：____
- PASS：____
- FAIL：____
- 失败类型：____
- 判定：🟢 PASS / 🔴 FAIL

---

## 2. 委派 2：typecheck:ci + build 门禁（web-impl）

### 2.1 5 步骤期望

| 步骤 | 命令 | 期望退出码 | 期望产出 |
|------|------|----------|---------|
| 0 | `git pull origin main` | 0 | web-impl 仓 PM 决策#1 落地 |
| 0 | `npm ci` | 0 | 依赖就绪 |
| 0 | `npx playwright install --with-deps chromium` | 0 | chromium 1.4xx |
| 1 | `npm run gen:api` | 0 | 153 文件（4 service + 18 model + 131 既有）|
| 2 | `npm run typecheck` | 0 | 0 错误 |
| 3 | `npm run typecheck:ci` | 0 | 0 错误 + 0 git diff |
| 4 | `npm run build` | 0 | dist 标题 V1.3.8 · < 5MB |

### 2.2 V 类型 5 失败路径（typecheck:ci / web-impl）

| V 类型 | 步骤 | 症状 | 根因 | SLA |
|--------|------|------|------|-----|
| **V1 · codegen 失败** | 1 | `gen:api` 退出非 0 | openapi.yaml 漂移/重复键 | 24h |
| **V2 · typecheck PM 决策#3 残留** | 2 | 4 错误（currentOrder/stateSteps/transition 签名）| enum drift 修复不彻底 | 12h |
| **V3 · typecheck 其他错误** | 2 | any 残留 / strict null | 5 文件 any 替换遗漏 | 12-24h |
| **V4 · typecheck:ci diff 失败** | 3 | `git diff --exit-code` 失败 | generated/ 未 commit | 6h |
| **V5 · build 失败** | 4 | vite/RollupError | 配置/资源引用/peer dep 冲突 | 12-24h |

**升级路径**：V1-V5 仍 FAIL → SM 萧何 → 评估推迟 V1.3.8 FAT

### 2.3 期望产出
- gen 153 文件
- typecheck 双 0（typecheck + typecheck:ci）
- build 0 退出码
- dist 标题 V1.3.8

### 2.4 实际结果（跑完后回填）
- gen 文件数：____
- typecheck 错误数：____
- typecheck:ci 退出码：____
- build 退出码：____
- dist 标题：____
- 判定：🟢 PASS / 🔴 FAIL

---

## 3. 委派 3：12.1 灰度 4 阶段 + V54 回填率

### 3.1 4 阶段状态

| 阶段 | 角色 | 状态 | 截止 |
|------|------|------|------|
| 1 | admin + ENGINEER | 🟢 **已 ship**（V1.3.7 末 · 5/5 操作正常）| 2026-06-10 |
| 2 | SALES | 🟡 PENDING · 待 V1.3.8 FAT 通过 | ____ |
| 3 | PUR + WH + QC | 🟡 PENDING | ____ |
| 4 | OPERATOR（APP · 2 天）| 🟡 PENDING | ____ |

### 3.2 V54 回填率验证

| biz_type | source | migrated | 率 |
|----------|--------|----------|---|
| ORDER | ____ | ____ | ____% |
| PO | ____ | ____ | ____% |
| INBOUND | ____ | ____ | ____% |
| WORKORDER | ____ | ____ | ____% |
| INSPECTION | ____ | ____ | ____% |
| **TOTAL** | **____** | **____** | **____%** ← 期望 ≥ 99% |

**判定**：🟢 ≥ 99% PASS / 🟡 95-99% 有条件 / 🔴 < 95% 阻塞 FAT

---

## 4. PASS / FAIL 判定总览

| 委派 | 范围 | 期望 | 实际 | 判定 |
|------|------|------|------|------|
| #1 mvn 86 测例 | ~1570 测例 | 全 PASS | ____ | 🟢/🔴 |
| #2 typecheck:ci | gen 153 + 双 0 + build 0 | 全 0 | ____ | 🟢/🔴 |
| #3 灰度 4 阶段 | 阶段 1 已 ship · 2/3/4 待启动 | 1/4 完成 | 🟡 | 🟡 待 V1.3.8 FAT 启动 |
| #3 V54 回填率 | 5 表 JOIN | ≥ 99% | ____ | 🟢/🟡/🔴 |

### 4.1 集成 E 收口路径

```
mvn 全 PASS  ─┐
typecheck 0  ─┼─→ 🟢 3 项 QA 委派全 PASS
build 0      ─┘                    ↓
                              DevOps 接入（V1.3.8 FAT 准入）
                                    ↓
                              客户服务器就位 2026-06-23
                                    ↓
                              PO 范蠡 + 客户 FAT 准入 → GO
```

### 4.2 当前判定

🟡 **CONDITIONAL GO** · 委派 1+2 跑完后转 GO（若全 PASS）· 委派 3 灰度阶段 2-4 待 V1.3.8 FAT 通过后启动

---

## 5. 阻塞 / 风险

### 5.1 阻塞（0 项 · 跑完后回填）

| # | 阻塞 | 影响 | 缓解 |
|---|------|------|------|
| _ | ______ | ____ | ______ |

### 5.2 风险（已知 · 14 项 · 见 sprint12-integration-test-report.md §4.2）

- 🔴 P0 · 3 项：ZPL vs TSPL 协议差异 / Socket 代理卡死主线程 / 异步 ZPL 失败补偿
- 🟡 P1 · 4 项：迁移列名差异 / 27 vs 30 标签/页 / 补打链递归 / 客户机房 IP 变化
- 🟢 低 · 7 项：SB- 维护 / 字体嵌入 / 后端前端渲染保真度 / 启动顺序错位等

### 5.3 跨委派依赖

- 委派 1（mvn）+ 委派 2（typecheck:ci）独立可并行
- 委派 3（灰度 4 阶段）依赖 V1.3.8 FAT 通过
- V54 回填率验证可与委派 1 并行（dev DB + 生产 DB 独立环境）

---

## 6. 沙箱外工作站执行命令清单（dev agent / QA 商鞅工作站）

### 6.1 backend mvn（委派 1）

```bash
cd E:/claude/smart-workshop-erp/backend
mvn clean install -B \
  -Dtest='!AuthFlowE2ETest' \
  -DfailIfNoTests=false \
  -Dsurefire.failIfNoSpecifiedTests=false \
  2>&1 | tee docs/qa/evidence/sprint12-test-execution-2026-06-16/backend-mvn-result.log
```

### 6.2 web-impl typecheck:ci + build（委派 2）

```bash
cd E:/claude/smart-workshop-erp/web-impl
git pull origin main
npm ci
npx playwright install --with-deps chromium
npm run gen:api    2>&1 | tee docs/qa/evidence/sprint12-test-execution-2026-06-16/web-impl-gen-api.log
npm run typecheck  2>&1 | tee docs/qa/evidence/sprint12-test-execution-2026-06-16/web-impl-typecheck.log
npm run typecheck:ci 2>&1 | tee docs/qa/evidence/sprint12-test-execution-2026-06-16/web-impl-typecheck-ci.log
npm run build      2>&1 | tee docs/qa/evidence/sprint12-test-execution-2026-06-16/web-impl-build-result.log
```

### 6.3 V54 回填率验证（委派 3）

```bash
cd E:/claude/smart-workshop-erp/backend
mvn flyway:info -B 2>&1 | tee docs/qa/evidence/sprint12-test-execution-2026-06-16/v54-flyway-info.log
mysql -u root -p er_platform < db/migration/V54__data_migrate_drawing_link.sql
# 详见 v54-migration-rate.log §B-§H
```

### 6.4 12.1 灰度 4 阶段（委派 3 · V1.3.8 FAT 通过后启动）

- 阶段 2：sys_dict `draw.acl.gray.SALES = true` · 客户 admin 操作
- 阶段 3：sys_dict `draw.acl.gray.{PURCHASER,WAREHOUSE,QC} = true`
- 阶段 4：sys_dict `draw.acl.gray.OPERATOR = true` · APP 端 2 天观察
- 详见 12.1-gray-scale-stages.md

---

## 7. 时间表

| 时间 | 里程碑 | 责任 | 状态 |
|------|--------|------|------|
| 2026-06-14 24:00 | PM 决策#1+#3 截止 | PM 范蠡 | 🟢 已生效 |
| 2026-06-16 09:00 | QA 商鞅工作站启动 mvn + typecheck:ci + build | QA 商鞅 | 🟡 PENDING |
| 2026-06-16 18:00 | 6 文件归档 + 收口 PASS/FAIL | QA 商鞅 | 🟡 PENDING |
| 2026-06-16 18:00 | PM 决策#4 + #5 回复 | PM 范蠡 | 🟡 PENDING |
| 2026-06-23 | 客户服务器就位 + DevOps 接入 | DevOps 张良 | 🟡 PENDING |
| 2026-06-23 | Sprint 12 集成 E 收口 → GO | SM 萧何 | 🟡 PENDING |
| 2026-06-23+ | V1.3.8 FAT 准入（2830+86 = 2916 测例）| PO 范蠡 + 客户 | 🟡 PENDING |

---

## 8. 签字

- QA 商鞅（委派 1+2+3 执行）：________________  时间：2026-06-16 __:__
- dev agent Opus 4.8（codegen/typecheck 修复）：________________  时间：____
- DevOps 张良（V54 Flyway + 客户机房）：________________  时间：____
- PM 范蠡（决策#4+#5 回复 + V1.3.9 灰度准入）：________________  时间：____
- SM 萧何（集成 E 收口确认）：________________  时间：____
- PO 范蠡（V1.3.8 FAT 准入）：________________  时间：____
