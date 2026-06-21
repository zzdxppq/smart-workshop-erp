<template>
  <div class="anomaly-rate-gauge" data-testid="anomaly-rate-gauge">
    <h4 class="chart-title">异常率（REJECTED 占比）</h4>
    <div v-if="hasData" ref="chartRef" class="chart-canvas" data-testid="gauge-chart-canvas"></div>
    <div v-else class="chart-empty" data-testid="gauge-chart-empty">
      <el-icon><DocumentRemove /></el-icon>
      <span>暂无驳回事件数据</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onBeforeUnmount, ref, shallowRef, watch, computed } from 'vue'
import { DocumentRemove } from '@element-plus/icons-vue'
import * as echarts from 'echarts/core'
import { GaugeChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { WorkflowEventStats } from '@/api/generated/models/WorkflowEventStats'

/**
 * V1.3.9 Sprint 13 · Story 13.4 · 异常率仪表盘
 *
 * <p>数据源: `WorkflowEventStats.byEventType.REJECTED` / `totalCount`
 * <p>ECharts GaugeChart · 阈值 80% 红色 · 50% 橙色 · 30% 黄色
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

echarts.use([CanvasRenderer, GaugeChart, TitleComponent, TooltipComponent])

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
  const total = props.stats?.totalCount ?? 0
  return !!map && total > 0 && (map['REJECTED'] ?? 0) >= 0
})

/**
 * IMPL 注意事项 6.2：ECharts option 用 shallowRef 包裹
 */
function buildOption(stats: WorkflowEventStats | null): echarts.EChartsCoreOption {
  const map = stats?.byEventType || {}
  const total = stats?.totalCount ?? 0
  const rejected = (map['REJECTED'] || 0) as number
  const rate = total > 0 ? Math.round((rejected / total) * 1000) / 10 : 0
  return {
    tooltip: { formatter: `{a} <br/>异常率: ${rate}%` },
    series: [
      {
        name: '异常率',
        type: 'gauge',
        min: 0,
        max: 100,
        splitNumber: 5,
        radius: '85%',
        center: ['50%', '60%'],
        progress: { show: true, width: 18 },
        axisLine: {
          lineStyle: {
            width: 18,
            color: [
              [0.3, '#67C23A'],
              [0.5, '#E6A23C'],
              [0.8, '#F56C6C'],
              [1, '#C45656'],
            ],
          },
        },
        pointer: { width: 4 },
        axisTick: { show: true, distance: -22, length: 6, lineStyle: { color: '#fff' } },
        splitLine: { distance: -22, length: 12, lineStyle: { color: '#fff' } },
        axisLabel: { distance: -10, color: '#666', fontSize: 11 },
        anchor: { show: true, size: 14, itemStyle: { borderColor: '#666', borderWidth: 1 } },
        title: { offsetCenter: [0, '80%'], fontSize: 12, color: '#666' },
        detail: {
          valueAnimation: true,
          offsetCenter: [0, '40%'],
          formatter: '{value}%',
          fontSize: 22,
          color: '#303133',
        },
        data: [{ value: rate, name: 'REJECTED 占比' }],
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
.anomaly-rate-gauge {
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
