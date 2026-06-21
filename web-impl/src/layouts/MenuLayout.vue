<template>
  <div class="menu-layout">
    <aside v-if="menuGroups.length" class="sub-sidebar">
      <div v-if="moduleTitle" class="sub-sidebar-title">{{ moduleTitle }}</div>
      <nav class="sub-menu-nav">
        <section v-for="(group, gi) in menuGroups" :key="gi" class="menu-group">
          <div v-if="group.label" class="menu-group-label">{{ group.label }}</div>
          <router-link
            v-for="m in group.items"
            :key="m.path"
            :to="m.path"
            class="sub-menu-link"
            :class="{ active: activeMenu === m.path }"
          >
            {{ m.title }}
          </router-link>
        </section>
      </nav>
    </aside>
    <div class="menu-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { canAccessRoute } from '@/utils/menuAccess'

interface MenuItem {
  path: string
  title: string
  group?: string
}

const route = useRoute()
const auth = useAuthStore()

const layoutRecord = computed(() => {
  const candidates = route.matched.filter(
    (r) => r.children?.length && r.children.some((c) => c.meta?.title && !c.path.includes(':')),
  )
  return candidates.length ? candidates[candidates.length - 1] : undefined
})

const moduleTitle = computed(() => layoutRecord.value?.meta?.title as string | undefined)

const flatMenus = computed(() => {
  const layout = layoutRecord.value
  if (!layout?.children) return [] as MenuItem[]

  const base = layout.path.startsWith('/') ? layout.path : `/${layout.path}`
  const roles = auth.userRoles
  const menuPaths = auth.menuPaths
  return layout.children
    .filter((c) => c.meta?.title && !c.path.includes(':') && !c.meta?.hideInMenu)
    .map((c) => ({
      path: (c.meta?.menuPath as string | undefined) ?? `${base}/${c.path}`.replace(/\/+/g, '/'),
      title: String(c.meta!.title),
      group: c.meta?.menuGroup as string | undefined,
      roles: c.meta?.roles as string[] | undefined,
    }))
    .filter((m) => canAccessRoute(m.path, roles, menuPaths, m.roles))
    .map(({ path, title, group }) => ({ path, title, group }))
})

const menuGroups = computed(() => {
  const items = flatMenus.value
  if (!items.length) return []

  const hasGroup = items.some((m) => m.group)
  if (!hasGroup) {
    return [{ label: '', items }]
  }

  const map = new Map<string, MenuItem[]>()
  for (const m of items) {
    const key = m.group || '其他'
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(m)
  }
  return Array.from(map.entries()).map(([label, groupItems]) => ({ label, items: groupItems }))
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/hr')) return '/hr/employees'
  const exact = flatMenus.value.find((m) => m.path === route.path)
  if (exact) return exact.path
  const prefix = flatMenus.value.find((m) => route.path.startsWith(`${m.path}/`))
  if (prefix) return prefix.path
  if (route.path.startsWith('/warehouse/')) {
    const wh = flatMenus.value.find((m) => m.path.startsWith('/warehouse/'))
    if (wh) return wh.path
  }
  return route.path
})
</script>

<style scoped>
.menu-layout {
  display: flex;
  gap: 0;
  min-height: calc(100vh - 96px);
  margin: -20px;
}

.sub-sidebar {
  display: flex;
  flex-direction: column;
  width: 200px;
  flex-shrink: 0;
  background: var(--erp-bg-card);
  border-right: 1px solid var(--erp-border);
}

.sub-sidebar-title {
  padding: 14px 16px 10px;
  font-size: 13px;
  font-weight: 600;
  color: var(--erp-text-secondary);
  border-bottom: 1px solid var(--erp-border);
  letter-spacing: 0.02em;
}

.sub-menu-nav {
  flex: 1;
  overflow-y: auto;
  padding: 8px 8px 12px;
}

.menu-group + .menu-group {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid var(--erp-border);
}

.menu-group-label {
  padding: 4px 12px 6px;
  font-size: 11px;
  font-weight: 600;
  color: var(--erp-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

.sub-menu-link {
  display: block;
  padding: 0 12px;
  height: 38px;
  line-height: 38px;
  margin: 2px 0;
  border-radius: var(--erp-radius-md);
  font-size: 13px;
  color: var(--erp-text-secondary);
  text-decoration: none;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sub-menu-link:hover {
  color: var(--erp-color-primary);
  background: var(--erp-color-primary-light);
}

.sub-menu-link.active {
  color: var(--erp-color-primary);
  font-weight: 600;
  background: var(--erp-color-primary-light);
  box-shadow: inset 3px 0 0 var(--erp-color-primary);
}

.menu-content {
  flex: 1;
  min-width: 0;
  padding: 20px;
  overflow: auto;
}
</style>
