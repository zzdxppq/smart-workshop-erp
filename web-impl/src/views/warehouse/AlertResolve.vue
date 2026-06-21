<template>
  <div v-loading="loading">
    <h2>预警解决</h2>
    <el-alert
      v-if="!alertId"
      type="info"
      show-icon
      :closable="false"
      title="请从「库存预警」列表点击「处理」进入，或选择待处理预警。"
      style="margin-bottom: 16px"
    />
    <el-descriptions v-if="alertInfo" :column="2" border style="margin-bottom: 16px">
      <el-descriptions-item label="料号">{{ alertInfo.materialCode }}</el-descriptions-item>
      <el-descriptions-item label="级别">{{ dictLabel(EVENT_LEVEL, str(alertInfo.alertLevel)) }}</el-descriptions-item>
      <el-descriptions-item label="当前库存">{{ alertInfo.currentQty }}</el-descriptions-item>
      <el-descriptions-item label="安全库存">{{ alertInfo.minQty }}</el-descriptions-item>
      <el-descriptions-item label="状态" :span="2">
        {{ dictLabel(INVENTORY_ALERT_STATUS, str(alertInfo.status)) }}
      </el-descriptions-item>
      <el-descriptions-item label="消息" :span="2">{{ alertInfo.message }}</el-descriptions-item>
    </el-descriptions>
    <el-form v-if="alertId" :model="form" label-width="120px">
      <el-form-item v-if="!alertInfo" label="选择预警">
        <el-select
          v-model="form.alertId"
          filterable
          placeholder="请选择待处理预警"
          style="width: 100%"
          @change="loadAlert"
        >
          <el-option
            v-for="item in openAlerts"
            :key="item.id"
            :label="`${item.materialCode} · ${item.message}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="解决备注">
        <el-input v-model="form.note" type="textarea" placeholder="填写处理说明（可选）" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="!alertId" @click="resolve">解决</el-button>
        <el-button :disabled="!alertId" @click="archive">归档</el-button>
        <el-button @click="$router.push('/warehouse/inventory-alert')">返回预警列表</el-button>
      </el-form-item>
    </el-form>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useInventoryStore } from '@/stores/inventory'
import {
  dictLabel,
  EVENT_LEVEL,
  INVENTORY_ALERT_STATUS,
} from '@/utils/dictLabels'
import { parsePageItems } from '@/utils/apiPage'

const route = useRoute()
const router = useRouter()
const inventoryStore = useInventoryStore()
const loading = ref(false)
const alertInfo = ref<Record<string, unknown> | null>(null)
const openAlerts = ref<Array<{ id: number; materialCode?: string; message?: string }>>([])
const form = ref({ alertId: 0, note: '' })

const alertId = computed(() => {
  const pId = Number(route.params.id)
  const qId = Number(route.query.id)
  const fromForm = form.value.alertId
  return pId || qId || fromForm || 0
})

function str(v: unknown) {
  return v == null ? '' : String(v)
}

async function loadOpenAlerts() {
  try {
    const res = await inventoryStore.listAlerts({ status: 'OPEN', pageNum: 1, pageSize: 100 })
    openAlerts.value = parsePageItems(res).items as typeof openAlerts.value
  } catch {
    openAlerts.value = []
  }
}

async function loadAlert(id?: number) {
  const targetId = id ?? alertId.value
  if (!targetId) {
    alertInfo.value = null
    return
  }
  loading.value = true
  try {
    const list = await inventoryStore.listAlerts({ pageNum: 1, pageSize: 200 })
    const items = parsePageItems(list).items as Record<string, unknown>[]
    alertInfo.value = items.find((a) => Number(a.id) === targetId) ?? null
    form.value.alertId = targetId
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载预警失败')
    alertInfo.value = null
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await loadOpenAlerts()
  const pId = Number(route.params.id)
  if (pId) {
    form.value.alertId = pId
    await loadAlert(pId)
  }
})

const resolve = async () => {
  if (!alertId.value) {
    ElMessage.warning('请选择待处理预警')
    return
  }
  loading.value = true
  try {
    await inventoryStore.resolveAlert(alertId.value, { resolutionNote: form.value.note })
    ElMessage.success('已解决')
    router.push('/warehouse/inventory-alert')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '操作失败')
  } finally {
    loading.value = false
  }
}

const archive = async () => {
  if (!alertId.value) {
    ElMessage.warning('请选择待处理预警')
    return
  }
  loading.value = true
  try {
    await inventoryStore.archiveAlert(alertId.value)
    ElMessage.success('已归档')
    router.push('/warehouse/inventory-alert')
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '操作失败')
  } finally {
    loading.value = false
  }
}
</script>
