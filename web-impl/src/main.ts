import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import VXETable from 'vxe-table'
import 'vxe-table/lib/style.css'

import App from './App.vue'
import router from './router'
import './styles/tokens/tokens.css'
import './styles/index.scss'
import './styles/industrial.scss'
import { registerErpComponents } from '@/components/erp'
import { installGatewayFetchPatch } from '@/utils/serviceRoute'
import { installAuthFetchGuard } from '@/utils/authSession'
import { setupOpenApiClient } from '@/api/setupOpenApi'

import { useTheme } from '@/composables/useTheme'

installGatewayFetchPatch()
installAuthFetchGuard()
setupOpenApiClient()

// 主界面默认工业深灰蓝，与登录页视觉一致
useTheme().setTheme('industrial')

/**
 * 昆山佰泰胜专属 ERP 系统 V1.3.9 · Web 入口
 * PRD: ../smart-workshop-erp/docs/prd.md
 * 架构: docs/architecture/architect-handoff.md
 */
const app = createApp(App)

// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })
app.use(VXETable)
registerErpComponents(app)

app.mount('#app')

console.log(`
╔════════════════════════════════════════════╗
║  昆山佰泰胜专属 ERP V1.3.7 · Web 启动成功    ║
║  Vue 3 + Vite 5 + Element Plus 2.4        ║
║  顶层菜单 7 个 · 13 Epic · 54 Story        ║
╚════════════════════════════════════════════╝
`)
