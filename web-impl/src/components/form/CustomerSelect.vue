<template>
  <el-select
    :model-value="modelValue"
    filterable
    clearable
    :placeholder="placeholder"
    :loading="loading"
    :disabled="disabled"
    style="width: 100%"
    @focus="reload"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-option v-for="c in options" :key="c.id" :label="customerLabel(c)" :value="c.id" />
  </el-select>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useMasterData, customerLabel, type CustomerOption } from '@/composables/useMasterData'

withDefaults(defineProps<{
  modelValue?: number | null
  placeholder?: string
  disabled?: boolean
}>(), {
  placeholder: '请选择客户',
  disabled: false,
})

const emit = defineEmits<{ 'update:modelValue': [value: number | undefined | null] }>()

const { loadCustomers } = useMasterData()
const options = ref<CustomerOption[]>([])
const loading = ref(false)

async function reload() {
  loading.value = true
  try {
    options.value = await loadCustomers(true)
  } finally {
    loading.value = false
  }
}

reload()
</script>
