<template>
  <div class="outsource-panel">
    <h2>{{ selectedId ? '委外状态机' : '委外面板' }}</h2>
    <el-alert type="info" :closable="false" title="按委外状态分组展示；到货扫码/过站请在 Android APP 操作。" style="margin-bottom: 12px" />

    <div v-if="selectedId" v-loading="stateLoading" class="state-focus">
      <div class="state-focus__toolbar">
        <el-button class="erp-btn-ghost" @click="clearFocus">← 返回全部委外单</el-button>
      </div>
      <h3>委外单 #{{ stateOrder?.outsourceNo ?? selectedId }}</h3>
      <el-descriptions v-if="stateOrder" :column="2" border style="margin-bottom: 12px">
        <el-descriptions-item label="委外单号">{{ stateOrder.outsourceNo ?? '—' }}</el-descriptions-item>
        <el-descriptions-item label="当前状态">
          <ErpStatusTag :status="currentState ?? stateOrder.status" />
        </el-descriptions-item>
      </el-descriptions>
      <el-button
        v-for="s in allowedNext"
        :key="s"
        type="primary"
        size="small"
        :disabled="stateLoading"
        @click="doTransition(s)"
      >
        → {{ outsourceStateLabel(s) }}
      </el-button>
    </div>

    <template v-else>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="工单号">
        <el-input v-model="keyword" clearable placeholder="WO..." @keyup.enter="reload" />
      </el-form-item>
      <el-form-item label="返修≥2">
        <el-switch v-model="reworkOnly" @change="reload" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="reload">刷新</el-button>
      </el-form-item>
    </el-form>

    <el-collapse v-model="expandedGroups">
      <el-collapse-item v-for="g in OUTSOURCE_PANEL_GROUPS" :key="g.key" :name="g.key">
        <template #title>
          <span>{{ g.label }}</span>
          <el-tag size="small" style="margin-left: 8px">{{ groupRows(g.states).length }}</el-tag>
        </template>
        <el-table :data="groupRows(g.states)" stripe border size="small" :row-class-name="rowClassName">
          <el-table-column prop="outsourceNo" label="委外单号" min-width="150" />
          <el-table-column prop="workorderNo" label="工单" min-width="130" />
          <el-table-column prop="supplierName" label="厂商" min-width="110" />
          <el-table-column prop="processName" label="工序" min-width="100" />
          <el-table-column label="状态" width="110">
            <template #default="{ row }">
              <el-tag :type="outsourceStateTagType(row.status)" size="small">
                {{ outsourceStateLabel(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reworkCount" label="返修" width="70">
            <template #default="{ row }">
              <span :class="{ 'rework-warn': (row.reworkCount ?? 0) >= 2 }">{{ row.reworkCount ?? 0 }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deliveryDate" label="计划交期" width="110" />
          <el-table-column label="操作" width="160" fixed="right">
            <template #default="{ row }">
              <el-button size="small" link @click="goDetail(row)">详情</el-button>
              <el-button size="small" link type="primary" @click="goState(row)">状态机</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!groupRows(g.states).length" description="暂无数据" />
      </el-collapse-item>
    </el-collapse>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useOutsourceStore } from '@/stores/outsource'
import { parsePageItems } from '@/utils/apiPage'
import {
  OUTSOURCE_PANEL_GROUPS,
  outsourceStateLabel,
  outsourceStateTagType,
} from '@/constants/outsourceStates'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useOutsourceStateMachine, type OutsourceState } from '@/composables/useOutsourceStateMachine'

interface OutsourceRow {
  id?: number
  outsourceNo?: string
  workorderNo?: string
  supplierName?: string
  processName?: string
  status?: string
  reworkCount?: number
  deliveryDate?: string
}

const router = useRouter()
const route = useRoute()
const outsourceStore = useOutsourceStore()
const loading = ref(false)
const keyword = ref('')
const reworkOnly = ref(false)
const rows = ref<OutsourceRow[]>([])
const expandedGroups = ref(OUTSOURCE_PANEL_GROUPS.map((g) => g.key))

const selectedId = computed(() => {
  const q = route.query.outsourceId
  const id = q ? Number(q) : NaN
  return Number.isFinite(id) && id > 0 ? id : null
})
const selectedIdRef = ref<number | null>(null)
watch(selectedId, (v) => { selectedIdRef.value = v }, { immediate: true })

const {
  currentState,
  allowedNext,
  order: stateOrder,
  loading: stateLoading,
  transition,
  refresh: refreshState,
} = useOutsourceStateMachine(selectedIdRef)

watch(selectedIdRef, (id) => {
  if (id) refreshState()
})

function groupRows(states: string[]) {
  return rows.value.filter((r) => states.includes(String(r.status ?? '')))
}

function isOverdue(row: OutsourceRow) {
  const d = String(row.deliveryDate ?? '')
  if (!d) return false
  return new Date(d) < new Date() && !['COMPLETED', 'CLOSED', 'STORED'].includes(String(row.status ?? ''))
}

function rowClassName({ row }: { row: OutsourceRow }) {
  return isOverdue(row) ? 'tr-overdue' : ''
}

function goDetail(row: OutsourceRow) {
  if (row.outsourceNo) router.push(`/production/outsource-detail/${row.outsourceNo}`)
}

function goState(row: OutsourceRow) {
  if (row.id) {
    router.push({ path: '/production/outsub-panel', query: { outsourceId: String(row.id) } })
  }
}

function clearFocus() {
  router.push({ path: '/production/outsub-panel' })
}

async function doTransition(s: OutsourceState) {
  try {
    let reason: string | undefined
    if (s === 'REWORK') {
      const { value } = await ElMessageBox.prompt('请输入返修原因', '状态推进', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /.+/,
        inputErrorMessage: '原因不能为空',
      })
      reason = value
    }
    await transition(s, reason)
    ElMessage.success('状态已更新')
    await reload()
  } catch (e: unknown) {
    if (e === 'cancel') return
    ElMessage.error((e as { message?: string })?.message || '推进失败')
  }
}

async function reload() {
  loading.value = true
  try {
    const r = await outsourceStore.listOrders({
      workorderNo: keyword.value || undefined,
      pageNum: 1,
      pageSize: 200,
    })
    const { items } = parsePageItems(r)
    rows.value = (items as OutsourceRow[]).filter((row) =>
      !reworkOnly.value || (row.reworkCount ?? 0) >= 2,
    )
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    rows.value = []
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (selectedId.value) {
    refreshState()
  } else {
    reload()
  }
})
</script>

<style scoped>
.outsource-panel {
  padding: 16px;
}
.state-focus {
  padding: 12px 0;
}
.state-focus__toolbar {
  margin-bottom: 12px;
}
.rework-warn {
  color: var(--el-color-danger);
  font-weight: 700;
}
:deep(.tr-overdue) {
  background-color: rgba(245, 108, 108, 0.12) !important;
}
</style>
