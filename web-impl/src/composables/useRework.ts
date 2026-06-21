import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.31 · 返修 composable
 */
export function useRework() {
  const reworkNo = ref<string>('')
  const workorderNo = ref<string>('')
  const reason = ref<string>('')
  const priority = ref<'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'>('NORMAL')
  const alerted = ref(false)

  const isUrgent = computed(() => priority.value === 'URGENT')

  function alert() {
    alerted.value = true
  }

  function setPriority(p: typeof priority.value) {
    priority.value = p
  }

  return { reworkNo, workorderNo, reason, priority, alerted, isUrgent, alert, setPriority }
}