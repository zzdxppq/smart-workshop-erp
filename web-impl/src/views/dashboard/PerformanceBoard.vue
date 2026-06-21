<template>
  <div v-loading="loading" class="performance-board" :class="{ 'tv-mode': tvMode }">
    <div class="board-header">
      <div>
        <h2>生产绩效看板</h2>
        <p class="subtitle">操作工个人 / 每台机 生产数据实时排行</p>
      </div>
      <div v-if="!tvMode" class="controls">
        <el-radio-group v-model="period" size="small" @change="reload">
          <el-radio-button value="day">今日</el-radio-button>
          <el-radio-button value="week">本周</el-radio-button>
          <el-radio-button value="month">本月</el-radio-button>
        </el-radio-group>
        <el-button size="small" style="margin-left: 12px" @click="reload">刷新</el-button>
      </div>
      <div v-else class="tv-indicator">
        <span class="live-dot" />
        车间大屏 · {{ tvSlideLabel }} · {{ tvCountdown }}s 后切换
      </div>
    </div>

    <DashboardKpiGrid v-if="!tvMode" :items="kpiItems" :span="6" />

    <el-card v-if="!tvMode" shadow="never" header="近 30 天趋势" style="margin-bottom: 16px">
      <v-chart :option="trendChart" autoresize style="height: 280px" />
    </el-card>

    <el-row v-if="!tvMode" :gutter="16">
      <el-col :md="12">
        <el-card shadow="never" header="操作工个人排行">
          <RankTable :rows="operatorRank" group-by="operator" @operator-click="openDetail" />
        </el-card>
        <p class="hint">点击员工姓名可查看个人生产明细</p>
      </el-col>
      <el-col :md="12">
        <el-card shadow="never" header="机台排行">
          <RankTable :rows="machineRank" group-by="machine" />
        </el-card>
      </el-col>
    </el-row>

    <div v-else class="tv-carousel">
      <transition name="tv-fade" mode="out-in">
        <div v-if="tvSlide === 0" key="operator" class="tv-panel">
          <h3>操作工产量排行 · {{ periodLabel }}</h3>
          <RankTable :rows="operatorRank" group-by="operator" large />
        </div>
        <div v-else-if="tvSlide === 1" key="machine" class="tv-panel">
          <h3>机台产出排行 · {{ periodLabel }}</h3>
          <RankTable :rows="machineRank" group-by="machine" large />
        </div>
        <div v-else key="trend" class="tv-panel">
          <h3>近 30 天产量 / 合格率趋势</h3>
          <v-chart :option="trendChartTv" autoresize class="tv-chart" />
        </div>
      </transition>
      <div class="tv-dots">
        <span v-for="i in 3" :key="i" class="dot" :class="{ active: tvSlide === i - 1 }" />
      </div>
    </div>

    <OperatorDetailDrawer
      v-model:open="detailOpen"
      :operator-id="detailOperatorId"
      :operator-name="detailOperatorName"
      :period="period"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import VChart from 'vue-echarts'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import RankTable from '@/components/dashboard/PerformanceRankTable.vue'
import DashboardKpiGrid from '@/components/dashboard/DashboardKpiGrid.vue'
import OperatorDetailDrawer from '@/components/dashboard/OperatorDetailDrawer.vue'

const route = useRoute()
const api = useBaseStore().api
const tvMode = ref(route.query.mode === 'tv')
const loading = ref(false)
const period = ref('day')
const operatorRank = ref<Record<string, unknown>[]>([])
const machineRank = ref<Record<string, unknown>[]>([])
const summary = ref<Record<string, unknown>>({})
const trend = ref<Record<string, unknown>[]>([])
const tvSlide = ref(0)
const tvCountdown = ref(15)
const detailOpen = ref(false)
const detailOperatorId = ref<number | null>(null)
const detailOperatorName = ref('')

let reloadTimer: ReturnType<typeof setInterval> | undefined
let carouselTimer: ReturnType<typeof setInterval> | undefined
let countdownTimer: ReturnType<typeof setInterval> | undefined

const periodLabel = computed(() =>
  ({ day: '今日', week: '本周', month: '本月' })[period.value] ?? '今日',
)
const tvSlideLabel = computed(() => ['操作工排行', '机台排行', '30天趋势'][tvSlide.value])

const kpiItems = computed(() => [
  {
    key: 'qty',
    label: '总产量',
    value: Number(summary.value.totalQty ?? 0).toLocaleString(),
    suffix: ' 件',
  },
  {
    key: 'pass',
    label: '平均合格率',
    value: Math.round(Number(summary.value.avgPassRate ?? 0) * 1000) / 10,
    suffix: '%',
    color: '#3fb950',
  },
  {
    key: 'staff',
    label: '在岗人数',
    value: `${summary.value.onDutyCount ?? 0}/${summary.value.totalStaff ?? 0}`,
  },
  {
    key: 'util',
    label: '设备稼动率',
    value: Math.round(Number(summary.value.equipmentUtilization ?? 0) * 1000) / 10,
    suffix: '%',
    color: '#58a6ff',
  },
])

