# DevOps 接入计划 · Sprint 10 web-impl CI 接入 + Nexus 镜像确认

> **作者**：DevOps 张良
> **日期**：2026-06-14
> **Sprint**：V1.3.8 S10 · 集成 E 验证 DevOps 委派
> **依据**：Sprint 10 集成 E 验证报告 §3.2 委派 4 + PM 决策#1（`docs/qa/evidence/sprint10-pm-decision-1-git-remote.md`）+ 10.2 dev log §6 + 10.4 dev log §1-3
> **状态**：🟡 **计划已起草** · 5 步骤就位 · 顺序约束遵循集成 E 验证 CONDITIONAL GO 路径
> **截止**：2026-06-15

---

## 1. 计划总览

| # | 步骤 | 责任 | 前置 | 截止 | 状态 |
|---|------|------|------|------|------|
| 1 | 内部 GitLab 创建 web-impl 空仓 | DevOps 张良 | PM 决策#1 已发（2026-06-13） | 2026-06-14 EOD | 🟡 计划就位 |
| 2 | web-impl/.gitlab-ci.yml 接入 GitLab CI | DevOps 张良 + web-impl dev | 步骤 1 + PM 决策#1 完成 + 13 commit push | 2026-06-15 EOD | 🟡 计划就位 |
| 3 | CI Runner 验证（lint + typecheck:ci + e2e） | DevOps 张良 + QA 商鞅 | 步骤 2 + QA 委派 1/2 完成 | 2026-06-15 EOD | 🟡 计划就位 |
| 4 | Nexus 镜像确认（android-impl gradle wrapper） | DevOps 张良 | 步骤 1 + GitLab Runner 网络拓扑已知 | 2026-06-15 EOD | 🟡 计划就位 |
| 5 | CI artifact 归档验证（30 天） | DevOps 张良 | 步骤 3 Pipeline 首次跑通 | 2026-06-15 EOD | 🟡 计划就位 |

---

## 2. 顺序约束（与 PM 决策#1 + QA 委派#1/#2 协同）

集成 E 验证 CONDITIONAL GO 路径图：

```
PM 决策#1 (web-impl git remote) ──┐
                                  │
QA 委派#1 (14 E2E test-execute) ─┤
                                  ├──→  DevOps 接入 CI ──→ Pipeline 验证 ──→ V1.3.8 FAT 准入
QA 委派#2 (typecheck:ci 全量)   ─┘
```

### 2.1 硬前置（必须先完成）

1. **PM 决策#1**（2026-06-13 已发，✅ 完成）
   - 选项 A · 内部 GitLab · multi-repo 架构首次落地
   - web-impl 仓从 untracked 状态 → 内部 GitLab 独立仓
   - 依据：`docs/qa/evidence/sprint10-pm-decision-1-git-remote.md`

2. **10.5 OutsourceStateMachine.vue 4 typecheck 错误修复**（PM 决策#1 §8.3 前置依赖）
   - 否则 typecheck:ci gate 在 CI 跑时会红
   - 修复路径已明：(a) `currentOrder` 类型 → `OutsourceOrder` · (b) 7 状态机 `stateSteps` 与 codegen 对齐 · (c) `useOutsourceStateMachine` composable 协调

3. **QA 委派#1**（14 E2E test-execute + 9.2 4 回归）
   - 14 spec 文件已生成 · 运行时验证委托 QA 商鞅
   - 关联：10.2 dev log §5.2 / §5.4

4. **QA 委派#2**（typecheck:ci 全量校验 + 10.5 4 错误修复后回归）
   - 含 generated/ 全量 vue-tsc + git diff gate
   - 关联：10.1 dev log §9 + 10.5 dev log §9

### 2.2 DevOps 接入时序

- **步骤 1** 必须在 PM 决策#1 完成后启动（已完成 ✅）
- **步骤 2** 必须在 13 commit push 后启动（web-impl dev 执行）
- **步骤 3** 必须在 typecheck:ci + 14 E2E 至少本地通过后启动
- **步骤 4** 独立于步骤 1-3，可并行
- **步骤 5** 必须在步骤 3 首次 Pipeline 跑通后验证

