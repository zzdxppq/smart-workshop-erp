import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.15 · 排产甘特图 composable
 */
export function useSchedule() {
  const schedules = ref<any[]>([])

  const ganttData = computed(() => {
    return schedules.value.map(s => ({
      id: s.workorderId,
      name: `工单 ${s.workorderId}`,
      start: s.planStart,
      end: s.planEnd,
      equipment: s.equipmentType,
      status: s.status,
    }))
  })

  const conflicts = computed(() => {
    const result: any[] = []
    const map: any = {}
    for (const s of schedules.value) {
      const key = s.equipmentId + '-' + s.planStart
      if (map[key]) {
        result.push({ s1: map[key], s2: s })
      } else {
        map[key] = s
      }
    }
    return result
  })

  function setSchedules(data: any[]) {
    schedules.value = data
  }

  return { schedules, ganttData, conflicts, setSchedules }
}
