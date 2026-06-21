<template>
  <ErpPageShell
    title="待办任务中心"
    description="工程师待处理任务汇总；从「报价工艺定义」「订单工程转化」转入的任务会自动出现在日历。"
  >
    <el-row :gutter="16">
      <el-col :md="10">
        <el-card header="待办列表" shadow="never">
          <el-table v-loading="loading" :data="tasks" size="small" stripe max-height="420">
            <el-table-column prop="refNo" label="单号" min-width="130" />
            <el-table-column label="来源" width="120">
              <template #default="{ row }">
                {{ row.source === 'QUOTE_PROCESS' ? '报价工艺定义' : '订单工程转化' }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <EngineerTaskStatusTag :phase="row.phase" />
              </template>
            </el-table-column>
            <el-table-column prop="dueDate" label="计划日" width="110" />
            <el-table-column label="操作" width="100" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="goTask(row)">打开</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !tasks.length" description="暂无待办" />
        </el-card>
      </el-col>
      <el-col :md="14">
        <el-card header="任务日历（自动生成）" shadow="never">
          <ScheduleCalendar :events="calendarEvents" :show-title="false" />
        </el-card>
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import ScheduleCalendar from '@/views/production/ScheduleCalendar.vue'
import EngineerTaskStatusTag from '@/components/engineering/EngineerTaskStatusTag.vue'
import type { EngineerTaskRecord } from '@/utils/engineeringTask'
import { useEngineeringStore } from '@/stores/engineering'

const router = useRouter()
const eng = useEngineeringStore()
const loading = ref(false)

const tasks = ref<EngineerTaskRecord[]>([])

const calendarEvents = computed(() =>
  tasks.value
    .filter((t) => t.dueDate && t.phase !== 'COMPLETED')
    .map((t) => ({
      title: `${t.source === 'QUOTE_PROCESS' ? '报价' : '订单'} · ${t.refNo}`,
      date: t.dueDate!,
    })),
)

async function refresh() {
  loading.value = true
  try {
    const list = await eng.listTasks()
    tasks.value = list
      .filter((t) => t.phase !== 'COMPLETED')
      .sort((a, b) => a.phase.localeCompare(b.phase))
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    tasks.value = []
  } finally {
    loading.value = false
  }
}

function goTask(row: EngineerTaskRecord | Record<string, unknown>) {
  const task = row as EngineerTaskRecord
  if (task.source === 'QUOTE_PROCESS') {
    void router.push({ path: '/engineering/quote-confirmation', query: { refId: String(task.refId) } })
  } else {
    void router.push({ path: '/engineering/order-conversion', query: { refId: String(task.refId) } })
  }
}

onMounted(refresh)
</script>
