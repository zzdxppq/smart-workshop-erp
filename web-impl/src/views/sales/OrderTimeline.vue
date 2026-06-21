<template>
  <div class="order-timeline">
    <el-page-header @back="$router.push(`/sales/orders/${orderId}`)">
      <template #content>订单时间线</template>
    </el-page-header>
    <OrderTimelinePanel :order-id="orderId" style="margin-top: 16px" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import OrderTimelinePanel from '@/components/sales/OrderTimelinePanel.vue'

const route = useRoute()
const orderId = computed(() => String(route.params.id))
</script>

<style scoped>
.order-timeline {
  padding: 16px;
}
</style>