---

## 3. 步骤 1：内部 GitLab 创建 web-impl 空仓

### 3.1 目标

- 内部 GitLab 创建 `web-impl` 空仓（**不勾选** README / .gitignore / LICENSE · 保持空仓状态）
- 仓命名空间：`internal/smart-workshop/web-impl`（依据 multi-repo 规范）
- 可见性：`internal`（PM 决策#1 选项 A · 商业合同保密性）
- 关联：PM 决策#1 §5.1 委派 1

### 3.2 详细命令

```bash
# 1) 内部 GitLab API 创建空仓
curl -X POST https://gitlab.internal/api/v4/projects \
  -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  -F "name=web-impl" \
  -F "visibility=internal" \
  -F "namespace_id=1" \
  -F "initialize_with_readme=false" \
  -F "default_branch=main"
# 期望：返回 project_id + web_url

# 2) 记录 GitLab URL（替换占位符）
export GITLAB_WEBIMPL_URL="https://gitlab.internal/internal/smart-workshop/web-impl.git"
# 验证：项目存在
curl -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  "https://gitlab.internal/api/v4/projects/internal%2Fsmart-workshop%2Fweb-impl" | jq '.id, .web_url'

# 3) 配置保护分支（main · 禁止 force push）
curl -X POST "https://gitlab.internal/api/v4/projects/<project_id>/protected_branches" \
  -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  -F "name=main" \
  -F "push_access_level=40" \
  -F "merge_access_level=40" \
  -F "unprotect_access_level=40"
# 期望：返回 201 Created

# 4) 注册 CI Runner（共享 runner 或专属 runner）
# 优先：smart-workshop 专属 runner（已与 backend 仓共用）
curl -X POST "https://gitlab.internal/api/v4/projects/<project_id>/runners" \
  -H "PRIVATE-TOKEN: $GITLAB_TOKEN" \
  -F "runner_id=<smart-workshop-shared-runner-id>"
# 期望：runner enabled · tag=sprint10,web-impl

# 5) 配置 CI/CD 变量（如需）
# - NPM_CONFIG_REGISTRY（若内网 npm 镜像）
# - E2E_BASE_URL（如固定到 staging）
# - NEXUS_GRADLE_URL（步骤 4 同步配置）
```

### 3.3 期望结果

| 项 | 期望 |
|----|------|
| `project_id` | 数字 ID（记录到本计划附录） |
| `web_url` | `https://gitlab.internal/internal/smart-workshop/web-impl` |
| `default_branch` | `main` |
| 保护分支 | main 禁止 force push |
| CI Runner | 至少 1 个 runner enabled 且 tag 包含 `web-impl` |
| 仓状态 | 空（无 README / 无 .gitignore） |

### 3.4 通知

- ✅ 通知 PM 范蠡：GitLab URL 就位
- ✅ 通知 web-impl dev agent Opus 4.8：可启动本地仓 init + 13 commit push
- ✅ 通知 QA 商鞅：CI Runner 已就位 · 可执行 typecheck:ci + 14 E2E test-execute
- ✅ 通知 SM 萧何：步骤 1 完成 · 步骤 2 启动条件已就绪

---

## 4. 步骤 2：web-impl/.gitlab-ci.yml 接入 GitLab CI

### 4.1 目标

- `.gitlab-ci.yml`（10.2 dev log §6 已生成 · 3 stage · 4 worker parallel）接入 GitLab repo
- 验证 GitLab CI 识别 pipeline（merge_request / main push 触发）
- 关联：PM 决策#1 §5.1 委派 3

### 4.2 详细命令

