<!--
  V1.3.9 Sprint 12 Story 12.4 · AC-12.4.5
  PrintButton 组件 · web-impl 主仓
  - 1 台 LABEL → 直打（ZPL_DIRECT）
  - 0 台 → 弹"管理员先配置"
  - 3 台 → 弹 el-dialog 选
  - PDF_BROWSER 模式 → 调 /print/labels/pdf-a4 拿 base64 + 触发 window.print()
-->
<template>
  <el-button
    :type="type"
    :size="size"
    :loading="loading"
    :disabled="disabled"
    :data-testid="`print-button-${codeType}`"
    @click="onClick"
  >
    <el-icon><Printer /></el-icon>
    <span style="margin-left: 4px">{{ label || '打印' }}</span>
  </el-button>

  <!-- 多台选择弹窗 -->
  <el-dialog
    v-model="dialogVisible"
    title="选择打印机"
    width="480px"
    :data-testid="`print-dialog-${codeType}`"
  >
    <el-radio-group v-model="selectedPrinterId" class="print-dialog__radios">
      <el-radio
        v-for="p in printers"
        :key="p.id"
        :value="p.id"
        :label="p.id"
        border
        class="print-dialog__radio"
        :data-testid="`print-radio-${p.id}`"
      >
        <div class="print-dialog__printer">
          <span class="print-dialog__name">{{ p.name }}</span>
          <el-tag size="small">{{ p.protocol }}</el-tag>
          <span class="print-dialog__ip">{{ p.ip }}:{{ p.port }}</span>
        </div>
      </el-radio>
    </el-radio-group>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="loading" :disabled="!selectedPrinterId" @click="confirmSelect">
        确定打印
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Printer } from '@element-plus/icons-vue'
import http from '@/utils/http'

/**
 * PrintButton · V1.3.9 Sprint 12 Story 12.4 (AC-12.4.5)
 *
 * <p>三态逻辑：
 * <ul>
 *   <li>0 台 → ElMessage.error("管理员先配置")，不调后端</li>
 *   <li>1 台 → 直接 ZPL_DIRECT 打印</li>
 *   <li>3 台 → 弹 el-dialog 选 + 调端点</li>
 * </ul>
 *
 * <p>PDF_BROWSER 模式：调 /print/labels/pdf-a4 → base64 → 触发 window.print()
 *
 * @author dev agent Opus 4.8
 * @since 2026-06-14
 */
const props = withDefaults(defineProps<{
  /** 业务编码类型 */
  codeType: 'GD' | 'LZ' | 'SB' | 'WW' | 'WL' | 'DRAWING'
  /** QR 内容 / 业务编码 */
  codeValue: string
  /** 文本行 · max 6 */
  lines?: string[]
  /** 色条 HEX（默认 #1E40AF） */
  colorBarHex?: string
  /** 份数 1-100 */
  copies?: number
  /** 备注 */
  remark?: string
  /** tenantId 默认 1 */
  tenantId?: number
  /** 按钮 type */
  type?: 'primary' | 'success' | 'warning' | 'info' | 'danger'
  /** 按钮 size */
  size?: 'small' | 'default' | 'large'
  /** 按钮文字 */
  label?: string
  /** 强制 disabled */
  disabled?: boolean
  /** 模式：ZPL_DIRECT / PDF_BROWSER / AUTO（默认 AUTO · 1台直打） */
  forceMode?: 'ZPL_DIRECT' | 'PDF_BROWSER' | 'AUTO'
}>(), {
  lines: () => [],
  colorBarHex: '#1E40AF',
  copies: 1,
  remark: '',
  tenantId: 1,
  type: 'primary',
  size: 'default',
  label: '打印',
  disabled: false,
  forceMode: 'AUTO',
})

const emit = defineEmits<{
  (e: 'print-success', payload: { printLogId: number; mode: string }): void
  (e: 'print-failed', err: { code: number; message: string }): void
}>()

const loading = ref(false)
const printers = ref<Array<{ id: number; name: string; protocol: string; ip: string; port: number }>>([])
const dialogVisible = ref(false)
const selectedPrinterId = ref<number | null>(null)

