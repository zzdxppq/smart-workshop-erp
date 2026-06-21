import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.13 · 库位树 composable
 */
export function useLocationTree() {
  const tree = ref<any[]>([])

  const warehouseCodes = computed(() => tree.value.map(w => w.warehouseCode))
  const allLocations = computed(() => {
    const result: any[] = []
    for (const w of tree.value) {
      for (const [zone, locs] of Object.entries(w.zones || {})) {
        for (const loc of (locs as any[])) {
          result.push({ ...loc, warehouse: w.warehouseCode, zone })
        }
      }
    }
    return result
  })

  function setTree(data: any[]) {
    tree.value = data
  }

  return { tree, warehouseCodes, allLocations, setTree }
}