```bash
# 1) web-impl dev 在本地仓执行（步骤 1 完成后）
cd web-impl

# 1.1) 本地仓 init（如未 init）
test -d .git || git init
git config user.name "web-impl dev agent"
git config user.email "dev-agent@smart-workshop.internal"

# 1.2) 创建 .gitignore（multi-repo 标准模板）
cat > .gitignore <<'EOF'
# Dependencies
node_modules/
.pnp
.pnp.js

# Build
dist/
build/
*.local

# Testing
coverage/
playwright-report/
test-results/
.nyc_output/

# Logs
npm-debug.log*
yarn-debug.log*
yarn-error.log*
*.log

# Environment
.env
.env.local
.env.*.local

# IDE
.vscode/
.idea/
*.swp
.DS_Store

# Cache
.cache/
.parcel-cache/
.eslintcache

# Misc
docs/_ops_inline.png
EOF

# 1.3) 关联 origin
git remote add origin https://gitlab.internal/internal/smart-workshop/web-impl.git
git remote -v
# 期望：origin 指向内部 GitLab URL

# 1.4) 按 PM 决策#1 §5.2 拆分 12 commit（10.1: 5 + 10.2: 2 + 10.5: 5）
# 注：13+ commit 包含 backend 协同（不在 web-impl 范围）
# 见 `docs/qa/evidence/sprint10-pm-decision-1-git-remote.md` §7 详细清单

# Story 10.1 · 5 commit
git add package.json package-lock.json
git commit -m "feat(codegen): npm scripts gen:api/typecheck/typecheck:ci + 依赖升级

- openapi-typescript-codegen 0.29.0
- vue-tsc 2.1.10
- typecheck:ci 含 git diff --exit-code src/api/generated gate"

git add tsconfig.json
git commit -m "feat(codegen): tsconfig.json include src/api/generated/**/*.ts"

git add src/api/generated/v138.ts
git commit -m "feat(codegen): v138.ts re-export shim 替换手写 stub"

git add src/api/generated/
git commit -m "chore(codegen): generated/ 全量 codegen 输出

- 40 service stubs
- 100 models
- 148 operations
- core + index"

git add src/views/v138/GmSummary.vue src/views/v138/BatchIncoming.vue \
        src/views/v138/MaterialDetail.vue src/views/reports/CustomerAnalysis.vue
git commit -m "refactor(types): GmSummary/BatchIncoming/MaterialDetail/CustomerAnalysis 收紧 unknown 类型"

# Story 10.2 · 2 commit
git add e2e/sprint10/
git commit -m "feat(e2e): Playwright 14 spec + helpers (sprint10/)

- 4 业务域：A 认证审批 / B 订单利润 / C 委外返修 / D 库存报表
- helpers.ts 含 DB 隔离 + 登录 + 委外推进 + 5 query helper
- 4 worker parallel · fullyParallel: true"

git add .gitlab-ci.yml playwright.config.ts
git commit -m "ci: .gitlab-ci.yml 3 stage (lint/typecheck/e2e) + playwright.config chromium project

- 3 stage: lint / typecheck / e2e
- e2e:sprint10 含 4 worker parallel
- e2e:regression-9.2 保留 4 happy path
- artifact expire_in: 30 days"

# Story 10.5 · 5 commit（**必须先修 4 typecheck 错误**· PM 决策#1 §8.3 前置）
git add src/views/sales/Quotes.vue
git commit -m "refactor(types): sales/Quotes.vue any → Quote[]"

git add src/views/sales/Orders.vue
git commit -m "refactor(types): sales/Orders.vue any → Order[]"

git add src/views/finance/Profit.vue
git commit -m "refactor(types): finance/Profit.vue any → OrderProfit[]"

git add src/views/production/OutsourceStateMachine.vue
git commit -m "refactor(types): production/OutsourceStateMachine.vue any → OutsourceState*

- 含 PM 决策#3 enum drift 修复
- 7 状态机 stateSteps 与 codegen 联合类型对齐"

git add src/views/quality/InspectionCreate.vue
git commit -m "refactor(types): quality/InspectionCreate.vue InspectionFormPayload + QualityStatus enum (Option A)"

# 1.5) 验证 commit 顺序
git log --oneline | head -20
# 期望：12 commit 按 Story 顺序排列

# 1.6) push 到 origin main
git push -u origin main
# 期望：12 commit 全部 push 成功
```

### 4.3 期望结果

| 项 | 期望 |
|----|------|
| GitLab web_url | 12 commit 在 `main` 分支可见 |
| CI Pipeline 触发 | 首次 push 触发 3 stage（lint / typecheck / e2e）|
| Pipeline 状态 | 🟡 **EXPECTED RUNNING**（步骤 3 验证） |
| Runner 占用 | 至少 1 个 runner 接收 job |

