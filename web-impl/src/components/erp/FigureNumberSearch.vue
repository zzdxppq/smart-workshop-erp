<template>
  <div class="figure-search">
    <el-autocomplete
      ref="inputRef"
      v-model="innerValue"
      :fetch-suggestions="querySearch"
      :placeholder="placeholder"
      :disabled="disabled"
      clearable
      value-key="drawingNo"
      style="width: 100%"
      @select="onSelect"
    >
      <template #default="{ item }">
        <div class="figure-option">
          <span class="figure-no">{{ item.drawingNo }}</span>
          <span v-if="showVersion && item.version" class="figure-ver">v{{ item.version }}</span>
          <span v-if="item.title" class="figure-cat">{{ item.title }}</span>
        </div>
      </template>
    </el-autocomplete>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { E3DrawingService } from '@/api/generated/services/E3DrawingService'
import type { Drawing } from '@/api/generated/models/Drawing'
import { parsePageItems } from '@/utils/apiPage'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  showVersion?: boolean
  disabled?: boolean
}>(), {
  modelValue: '',
  placeholder: '请输入图号',
  showVersion: true,
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  select: [drawing: Drawing]
}>()

const innerValue = ref(props.modelValue)
const inputRef = ref()

watch(() => props.modelValue, (v) => { innerValue.value = v ?? '' })
watch(innerValue, (v) => emit('update:modelValue', v))

async function querySearch(query: string, cb: (items: Drawing[]) => void) {
  if (!query || query.length < 1) {
    cb([])
    return
  }
  try {
    const r = await E3DrawingService.listDrawings(query, undefined, undefined, undefined, undefined, 'RELEASED', 1, 20)
    const { items } = parsePageItems(r)
    cb(items as Drawing[])
  } catch {
    cb([])
  }
}

function onSelect(item: Drawing) {
  innerValue.value = item.drawingNo ?? ''
  emit('select', item)
}

function focus() {
  (inputRef.value as { focus?: () => void })?.focus?.()
}

defineExpose({ focus })
</script>

<style scoped>
.figure-option {
  display: flex;
  gap: 8px;
  align-items: center;
}
.figure-no { font-weight: 600; color: var(--erp-text-primary, #1f2328); }
.figure-ver { color: var(--erp-color-primary, #0969da); font-size: 12px; }
.figure-cat { color: var(--erp-text-muted, #8c959f); font-size: 12px; }
</style>
