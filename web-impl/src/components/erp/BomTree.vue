<template>
  <div class="bom-tree">
    <div v-if="editable" class="bom-toolbar">
      <el-button size="small" @click="startEdit">F2 编辑</el-button>
      <el-button size="small" type="primary" :disabled="!editing" @click="saveEdit">Enter 保存</el-button>
      <el-button size="small" @click="cancelEdit">Esc 取消</el-button>
    </div>
    <el-tree
      ref="treeRef"
      :data="treeData"
      :props="{ label: 'label', children: 'children' }"
      node-key="id"
      default-expand-all
      highlight-current
      @node-click="onNodeClick"
    >
      <template #default="{ node, data }">
        <span class="bom-node">
          <span>{{ node.label }}</span>
          <span v-if="showCost && data.cost != null" class="bom-cost">¥{{ data.cost }}</span>
          <span v-if="data.qty != null" class="bom-qty">×{{ data.qty }}</span>
        </span>
      </template>
    </el-tree>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

export interface BomNode {
  id: string
  label: string
  qty?: number
  cost?: number
  children?: BomNode[]
}

const props = withDefaults(defineProps<{
  data?: BomNode[]
  editable?: boolean
  showCost?: boolean
}>(), {
  data: () => [],
  editable: false,
  showCost: false,
})

const emit = defineEmits<{
  'node-click': [node: BomNode]
  save: [data: BomNode[]]
}>()

const treeData = ref<BomNode[]>([...props.data])
const editing = ref(false)
const snapshot = ref<BomNode[]>([])

watch(() => props.data, (d) => { treeData.value = [...(d ?? [])] }, { deep: true })

function onNodeClick(data: BomNode) {
  emit('node-click', data)
}

function startEdit() {
  snapshot.value = JSON.parse(JSON.stringify(treeData.value))
  editing.value = true
}

function saveEdit() {
  editing.value = false
  emit('save', treeData.value)
}

function cancelEdit() {
  treeData.value = snapshot.value
  editing.value = false
}

defineExpose({ startEdit, saveEdit, cancelEdit })
</script>

<style scoped>
.bom-toolbar { margin-bottom: 8px; }
.bom-node { display: flex; gap: 12px; align-items: center; }
.bom-cost { font-family: var(--erp-font-mono); color: var(--erp-color-accent-purple, #8250df); }
.bom-qty { color: var(--erp-text-muted); font-size: 12px; }
</style>
