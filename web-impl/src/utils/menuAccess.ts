import {
  canAccessModule as canAccessModuleByRole,
  canAccessRoute as canAccessRouteByRole,
  isAdmin,
  normalizeRoles,
} from '@/utils/roleAccess'

function normalizePath(path: string): string {
  return path.replace(/\/+/g, '/').replace(/\/$/, '') || '/'
}

/** 顶级模块 path（如 /dashboard、/sales），不作为子路由前缀授权 */
function isTopModulePath(path: string): boolean {
  return normalizePath(path).split('/').filter(Boolean).length === 1
}

/** 是否已加载 DB 菜单权限 */
export function hasDbMenuPaths(menuPaths: string[] | null | undefined): boolean {
  return Array.isArray(menuPaths) && menuPaths.length > 0
}

/** 顶级模块：path 本身或其子路由在授权列表中 */
export function canAccessModuleByMenu(
  modulePath: string,
  menuPaths: string[] | null | undefined,
): boolean {
  if (!hasDbMenuPaths(menuPaths)) return false
  const module = normalizePath(modulePath)
  return menuPaths!.some((allowed) => {
    const a = normalizePath(allowed)
    return a === module || a.startsWith(`${module}/`)
  })
}

/** 子菜单 / 详情页：精确或前缀匹配（顶级模块 path 不向下继承） */
export function canAccessRouteByMenu(
  path: string,
  menuPaths: string[] | null | undefined,
): boolean {
  if (!hasDbMenuPaths(menuPaths)) return false
  const route = normalizePath(path)

  // 授权菜单 path 为当前路由的前缀（详情页 / hideInMenu 子路由）
  for (const allowed of menuPaths!) {
    const a = normalizePath(allowed)
    if (route === a || route.startsWith(`${a}/`)) return true
  }

  // 逐层向上检查父级路径是否有权限
  // 例如 /production/workorder-detail/123 尝试匹配 /production/workorder-detail, /production
  const segments = route.split('/').filter(Boolean)

  for (let i = segments.length; i > 0; i--) {
    const parentPath = '/' + segments.slice(0, i).join('/')

    for (const allowed of menuPaths!) {
      const a = normalizePath(allowed)

      // 1. 精确匹配父级路径
      if (parentPath === a) return true
      // 2. 顶级模块（如 /production）允许所有子路径
      if (isTopModulePath(a) && parentPath.startsWith(a)) return true
      // 3. 父级路径是 allowed 的子路径
      if (parentPath.startsWith(`${a}/`)) return true
    }
  }

  return false
}

/** 顶级模块：优先 DB menuPaths，否则回退硬编码 RBAC */
export function canAccessModule(
  modulePath: string,
  userRoles: string[] | undefined | null,
  menuPaths?: string[] | null,
): boolean {
  if (isAdmin(normalizeRoles(userRoles))) return true
  if (hasDbMenuPaths(menuPaths)) {
    return canAccessModuleByMenu(modulePath, menuPaths)
  }
  return canAccessModuleByRole(modulePath, userRoles)
}

/** 子菜单 / 详情页路由 */
export function canAccessRoute(
  path: string,
  userRoles: string[] | undefined | null,
  menuPaths?: string[] | null,
  explicitRoles?: string[],
): boolean {
  if (isAdmin(normalizeRoles(userRoles))) return true
  if (explicitRoles?.length) {
    return canAccessRouteByRole(path, userRoles, explicitRoles)
  }
  if (hasDbMenuPaths(menuPaths)) {
    if (canAccessRouteByMenu(path, menuPaths)) return true
    // 已授权某模块菜单时，同模块 hideInMenu 子路由（料号详情等）按 RBAC 放行
    const module = '/' + (normalizePath(path).split('/').filter(Boolean)[0] ?? '')
    if (canAccessModuleByMenu(module, menuPaths)) {
      return canAccessRouteByRole(path, userRoles)
    }
    return canAccessRouteByRole(path, userRoles)
  }
  return canAccessRouteByRole(path, userRoles)
}
