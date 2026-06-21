<template>
  <div class="workload-by-approver" data-testid="workload-by-approver">
    <h4 class="chart-title">按审批角色工作量</h4>
    <div v-if="hasData" ref="chartRef" class="chart-canvas" data-testid="workload-chart-canvas"></div>
    <div v-else class="chart-empty" data-testid="workload-chart-empty">
      <el-icon><DocumentRemove /></el-icon>
      <span>暂无审批工作量数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, shallowRef, watch, computed } from 'vue'
import { DocumentRemove } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { WorkflowEventStats } from '@/api/generated/models/WorkflowEventStats'

/**
 * V1.3.9 Sprint 13 · Story 13.4 · 按审批角色工作量柱状图
 *
 * <p>数据源: `WorkflowEventStats.byApproverRole`
 * <p>ECharts BarChart · 横向柱状图 · PROCUREMENT_MANAGER / SALES_MANAGER / GM / ADMIN
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

echarts.use([CanvasRenderer, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

const props = withDefaults(defineProps<{
  stats: WorkflowEventStats | null
  height?: string
}>(), {
  height: '320px',
})

const chartRef = ref<HTMLDivElement | null>(null)
const chartInstance = shallowRef<echarts.ECharts | null>(null)
const chartOption = shallowRef<echarts.EChartsCoreOption>({})

const hasData = computed(() => {
  const map = props.stats?.byApproverRole
  return !!map && Object.keys(map).length > 0
})

/**
 * IMPL 注意事项 6.2：ECharts option 用 shallowRef 包裹（避免深度响应式开销）
 */
function buildOption(stats: WorkflowEventStats | null): echarts.EChartsCoreOption {
  const map = stats?.byApproverRole || {}
  const roles = Object.keys(map)
  const counts = Object.values(map)
  return {
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', data: roles, axisLabel: { interval: 0, rotate: 0 } },
    yAxis: { type: 'value', name: '事件数' },
    series: [
      {
        name: '审批事件数',
        type: 'bar',
        data: counts,
        itemStyle: { color: '#409EFF', borderRadius: [4, 4, 0, 0] },
        barMaxWidth: 60,
        label: { show: true, position: 'top' },
      },
    ],
  }
}

function initChart() {
  if (!chartRef.value) return
  if (chartInstance.value) {
    chartInstance.value.dispose()
  }
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
  if (chartInstance.value) {
    updateChart()
  }
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
 * IMPL 注意事项 6.3：onUnmounted 销毁 ECharts 实例（避免内存泄漏）
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
.workload-by-approver {
  background: #fff;
  border-radius: 6px;
  padding: 12px;
  height: 100%;
  display: flex;
  flex-direction: column;
}
.chart-title {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #303133;
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
