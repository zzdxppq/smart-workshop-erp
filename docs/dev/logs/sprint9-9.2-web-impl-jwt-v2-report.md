# Story 9.2 IMPL 报告 · web-impl JWT v2 + 8 测例

> **报告人**：dev agent Opus 4.8
> **日期**：2026-06-13
> **状态**：🟢 18/18 JWT 测例 PASS（10 Sprint 8 回归 + 8 Sprint 9 新增）

---

## 1. 改动清单

### 1.1 jwt.ts v2 升级

| 改进 | v1（Sprint 8） | v2（Sprint 9） |
|------|---------------|---------------|
| base64url → base64 | 简易 `.replace` | 标准化 `base64UrlToBase64()` helper（含 padding 补齐） |
| 解码 | `atob + 手写 percent encoding` | `atob + Uint8Array + TextDecoder('utf-8')` 标准 UTF-8 解码 |
| 测试 helper | 无 | `makeTestToken()` + `base64UrlEncode()` |
| 浏览器兼容 | 浏览器 only | 浏览器 + Node 双环境（Buffer fallback） |

### 1.2 jwt.test.ts 18 测例

10 Sprint 8 回归测例 + 8 Sprint 9 新增：

| 测例组 | 数量 |
|--------|------|
| Sprint 8 回归（parseJwt / extractRoles / isExpired / extractUserId） | 10 |
| 9.2.a 中文 username UTF-8 解码 | 1 |
| 9.2.b 中文 permissions | 1 |
| 9.2.c base64UrlEncode 无 padding/+/字符 | 1 |
| 9.2.d base64UrlEncode 长字符串无非法字符 | 1 |
| 9.2.e makeTestToken 生成 3 段 JWT | 1 |
| 9.2.f extractUsername 中文 | 1 |
| 9.2.g parseJwt 空 roles | 1 |
| 9.2.h parseJwt 无 roles 字段（兜底 []） | 1 |

## 2. mvn test 验证（vitest node 环境）

```
$ ./node_modules/.bin/vitest run --config vite.config.test.ts src/utils/jwt.test.ts

✓ src/utils/jwt.test.ts (18 tests) 7ms
  Test Files  1 passed (1)
       Tests  18 passed (18)
```

## 3. 关键设计决策

### 3.1 用 TextDecoder 替代手写 percent encoding

v1：
```typescript
decodeURIComponent(decoded.split('').map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2)).join(''))
```

v2：
```typescript
const bytes = new Uint8Array(binary.length);
for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i);
const json = new TextDecoder('utf-8').decode(bytes);
```

**理由**：TextDecoder 是浏览器/Node 16+ 标准 API，正确处理 UTF-8 surrogate pair。手写 percent encoding 容易出错（emoji/中文 surrogate）。

### 3.2 测试 helper 内置（不再每次手写 base64）

`makeTestToken(payload)` + `base64UrlEncode(input)` 让测试代码更可读：
```typescript
// Before
const token = `eyJ.${btoa(JSON.stringify(payload)).replace(...)}.sig`;

// After
const token = makeTestToken(payload);
```

### 3.3 浏览器 + Node 双环境支持

```typescript
const isBrowser = typeof window !== 'undefined' && typeof btoa !== 'undefined';
if (isBrowser) {
  // TextEncoder + btoa
} else {
  // Buffer (Node)
}
```

**理由**：vitest 默认 node 环境，但 web-impl 部署到浏览器。生产代码必须双环境兼容。

## 4. 已知遗留（Sprint 9 H）

| # | 遗留项 | 处理方 |
|---|--------|--------|
| 1 | OpenAPI TypeScript codegen 集成 | Sprint 9 H（需后端 OpenAPI YAML 完整） |
| 2 | Playwright E2E（web-impl 14 端点） | Sprint 9 H（需 web-impl 部署） |

## 5. Sprint 9 累计

| Story | 测例 | 通过 |
|-------|------|------|
| 9.1 workflow_event 触发 | 12 | 12/12 ✅ |
| 9.2 JWT v2（web-impl） | 18 | 18/18 ✅ |
| **合计** | **30** | **30/30** |

## 6. 签字

- **dev agent Opus 4.8** · 2026-06-13 · 18/18 PASS
- **architect 鲁班** · TextDecoder 标准化接受
- **QA 商鞅** · Playwright E2E 待 Sprint 9 H