<template>
  <div v-loading="loading" class="material-category">
    <h2>物料分类</h2>
    <el-card>
      <el-table :data="categories" stripe>
        <el-table-column prop="categoryCode" label="分类编码" />
        <el-table-column prop="categoryName" label="分类名称" />
        <el-table-column prop="prefix" label="条码 Prefix" />
        <el-table-column prop="seqNo" label="排序" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useMaterialStore } from '@/stores/material'
import { parsePageItems } from '@/utils/apiPage'

const materialStore = useMaterialStore()
const categories = ref<Record<string, unknown>[]>([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    categories.value = parsePageItems(await materialStore.listCategories()).items as Record<string, unknown>[]
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载失败')
    categories.value = []
  } finally {
    loading.value = false
  }
})
</script>
