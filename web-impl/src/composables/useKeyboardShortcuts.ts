import { onMounted, onUnmounted, ref } from 'vue'

export type ShortcutAction =
  | 'figureSearch'
  | 'saveDraft'
  | 'submit'
  | 'toggleTax'
  | 'newItem'
  | 'refresh'
  | 'closeDrawer'
  | 'globalSearch'

export interface ShortcutBinding {
  action: ShortcutAction
  keys: string
  label: string
  handler?: () => void
}

const DEFAULT_BINDINGS: ShortcutBinding[] = [
  { action: 'figureSearch', keys: 'F8', label: '图号联想' },
  { action: 'saveDraft', keys: 'ctrl+s', label: '保存草稿' },
  { action: 'submit', keys: 'ctrl+enter', label: '提交审批' },
  { action: 'toggleTax', keys: 'alt+t', label: '含税切换' },
  { action: 'newItem', keys: 'n', label: '新建' },
  { action: 'refresh', keys: 'r', label: '刷新' },
  { action: 'closeDrawer', keys: 'escape', label: '关闭抽屉' },
  { action: 'globalSearch', keys: 'ctrl+k', label: '全局搜索' },
  { action: 'globalSearch', keys: '/', label: '全局搜索' },
  { action: 'figureSearch', keys: 'f2', label: 'BOM 编辑' },
  { action: 'saveDraft', keys: 'enter', label: '保存节点' },
]

const STORAGE_KEY = 'erp-keyboard-shortcuts'

function loadBindings(): ShortcutBinding[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw) as ShortcutBinding[]
  } catch { /* ignore */ }
  return DEFAULT_BINDINGS.map((b) => ({ ...b }))
}

function normalizeKey(e: KeyboardEvent): string {
  const parts: string[] = []
  if (e.ctrlKey || e.metaKey) parts.push('ctrl')
  if (e.altKey) parts.push('alt')
  if (e.shiftKey) parts.push('shift')
  const key = e.key.length === 1 ? e.key.toLowerCase() : e.key.toLowerCase()
  if (!['control', 'alt', 'shift', 'meta'].includes(key)) parts.push(key)
  return parts.join('+')
}

/**
 * 全局/页面级快捷键（Spec 8.1 · 可配置）
 */
export function useKeyboardShortcuts(handlers: Partial<Record<ShortcutAction, () => void>> = {}) {
  const bindings = ref(loadBindings())
  const enabled = ref(true)

  function saveBindings(next: ShortcutBinding[]) {
    bindings.value = next
    localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  }

  function resetBindings() {
    saveBindings(DEFAULT_BINDINGS.map((b) => ({ ...b })))
  }

  function onKeyDown(e: KeyboardEvent) {
    if (!enabled.value) return
    const target = e.target as HTMLElement
    if (target.tagName === 'INPUT' || target.tagName === 'TEXTAREA') {
      const combo = normalizeKey(e)
      const allowedInInput = ['f8', 'ctrl+s', 'ctrl+enter', 'alt+t', 'escape', 'ctrl+k']
      if (!allowedInInput.includes(combo)) return
    }
    const combo = normalizeKey(e)
    for (const b of bindings.value) {
      if (b.keys.toLowerCase() === combo) {
        const fn = handlers[b.action] ?? b.handler
        if (fn) {
          e.preventDefault()
          fn()
        }
        break
      }
    }
  }

  onMounted(() => window.addEventListener('keydown', onKeyDown))
  onUnmounted(() => window.removeEventListener('keydown', onKeyDown))

  return { bindings, enabled, saveBindings, resetBindings, DEFAULT_BINDINGS }
}

export function getShortcutLabel(action: ShortcutAction): string {
  const b = loadBindings().find((x) => x.action === action)
  return b?.keys.toUpperCase() ?? action
}
