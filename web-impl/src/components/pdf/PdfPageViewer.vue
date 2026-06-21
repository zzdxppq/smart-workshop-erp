<template>
  <div v-loading="loading" class="pdf-page-viewer">
    <div v-if="pdfDoc" class="toolbar">
      <el-button size="small" :disabled="pageNum <= 1" @click="goPage(pageNum - 1)">上一页</el-button>
      <el-input-number
        v-model="pageNum"
        size="small"
        :min="1"
        :max="pageCount"
        controls-position="right"
        style="width: 110px"
        @change="renderCurrentPage"
      />
      <span class="page-total">/ {{ pageCount }}</span>
      <el-button size="small" :disabled="pageNum >= pageCount" @click="goPage(pageNum + 1)">下一页</el-button>
      <el-divider direction="vertical" />
      <el-button size="small" :disabled="scale <= 0.5" @click="setScale(scale - 0.1)">缩小</el-button>
      <span class="zoom-label">{{ Math.round(scale * 100) }}%</span>
      <el-button size="small" :disabled="scale >= 3" @click="setScale(scale + 0.1)">放大</el-button>
      <el-button size="small" @click="fitWidth">适应宽度</el-button>
      <el-button v-if="allowPrint" size="small" type="primary" @click="printPdf">打印</el-button>
    </div>

    <div v-if="pdfDoc" ref="wrapRef" class="canvas-wrap">
      <canvas ref="canvasRef" />
    </div>

    <el-alert
      v-else-if="textFallback"
      type="info"
      :closable="false"
      title="当前为文本摘要预览（种子图纸无 MinIO PDF 时回退）"
      style="margin-bottom: 12px"
      show-icon
    >
      <template #default>
        <div class="text-fallback">
          <div v-for="(line, idx) in fallbackLines" :key="idx" class="fb-line">{{ line }}</div>
        </div>
      </template>
    </el-alert>

    <el-empty v-if="!loading && !pdfDoc && !textFallback" description="暂无预览内容" />
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, nextTick } from 'vue'
import * as pdfjsLib from 'pdfjs-dist'
// 直接 import worker 让 Vite 自动打包（修复 fake worker failed）
import PdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import type { PDFDocumentProxy } from 'pdfjs-dist'

pdfjsLib.GlobalWorkerOptions.workerSrc = PdfWorker

const props = withDefaults(defineProps<{
  source?: Blob | null
  allowPrint?: boolean
}>(), {
  source: null,
  allowPrint: true,
})

const loading = ref(false)
const pdfDoc = ref<PDFDocumentProxy | null>(null)
const pageNum = ref(1)
const pageCount = ref(0)
const scale = ref(1.2)
const textFallback = ref('')
const canvasRef = ref<HTMLCanvasElement | null>(null)
const wrapRef = ref<HTMLDivElement | null>(null)
let objectUrl: string | null = null

const fallbackLines = computed(() =>
  textFallback.value.split(/\r?\n/).filter(l => l.trim().length > 0),
)

watch(() => props.source, (blob) => {
  void loadSource(blob ?? null)
}, { immediate: true })

onBeforeUnmount(() => {
  destroyDoc()
  revokeUrl()
})

async function loadSource(blob: Blob | null) {
  destroyDoc()
  revokeUrl()
  textFallback.value = ''
  pageNum.value = 1
  pageCount.value = 0
  if (!blob || blob.size === 0) return

  loading.value = true
  try {
    const buf = await blob.arrayBuffer()
    const bytes = new Uint8Array(buf)
    if (!isPdf(bytes)) {
      textFallback.value = new TextDecoder().decode(bytes)
      return
    }
    const task = pdfjsLib.getDocument({ data: bytes })
    pdfDoc.value = await task.promise
    pageCount.value = pdfDoc.value.numPages
    await nextTick()
    await renderCurrentPage()
  } catch (e: unknown) {
    textFallback.value = (e as Error)?.message || 'PDF 解析失败'
  } finally {
    loading.value = false
  }
}

function isPdf(bytes: Uint8Array) {
  return bytes.length >= 5
    && bytes[0] === 0x25 && bytes[1] === 0x50 && bytes[2] === 0x44 && bytes[3] === 0x46
}

async function renderCurrentPage() {
  if (!pdfDoc.value || !canvasRef.value) return
  const page = await pdfDoc.value.getPage(pageNum.value)
  const viewport = page.getViewport({ scale: scale.value })
  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  canvas.width = viewport.width
  canvas.height = viewport.height
  await page.render({ canvasContext: ctx, viewport }).promise
}

function goPage(n: number) {
  if (n < 1 || n > pageCount.value) return
  pageNum.value = n
  void renderCurrentPage()
}

async function setScale(next: number) {
  scale.value = Math.min(3, Math.max(0.5, next))
  await renderCurrentPage()
}

async function fitWidth() {
  if (!pdfDoc.value || !wrapRef.value) return
  const page = await pdfDoc.value.getPage(pageNum.value)
  const base = page.getViewport({ scale: 1 })
  const width = wrapRef.value.clientWidth - 24
  scale.value = Math.min(3, Math.max(0.5, width / base.width))
  await renderCurrentPage()
}

function printPdf() {
  if (!props.source) return
  objectUrl = URL.createObjectURL(props.source)
  const w = window.open(objectUrl)
  w?.print()
}

function destroyDoc() {
  if (pdfDoc.value) {
    void pdfDoc.value.destroy()
    pdfDoc.value = null
  }
}

function revokeUrl() {
  if (objectUrl) {
    URL.revokeObjectURL(objectUrl)
    objectUrl = null
  }
}
</script>

<style scoped>
.pdf-page-viewer { width: 100%; }
.toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.page-total, .zoom-label {
  font-size: 13px;
  color: var(--erp-text-muted);
}
.canvas-wrap {
  overflow: auto;
  max-height: 70vh;
  border: 1px solid var(--el-border-color-light);
  border-radius: 4px;
  background: #f5f7fa;
  padding: 12px;
  text-align: center;
}
.text-fallback {
  margin-top: 8px;
  max-height: 50vh;
  overflow: auto;
  background: var(--erp-bg-card, #fff);
  border: 1px solid var(--erp-border, #e2e8f0);
  border-radius: 6px;
  padding: 12px 16px;
  font-family: 'JetBrains Mono', 'SF Mono', Menlo, monospace;
  font-size: 12px;
  line-height: 1.7;
  color: var(--erp-text-primary, #1e293b);
}
.fb-line {
  padding: 4px 0;
  border-bottom: 1px dashed rgba(0, 0, 0, 0.04);
}
.fb-line:last-child { border-bottom: 0; }
</style>
