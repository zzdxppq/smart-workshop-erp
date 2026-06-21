<template>
  <div class="main-layout">
    <aside class="sidebar" :class="{ collapsed: sidebarCollapsed }">
      <div class="sidebar-brand">
        <span class="brand-mark">BTS</span>
        <span v-show="!sidebarCollapsed" class="brand-text">佰泰胜 ERP</span>
      </div>
      <el-menu
        :default-active="activeTopMenu"
        class="sidebar-menu"
        :collapse="sidebarCollapsed"
        router
      >
        <el-menu-item v-for="m in mainMenus" :key="m.path" :index="m.path">
          <el-icon><component :is="m.icon" /></el-icon>
          <template #title>{{ m.title }}</template>
        </el-menu-item>
      </el-menu>
      <button
        type="button"
        class="sidebar-toggle"
        :title="sidebarCollapsed ? '展开菜单' : '收起菜单'"
        @click="sidebarCollapsed = !sidebarCollapsed"
      >
        <el-icon><component :is="sidebarCollapsed ? 'Expand' : 'Fold'" /></el-icon>
      </button>
    </aside>

    <div class="main-column">
      <header class="topbar">
        <div class="topbar-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumbModule">{{ breadcrumbModule }}</el-breadcrumb-item>
            <el-breadcrumb-item v-if="breadcrumbPage">{{ breadcrumbPage }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="topbar-right">
          <el-button link type="primary" @click="openSearch">搜索 Ctrl+K</el-button>
          <el-button link @click="toggleTheme">{{ themeLabel }}</el-button>
          <span class="user-name">{{ displayName }}</span>
          <el-button link type="primary" @click="onLogout">退出</el-button>
        </div>
      </header>

      <main class="content-area">
        <router-view />
      </main>
    </div>
    <GlobalSearchPalette ref="searchPaletteRef" v-model="searchOpen" />
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canAccessModule } from '@/utils/menuAccess'
import GlobalSearchPalette from '@/components/layout/GlobalSearchPalette.vue'
import { useTheme } from '@/composables/useTheme'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const { mode, toggleTheme } = useTheme()
const sidebarCollapsed = ref(false)
const searchOpen = ref(false)
const searchPaletteRef = ref<InstanceType<typeof GlobalSearchPalette>>()

function openSearch() {
  searchOpen.value = true
  searchPaletteRef.value?.open()
}

function onGlobalKeyDown(e: KeyboardEvent) {
  if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'k') {
    e.preventDefault()
    openSearch()
  }
}

onMounted(() => {
  window.addEventListener('keydown', onGlobalKeyDown)
  void auth.fetchMenus()
})
onUnmounted(() => window.removeEventListener('keydown', onGlobalKeyDown))

const mainMenusAll = [
  { path: '/dashboard', title: '工作台', icon: 'HomeFilled' },
  { path: '/sales', title: '销售', icon: 'Money' },
  { path: '/engineering', title: '工程', icon: 'Goods' },
  { path: '/production', title: '生产', icon: 'Tools' },
  { path: '/sourcing', title: '采购', icon: 'ShoppingCart' },
  { path: '/warehouse', title: '仓储', icon: 'Box' },
  { path: '/quality', title: '品质', icon: 'Medal' },
  { path: '/finance', title: '财务', icon: 'CreditCard' },
  { path: '/hr', title: '人事', icon: 'User' },
  { path: '/admin', title: '管理', icon: 'Setting' },
]

const mainMenus = computed(() => {
  return mainMenusAll.filter((m) => canAccessModule(m.path, auth.userRoles, auth.menuPaths))
})

const activeTopMenu = computed(() => {
  const path = route.path
  if (path.startsWith('/material/barcode')) return '/warehouse'
  if (path.startsWith('/material') || path.startsWith('/engineering')) return '/engineering'
  const seg = route.path.split('/').filter(Boolean)[0]
  return seg ? `/${seg}` : '/dashboard'
})

const displayName = computed(() => auth.user?.realName || auth.user?.username || '用户')

const themeLabel = computed(() => (mode.value === 'light' ? '工业风' : '浅色办公'))

const breadcrumbModule = computed(() => {
  const top = route.matched.find((r) => r.children?.length && r.meta?.title)
  return top?.meta?.title as string | undefined
})

const breadcrumbPage = computed(() => {
  const leaf = route.matched[route.matched.length - 1]
  if (leaf?.meta?.title && leaf.meta.title !== breadcrumbModule.value) {
    return leaf.meta.title as string
  }
  return undefined
})

async function onLogout() {
  await auth.logoutRemote()
  router.push('/login')
}
</script>

<style scoped>
.main-layout {
  display: flex;
  height: 100vh;
  background: var(--erp-bg-page);
  color: var(--erp-text-primary);
}

.sidebar {
  display: flex;
  flex-direction: column;
  width: 220px;
  background: var(--erp-bg-card);
  border-right: 1px solid var(--erp-border);
  transition: width 0.2s ease;
}

.sidebar.collapsed {
  width: 64px;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  height: 56px;
  padding: 0 16px;
  border-bottom: 1px solid var(--erp-border);
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: linear-gradient(135deg, var(--erp-color-primary), var(--erp-color-primary-dark));
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  flex-shrink: 0;
}

.brand-text {
  font-size: 15px;
  font-weight: 600;
  white-space: nowrap;
}

.sidebar-menu {
  flex: 1;
  border-right: none;
  overflow-y: auto;
}

.sidebar-menu:not(.el-menu--collapse) {
  width: 100%;
}

.sidebar-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  border: none;
  border-top: 1px solid var(--erp-border);
  background: transparent;
  color: var(--erp-text-secondary);
  cursor: pointer;
}

.sidebar-toggle:hover {
  color: var(--erp-color-primary);
  background: var(--erp-color-primary-light);
}

.main-column {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
  padding: 0 20px;
  background: var(--erp-bg-card);
  border-bottom: 1px solid var(--erp-border);
}

.topbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-name {
  font-size: 14px;
  color: var(--erp-text-secondary);
}

.content-area {
  flex: 1;
  overflow: auto;
  padding: 20px;
  background: var(--erp-bg-page);
}
</style>
