<template>
  <div class="label-preview" data-testid="label-preview">
    <div class="label-preview__canvas">
      <img
        v-if="previewUrl"
        :src="previewUrl"
        :alt="`${type} 标签预览`"
        class="label-preview__img"
        :style="{ width: scaledWidth + 'px', height: scaledHeight + 'px' }"
        data-testid="label-preview-img"
        @load="onLoad"
        @error="onError"
      />
      <div v-else-if="error" class="label-preview__error" data-testid="label-preview-error">
        <el-icon><Warning /></el-icon>
        <span>{{ error }}</span>
      </div>
      <div v-else class="label-preview__loading" data-testid="label-preview-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>渲染中...</span>
      </div>
    </div>
    <div v-if="meta" class="label-preview__meta">
      <el-tag :color="meta.colorStrip" effect="dark" size="small">
        {{ meta.type }}
      </el-tag>
      <span class="label-preview__name">{{ meta.name }}</span>
      <span class="label-preview__company">{{ companyName }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Warning, Loading } from '@element-plus/icons-vue'
import http from '@/utils/http'

/**
 * LabelPreview · V1.3.9 Sprint 12 Story 12.3 (AC-12.3.4)
 *
 * <p>三仓预览组件 · web-impl 调后端 /label-templates/preview 拿 base64 PNG
 * <p>50mm×30mm 物理尺寸 · 缩放到 200% 显示（CSS px）
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */

interface LabelMeta {
  type: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL'
  name: string
  colorStrip: string
  reuseFrom?: string | null
}

const props = withDefaults(defineProps<{
  /** 模板类型 */
  type: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL'
  /** 二维码内容（纯文本 · APP 扫码壳按前缀路由） */
  qrContent: string
  /** 下方明文 · ≤6 行 · 单行 ≤ 50 字符 */
  lines?: string[]
  /** 厂名（可选 · 默认从后端 /list 拿） */
  factoryName?: string
  /** 显示缩放比例 · 默认 200% */
  scale?: number
  /** tenantId · 默认 1 */
  tenantId?: number
  /** 调后端的 API 路径前缀 */
  apiPrefix?: string
}>(), {
  lines: () => [],
  scale: 2.0,
  tenantId: 1,
  apiPrefix: '/label-templates',
})

const previewUrl = ref<string>('')
const error = ref<string>('')
const meta = ref<LabelMeta | null>(null)
const companyName = ref<string>('昆山佰泰胜精密加工')
const rendered = ref<boolean>(false)

const scaledWidth = computed(() => Math.round(50 * 3.78 * props.scale))   // 50mm × 3.78 px/mm
const scaledHeight = computed(() => Math.round(30 * 3.78 * props.scale))  // 30mm × 3.78 px/mm

async function fetchMeta() {
  try {
    const res: any = await http.get(`${props.apiPrefix}`, { params: { type: props.type, tenantId: props.tenantId } })
    if (res.code === 0 && res.data) {
      const t = (res.data.templates || []).find((x: any) => x.type === props.type)
      if (t) {
        meta.value = {
          type: t.type,
          name: t.name,
          colorStrip: t.colorStrip,
          reuseFrom: t.reuseFrom,
        }
      }
      if (res.data.companyName) companyName.value = res.data.companyName
    }
  } catch (e) {
    // meta 失败不影响预览渲染
    console.warn('[LabelPreview] fetchMeta 失败', e)
  }
}

async function fetchPreview() {
  if (!props.qrContent) {
    error.value = 'qrContent 不能为空'
    return
  }
  error.value = ''
  previewUrl.value = ''
  rendered.value = false
  const start = performance.now()
  try {
    const res: any = await http.post(`${props.apiPrefix}/preview`, {
      type: props.type,
      data: {
        qrContent: props.qrContent,
        lines: props.lines || [],
        factoryName: props.factoryName,
      },
      format: 'PNG',
    }, { params: { tenantId: props.tenantId } })
    if (res.code === 0 && res.data && res.data.base64) {
      previewUrl.value = res.data.base64
    } else {
      error.value = res.message || '渲染失败'
      ElMessage.error(error.value)
    }
  } catch (e: any) {
    error.value = e?.message || '网络错误'
    ElMessage.error(error.value)
  } finally {
    const ms = Math.round(performance.now() - start)
    // QA TC-12.3.4.1：web-impl 加载 < 500ms（P95）
    if (ms > 500 && !rendered.value) {
      console.warn(`[LabelPreview] 渲染耗时 ${ms}ms > 500ms`)
    }
  }
}

function onLoad() {
  rendered.value = true
}

function onError() {
  error.value = '图片加载失败'
}

watch(() => [props.type, props.qrContent, props.lines, props.factoryName], () => {
  fetchPreview()
}, { deep: true })

onMounted(async () => {
  await fetchMeta()
  await fetchPreview()
})

defineExpose({ refresh: fetchPreview })
</script>

<style scoped>
.label-preview {
  display: inline-flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px;
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  background: #fafafa;
}

.label-preview__canvas {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 60px;
}

.label-preview__img {
  display: block;
  border: 1px solid #dcdfe6;
  background: #fff;
  object-fit: contain;
}

.label-preview__loading,
.label-preview__error {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #909399;
  font-size: 14px;
}

.label-preview__error {
  color: #f56c6c;
}

.label-preview__meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #606266;
}

.label-preview__name {
  font-weight: 600;
}

.label-preview__company {
  margin-left: auto;
  color: #909399;
  font-size: 12px;
}
</style>