### 4.4 DevOps 介入点

- **.gitlab-ci.yml 增补**（如有需要）：
  - typecheck stage 已含 `rm -rf node_modules/.cache`（10.1 architect review §6 IMPL 注意事项 2）
  - 与 backend 仓共用 Nexus 镜像（步骤 4 落地）
  - cache 配置（`node_modules/` + `~/.npm/`）以加速 CI
- **CI/CD 变量注入**（GitLab Settings > CI/CD > Variables）：
  - `NEXUS_GRADLE_URL`（步骤 4 同步）
  - `NPM_CONFIG_REGISTRY`（如内网 npm 镜像）
  - `E2E_BASE_URL`（如固定到 staging）
- **Runner tag 配置**：
  - job 添加 `tags: [sprint10, web-impl]`

---

## 5. 步骤 3：CI Runner 验证

### 5.1 目标

- 验证 GitLab CI Runner 可执行 3 stage：
  1. **lint** · `npm run lint` · 退出 0
  2. **typecheck** · `npm run typecheck:ci` · 退出 0（PM 决策#1+#3 完成后）
  3. **e2e** · 含 `e2e:sprint10`（14 测例）+ `e2e:regression-9.2`（4 回归）
- 关联：10.2 dev log §6 + 集成 E 委派 1 + 委派 2

### 5.2 详细命令（GitLab CI 自动触发）

```bash
# 1) 触发 Pipeline（首次 push 后自动）
# GitLab UI: Pipelines > Run Pipeline > main

# 2) 观察 Pipeline
# GitLab UI: CI/CD > Pipelines > #<pipeline_id>

# 3) lint job 验证
# - image: node:20-bullseye-slim
# - script: npm ci --legacy-peer-deps && npm run lint
# - 期望：PASS · 退出码 0

# 4) typecheck job 验证
# - image: node:20-bullseye-slim
# - script: rm -rf node_modules/.cache && npm run typecheck:ci
# - 期望：PASS · 退出码 0（10.1 修复 + 10.5 4 错误修复后）
# - 包含：vue-tsc --noEmit 退出 0 + git diff --exit-code src/api/generated 退出 0

# 5) e2e:sprint10 job 验证（4 worker parallel）
# - image: mcr.microsoft.com/playwright:v1.49.0-jammy
# - services: erp-backend:latest
# - script: npx playwright test e2e/sprint10/ --project=chromium --reporter=junit,html,list --workers=4
# - 期望：14/14 PASS · 0 FAIL · 0 flake · ≤ 2min

# 6) e2e:regression-9.2 job 验证
# - script: npx playwright test e2e/dashboard-production.spec.ts e2e/salesperson-order-crud.spec.ts e2e/salesperson-quote-crud.spec.ts e2e/salesperson-50k-order-approve.spec.ts --project=chromium --reporter=junit,html
# - 期望：4/4 PASS
```

### 5.3 期望结果

| Job | 期望结果 | 失败处理 |
|-----|---------|---------|
| `lint` | PASS · 0 警告（如有 warning 需 fix） | 通知 10.2 dev + 10.5 dev 检查 ESLint 规则 |
| `typecheck` | PASS · vue-tsc 0 错误 · git diff 退出 0 | 10.5 4 错误未修 → 通知 10.5 dev · 1.5 codegen 漂移 → 通知 10.1 dev |
| `e2e:sprint10` | 14/14 PASS · ≤ 2min | seed 数据 mock id 缺失 → 通知 QA 商鞅补 seed · Playwright 超时 → 调整 worker 或 stage 超时 |
| `e2e:regression-9.2` | 4/4 PASS | 9.2 行为回归 → 通知 10.2 dev + QA 商鞅 |

### 5.4 验证清单

- [ ] Pipeline 触发成功（merge_request / main push）
- [ ] lint PASS
- [ ] typecheck PASS（含 10.5 4 错误修复后）
- [ ] e2e:sprint10 14/14 PASS
- [ ] e2e:regression-9.2 4/4 PASS
- [ ] artifact 可下载（playwright-report/ + test-results/ + junit.xml）
- [ ] 30 天过期策略生效

