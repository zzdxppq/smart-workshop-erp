import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.30 · 不良品 composable
 */
export type DefectLevel = 'MINOR' | 'MAJOR' | 'CRITICAL'

export function useDefect() {
  const defectNo = ref<string>('')
  const level = ref<DefectLevel>('MINOR')
  const reworkable = ref(true)
  const scrapped = ref(false)
  const cause = ref<string>('')
  const measures = ref<string>('')

  const isCritical = computed(() => level.value === 'CRITICAL')
  const canScrapped = computed(() => level.value === 'MAJOR' || level.value === 'CRITICAL')

  function setLevel(l: DefectLevel) {
    level.value = l
  }

  function markScrapped() {
    if (!canScrapped.value) return false
    scrapped.value = true
    reworkable.value = false
    return true
  }

  return {
    defectNo, level, reworkable, scrapped, cause, measures,
    isCritical, canScrapped, setLevel, markScrapped,
  }
}