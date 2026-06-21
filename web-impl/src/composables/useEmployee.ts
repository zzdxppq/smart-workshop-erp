import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.41 · 员工 composable
 */
export function useEmployee() {
  const employees = ref<any[]>([])
  const keyword = ref<string>('')
  const deptId = ref<number | null>(null)

  const filtered = computed(() => {
    let list = employees.value
    if (keyword.value) {
      list = list.filter((e) => e.name?.includes(keyword.value) || e.code?.includes(keyword.value))
    }
    if (deptId.value !== null) {
      list = list.filter((e) => e.deptId === deptId.value)
    }
    return list
  })

  function setEmployees(data: any[]) {
    employees.value = data
  }

  function setKeyword(k: string) {
    keyword.value = k
  }

  function setDept(id: number | null) {
    deptId.value = id
  }

  return { employees, keyword, deptId, filtered, setEmployees, setKeyword, setDept }
}