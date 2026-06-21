import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.32 · RFQ 询比价 composable
 */
export function useRfq() {
  const rfqId = ref<number | null>(null)
  const status = ref<'DRAFT' | 'QUOTING' | 'AWARDED' | 'CLOSED'>('DRAFT')
  const quotes = ref<any[]>([])
  const winner = ref<any>(null)

  const isAwardable = computed(() => status.value === 'QUOTING' && quotes.value.length >= 2)

  function setQuotes(data: any[]) {
    quotes.value = data
  }

  function pickWinner(q: any) {
    winner.value = q
  }

  function reset() {
    rfqId.value = null
    status.value = 'DRAFT'
    quotes.value = []
    winner.value = null
  }

  return { rfqId, status, quotes, winner, isAwardable, setQuotes, pickWinner, reset }
}