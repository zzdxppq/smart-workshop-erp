<template>
  <div>
    <el-tabs v-model="tab">
      <el-tab-pane label="薪酬列表" name="list">
        <el-alert type="info" :closable="false" title="作业人员仅可查看本人相关金额" style="margin-bottom: 12px" />
        <el-form :inline="true" class="erp-filter-bar">
          <el-form-item label="期间">
            <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="onSearch" />
          </el-form-item>
          <el-form-item>
            <el-button class="erp-btn-secondary" :loading="loading" @click="onSearch">查询</el-button>
            <el-button type="success" class="erp-btn-success" :loading="calculating" @click="runCalc">运行核算</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="loading" class="erp-table" :data="items" stripe>
          <el-table-column prop="empName" label="员工" min-width="100" />
          <el-table-column label="基本工资" width="100" align="right">
            <template #default="{ row }">
              <span class="erp-num-highlight">{{ row.base ?? row.baseSalary }}</span>
            </template>
          </el-table-column>
          <el-table-column label="计件" width="90" align="right">
            <template #default="{ row }">
              <span class="erp-num-highlight">{{ row.piecePay ?? '—' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="绩效奖" width="90" align="right">
            <template #default="{ row }">
              <span class="erp-num-highlight">{{ row.performanceBonus ?? row.bonus }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="overtime" label="加班" width="80" align="right">
            <template #default="{ row }">
              <span class="erp-num-highlight">{{ row.overtime ?? row.overtimePay }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="deduction" label="扣款" width="80" align="right" />
          <el-table-column prop="net" label="实发" width="100" align="right">
            <template #default="{ row }">
              <span class="erp-num-highlight">{{ row.net ?? row.netSalary }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <ErpStatusTag :status="row.status" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <el-button size="small" class="erp-btn-ghost" @click="$router.push(`/hr/payroll/${row.id}`)">详情</el-button>
              <el-button size="small" link type="primary" @click="downloadSlip(row.id)">工资条</el-button>
            </template>
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

      <el-tab-pane label="工资账套配置" name="package">
        <el-button type="primary" style="margin-bottom: 12px" @click="openPkg()">新增账套</el-button>
        <el-table v-loading="pkgLoading" :data="packages" stripe>
          <el-table-column prop="packageName" label="账套名称" min-width="140" />
          <el-table-column prop="position" label="适用岗位" width="120" />
          <el-table-column prop="baseSalary" label="基本工资" width="100" align="right" />
          <el-table-column prop="positionSalary" label="岗位工资" width="100" align="right" />
          <el-table-column prop="pieceUnitPrice" label="计件单价" width="100" align="right" />
          <el-table-column label="A级系数" width="90" align="right">
            <template #default="{ row }">{{ (Number(row.performanceARate) * 100).toFixed(0) }}%</template>
          </el-table-column>
          <el-table-column label="操作" width="80">
            <template #default="{ row }">
              <el-button link type="primary" @click="openPkg(row)">编辑</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="pkgVisible" :title="pkgForm.id ? '编辑工资账套' : '新增工资账套'" width="520px">
      <el-form :model="pkgForm" label-width="110px">
        <el-form-item label="账套名称"><el-input v-model="pkgForm.packageName" /></el-form-item>
        <el-form-item label="适用岗位"><el-input v-model="pkgForm.position" /></el-form-item>
        <el-form-item label="基本工资"><el-input-number v-model="pkgForm.baseSalary" :min="0" :step="100" /></el-form-item>
        <el-form-item label="岗位工资"><el-input-number v-model="pkgForm.positionSalary" :min="0" :step="100" /></el-form-item>
        <el-form-item label="计件单价"><el-input-number v-model="pkgForm.pieceUnitPrice" :min="0" :step="0.5" :precision="2" /></el-form-item>
        <el-form-item label="A级绩效系数"><el-input-number v-model="pkgForm.performanceARate" :min="0" :max="1" :step="0.05" :precision="2" /></el-form-item>
        <el-form-item label="B级绩效系数"><el-input-number v-model="pkgForm.performanceBRate" :min="0" :max="1" :step="0.05" :precision="2" /></el-form-item>
        <el-form-item label="社保比例"><el-input-number v-model="pkgForm.socialInsuranceRate" :min="0" :max="1" :step="0.01" :precision="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pkgVisible = false">取消</el-button>
        <el-button type="primary" :loading="pkgSaving" @click="savePkg">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useHrStore } from '@/stores/hr'
import { usePayroll } from '@/composables/usePayroll'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const hrStore = useHrStore()
const tab = ref('list')
const period = ref<string>('2026-06')
const calculating = ref(false)
const pkgLoading = ref(false)
const pkgSaving = ref(false)
const packages = ref<any[]>([])
const pkgVisible = ref(false)
const pkgForm = ref<any>({})
const { setItems } = usePayroll()

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  hrStore.listPayrolls({ period: period.value, ...params }),
)

watch(items, (list) => setItems(list), { immediate: true })

function onSearch() {
  pageNum.value = 1
  reload({ period: period.value })
}
function onPageChange() {
  reload({ period: period.value })
}

async function runCalc() {
  calculating.value = true
  try {
    const r = await hrStore.calculatePayrollBatch({ period: period.value })
    ElMessage.success(`薪酬核算完成：新增 ${r?.created ?? 0} 条，跳过 ${r?.skipped ?? 0} 条`)
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '核算失败')
  } finally {
    calculating.value = false
  }
}

async function downloadSlip(id: number) {
  window.open(hrStore.payrollPdfUrl(id), '_blank')
}

async function loadPackages() {
  pkgLoading.value = true
  try {
    packages.value = await hrStore.listSalaryPackages()
  } finally {
    pkgLoading.value = false
  }
}

function openPkg(row?: any) {
  pkgForm.value = row
    ? { ...row }
    : {
        packageName: '',
        position: '',
        baseSalary: 5000,
        positionSalary: 2000,
        pieceUnitPrice: 8.5,
        performanceARate: 0.2,
        performanceBRate: 0.1,
        performanceCRate: 0.05,
        performanceDRate: 0,
        socialInsuranceRate: 0.105,
        taxThreshold: 5000,
        taxRate: 0.1,
        isDefault: 0,
      }
  pkgVisible.value = true
}

async function savePkg() {
  pkgSaving.value = true
  try {
    await hrStore.saveSalaryPackage(pkgForm.value)
    ElMessage.success('已保存')
    pkgVisible.value = false
    loadPackages()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    pkgSaving.value = false
  }
}

onMounted(() => {
  onSearch()
  loadPackages()
})
</script>