async function onClick() {
  // PDF_BROWSER 强制模式
  if (props.forceMode === 'PDF_BROWSER') {
    await sendPdfA4()
    return
  }

  loading.value = true
  try {
    // 1. 查可用打印机
    const res: any = await http.get('/printers/available', {
      params: { type: 'LABEL', tenantId: props.tenantId },
    })
    const list: any[] = res?.data?.printers || []
    const count: number = res?.data?.count || 0

    if (count === 0) {
      // 0 台 → 弹"管理员先配置"（前端防御 · 不调后端）
      ElMessage.error('管理员先配置打印机')
      emit('print-failed', { code: 50201, message: '管理员先配置打印机' })
      return
    }

    if (count === 1) {
      // 1 台 → 直打
      const p = list[0]
      await sendZpl(p.id, p.protocol)
      return
    }

    // 3 台 → 弹选
    printers.value = list
    selectedPrinterId.value = null
    dialogVisible.value = true
  } catch (e: any) {
    ElMessage.error(`查询打印机失败: ${e?.message || '未知错误'}`)
    emit('print-failed', { code: e?.code || 50001, message: e?.message || '' })
  } finally {
    loading.value = false
  }
}

async function confirmSelect() {
  if (!selectedPrinterId.value) return
  const p = printers.value.find(x => x.id === selectedPrinterId.value)
  if (!p) return
  dialogVisible.value = false
  await sendZpl(p.id, p.protocol)
}

async function sendZpl(printerId: number, protocol: string) {
  loading.value = true
  try {
    const res: any = await http.post('/print/labels/zpl', {
      templateCode: props.codeType,
      qrContent: props.codeValue,
      lines: props.lines,
      colorBarHex: props.colorBarHex,
      printerId,
      count: props.copies,
      remark: props.remark,
    })
    if (res.code === 0) {
      ElMessage.success(`已发送到 ${protocol === 'ZPL' ? 'Zebra' : 'TSC'} · 模式一`)
      emit('print-success', { printLogId: res.data.printLogId, mode: 'ZPL_DIRECT' })
    } else {
      ElMessage.error(res.message || '打印失败')
      emit('print-failed', { code: res.code, message: res.message })
    }
  } catch (e: any) {
    ElMessage.error(`ZPL 发送失败: ${e?.message || '未知错误'}`)
    emit('print-failed', { code: e?.code || 50203, message: e?.message || '' })
  } finally {
    loading.value = false
  }
}

async function sendPdfA4() {
  loading.value = true
  try {
    const res: any = await http.post('/print/labels/pdf-a4', {
      items: [{
        templateCode: props.codeType,
        qrContent: props.codeValue,
        lines: props.lines,
        colorBarHex: props.colorBarHex,
      }],
      remark: props.remark,
    })
    if (res.code === 0) {
      ElMessage.success('PDF 已生成 · 即将弹出打印对话框')
      // base64 → blob → iframe 触发 window.print()
      const pdfBase64 = res.data.pdfBase64
      const blob = base64ToBlob(pdfBase64, 'application/pdf')
      const url = URL.createObjectURL(blob)
      const iframe = document.createElement('iframe')
      iframe.style.display = 'none'
      iframe.src = url
      document.body.appendChild(iframe)
      iframe.onload = () => {
        try {
          iframe.contentWindow?.print()
        } catch (e) {
          window.print()
        }
      }
      emit('print-success', { printLogId: res.data.printLogId, mode: 'PDF_BROWSER' })
    } else {
      ElMessage.error(res.message || 'PDF 生成失败')
      emit('print-failed', { code: res.code, message: res.message })
    }
  } catch (e: any) {
    ElMessage.error(`PDF 生成失败: ${e?.message || '未知错误'}`)
    emit('print-failed', { code: e?.code || 50001, message: e?.message || '' })
  } finally {
    loading.value = false
  }
}

function base64ToBlob(base64: string, contentType: string): Blob {
  const byteCharacters = atob(base64)
  const byteNumbers = new Array(byteCharacters.length)
  for (let i = 0; i < byteCharacters.length; i++) {
    byteNumbers[i] = byteCharacters.charCodeAt(i)
  }
  const byteArray = new Uint8Array(byteNumbers)
  return new Blob([byteArray], { type: contentType })
}
</script>

<style scoped>
.print-dialog__radios {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}
.print-dialog__radio {
  width: 100%;
  margin-right: 0;
}
.print-dialog__printer {
  display: flex;
  align-items: center;
  gap: 8px;
}
.print-dialog__name {
  font-weight: 600;
}
.print-dialog__ip {
  color: #909399;
  font-size: 12px;
}
</style>
