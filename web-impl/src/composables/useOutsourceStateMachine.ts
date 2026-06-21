import { ref, computed, type Ref } from 'vue'
import { ElMessage } from 'element-plus'
import { E6OutsourceStateMachineService } from '@/api/generated/services/E6OutsourceStateMachineService'
import type { OutsourceOrder } from '@/api/generated/models/OutsourceOrder'
import type { OutsourceStateHistory } from '@/api/generated/models/OutsourceStateHistory'
import { useAuthStore } from '@/stores/auth'
import { extractRoles } from '@/utils/jwt'

/**
 * V1.3.7 Story 1.18+ · 委外 7 状态机 composable（接 E6OutsourceStateMachineService）
 */
export const OutsourceState = ['DRAFT', 'SUBMITTED', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'REWORK', 'CLOSED'] as const
export type OutsourceState = (typeof OutsourceState)[number]

const FALLBACK_TRANSITIONS: Record<OutsourceState, OutsourceState[]> = {
  DRAFT: ['SUBMITTED'],
  SUBMITTED: ['ACCEPTED', 'REWORK'],
  ACCEPTED: ['IN_PROGRESS'],
  IN_PROGRESS: ['COMPLETED', 'REWORK'],
  COMPLETED: ['CLOSED'],
  REWORK: ['IN_PROGRESS', 'COMPLETED'],
  CLOSED: [],
}

function isOutsourceState(v: unknown): v is OutsourceState {
  return typeof v === 'string' && (OutsourceState as readonly string[]).includes(v)
}

function resolveOperatorRole(token: string): string {
  const roles = extractRoles(token)
  if (roles.includes('PROD_PLANNER')) return 'PROD_PLANNER'
  if (roles.includes('PURCHASER')) return 'PURCHASER'
  if (roles.includes('QC')) return 'QC'
  if (roles.includes('FINANCE')) return 'FINANCE'
  if (roles.includes('GM')) return 'GM'
  return roles[0] || 'PROD_PLANNER'
}

export function useOutsourceStateMachine(outsourceId: Ref<number | null>) {
  const authStore = useAuthStore()
  const currentState = ref<OutsourceState>('DRAFT')
  const history = ref<OutsourceStateHistory[]>([])
  const order = ref<OutsourceOrder | null>(null)
  const loading = ref(false)
  const transitions = ref<Record<string, OutsourceState[]>>({ ...FALLBACK_TRANSITIONS })

  const allowedNext = computed(() => transitions.value[currentState.value] ?? [])
  const isClosed = computed(() => currentState.value === 'CLOSED')

  async function loadMatrix() {
    try {
      const r = await E6OutsourceStateMachineService.getOutsourceStateMatrix()
      if (r?.code === 0 && r.data?.transitions) {
        const raw = r.data.transitions as Record<string, string[]>
        const mapped: Record<string, OutsourceState[]> = { ...FALLBACK_TRANSITIONS }
        for (const [from, targets] of Object.entries(raw)) {
          mapped[from] = targets.filter(isOutsourceState)
        }
        transitions.value = mapped
      }
    } catch {
      /* 矩阵可选，失败时用本地 fallback */
    }
  }

  async function loadCurrent() {
    if (outsourceId.value == null) return
    loading.value = true
    try {
      const r = await E6OutsourceStateMachineService.getOutsourceCurrentState(outsourceId.value)
      if (r?.code === 0 && r.data) {
        order.value = r.data
        const st = (r.data as OutsourceOrder & { status?: string }).status
        if (isOutsourceState(st)) currentState.value = st
      } else {
        ElMessage.error(r?.message || '加载委外单失败')
      }
    } catch (e: unknown) {
      ElMessage.error((e as { message?: string })?.message || '加载委外单失败')
    } finally {
      loading.value = false
    }
  }

  async function loadHistory() {
    if (outsourceId.value == null) return
    try {
      const r = await E6OutsourceStateMachineService.getOutsourceStateHistory(outsourceId.value)
      if (r?.code === 0) {
        history.value = r.data || []
      }
    } catch {
      history.value = []
    }
  }

  async function refresh() {
    await Promise.all([loadMatrix(), loadCurrent(), loadHistory()])
  }

  async function transition(to: OutsourceState, reason?: string) {
    if (outsourceId.value == null) {
      throw new Error('请先选择委外单')
    }
    if (!allowedNext.value.includes(to)) {
      throw new Error(`Invalid transition: ${currentState.value} → ${to}`)
    }
    loading.value = true
    try {
      const r = await E6OutsourceStateMachineService.advanceOutsourceState({
        outsourceId: outsourceId.value,
        targetState: to,
        operatorRole: resolveOperatorRole(authStore.token),
        reason,
      })
      if (r?.code === 0) {
        if (r.data) order.value = r.data
        if (isOutsourceState((r.data as OutsourceOrder & { status?: string })?.status)) {
          currentState.value = (r.data as OutsourceOrder & { status?: string }).status as OutsourceState
        } else {
          currentState.value = to
        }
        await loadHistory()
      } else {
        throw new Error(r?.message || '状态推进失败')
      }
    } finally {
      loading.value = false
    }
  }

  return {
    currentState,
    history,
    order,
    allowedNext,
    isClosed,
    loading,
    transition,
    loadCurrent,
    loadHistory,
    loadMatrix,
    refresh,
  }
}