---

## 6. 步骤 4：Nexus 镜像确认（android-impl gradle wrapper）

### 6.1 目标

- 确认 GitLab Runner 可访问 Gradle 8.7 binary 下载源
- 若内网 Runner 无外网 → 切到 Nexus 镜像
- 同步改 `android-impl/gradle/wrapper/gradle-wrapper.properties` distributionUrl
- 关联：10.4 dev log §1 + 集成 E 委派 4 + 10.4 architect review §6 IMPL 注意事项 2

### 6.2 详细命令

```bash
# 1) 当前 android-impl/gradle/wrapper/gradle-wrapper.properties
# distributionUrl=https\://services.gradle.org/distributions/gradle-8.7-bin.zip
# 验证：cat android-impl/gradle/wrapper/gradle-wrapper.properties

# 2) 测试外网连通性（在 GitLab Runner 沙箱中）
# 在 GitLab CI job 中增加 connectivity check（一次性）
curl -I https://services.gradle.org/distributions/gradle-8.7-bin.zip
# 期望：HTTP 200
# 失败：内网 Runner 无外网 → 切到 Nexus 镜像

# 3) 若外网通：保留官方源（不需改）
# 若外网不通：切到 Nexus 镜像

# 3.1) 推荐 Nexus 仓库路径（与 backend 仓共用）
#   https://nexus.internal/repository/gradle-distributions/gradle-8.7-bin.zip
# 或
#   https://nexus.internal/repository/maven-proxy/gradle-8.7-bin.zip

# 3.2) 验证 Nexus 镜像可达
curl -I https://nexus.internal/repository/gradle-distributions/gradle-8.7-bin.zip
# 期望：HTTP 200 · Content-Length 与官方源一致

# 4) 同步改 android-impl/gradle/wrapper/gradle-wrapper.properties
sed -i 's|distributionUrl=.*|distributionUrl=https\\://nexus.internal/repository/gradle-distributions/gradle-8.7-bin.zip|' \
  android-impl/gradle/wrapper/gradle-wrapper.properties

# 5) 验证改动
cat android-impl/gradle/wrapper/gradle-wrapper.properties
# 期望：distributionUrl=https\://nexus.internal/repository/gradle-distributions/gradle-8.7-bin.zip

# 6) 提交改动（android-impl 仓）
cd android-impl
git add gradle/wrapper/gradle-wrapper.properties
git commit -m "chore(gradle): distributionUrl 切到 Nexus 镜像 (Sprint 10 DevOps 接入 #4)"
git push origin main
# 期望：android-impl 仓 commit 就位
```

### 6.3 期望结果

| 项 | 期望 |
|----|------|
| GitLab Runner 外网 | 通：保留官方源 / 不通：切 Nexus 镜像 |
| Nexus 镜像 | `https://nexus.internal/repository/gradle-distributions/gradle-8.7-bin.zip` 可达 |
| distributionUrl | 已同步改到 Nexus 镜像 |
| android-impl 仓 | commit 就位（`chore(gradle): distributionUrl 切到 Nexus 镜像`） |

### 6.4 风险与缓解

| 风险 | 等级 | 缓解 |
|------|------|------|
| Nexus 镜像未配置 gradle-distributions repo | 🟡 中 | 联系 Nexus 管理员 · 或先保留官方源（若 Runner 有外网）|
| GitLab Runner 沙箱无 curl/外网 | 🟡 中 | 一次性预热（手动下载 gradle-8.7-bin.zip 到 Runner 缓存目录）|
| Gradle 8.7 与 AGP 8.5 兼容性 | 🟢 低 | 10.4 dev log §4 已确认兼容 |

---

## 7. 步骤 5：CI artifact 归档验证

### 7.1 目标

- 验证 `.gitlab-ci.yml` 中 `e2e:sprint10` + `e2e:regression-9.2` job 的 artifact 归档策略
- 30 天过期策略生效
- playwright-report/ + test-results/ + junit.xml 可下载
- 关联：10.2 dev log §6 + .gitlab-ci.yml 第 67-78 行 + 第 103-114 行

