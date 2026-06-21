import { ref, computed } from 'vue'

/**
 * V1.3.7 Story 1.45 · 仓库权限 composable
 */
export function useWarehousePermission() {
  const warehouseId = ref<number | null>(null)
  const userId = ref<number | null>(null)
  const permissions = ref<string[]>([])

  const canInbound = computed(() => permissions.value.includes('INBOUND'))
  const canOutbound = computed(() => permissions.value.includes('OUTBOUND'))
  const canInventory = computed(() => permissions.value.includes('INVENTORY'))

  function setWarehouse(id: number) {
    warehouseId.value = id
  }

  function setPermissions(perms: string[]) {
    permissions.value = perms
  }

  function grant(perm: string) {
    if (!permissions.value.includes(perm)) permissions.value.push(perm)
  }

  function revoke(perm: string) {
    permissions.value = permissions.value.filter((p) => p !== perm)
  }

  return {
    warehouseId, userId, permissions,
    canInbound, canOutbound, canInventory,
    setWarehouse, setPermissions, grant, revoke,
  }
}