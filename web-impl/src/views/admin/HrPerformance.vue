<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="绩效列表" name="list">
        <el-form :inline="true" class="erp-filter-bar">
          <el-form-item label="期间">
            <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="onSearch" />
          </el-form-item>
          <el-form-item>
            <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
            <el-button type="primary" :loading="calculating" @click="runAutoCalc">自动核算</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" class="erp-table" :data="items" stripe>
          <el-table-column prop="employeeName" label="员工" min-width="100" />
          <el-table-column prop="department" label="部门" min-width="100" />
          <el-table-column prop="score" label="绩效分" width="100" align="right">
            <template #default="{ row }">
              <span class="erp-num-highlight">{{ row.score ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="grade" label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="gradeType(row.grade)" size="small">{{ row.grade ?? '—' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="kpiItems" label="考核项" min-width="180" show-overflow-tooltip />
          <el-table-column label="期间" width="110">
            <template #default="{ row }">{{ row.periodYear }}-{{ String(row.periodMonth).padStart(2, '0') }}</template>
          </el-table-column>
        </el-table>
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          class="erp-pagination"
          @current-change="onPageChange"
          @size-change="onSearch"
        />
      </el-tab-pane>

      <el-tab-pane label="考核方案配置" name="scheme">
        <el-button type="primary" style="margin-bottom: 12px" @click="openScheme()">新增方案</el-button>
        <el-table v-loading="schemeLoading" :data="schemes" stripe>
          <el-table-column prop="schemeName" label="方案名称" min-width="140" />
          <el-table-column prop="position" label="适用岗位" width="120" />
          <el-table-column label="产量权重" width="100" align="right">
            <template #default="{ row }">{{ row.outputWeight }}%</template>
          </el-table-column>
          <el-table-column label="质量权重" width="100" align="right">
            <template #default="{ row }">{{ row.qualityWeight }}%</template>
          </el-table-column>
          <el-table-column label="出勤权重" width="100" align="right">
            <template #default="{ row }">{{ row.attendanceWeight }}%</template>
          </el-table-column>
          <el-table-column label="默认" width="70">
            <template #default="{ row }">
              <el-tag v-if="row.isDefault" size="small" type="success">是</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="openScheme(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="绩效申诉" name="appeals">
        <el-table v-loading="appealLoading" :data="appeals" stripe>
          <el-table-column prop="employeeName" label="员工" width="100" />
          <el-table-column label="期间" width="100">
            <template #default="{ row }">{{ row.periodYear }}-{{ String(row.periodMonth).padStart(2, '0') }}</template>
          </el-table-column>
          <el-table-column prop="reason" label="申诉理由" min-width="180" show-overflow-tooltip />
          <el-table-column label="状态" width="100">
            <template #default="{ row }"><ErpStatusTag :status="row.status" /></template>
          </el-table-column>
          <el-table-column prop="reply" label="回复" min-width="140" show-overflow-tooltip />
          <el-table-column label="操作" width="160">
            <template #default="{ row }">
              <template v-if="row.status === 'PENDING'">
                <el-button link type="success" @click="resolveAppeal(row.id, 'APPROVED')">通过</el-button>
                <el-button link type="danger" @click="resolveAppeal(row.id, 'REJECTED')">驳回</el-button>
              </template>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="schemeVisible" :title="schemeForm.id ? '编辑考核方案' : '新增考核方案'" width="480px">
      <el-form :model="schemeForm" label-width="100px">
        <el-form-item label="方案名称"><el-input v-model="schemeForm.schemeName" /></el-form-item>
        <el-form-item label="适用岗位"><el-input v-model="schemeForm.position" placeholder="如 CNC 操作员" /></el-form-item>
        <el-form-item label="产量权重%"><el-input-number v-model="schemeForm.outputWeight" :min="0" :max="100" /></el-form-item>
        <el-form-item label="质量权重%"><el-input-number v-model="schemeForm.qualityWeight" :min="0" :max="100" /></el-form-item>
        <el-form-item label="出勤权重%"><el-input-number v-model="schemeForm.attendanceWeight" :min="0" :max="100" /></el-form-item>
        <el-form-item label="默认方案"><el-switch v-model="schemeForm.isDefault" :active-value="1" :inactive-value="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="schemeVisible = false">取消</el-button>
        <el-button type="primary" :loading="schemeSaving" @click="saveScheme">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useHrStore } from '@/stores/hr'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const hrStore = useHrStore()
const tab = ref('list')
const period = ref('2026-06')
const calculating = ref(false)
const schemeLoading = ref(false)
const schemeSaving = ref(false)
const schemes = ref<any[]>([])
const schemeVisible = ref(false)
const schemeForm = ref<any>({ outputWeight: 40, qualityWeight: 30, attendanceWeight: 30, isDefault: 0 })
const appealLoading = ref(false)
const appeals = ref<any[]>([])

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  hrStore.listPerformances({ period: period.value, ...params }),
)

function gradeType(g: string) {
  if (g === 'A') return 'success'
  if (g === 'B') return 'info'
  if (g === 'C') return 'warning'
  return 'danger'
}

function onSearch() {
  pageNum.value = 1
  reload({ period: period.value })
}
function onPageChange() {
  reload({ period: period.value })
}

async function runAutoCalc() {
  calculating.value = true
  try {
    const r = await hrStore.calculatePerformance({ period: period.value })
    ElMessage.success(`绩效核算完成：新增 ${r?.created ?? 0}，更新 ${r?.updated ?? 0}`)
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '核算失败')
  } finally {
    calculating.value = false
  }
}

async function loadSchemes() {
  schemeLoading.value = true
  try {
    schemes.value = await hrStore.listPerformanceSchemes()
  } finally {
    schemeLoading.value = false
  }
}

async function loadAppeals() {
  appealLoading.value = true
  try {
    appeals.value = await hrStore.listPerformanceAppeals()
  } finally {
    appealLoading.value = false
  }
}

async function resolveAppeal(id: number, status: string) {
  try {
    await hrStore.resolvePerformanceAppeal(id, { status, reply: status === 'APPROVED' ? '申诉通过' : '申诉驳回' })
    ElMessage.success('已处理')
    loadAppeals()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '处理失败')
  }
}

function openScheme(row?: any) {
  schemeForm.value = row
    ? { ...row }
    : { schemeName: '', position: '', outputWeight: 40, qualityWeight: 30, attendanceWeight: 30, isDefault: 0 }
  schemeVisible.value = true
}

async function saveScheme() {
  schemeSaving.value = true
  try {
    await hrStore.savePerformanceScheme(schemeForm.value)
    ElMessage.success('已保存')
    schemeVisible.value = false
    loadSchemes()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    schemeSaving.value = false
  }
}

onMounted(() => {
  onSearch()
  loadSchemes()
  loadAppeals()
})
</script>