const trendChart = computed(() => buildTrendOption())
const trendChartTv = computed(() => buildTrendOption())

function buildTrendOption() {
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['产量', '合格率%'] },
    grid: { left: 48, right: 48, bottom: 32 },
    xAxis: {
      type: 'category',
      data: trend.value.map((t) => String(t.statDate ?? '').slice(5)),
    },
    yAxis: [
      { type: 'value', name: '产量' },
      { type: 'value', name: '%', max: 100 },
    ],
    series: [
      {
        name: '产量',
        type: 'line',
        smooth: true,
        yAxisIndex: 0,
        data: trend.value.map((t) => Number(t.finishedQty ?? 0)),
      },
      {
        name: '合格率%',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        lineStyle: { type: 'dashed' },
        data: trend.value.map((t) => Math.round(Number(t.passRate ?? 0) * 1000) / 10),
      },
    ],
  }
}

async function fetchBoard(periodVal: string, groupByVal: string) {
  return unwrapResult<Record<string, unknown>>(
    await api.get('/dashboard/performance', { params: { period: periodVal, groupBy: groupByVal } }),
  )
}

function openDetail(row: Record<string, unknown>) {
  detailOperatorId.value = Number(row.operatorId ?? 0) || null
  detailOperatorName.value = String(row.operatorName ?? '')
  detailOpen.value = true
}

async function reload() {
  loading.value = true
  try {
    const board = await fetchBoard(period.value, 'operator')
    operatorRank.value = (board.operatorList as Record<string, unknown>[]) ?? (board.list as Record<string, unknown>[]) ?? []
    machineRank.value = (board.machineList as Record<string, unknown>[]) ?? []
    summary.value = (board.summary as Record<string, unknown>) ?? {}
    const tr = unwrapResult<Record<string, unknown>[]>(
      await api.get('/dashboard/performance/trend', { params: { days: 30 } }),
    )
    trend.value = Array.isArray(tr) ? tr : []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function nextTvSlide() {
  tvSlide.value = (tvSlide.value + 1) % 3
  tvCountdown.value = 15
}

function startTvCarousel() {
  tvCountdown.value = 15
  countdownTimer = setInterval(() => {
    tvCountdown.value = Math.max(0, tvCountdown.value - 1)
  }, 1000)
  carouselTimer = setInterval(nextTvSlide, 15_000)
}

function stopTvCarousel() {
  if (carouselTimer) clearInterval(carouselTimer)
  if (countdownTimer) clearInterval(countdownTimer)
}

watch(() => route.query.mode, (m) => {
  tvMode.value = m === 'tv'
  if (tvMode.value) {
    period.value = 'week'
    startTvCarousel()
  } else {
    stopTvCarousel()
  }
  reload()
})

onMounted(() => {
  if (tvMode.value) {
    period.value = 'week'
    document.body.classList.add('erp-tv-mode')
    startTvCarousel()
  }
  reload()
  reloadTimer = setInterval(reload, tvMode.value ? 60_000 : 30_000)
})
onUnmounted(() => {
  document.body.classList.remove('erp-tv-mode')
  stopTvCarousel()
  if (reloadTimer) clearInterval(reloadTimer)
})
</script>

<style scoped>
.performance-board { padding: 16px; }
.board-header { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 16px; flex-wrap: wrap; gap: 12px; }
.board-header h2 { margin: 0; }
.subtitle { margin: 4px 0 0; font-size: 13px; color: var(--erp-text-secondary); }
.controls { display: flex; align-items: center; flex-wrap: wrap; }
.hint { margin: 8px 0 0; font-size: 12px; color: var(--erp-text-muted); }
.tv-indicator { font-size: 14px; color: var(--erp-text-secondary); display: flex; align-items: center; gap: 8px; }
.live-dot { width: 8px; height: 8px; border-radius: 50%; background: #3fb950; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.4; } }

.tv-mode {
  min-height: 100vh;
  padding: 24px 32px;
  background: #0d1117;
  color: #e6edf3;
}
.tv-mode .board-header h2 { font-size: 32px; color: #fff; }
.tv-carousel { min-height: calc(100vh - 100px); }
.tv-panel h3 { font-size: 26px; margin: 0 0 20px; color: #58a6ff; }
.tv-chart { height: 480px; width: 100%; }
.tv-dots { display: flex; justify-content: center; gap: 10px; margin-top: 24px; }
.tv-dots .dot { width: 10px; height: 10px; border-radius: 50%; background: #484f58; }
.tv-dots .dot.active { background: #58a6ff; }
.tv-fade-enter-active, .tv-fade-leave-active { transition: opacity 0.6s ease; }
.tv-fade-enter-from, .tv-fade-leave-to { opacity: 0; }
</style>

<style>
body.erp-tv-mode .el-aside,
body.erp-tv-mode .erp-sidebar,
body.erp-tv-mode .layout-header { display: none !important; }
body.erp-tv-mode .el-main { padding: 0 !important; max-width: 100% !important; }
</style>
