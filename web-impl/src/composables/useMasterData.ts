import { useBaseStore } from '@/stores/_base'
import { useMaterialStore } from '@/stores/material'
import { useSourcingStore } from '@/stores/sourcing'
import { useWorkorderStore } from '@/stores/workorder'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

export interface VendorOption {
  id: number
  vendorCode?: string
  vendorName?: string
}

export interface MaterialOption {
  id: number
  materialCode: string
  materialName?: string
  spec?: string
}

export interface CustomerOption {
  id: number
  customerName?: string
  customerCode?: string
}

export interface PoOption {
  id: number
  poNo?: string
  supplierName?: string
  status?: string
}

export interface WorkorderOption {
  id: number
  workorderNo?: string
  materialCode?: string
  productName?: string
}

let vendorsCache: VendorOption[] | null = null
let materialsCache: MaterialOption[] | null = null
let customersCache: CustomerOption[] | null = null
let posCache: PoOption[] | null = null
let workordersCache: WorkorderOption[] | null = null

export function vendorLabel(v: VendorOption): string {
  const name = v.vendorName || '未命名厂商'
  const code = v.vendorCode ? ` · ${v.vendorCode}` : ''
  return `${name}${code}`
}

export function materialLabel(m: MaterialOption): string {
  const name = m.materialName ? ` · ${m.materialName}` : ''
  const spec = m.spec ? ` (${m.spec})` : ''
  return `${m.materialCode}${name}${spec}`
}

export function customerLabel(c: CustomerOption): string {
  const name = c.customerName || '未命名客户'
  return c.customerCode ? `${name} · ${c.customerCode}` : name
}

export function poLabel(p: PoOption): string {
  const no = p.poNo || `PO#${p.id}`
  const vendor = p.supplierName ? ` · ${p.supplierName}` : ''
  return `${no}${vendor}`
}

export function workorderLabel(w: WorkorderOption): string {
  const no = w.workorderNo || `WO#${w.id}`
  const mat = w.materialCode ? ` · ${w.materialCode}` : ''
  return `${no}${mat}`
}

export function useMasterData() {
  async function loadVendors(force = false): Promise<VendorOption[]> {
    if (vendorsCache && !force) return vendorsCache
    const { items } = parsePageItems(
      await useSourcingStore().listVendors({ pageNum: 1, pageSize: 500 }),
    )
    vendorsCache = items as VendorOption[]
    return vendorsCache
  }

  async function loadMaterials(force = false): Promise<MaterialOption[]> {
    if (materialsCache && !force) return materialsCache
    const raw = await useMaterialStore().listMaterials()
    const data = unwrapResult<MaterialOption[] | { items?: MaterialOption[] }>(raw)
    materialsCache = Array.isArray(data) ? data : (data?.items ?? [])
    return materialsCache
  }

  async function loadCustomers(force = false): Promise<CustomerOption[]> {
    if (customersCache && !force) return customersCache
    const { items } = parsePageItems(
      await useBaseStore().api.get('/customers', { params: { pageNum: 1, pageSize: 500 } }),
    )
    customersCache = (items as CustomerOption[]).map((c) => ({
      id: c.id,
      customerName: (c as { customerName?: string; name?: string }).customerName
        ?? (c as { name?: string }).name,
      customerCode: (c as { customerCode?: string; code?: string }).customerCode
        ?? (c as { code?: string }).code,
    }))
    return customersCache
  }

  async function loadPos(force = false): Promise<PoOption[]> {
    if (posCache && !force) return posCache
    const { items } = parsePageItems(
      await useSourcingStore().listPos({ pageNum: 1, pageSize: 500 }),
    )
    posCache = items as PoOption[]
    return posCache
  }

  async function loadWorkorders(force = false): Promise<WorkorderOption[]> {
    if (workordersCache && !force) return workordersCache
    const { items } = parsePageItems(
      await useWorkorderStore().listWorkorders({ pageNum: 1, pageSize: 500 }),
    )
    workordersCache = items as WorkorderOption[]
    return workordersCache
  }

  function resolveMaterialId(codeOrId: string | number | undefined, materials: MaterialOption[]): number | undefined {
    if (codeOrId == null) return undefined
    if (typeof codeOrId === 'number') return codeOrId
    const byCode = materials.find((m) => m.materialCode === codeOrId)
    return byCode?.id
  }

  return {
    loadVendors,
    loadMaterials,
    loadCustomers,
    loadPos,
    loadWorkorders,
    resolveMaterialId,
    invalidateCustomersCache: () => { customersCache = null },
    vendorLabel,
    materialLabel,
    customerLabel,
    poLabel,
    workorderLabel,
  }
}
