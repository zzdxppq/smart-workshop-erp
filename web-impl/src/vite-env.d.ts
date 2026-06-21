/// <reference types="vite/client" />

interface ImportMetaEnv {
  // V1.3.9 Sprint 14 Story 13.7 baseline-typecheck-fix
  // 字段覆盖：
  //   VITE_API_BASE_URL · API 基地址（utils/http.ts L13 引用）
  //   VITE_USE_MOCK     · mock 开关（'true' | 'false' 字面量类型）
  //   VITE_APP_TITLE    · 应用标题（用于 dist 标题）
  readonly VITE_API_BASE_URL?: string
  readonly VITE_USE_MOCK?: 'true' | 'false'
  readonly VITE_APP_TITLE?: string
  readonly MODE: string
  readonly DEV: boolean
  readonly PROD: boolean
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