### 7.2 artifact 配置确认（来自 .gitlab-ci.yml）

```yaml
# 已在 10.2 dev log §6 中实现 · DevOps 仅需验证
e2e:sprint10:
  artifacts:
    when: always
    paths:
      - playwright-report/
      - test-results/
    reports:
      junit: junit.xml
    expire_in: 30 days

e2e:regression-9.2:
  artifacts:
    when: always
    paths:
      - playwright-report/
      - test-results/
    reports:
      junit: junit.xml
    expire_in: 30 days
```

### 7.3 详细命令

```bash
# 1) 验证 CI artifact 可访问（GitLab UI）
# GitLab UI: CI/CD > Pipelines > #<pipeline_id> > #<job_id> > 右侧 Artifacts 区
# 期望：
#   - playwright-report/ (HTML 报告)
#   - test-results/ (trace.zip + 截图)
#   - junit.xml (junit 报告)
# 下载测试

# 2) 验证 30 天过期策略
# GitLab UI: 仓 Settings > CI/CD > Artifacts expiration
# 或 Pipeline 详情页 Artifacts 区显示过期时间

# 3) 若过期策略冲突，调整 .gitlab-ci.yml
# 例：与仓级 policy 冲突 → 删 job 级 expire_in · 或调整仓级 policy
```

### 7.4 期望结果

| 项 | 期望 |
|----|------|
| `playwright-report/` | 可下载 · HTML 报告含 14/14 测例详情 |
| `test-results/` | 可下载 · 含 trace.zip + 失败时截图 |
| `junit.xml` | 可下载 · GitLab UI 直接渲染测试结果 |
| 过期策略 | 30 天 · 与仓级 policy 不冲突 |
| Artifacts 路径 | `https://gitlab.internal/internal/smart-workshop/web-impl/-/jobs/<job_id>/artifacts` |

### 7.5 失败处理

- **Artifact 归档失败**：30 天过期策略冲突
  - 调整方案 1：删 `.gitlab-ci.yml` 中 `expire_in` · 让仓级 policy 生效
  - 调整方案 2：调整仓级 policy 与 job 级一致
  - 调整方案 3：CI 调试模式（`when: always` 改 `when: on_success` 临时定位）

---

## 8. 失败处理流程

| # | 失败场景 | 等级 | 流程 | 责任 |
|---|---------|------|------|------|
| 1 | **GitLab 创建空仓失败** · namespace 权限不足 | 🟡 中 | (1) curl 验证 API token · (2) 联系 GitLab 管理员开通 namespace · (3) fallback 用 root namespace（不推荐，违反 multi-repo）| DevOps 张良 → GitLab admin |
| 2 | **CI Runner 无外网** · distributionUrl 下载失败 | 🟡 中 | (1) curl 验证外网 · (2) 切 Nexus 镜像（步骤 4）· (3) 预热 Runner 缓存 | DevOps 张良 → 10.4 dev |
| 3 | **Pipeline 失败** · CI 配置错误 | 🟡 中 | (1) 查看 job log · (2) 通知 10.2 dev（CI yaml）+ QA 商鞅（typecheck + e2e 内容）· (3) 必要时 hotfix `.gitlab-ci.yml` | DevOps 张良 + 10.2 dev + QA 商鞅 |
| 4 | **Artifact 归档失败** · 30 天过期策略冲突 | 🟢 低 | (1) 删 `expire_in` 让仓级 policy 生效 · (2) 或调整仓级 policy · (3) 重新触发 job | DevOps 张良 |
| 5 | **typecheck:ci 失败** · 10.5 4 错误未修 | 🟡 中 | (1) 通知 10.5 dev 修 4 错误（PM 决策#1 §8.3 前置）· (2) 修完 push → 触发 Pipeline | 10.5 dev → DevOps 张良 |
| 6 | **e2e:sprint10 部分失败** · seed 数据 mock id 缺失 | 🟡 中 | (1) 通知 QA 商鞅补 seed SQL · (2) Sprint 11 backlog | QA 商鞅 → DevOps 张良 |
| 7 | **Nexus 镜像未配置** | 🟡 中 | (1) 联系 Nexus 管理员 · (2) 临时用官方源（若 Runner 有外网）· (3) 同步 backend 仓镜像实践 | DevOps 张良 → Nexus admin |
| 8 | **CI Runner 资源不足** · 4 worker parallel 调度失败 | 🟡 中 | (1) 检查 Runner 并发数 · (2) 调小 worker 数（4 → 2）· (3) 申请额外 Runner | DevOps 张良 |

