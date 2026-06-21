<template>
  <ErpPageShell title="料号查询" description="输入料号，查看 7 Tab 完整料号详情。">
    <el-form :inline="true" @submit.prevent="onSearch">
      <el-form-item label="料号">
        <el-input v-model="code" clearable placeholder="如 WL-0001、RM-STEEL-45" style="width: 240px" @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询详情</el-button>
      </el-form-item>
    </el-form>
    <el-alert v-if="error" type="error" :title="error" show-icon style="margin-top: 12px" />
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'

const router = useRouter()
const route = useRoute()
const api = useBaseStore().api
const code = ref('')
const loading = ref(false)
const error = ref('')

async function onSearch() {
  const kw = code.value.trim()
  if (!kw) {
    error.value = '请输入料号'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = unwrapResult<{ id?: number }>(await api.get('/materials/lookup', { params: { code: kw } }))
    if (!data?.id) {
      error.value = '未找到该料号'
      return
    }
    router.push({ name: 'MaterialDetail', params: { id: String(data.id) } })
  } catch (e: unknown) {
    error.value = (e as { message?: string })?.message || '查询失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  const q = route.query.code as string | undefined
  if (q?.trim()) {
    code.value = q.trim()
    void onSearch()
  }
})
</script>
