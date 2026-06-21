/** 机台列表行（对接 erp-production MachineService） */
export interface MachineRowView {
  id: number
  code: string
  name: string
  status?: string
  loadPercent: number
  oee: number
}

/** 工艺库列表行（对接 erp-production ProcessService） */
export interface ProcessRowView {
  id: number
  code: string
  name: string
  standardTime: number | string
  equipmentType: string
  processType?: string
}

export function normalizeMachineRow(raw: Record<string, unknown>, index = 0): MachineRowView {
  const load = Number(raw.loadPercent ?? raw.load ?? raw.oee ?? 50)
  return {
    id: Number(raw.id ?? index + 1),
    code: String(raw.code ?? raw.machineCode ?? raw.machineNo ?? `CNC-0${index + 1}`),
    name: String(raw.name ?? raw.machineName ?? raw.machineNo ?? raw.code ?? `机台-${index + 1}`),
    status: raw.status as string | undefined,
    loadPercent: load,
    oee: load,
  }
}

export function normalizeProcessRow(raw: Record<string, unknown>): ProcessRowView {
  return {
    id: Number(raw.id ?? 0),
    code: String(raw.processCode ?? raw.code ?? ''),
    name: String(raw.processName ?? raw.name ?? ''),
    standardTime: (raw.totalEstimatedHours ?? raw.standardTime ?? '—') as number | string,
    equipmentType: String(raw.processType ?? raw.equipmentType ?? raw.machineType ?? '—'),
    processType: raw.processType as string | undefined,
  }
}

export function normalizeMachineList(list: unknown[]): MachineRowView[] {
  return (list as Record<string, unknown>[]).map((m, i) => normalizeMachineRow(m, i))
}

export function normalizeProcessList(list: unknown[]): ProcessRowView[] {
  return (list as Record<string, unknown>[]).map(normalizeProcessRow)
}