---

## 9. 风险与阻塞

### 9.1 风险

| # | 风险 | 来源 | 等级 | 缓解 |
|---|------|------|------|------|
| 1 | DevOps GitLab 仓创建延迟（内部审批） | PM 决策#1 §8.1 | 🟡 中 | 截止 2026-06-15（+1 day buffer）· fallback 改 gate 逻辑（`git status --porcelain`）|
| 2 | 13 commit push 失败（10.5 4 错误未修 → typecheck:ci 红） | PM 决策#1 §8.1 #5 | 🟡 中 | **前置条件**：10.5 dev 修 4 错误后再 push · 否则 CI 红 |
| 3 | Nexus 镜像未配置 gradle-distributions | 步骤 4 | 🟡 中 | 联系 Nexus 管理员 · 临时用官方源（若 Runner 有外网）|
| 4 | CI Runner 资源不足（4 worker parallel） | 步骤 3 | 🟡 中 | 调小 worker · 申请额外 Runner |
| 5 | 14 spec mock id（100/200 等）需 V1.3.7 seed 配合 | 10.2 §8 #5 | 🟡 中 | QA 商鞅验证 seed 数据一致性 · 若 seed 不含 mock id 需补 seed SQL |
| 6 | .gitlab-ci.yml 中 artifact 30 天过期与仓级 policy 冲突 | 步骤 5 | 🟢 低 | 删 job 级 expire_in · 让仓级 policy 生效 |

### 9.2 阻塞

**无硬阻塞** · 5 步骤均可在 2026-06-15 截止前完成 · 关键依赖：
- PM 决策#1（✅ 已发 · 2026-06-13）
- 10.5 OutsourceStateMachine.vue 4 错误修复（🟡 待 10.5 dev）
- QA 商鞅 14 E2E test-execute（🟡 委派）
- QA 商鞅 typecheck:ci 验证（🟡 委派 · 10.5 修复后）

---

## 10. 与 V1.3.8 FAT 准入的衔接

### 10.1 DevOps 接入路径

```
PM 决策#1 (✅ 2026-06-13)
   ↓
步骤 1 (GitLab 空仓创建)        ← 2026-06-14 EOD
   ↓
13 commit push (10.5 4 错误修复后)  ← 2026-06-15 EOD
   ↓
步骤 2 (.gitlab-ci.yml 接入)     ← 2026-06-15 EOD
   ↓
步骤 3 (CI Runner 验证 3 stage)  ← 2026-06-15 EOD
   ↓
步骤 4 (Nexus 镜像确认 · 并行)   ← 2026-06-15 EOD
   ↓
步骤 5 (CI artifact 归档验证)    ← 2026-06-15 EOD
   ↓
V1.3.8 FAT 准入                   ← 2026-06-16 SM 萧何收口
```

### 10.2 集成 E 验证收口

- ✅ PM 决策#1（web-impl git remote）已发 → **1/3 PM 决策闭环**
- 🟡 PM 决策#2（InspectionDTO schema）待 PM 回复
- 🟡 PM 决策#3（7 状态机 enum drift）待 PM 回复 + 10.5 dev 修完
- 🟡 QA 委派#1（14 E2E + 9.2 4 回归）待 QA 执行
- 🟡 QA 委派#2（typecheck:ci）待 10.5 修完后 QA 执行
- 🟡 QA 委派#3（gradle wrapper V3/V4/V7）Sprint 11 兜底
- 🟡 **DevOps 委派#4（CI 接入 + Nexus）本计划承接** · 2026-06-15 截止

