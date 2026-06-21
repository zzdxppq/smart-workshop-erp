/**
 * Global type declarations
 */
declare global {
  // Element Plus 全局未导入形式 (ElMessage, ElNotification 等)
  // eslint-disable-next-line no-var
  var ElMessage: any
  // eslint-disable-next-line no-var
  var ElNotification: any
  // eslint-disable-next-line no-var
  var ElMessageBox: any
  // eslint-disable-next-line no-var
  var ElLoading: any
}

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<{}, {}, any>
  export default component
}

export {}