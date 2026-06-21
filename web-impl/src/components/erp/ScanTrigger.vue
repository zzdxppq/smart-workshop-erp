<template>
  <div class="scan-trigger">
    <el-input
      ref="inputRef"
      v-model="code"
      :placeholder="placeholder"
      :disabled="disabled"
      clearable
      @keyup.enter="submit"
      @input="onWedgeInput"
    >
      <template #prepend>
        <el-select v-model="mode" style="width: 100px" :disabled="disabled">
          <el-option label="USB" value="usb" />
          <el-option label="手动" value="manual" />
          <el-option label="摄像头" value="camera" />
        </el-select>
      </template>
      <template #append>
        <el-button type="primary" :disabled="disabled" @click="submit">
          <el-icon><FullScreen /></el-icon>
          扫码
        </el-button>
      </template>
    </el-input>
    <video v-if="mode === 'camera' && cameraActive" ref="videoRef" class="camera-preview" autoplay playsinline />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onUnmounted } from 'vue'
import { FullScreen } from '@element-plus/icons-vue'

const props = withDefaults(defineProps<{
  modelValue?: string
  placeholder?: string
  disabled?: boolean
}>(), {
  modelValue: '',
  placeholder: '扫描或输入条码（GD-/LZ-/SB-/WL-/WW-）',
  disabled: false,
})

const emit = defineEmits<{
  'update:modelValue': [value: string]
  scan: [code: string]
}>()

const code = ref(props.modelValue)
const mode = ref<'usb' | 'manual' | 'camera'>('usb')
const inputRef = ref()
const videoRef = ref<HTMLVideoElement>()
const cameraActive = ref(false)
let wedgeBuffer = ''
let wedgeTimer: ReturnType<typeof setTimeout> | null = null
let mediaStream: MediaStream | null = null

watch(() => props.modelValue, (v) => { code.value = v ?? '' })
watch(code, (v) => emit('update:modelValue', v))
watch(mode, async (m) => {
  if (m === 'camera') await startCamera()
  else stopCamera()
})

function onWedgeInput() {
  if (mode.value !== 'usb') return
  if (wedgeTimer) clearTimeout(wedgeTimer)
  wedgeTimer = setTimeout(() => {
    if (code.value.length >= 4) submit()
    wedgeBuffer = ''
  }, 80)
  wedgeBuffer = code.value
}

function submit() {
  const trimmed = code.value.trim()
  if (!trimmed) return
  emit('scan', trimmed)
}

async function startCamera() {
  try {
    mediaStream = await navigator.mediaDevices.getUserMedia({ video: { facingMode: 'environment' } })
    cameraActive.value = true
    await new Promise((r) => setTimeout(r, 50))
    if (videoRef.value) videoRef.value.srcObject = mediaStream
  } catch {
    mode.value = 'manual'
  }
}

function stopCamera() {
  mediaStream?.getTracks().forEach((t) => t.stop())
  mediaStream = null
  cameraActive.value = false
}

function focus() {
  (inputRef.value as { focus?: () => void })?.focus?.()
}

onUnmounted(stopCamera)

defineExpose({ focus, submit })
</script>

<style scoped>
.scan-trigger { width: 100%; }
.camera-preview {
  width: 100%;
  max-height: 240px;
  margin-top: 8px;
  border-radius: var(--erp-radius-md, 4px);
  background: #000;
}
</style>