**判定**：5 步骤按计划执行后，DevOps 委派#4 闭环 · 集成 E 验证从 CONDITIONAL GO → GO 的 **4 项委派全部完成**（3 QA + 1 DevOps） · 衔接 V1.3.8 FAT 验收最终关。

### 10.3 后续运维（Sprint 11+）

- **CI 监控**：GitLab CI Pipeline 状态持续监控（lark/邮件告警）
- **Runner 维护**：定期清理 Runner 缓存（`node_modules/` + `~/.npm/`）
- **Nexus 同步**：跟踪 Gradle 8.7 → 后续版本升级
- **artifact 过期**：30 天后自动清理 · 必要时手动延长
- **.gitlab-ci.yml 演进**：随 Sprint 11+ 新增 stage（如 connectedAndroidTest）

---

## 11. 签字

- **DevOps 张良** · 2026-06-14 · DevOps 接入计划完成 · 5 步骤就位 · 截止 2026-06-15
- **PM 范蠡** · 2026-06-13 · PM 决策#1 已发（选项 A · 内部 GitLab）
- **SM 萧何** · 2026-06-14 · 集成 E 验证报告已生成 · DevOps 委派#4 已识别
- **web-impl dev agent Opus 4.8** · 2026-06-13/14 · 10.1/10.2/10.5 dev log 已交付 · 13+ commit 待 push
- **10.5 dev agent Opus 4.8** · 待补 · OutsourceStateMachine.vue 4 typecheck 错误修复（PM 决策#1 §8.3 前置）
- **QA 商鞅** · 待执行 · 14 E2E test-execute + typecheck:ci 全量校验（前置 10.5 修复完成）
- **architect 鲁班** · 2026-06-13 · 5 Story APPROVED + 14 条 IMPL 注意事项（已落实约束）

---

## 附录 A：CI/CD 变量清单

| 变量名 | 用途 | 可见性 | 备注 |
|--------|------|--------|------|
| `GITLAB_TOKEN` | API token | Masked + Protected | 内部 GitLab admin 颁发 |
| `NEXUS_GRADLE_URL` | Gradle 镜像 URL | Masked | `https://nexus.internal/repository/gradle-distributions/` |
| `NPM_CONFIG_REGISTRY` | npm 镜像 | Masked | 如内网 npm 镜像（可选）|
| `E2E_BASE_URL` | E2E 测试基址 | Variable | 默认 `http://localhost:8082` |
| `NEXUS_USER` / `NEXUS_PASSWORD` | Nexus 凭据 | Masked + Protected | 镜像下载用（可选）|

## 附录 B：CI Pipeline 预期时序

| 阶段 | 预计耗时 | 并行度 |
|------|---------|--------|
| lint | 1-2 min | 1 worker |
| typecheck | 2-3 min | 1 worker |
| e2e:sprint10 | 1.5-2 min | 4 worker parallel |
| e2e:regression-9.2 | 1-1.5 min | 1 worker |
| **总耗时** | **5.5-8.5 min** | e2e 阶段 4 worker 并行 |

## 附录 C：相关文件路径

- `web-impl/.gitlab-ci.yml` · 3 stage CI 配置
- `web-impl/playwright.config.ts` · chromium project 匹配 sprint10/
- `web-impl/e2e/sprint10/` · 14 spec + helpers.ts
- `android-impl/gradle/wrapper/gradle-wrapper.properties` · distributionUrl
- `docs/qa/evidence/sprint10-integration-test-report.md` · 集成 E 验证报告
- `docs/qa/evidence/sprint10-pm-decision-1-git-remote.md` · PM 决策#1
- `docs/dev/logs/10.2-dev-log.md` · Playwright E2E 14 端点 IMPL 报告
- `docs/dev/logs/sprint10-10.4-android-gradle-wrapper-report.md` · android gradle wrapper IMPL 报告
- `docs/qa/evidence/sprint10-devops-integration-plan.md` · **本计划**

---

**DevOps 接入计划完 · 5 步骤就位 · 顺序约束遵循集成 E CONDITIONAL GO 路径 · 截止 2026-06-15 · 衔接 V1.3.8 FAT 准入**
