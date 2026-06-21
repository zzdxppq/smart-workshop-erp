<template>
  <div class="order-change">
    <el-page-header @back="$router.push(`/sales/orders/${orderId}`)">
      <template #content>订单变更</template>
    </el-page-header>
    <OrderChangePanel
      :order-id="orderId"
      style="margin-top: 16px; max-width: 560px"
      @success="$router.push(`/sales/orders/${orderId}`)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import OrderChangePanel from '@/components/sales/OrderChangePanel.vue'

const route = useRoute()
const orderId = computed(() => String(route.params.id))
</script>

<style scoped>
.order-change {
  padding: 16px;
}
</style>
