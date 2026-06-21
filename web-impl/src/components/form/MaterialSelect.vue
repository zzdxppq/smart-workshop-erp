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
    <el-option
      v-for="m in options"
      :key="valueKey === 'materialCode' ? m.materialCode : m.id"
      :label="materialLabel(m)"
      :value="valueKey === 'materialCode' ? m.materialCode : m.id"
    />
  </el-select>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useMasterData, materialLabel, type MaterialOption } from '@/composables/useMasterData'

const props = withDefaults(defineProps<{
  modelValue?: number | string | null
  valueKey?: 'id' | 'materialCode'
  placeholder?: string
  disabled?: boolean
}>(), {
  valueKey: 'id',
  placeholder: '请选择物料',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: number | string | undefined | null]
  change: [material: MaterialOption | undefined]
}>()

const { loadMaterials } = useMasterData()
const options = ref<MaterialOption[]>([])
const loading = ref(false)

function onChange(val: number | string | undefined) {
  const m = props.valueKey === 'materialCode'
    ? options.value.find((x) => x.materialCode === val)
    : options.value.find((x) => x.id === val)
  emit('change', m)
}

onMounted(async () => {
  loading.value = true
  try {
    options.value = await loadMaterials()
  } finally {
    loading.value = false
  }
})
</script>
