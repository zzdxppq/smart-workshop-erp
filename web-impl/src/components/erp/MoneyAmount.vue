<template>
  <span v-if="displayOnly" class="money-display erp-num-highlight">{{ formatted }}</span>
  <div v-else class="money-amount">
    <el-input-number
      v-model="innerValue"
      :min="min"
      :max="max"
      :precision="precision"
      :disabled="disabled || masked"
      :controls-position="'right'"
      style="width: 100%"
    />
    <div class="money-meta">
      <el-select v-model="currency" size="small" style="width: 80px" :disabled="disabled">
        <el-option label="CNY" value="CNY" />
        <el-option label="USD" value="USD" />
      </el-select>
      <el-switch
        v-model="taxIncluded"
        inline-prompt
        active-text="含税"
        inactive-text="不含"
        :disabled="disabled"
        @change="emit('tax-change', taxIncluded)"
      />
      <span v-if="masked" class="masked-hint">***</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed } from 'vue'

const props = withDefaults(defineProps<{
  modelValue?: number
  currency?: string
  taxIncluded?: boolean
  min?: number
  max?: number
  precision?: number
  disabled?: boolean
  masked?: boolean
  /** 列表只读展示：不渲染输入框/币种/含税控件 */
  displayOnly?: boolean
}>(), {
  modelValue: 0,
  currency: 'CNY',
  taxIncluded: false,
  min: 0,
  precision: 2,
  disabled: false,
  masked: false,
  displayOnly: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: number]
  'update:currency': [value: string]
  'update:taxIncluded': [value: boolean]
  'tax-change': [included: boolean]
}>()

const innerValue = ref(props.modelValue)
const currency = ref(props.currency)
const taxIncluded = ref(props.taxIncluded)

const formatted = computed(() => {
  if (props.masked) return '***'
  const n = Number(innerValue.value ?? 0)
  return n.toLocaleString('zh-CN', { minimumFractionDigits: props.precision, maximumFractionDigits: props.precision })
})

watch(() => props.modelValue, (v) => { innerValue.value = v ?? 0 })
watch(innerValue, (v) => emit('update:modelValue', v ?? 0))
watch(currency, (v) => emit('update:currency', v))
watch(taxIncluded, (v) => emit('update:taxIncluded', v))

function toggleTax() {
  taxIncluded.value = !taxIncluded.value
}

defineExpose({ toggleTax })
</script>

<style scoped>
.money-display {
  display: inline-block;
  text-align: right;
  width: 100%;
  color: var(--erp-text-primary);
}
.money-amount { display: flex; flex-direction: column; gap: 8px; }
.money-meta { display: flex; align-items: center; gap: 12px; }
.masked-hint { color: var(--erp-text-muted); font-size: 12px; }
:deep(.el-input-number .el-input__inner) {
  font-family: var(--erp-font-mono);
  text-align: right;
}
</style>
