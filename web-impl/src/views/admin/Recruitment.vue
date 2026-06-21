<template>
  <div>
    <h2>招聘管理</h2>
    <el-button type="success" @click="$router.push('/admin/recruitment-create')">发布职位</el-button>
    <el-table v-loading="loading" :data="items" stripe border style="margin-top: 12px">
      <el-table-column prop="title" label="职位" min-width="140" />
      <el-table-column prop="deptName" label="部门" min-width="100" />
      <el-table-column prop="qty" label="招聘数" width="80" />
      <el-table-column prop="applied" label="申请数" width="80" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="postDate" label="发布日期" width="120" />
      <el-table-column label="操作" width="90" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/admin/recruitment-detail/${row.id}`)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      layout="total, prev, pager, next"
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useHrStore } from '@/stores/hr'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const hrStore = useHrStore()

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

onMounted(onSearch)
</script>
