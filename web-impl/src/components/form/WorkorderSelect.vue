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
    <el-option v-for="w in options" :key="w.id" :label="workorderLabel(w)" :value="w.id" />
  </el-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMasterData, workorderLabel, type WorkorderOption } from '@/composables/useMasterData'

withDefaults(defineProps<{
  modelValue?: number | null
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: '请选择工单',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined | null]
  change: [workorder: WorkorderOption | undefined]
}>()

const { loadWorkorders } = useMasterData()
const options = ref<WorkorderOption[]>([])
const loading = ref(false)

function onChange(id: number | undefined) {
  emit('change', options.value.find((w) => w.id === id))
}

onMounted(async () => {
  loading.value = true
  try {
    options.value = await loadWorkorders()
  } finally {
    loading.value = false
  }
})
</script>
