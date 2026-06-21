import { computed, ref } from 'vue'
import { useBaseStore } from '@/stores/_base'

export interface DeptNode {
  id: number
  parentId?: number | null
  deptName: string
  sort?: number
  status?: string
  children?: DeptNode[]
}

export interface DeptFlatOption {
  id: number
  deptName: string
  label: string
}

function flattenDepts(nodes: DeptNode[], prefix = ''): DeptFlatOption[] {
  const out: DeptFlatOption[] = []
  for (const n of nodes) {
    const label = prefix ? `${prefix} / ${n.deptName}` : n.deptName
    out.push({ id: n.id, deptName: n.deptName, label })
    if (n.children?.length) {
      out.push(...flattenDepts(n.children, label))
    }
  }
  return out
}

/** 从 /depts 加载部门树，供下拉选择 */
export function useDepartments(initialStatus = 'ACTIVE') {
  const api = useBaseStore().api
  const loading = ref(false)
  const tree = ref<DeptNode[]>([])
  const status = ref(initialStatus)

  const flatOptions = computed(() => flattenDepts(tree.value))

  const nameById = computed(() => {
    const map = new Map<number, string>()
    for (const o of flatOptions.value) {
      map.set(o.id, o.deptName)
    }
    return map
  })

  async function load(nextStatus?: string) {
    if (nextStatus !== undefined) {
      status.value = nextStatus
    }
    loading.value = true
    try {
      const res = await api.get('/depts', {
        params: { tree: true, status: status.value || undefined },
      })
      tree.value = (res?.data ?? res ?? []) as DeptNode[]
    } finally {
      loading.value = false
    }
  }

  function deptName(id?: number | null): string {
    if (id == null) return ''
    return nameById.value.get(id) ?? ''
  }

  return { loading, tree, flatOptions, nameById, load, deptName }
}
