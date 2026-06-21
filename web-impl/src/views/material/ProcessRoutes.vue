<template>
  <ErpPageShell title="工艺路线维护" description="E3-S4：绑定图号与工序，拖拽排序后发布；报价/委外单只读调用">
    <el-alert type="info" :closable="false" show-icon title="仅显示已完成工程转化（已绑定料号）的图纸；发布后供报价预览与委外单引用。" style="margin-bottom: 16px" />
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="图号">
        <DrawingPicker
          v-model="drawingNo"
          :require-material-code="true"
          style="width: 280px"
          @select="onDrawingPick"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :disabled="!canEdit" @click="goProductRoute">编辑工艺路线</el-button>
        <el-button @click="$router.push('/material/process')">工艺库</el-button>
      </el-form-item>
    </el-form>
    <p v-if="drawingNo" class="hint">
      当前图号：<strong>{{ drawingNo }}</strong>
      · 物料：<span :class="materialCode ? 'bound' : 'pending'">{{ materialCode || '（待工程转化）' }}</span>
      <el-tag v-if="materialCode" type="success" size="small">已绑定</el-tag>
    </p>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import DrawingPicker from '@/components/erp/DrawingPicker.vue'
import type { Drawing } from '@/api/generated/models/Drawing'

const router = useRouter()
const drawingNo = ref('')
const materialCode = ref('')
const drawingId = ref<number | undefined>()

const canEdit = computed(() => Boolean(drawingNo.value && materialCode.value))

function onDrawingPick(d: Drawing) {
  drawingNo.value = d.drawingNo ?? ''
  materialCode.value = d.materialCode ?? ''
  drawingId.value = d.id
}

function goProductRoute() {
  if (!materialCode.value) {
    ElMessage.warning('该图纸尚未工程转化，请先完成转化并生成料号')
    return
  }
  router.push({
    path: '/material/product-route',
    query: {
      drawingNo: drawingNo.value,
      materialCode: materialCode.value,
      drawingId: drawingId.value ? String(drawingId.value) : undefined,
    },
  })
}
</script>

<style scoped>
.hint { font-size: 14px; color: var(--erp-text-muted); }
.bound { color: var(--erp-color-success); font-weight: 500; }
.pending { color: var(--erp-color-warning); }
</style>
