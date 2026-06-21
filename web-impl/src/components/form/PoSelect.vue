<template>
  <el-select
    :model-value="modelValue"
    filterable
    clearable
    :placeholder="placeholder"
    :loading="loading"
    :disabled="disabled"
    style="width: 100%"
    @update:model-value="emit('update:modelValue', $event)"
    @change="onChange"
  >
    <el-option v-for="p in options" :key="p.id" :label="poLabel(p)" :value="p.id" />
  </el-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMasterData, poLabel, type PoOption } from '@/composables/useMasterData'

withDefaults(defineProps<{
  modelValue?: number | null
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: '请选择采购单',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined | null]
  change: [po: PoOption | undefined]
}>()

const { loadPos } = useMasterData()
const options = ref<PoOption[]>([])
const loading = ref(false)

function onChange(id: number | undefined) {
  emit('change', options.value.find((p) => p.id === id))
}

onMounted(async () => {
  loading.value = true
  try {
    options.value = await loadPos()
  } finally {
    loading.value = false
  }
})
</script>
