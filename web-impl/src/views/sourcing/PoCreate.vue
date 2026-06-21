<template>
  <ErpPageShell title="新建采购单已下线" description="请通过「采购转单」或「询比价定标 → 转采购单」生成采购订单。">
    <el-result icon="info" title="不可独立新建采购单">
      <template #sub-title>
        依 V1.3.8 规范：正常采购须由 MRP 缺料 → 采购申请 → 转采购单；特例走「无订单采购」。
      </template>
      <template #extra>
        <el-button type="primary" @click="$router.replace('/sourcing/purchase-transfer')">前往采购转单</el-button>
        <el-button @click="$router.replace('/sourcing/rfq')">询比价工作台</el-button>
      </template>
    </el-result>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'

const router = useRouter()
onMounted(() => {
  setTimeout(() => router.replace('/sourcing/purchase-transfer'), 3000)
})
</script>
