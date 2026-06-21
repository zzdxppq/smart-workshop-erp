import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.28 · FA 首件检 composable
 */
export function useFa() {
  const faNo = ref<string>('')
  const status = ref<'DRAFT' | 'SUBMITTED' | 'APPROVED' | 'REJECTED'>('DRAFT')
  const dimensions = ref<any[]>([])
  const items = ref<any[]>([])

  const approved = computed(() => status.value === 'APPROVED')
  const rejected = computed(() => status.value === 'REJECTED')

  function setDimensions(data: any[]) {
    dimensions.value = data
  }

  function addItem(item: any) {
    items.value.push(item)
  }

  function reset() {
    faNo.value = ''
    status.value = 'DRAFT'
    dimensions.value = []
    items.value = []
  }

  return { faNo, status, dimensions, items, approved, rejected, setDimensions, addItem, reset }
}