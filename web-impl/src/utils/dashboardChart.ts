import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'

use([CanvasRenderer, BarChart, LineChart, PieChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

export type DashboardMetric = { name?: string; value?: number | string; unit?: string }

export function metricValue(metrics: DashboardMetric[] | undefined, name: string, fallback = 0): number {
  const found = metrics?.find((m) => m.name === name)
  if (found?.value == null) return fallback
  return Number(found.value) || fallback
}

export function buildBarOption(metrics: DashboardMetric[] | undefined, title: string) {
  const list = metrics ?? []
  const textColor =
    typeof document !== 'undefined'
      ? getComputedStyle(document.documentElement).getPropertyValue('--erp-text-secondary').trim() || '#94a3b8'
      : '#94a3b8'
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 14, color: textColor } },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: list.map((m) => m.name ?? ''),
      axisLabel: { color: textColor },
    },
    yAxis: { type: 'value', axisLabel: { color: textColor } },
    series: [{
      type: 'bar',
      data: list.map((m) => Number(m.value ?? 0)),
      itemStyle: { color: '#3b66f5' },
    }],
  }
}

export function buildTrendOption(trend: Record<string, unknown>[] | undefined, title: string) {
  const rows = trend ?? []
  return {
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: rows.map((r) => String(r.period ?? r.name ?? '')) },
    yAxis: { type: 'value' },
    series: [{
      type: 'line',
      smooth: true,
      data: rows.map((r) => Number(r.value ?? r.totalAmount ?? r.total_profit ?? 0)),
      areaStyle: { opacity: 0.08 },
    }],
  }
}

export function rankMedalClass(rank: number) {
  if (rank <= 3) return 'rank-gold'
  if (rank <= 10) return 'rank-silver'
  return 'rank-normal'
}
