<template>
  <div>
    <el-alert type="info" :closable="false" show-icon title="系统账号说明"
      description="此处为只读查看 sys_user 登录账号。新建员工时可选择「开通登录」自动创建账号；禁用账号、分配角色等高级操作需系统管理员进入「管理 → 用户/角色」。"
      style="margin-bottom: 12px" />
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="用户名/姓名">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>
    <el-table v-loading="loading" class="erp-table" :data="items" stripe>
      <el-table-column prop="username" label="登录名" min-width="120" />
      <el-table-column prop="realName" label="姓名" min-width="100" />
      <el-table-column prop="role" label="角色" width="120" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column prop="email" label="邮箱" min-width="160" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useBaseStore } from '@/stores/_base'
import { usePagedList } from '@/composables/usePagedList'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const keyword = ref('')

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  useBaseStore().api.get('/users', {
    params: { keyword: keyword.value || undefined, ...params },
  }),
)

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

onMounted(onSearch)
</script>
