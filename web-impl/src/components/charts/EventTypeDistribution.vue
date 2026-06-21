<template>
  <div class="event-type-distribution" data-testid="event-type-distribution">
    <h4 class="chart-title">按事件类型分布</h4>
    <div v-if="hasData" ref="chartRef" class="chart-canvas" data-testid="event-type-chart-canvas"></div>
    <div v-else class="chart-empty" data-testid="event-type-chart-empty">
      <el-icon><DocumentRemove /></el-icon>
      <span>暂无事件类型数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, shallowRef, watch, computed } from 'vue'
import { DocumentRemove } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { PieChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { WorkflowEventStats } from '@/api/generated/models/WorkflowEventStats'

/**
 * V1.3.9 Sprint 13 · Story 13.4 · 按事件类型分布饼图
 *
 * <p>数据源: `WorkflowEventStats.byEventType`
 * <p>ECharts PieChart · 环形饼图 · CREATED / APPROVED / REJECTED / DELEGATED
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

echarts.use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent, LegendComponent])

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
  const map = props.stats?.byEventType
  return !!map && Object.keys(map).length > 0
})

/**
 * IMPL 注意事项 6.2：ECharts option 用 shallowRef 包裹
 */
function buildOption(stats: WorkflowEventStats | null): echarts.EChartsCoreOption {
  const map = stats?.byEventType || {}
  const data = Object.entries(map).map(([name, value]) => ({ name, value }))
  return {
    tooltip: {
      trigger: 'item',
      formatter: '{a} <br/>{b}: {c} ({d}%)',
    },
    legend: { orient: 'vertical', left: 'left', top: 'middle' },
    series: [
      {
        name: '事件类型',
        type: 'pie',
        radius: ['40%', '70%'],
        center: ['60%', '50%'],
        avoidLabelOverlap: true,
        itemStyle: {
          borderRadius: 4,
          borderColor: '#fff',
          borderWidth: 2,
        },
        label: { show: true, formatter: '{b}\n{d}%' },
        labelLine: { show: true },
        data,
        color: ['#67C23A', '#409EFF', '#F56C6C', '#E6A23C'],
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
.event-type-distribution {
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
