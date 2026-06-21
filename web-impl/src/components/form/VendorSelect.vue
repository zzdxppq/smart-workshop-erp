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
    <el-option v-for="v in options" :key="v.id" :label="vendorLabel(v)" :value="v.id" />
  </el-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMasterData, vendorLabel, type VendorOption } from '@/composables/useMasterData'

withDefaults(defineProps<{
  modelValue?: number | null
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: '请选择供应商',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: number | undefined | null]
  change: [vendor: VendorOption | undefined]
}>()

const { loadVendors } = useMasterData()
const options = ref<VendorOption[]>([])
const loading = ref(false)

function onChange(id: number | undefined) {
  emit('change', options.value.find((v) => v.id === id))
}

onMounted(async () => {
  loading.value = true
  try {
    options.value = await loadVendors()
  } finally {
    loading.value = false
  }
})
</script>
