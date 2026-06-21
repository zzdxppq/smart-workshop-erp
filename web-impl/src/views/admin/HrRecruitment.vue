<template>
  <div>
    <div class="erp-toolbar">
      <el-button type="primary" class="erp-btn-primary" @click="showCreate = true">新建招聘需求</el-button>
    </div>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="position" label="岗位" min-width="120" />
      <el-table-column prop="department" label="部门" min-width="100" />
      <el-table-column prop="headcount" label="人数" width="80" align="right">
        <template #default="{ row }">
          <span class="erp-num-highlight">{{ row.headcount ?? 1 }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="110">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" class="erp-btn-ghost" @click="viewDetail(row)">详情</el-button>
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

    <el-dialog v-model="showCreate" title="新建招聘需求" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="岗位" required>
          <el-input v-model="form.position" />
        </el-form-item>
        <el-form-item label="部门">
          <el-select v-model="form.department" clearable placeholder="选择部门" style="width: 100%" :loading="deptsLoading">
            <el-option v-for="d in flatOptions" :key="d.id" :label="d.label" :value="d.deptName" />
          </el-select>
        </el-form-item>
        <el-form-item label="人数">
          <el-input-number v-model="form.headcount" :min="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button class="erp-btn-ghost" @click="showCreate = false">取消</el-button>
        <el-button type="primary" class="erp-btn-primary" :loading="creating" @click="create">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useHrStore } from '@/stores/hr'
import { usePagedList } from '@/composables/usePagedList'
import { useDepartments } from '@/composables/useDepartments'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const { flatOptions, loading: deptsLoading, load: loadDepts } = useDepartments()

const hrStore = useHrStore()
const showCreate = ref(false)
const creating = ref(false)
const form = ref({ position: '', department: '', headcount: 1 })

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  hrStore.listRecruitments(params),
)

function onSearch() {
  pageNum.value = 1
  reload()
}
function onPageChange() {
  reload()
}

function viewDetail(row: { id?: number }) {
  if (row.id) ElMessage.info(`招聘需求 #${row.id}（详情页开发中）`)
}

async function create() {
  if (!form.value.position?.trim()) {
    ElMessage.warning('请填写岗位')
    return
  }
  creating.value = true
  try {
    await hrStore.createRecruitment(form.value)
    ElMessage.success('已创建')
    showCreate.value = false
    form.value = { position: '', department: '', headcount: 1 }
    onSearch()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '创建失败')
  } finally {
    creating.value = false
  }
}

onMounted(() => {
  loadDepts()
  onSearch()
})
</script>
