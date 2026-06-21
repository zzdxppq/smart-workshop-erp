/** 工程师待办任务状态（报价工艺定义 / 订单工程转化） */
export type EngineerTaskPhase = 'PENDING' | 'IN_PROGRESS' | 'COMPLETED'

export type EngineerTaskSource = 'QUOTE_PROCESS' | 'ORDER_CONVERSION'

export interface EngineerProcessStep {
  id: string
  processCode: string
  processName: string
  machineType: string
  estimatedMinutes: number
  /** 订单场景：细化参数 */
  spindleRpm?: number
  feedRate?: number
  cutDepth?: number
  toolNo?: string
}

export interface EngineerTaskRecord {
  source: EngineerTaskSource
  refId: number
  refNo: string
  title: string
  phase: EngineerTaskPhase
  steps?: EngineerProcessStep[]
  bomLines?: { materialCode: string; materialName: string; qty: number; unit: string }[]
  updatedAt?: string
  dueDate?: string
  workbenchId?: number
}

export const ENGINEER_TASK_PHASE_LABEL: Record<EngineerTaskPhase, string> = {
  PENDING: '待处理',
  IN_PROGRESS: '处理中',
  COMPLETED: '已完成',
}

/** 工艺库常用工序（车床/CNC/放电/线割等） */
export const DEFAULT_PROCESS_LIBRARY: Omit<EngineerProcessStep, 'id'>[] = [
  { processCode: 'LATHE', processName: '车床', machineType: '数控车床', estimatedMinutes: 60 },
  { processCode: 'CNC', processName: 'CNC 加工', machineType: 'CNC加工中心', estimatedMinutes: 90 },
  { processCode: 'EDM', processName: '放电', machineType: '电火花', estimatedMinutes: 45 },
  { processCode: 'WEDM', processName: '线割', machineType: '慢走丝线切割', estimatedMinutes: 50 },
  { processCode: 'MILL', processName: '铣床', machineType: '铣床', estimatedMinutes: 70 },
  { processCode: 'GRIND', processName: '磨床', machineType: '精密磨床', estimatedMinutes: 40 },
  { processCode: 'QC', processName: '检验', machineType: '三坐标', estimatedMinutes: 30 },
]

export function matchesEngineerPhaseFilter(phase: EngineerTaskPhase, filter?: EngineerTaskPhase | '') {
  if (!filter) return true
  return phase === filter
}

/** 从 API 行数据读取工程师阶段（quote-queue / order-queue 返回 engineerPhase） */
export function engineerPhaseFromRow(row: { engineerPhase?: EngineerTaskPhase; _engineerPhase?: EngineerTaskPhase }) {
  return row.engineerPhase ?? row._engineerPhase ?? 'PENDING'
}
