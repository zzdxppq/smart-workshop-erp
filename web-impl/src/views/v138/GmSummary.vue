<template>
  <ErpPageShell
    title="总经理汇总"
    description="采购、审批与委外核心指标及趋势分析，仅总经理与管理员可查看。"
  >
    <div v-loading="loading">
    <el-radio-group v-model="period" @change="loadData" style="margin-bottom: 16px">
      <el-radio-button value="LAST_7D">近 7 天</el-radio-button>
      <el-radio-button value="LAST_30D">近 30 天</el-radio-button>
      <el-radio-button value="LAST_90D">近 90 天</el-radio-button>
    </el-radio-group>

    <el-row v-if="data" :gutter="16">
      <el-col :span="8">
        <el-card>
          <el-statistic title="无订单采购 PO 数" :value="data.noOrderPoCount" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="无订单采购金额（元）" :value="data.noOrderPoAmount" :precision="2" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="紧急补料频次" :value="data.urgentReplenishCount" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card>
          <el-statistic
            title="金额阈值审批通过率"
            :value="Number((data.amountThresholdPassedRate * 100).toFixed(1))"
            suffix="%"
          />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic title="采购主管工作量" :value="data.procurementManagerWorkload" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card>
          <el-statistic
            title="委外成本占比"
            :value="Number((data.outsourceCostRatio * 100).toFixed(1))"
            suffix="%"
          />
        </el-card>
      </el-col>
    </el-row>

    <el-card v-if="data?.trendChart?.length" class="trend-card" header="30 天趋势">
      <v-chart :option="trendOption" autoresize style="height: 360px" />
    </el-card>
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import VChart from 'vue-echarts'
import type { GmSummaryResponse } from '@/api/generated/models/GmSummaryResponse'
import { use } from 'echarts/core'
import { CanvasRenderer } from 'echarts/renderers'
import { LineChart, BarChart } from 'echarts/charts'
import {
  TitleComponent,
  TooltipComponent,
  LegendComponent,
  GridComponent,
} from 'echarts/components'

use([CanvasRenderer, LineChart, BarChart, TitleComponent, TooltipComponent, LegendComponent, GridComponent])

/**
 * V1.3.8 Sprint 7 · Story 4.3 总经理汇总报表（前端骨架）
 *
 * 6 项指标 + trend_chart
 *
 * @author dev agent Opus 4.8 · 2026-06-13
 */

const period = ref('LAST_30D')
const data = ref<GmSummaryResponse | null>(null)
const loading = ref(false)

const trendOption = computed(() => {
  if (!data.value?.trendChart) return {}
  return {
    title: { text: '无订单采购趋势' },
    tooltip: { trigger: 'axis' },
    legend: { data: ['PO 数', '金额'] },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: {
      type: 'category',
      data: data.value.trendChart.map((p: any) => p.date),
    },
    yAxis: [
      { type: 'value', name: 'PO 数' },
      { type: 'value', name: '金额', position: 'right' },
    ],
    series: [
      {
        name: 'PO 数',
        type: 'bar',
        data: data.value.trendChart.map((p: any) => p.noOrderCount),
      },
      {
        name: '金额',
        type: 'line',
        yAxisIndex: 1,
        data: data.value.trendChart.map((p: any) => Number(p.amount)),
        smooth: true,
      },
    ],
  }
})

async function loadData() {
  loading.value = true
  try {
    data.value = unwrapResult<GmSummaryResponse>(
      await useBaseStore().api.get('/reports/gm-summary', { params: { period: period.value } }),
    )
  } catch (e: unknown) {
    ElMessage.error(`加载汇总失败：${(e as Error).message}`)
    data.value = null
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.trend-card {
  margin-top: 16px;
}
</style>