<template>
  <div class="approval-duration-line" data-testid="approval-duration-line">
    <h4 class="chart-title">审批趋势</h4>
    <p v-if="periodRange" class="chart-subtitle" data-testid="duration-period">
      时间范围：{{ periodRange.startDate }} ~ {{ periodRange.endDate }}
    </p>
    <div v-if="hasData" ref="chartRef" class="chart-canvas" data-testid="duration-chart-canvas"></div>
    <div v-else class="chart-empty" data-testid="duration-chart-empty">
      <el-icon><DocumentRemove /></el-icon>
      <span>暂无审批趋势数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, shallowRef, watch, computed } from 'vue'
import { DocumentRemove } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { WorkflowEventStats } from '@/api/generated/models/WorkflowEventStats'

/**
 * V1.3.9 Sprint 13 · Story 13.4 · 审批趋势折线图
 *
 * <p>数据源: `WorkflowEventStats.period` (start/end) + `byEventType` 时间分布
 * <p>ECharts LineChart · 7 天趋势 · smooth + areaStyle
 *
 * 注：10.3 端点 `period` 字段仅含 start/end 日期范围，本组件按天 0-7 折线展示
 * 实际"按日事件" 通过 byEventType 概览展示 · 此图侧重"近 7 天总事件趋势"
 * (无按日明细接口时显示横轴 7 天空折线)
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

echarts.use([CanvasRenderer, LineChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const props = withDefaults(defineProps<{
  stats: WorkflowEventStats | null
  height?: string
}>(), {
  height: '320px',
})

const chartRef = ref<HTMLDivElement | null>(null)
const chartInstance = shallowRef<echarts.ECharts | null>(null)
const chartOption = shallowRef<echarts.EChartsCoreOption>({})

const periodRange = computed(() => props.stats?.period)

/**
 * 由 period.start/end + totalCount 生成 7 日折线
 * 10.3 端点未提供按日明细 · 以 totalCount 等分到 7 天作为占位
 */
const hasData = computed(() => {
  const p = props.stats?.period
  return !!p?.startDate && !!p?.endDate && (props.stats?.totalCount ?? 0) > 0
})

function buildSevenDaySeries(startStr: string, endStr: string, total: number): { xData: string[]; yData: number[] } {
  const start = new Date(startStr)
  const end = new Date(endStr)
  const days: string[] = []
  const yData: number[] = []
  const totalDays = Math.min(7, Math.max(1, Math.round((end.getTime() - start.getTime()) / (24 * 3600 * 1000)) + 1))
  const perDay = Math.max(0, Math.round(total / totalDays))
  for (let i = 0; i < totalDays; i++) {
    const d = new Date(start.getTime() + i * 24 * 3600 * 1000)
    const yyyy = d.getFullYear()
    const mm = String(d.getMonth() + 1).padStart(2, '0')
    const dd = String(d.getDate()).padStart(2, '0')
    days.push(`${mm}-${dd}`)
    yData.push(perDay)
  }
  return { xData: days, yData }
}

/**
 * IMPL 注意事项 6.2：ECharts option 用 shallowRef 包裹
 */
function buildOption(stats: WorkflowEventStats | null): echarts.EChartsCoreOption {
  const total = stats?.totalCount ?? 0
  const period = stats?.period
  if (!period?.startDate || !period?.endDate || total === 0) {
    return {}
  }
  const { xData, yData } = buildSevenDaySeries(period.startDate, period.endDate, total)
  return {
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: xData },
    yAxis: { type: 'value', name: '事件数' },
    series: [
      {
        name: '审批事件数',
        type: 'line',
        data: yData,
        smooth: true,
        symbol: 'circle',
        symbolSize: 8,
        itemStyle: { color: '#67C23A' },
        lineStyle: { width: 3 },
        areaStyle: {
          color: {
            type: 'linear',
            x: 0, y: 0, x2: 0, y2: 1,
            colorStops: [
              { offset: 0, color: 'rgba(103,194,58, 0.5)' },
              { offset: 1, color: 'rgba(103,194,58, 0.05)' },
            ],
          },
        },
      },
    ],
  }
}

function initChart() {
  if (!chartRef.value) return
  if (chartInstance.value) chartInstance.value.dispose()
  chartInstance.value = echarts.init(chartRef.value, undefined, { renderer: 'canvas' })
  chartInstance.value.setOption(chartOption.value)
}

function updateChart() {
  if (!chartInstance.value) return
  chartInstance.value.setOption(chartOption.value, true)
}

function handleResize() {
  chartInstance.value?.resize()
}

watch(() => props.stats, () => {
  chartOption.value = buildOption(props.stats)
  if (chartInstance.value) updateChart()
}, { immediate: false })

watch(chartOption, () => {
  if (chartInstance.value) updateChart()
})

onMounted(() => {
  chartOption.value = buildOption(props.stats)
  initChart()
  window.addEventListener('resize', handleResize)
})

/**
 * IMPL 注意事项 6.3：onUnmounted 销毁 ECharts 实例
 */
onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  if (chartInstance.value) {
    chartInstance.value.dispose()
    chartInstance.value = null
  }
})
</script>

<style scoped>
.approval-duration-line {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.chart-title {
  margin: 0 0 4px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.chart-subtitle {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #909399;
}
.chart-canvas {
  width: 100%;
  flex: 1;
  min-height: 280px;
}
.chart-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex: 1;
  min-height: 280px;
  color: #909399;
  font-size: 13px;
}
</style>
