/**
 * Gateway Nacos 路由：/erp-{service}/auth/login
 */
export type ErpServiceId = 'erp-platform' | 'erp-business' | 'erp-production'

const SERVICE_PREFIX_RE = /^\/erp-(platform|business|production)(\/|$)/

const PLATFORM_PREFIXES = [
  '/auth', '/users', '/roles', '/depts', '/audit', '/print', '/label-templates', '/printers',
  '/changelogs', '/thresholds', '/params', '/dicts', '/platform', '/admin/workflows',
  '/admin/users', '/admin/email-templates', '/admin/field-encryption',
  '/email',
  '/workflows', '/approvals', '/files', '/app/login', '/app/sync', '/app/messages',
  '/app/scan/route',
]

const PRODUCTION_PREFIXES = [
  '/workorders', '/production', '/outsource-eta', '/outsource-quality',
  '/outsource-incoming', '/outsource-states', '/outsource-switches',
  '/app/production', '/app/workorders', '/app/transfer', '/reworks', '/mrp',
  '/machines', '/processes', '/allocations', '/outsub',
  '/dashboard/performance',
]

function matchPrefix(path: string, prefix: string): boolean {
  return path === prefix || path.startsWith(`${prefix}/`)
}

function matchAny(path: string, prefixes: string[]): boolean {
  return prefixes.some((p) => matchPrefix(path, p))
}

export function resolveServiceId(path: string): ErpServiceId {
  const p = path.split('?')[0]

  if (matchPrefix(p, '/ws') || matchPrefix(p, '/sse')) return 'erp-business'
  if (matchPrefix(p, '/outsource-cost')) return 'erp-business'
  if (matchPrefix(p, '/app/scan') && !matchPrefix(p, '/app/scan/route')) return 'erp-business'

  if (/^\/products\/[^/]+\/routes(\/|$)/.test(p)) return 'erp-production'
  if (matchPrefix(p, '/outsource')) return 'erp-production'
  if (matchAny(p, PRODUCTION_PREFIXES)) return 'erp-production'
  if (matchAny(p, PLATFORM_PREFIXES)) return 'erp-platform'

  return 'erp-business'
}

export function normalizeLogicalPath(url: string): string {
  if (SERVICE_PREFIX_RE.test(url)) {
    return url.replace(/^\/erp-(platform|business|production)/, '') || '/'
  }
  return url.startsWith('/') ? url : `/${url}`
}

function buildGatewayPath(service: ErpServiceId, logicalPath: string): string {
  const p = logicalPath.startsWith('/') ? logicalPath : `/${logicalPath}`
  return `/${service}${p}`
}

export function resolveGatewayUrl(input: string): string {
  if (!input) return input

  const isHttp = /^https?:\/\//i.test(input)
  const isWs = /^wss?:\/\//i.test(input)

  if (isHttp || isWs) {
    try {
      const u = new URL(input)
      if (SERVICE_PREFIX_RE.test(u.pathname)) return input
      const logical = normalizeLogicalPath(u.pathname)
      if (!logical.startsWith('/')) return input
      u.pathname = buildGatewayPath(resolveServiceId(logical), logical)
      return u.toString()
    } catch {
      return input
    }
  }

  const [pathPart, query = ''] = input.split('?')
  if (SERVICE_PREFIX_RE.test(pathPart)) return input

  const logical = normalizeLogicalPath(pathPart)
  if (!logical.startsWith('/')) return input

  const q = query ? `?${query}` : ''
  return `${buildGatewayPath(resolveServiceId(logical), logical)}${q}`
}

export function installGatewayFetchPatch(): void {
  if (typeof window === 'undefined' || (window as any).__erpGatewayFetchPatched) return
  const rawFetch = window.fetch.bind(window)
  window.fetch = (input: RequestInfo | URL, init?: RequestInit) => {
    if (typeof input === 'string') {
      return rawFetch(resolveGatewayUrl(input), init)
    }
    if (input instanceof URL) {
      return rawFetch(new URL(resolveGatewayUrl(input.toString())), init)
    }
    return rawFetch(input, init)
  }
  ;(window as any).__erpGatewayFetchPatched = true
}
