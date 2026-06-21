<!--
  V1.3.9 Sprint 12 Story 12.2 · AC-12.2.3 · /admin/printers
  打印机管理 · admin only · Element Plus + 同款 sys_dict 风格
  - 列表分页 + 多维过滤 (type / status / enabled)
  - 新增 / 编辑 / 删除 / 启停 toggle
  - 测试按钮 (TCP 2s 探活)
  - 30s 自动轮询刷新状态徽章
  - type=LABEL 联动显示 protocol/ip/port
-->
<template>
  <div class="printers-page">
    <h2 style="margin-bottom: 12px">打印机管理</h2>
    <el-alert
      type="info"
      :closable="false"
      title="V55 引入 · 心跳调度 60s 周期 · TCP 探活 2s 超时 · fail_count ≥ 2 才标 OFFLINE"
      style="margin-bottom: 12px"
    />

    <!-- 工具栏 -->
    <el-row :gutter="12" align="middle" style="margin-bottom: 12px">
      <el-col :span="3">
        <el-select v-model="filter.type" placeholder="类型" clearable @change="reload">
          <el-option label="普通 NORMAL" value="NORMAL" />
          <el-option label="标签 LABEL" value="LABEL" />
        </el-select>
      </el-col>
      <el-col :span="3">
        <el-select v-model="filter.status" placeholder="状态" clearable @change="reload">
          <el-option label="ONLINE" value="ONLINE" />
          <el-option label="OFFLINE" value="OFFLINE" />
          <el-option label="UNKNOWN" value="UNKNOWN" />
        </el-select>
      </el-col>
      <el-col :span="3">
        <el-select v-model="filter.enabled" placeholder="启停" clearable @change="reload">
          <el-option label="启用" :value="1" />
          <el-option label="停用" :value="0" />
        </el-select>
      </el-col>
      <el-col :span="6">
        <el-input v-model="filter.keyword" placeholder="搜索名称" clearable @keyup.enter="reload" />
      </el-col>
      <el-col :span="9" style="text-align: right">
        <el-button type="primary" @click="openCreate">+ 新增打印机</el-button>
      </el-col>
    </el-row>

    <!-- 表格 -->
    <el-table v-loading="listLoading" :data="filteredRows" stripe border>
      <el-table-column prop="name" label="名称" width="160" />
      <el-table-column label="类型" width="100">
        <template #default="{ row }">
          <el-tag :type="row.type === 'LABEL' ? 'warning' : 'info'" size="small">
            {{ row.type }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="port" label="端口" width="80" />
      <el-table-column prop="protocol" label="协议" width="120" />
      <el-table-column prop="modelSuggestion" label="型号" width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" :label="printerStatusLabel(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="最后心跳" width="180">
        <template #default="{ row }">
          {{ row.lastHeartbeatAt ? formatTime(row.lastHeartbeatAt) : '—' }}
        </template>
      </el-table-column>
      <el-table-column label="启停" width="80">
        <template #default="{ row }">
          <el-switch
            :model-value="(row as SysPrinter).enabled === 1"
            @change="(v: boolean) => toggleEnabled(row as SysPrinter, v ? 1 : 0)"
          />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{ row }">
          <el-button size="small" :loading="testingIds.has((row as SysPrinter).id)" @click="onTest(row as SysPrinter)">测试</el-button>
          <el-button size="small" @click="openEdit(row as SysPrinter)">编辑</el-button>
          <el-popconfirm
            :title="`确认删除打印机 ${(row as SysPrinter).name} ？`"
            @confirm="onDelete(row as SysPrinter)"
          >
            <template #reference>
              <el-button size="small" type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="filter.pageNum"
      v-model:page-size="filter.pageSize"
      :total="total"
      :page-sizes="[10, 20, 50]"
      layout="total, sizes, prev, pager, next"
      style="margin-top: 12px; justify-content: flex-end"
      @current-change="reload"
      @size-change="reload"
    />

    <!-- 新增 / 编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editing ? '编辑打印机' : '新增打印机'"
      width="560px"
    >
      <el-form :model="form" :rules="formRules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="类型" prop="type">
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio value="NORMAL">普通（OS 打印队列）</el-radio>
            <el-radio value="LABEL">标签（TCP 9100）</el-radio>
          </el-radio-group>
        </el-form-item>

        <template v-if="form.type === 'LABEL'">
          <el-form-item label="协议" prop="protocol">
            <el-select v-model="form.protocol" placeholder="选择协议">
              <el-option label="ZPL (Zebra)" value="ZPL" />
              <el-option label="TSPL (TSC)" value="TSPL" />
            </el-select>
          </el-form-item>
          <el-form-item label="IP" prop="ip">
            <el-input v-model="form.ip" placeholder="192.168.1.100" />
          </el-form-item>
          <el-form-item label="端口" prop="port">
            <el-input-number v-model="form.port" :min="1" :max="65535" />
          </el-form-item>
        </template>

        <el-form-item label="型号" prop="modelSuggestion">
          <el-select v-model="form.modelSuggestion" placeholder="选择型号">
            <el-option label="得力 DL-888B" value="DELI_DL888B" />
            <el-option label="斑马 ZD420" value="ZEBRA_ZD420" />
            <el-option label="TSC TTP-244 Pro" value="TSC_TTP244PRO" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="启用" prop="enabled">
          <el-switch v-model="formEnabledBool" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="onSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { E12PrinterService } from '@/api/generated/services/E12PrinterService'
import type { SysPrinter } from '@/api/generated/models/SysPrinter'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

function printerStatusLabel(s?: string) {
  return ({ ONLINE: '在线', OFFLINE: '离线', UNKNOWN: '未知' } as Record<string, string>)[s ?? ''] ?? s ?? '—'
}

// === 状态 ===
const filter = reactive({
  type: '' as '' | 'NORMAL' | 'LABEL',
  status: '' as '' | 'ONLINE' | 'OFFLINE' | 'UNKNOWN',
  enabled: undefined as number | undefined,
  keyword: '',
  pageNum: 1,
  pageSize: 20,
})
const rows = ref<SysPrinter[]>([])
const total = ref(0)
const listLoading = ref(false)
const testingIds = ref<Set<number>>(new Set())

// === 弹窗 ===
const dialogVisible = ref(false)
const editing = ref(false)
const editingId = ref<number | null>(null)
const formRef = ref()
const form = reactive<{
  name: string
  type: 'NORMAL' | 'LABEL'
  protocol: 'ZPL' | 'TSPL' | 'PDF_BROWSER' | ''
  ip: string
  port: number
  modelSuggestion: 'DELI_DL888B' | 'ZEBRA_ZD420' | 'TSC_TTP244PRO' | 'OTHER'
  enabled: 0 | 1
}>({
  name: '',
  type: 'NORMAL',
  protocol: 'PDF_BROWSER',
  ip: '',
  port: 9100,
  modelSuggestion: 'OTHER',
  enabled: 1,
})
const formEnabledBool = computed({
  get: () => form.enabled === 1,
  set: (v: boolean) => (form.enabled = v ? 1 : 0),
})

const formRules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  ip: [
    {
      validator: (_: unknown, v: string, cb: (e?: Error) => void) => {
        if (form.type === 'LABEL' && !v) {
          cb(new Error('LABEL 类型必填 IP'))
        } else {
          cb()
        }
      },
      trigger: 'blur',
    },
  ],
  protocol: [
    {
      validator: (_: unknown, v: string, cb: (e?: Error) => void) => {
        if (form.type === 'LABEL' && !v) {
          cb(new Error('请选择协议'))
        } else {
          cb()
        }
      },
      trigger: 'change',
    },
  ],
}

// === 计算属性 ===
const filteredRows = computed(() => {
  let r = rows.value
  if (filter.keyword) {
    const k = filter.keyword.toLowerCase()
    r = r.filter((p) => p.name?.toLowerCase().includes(k))
  }
  return r
})

// === 方法 ===
function formatTime(iso: string) {
  return iso.replace('T', ' ').slice(0, 19)
}

async function reload() {
  listLoading.value = true
  try {
    const pageData = parsePageItems(await E12PrinterService.listPrinters(
      filter.type || undefined,
      filter.status || undefined,
      filter.enabled as 0 | 1 | undefined,
      filter.pageNum,
      filter.pageSize,
      1,
    ))
    rows.value = pageData.items as SysPrinter[]
    total.value = pageData.total
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || String(e)
    ElMessage.error('加载失败：' + msg)
    rows.value = []
    total.value = 0
  } finally {
    listLoading.value = false
  }
}

async function onTest(row: SysPrinter) {
  if (row.id == null) return
  testingIds.value.add(row.id)
  try {
    const data = unwrapResult<{ status?: string; latencyMs?: number; protocolDetected?: string; hint?: string }>(
      await E12PrinterService.testPrinterConnection(row.id),
    )
    if (data?.status === 'ONLINE') {
      ElMessage.success(
        `连接成功 · 延迟 ${data.latencyMs ?? 0}ms · 协议 ${data.protocolDetected || 'UNKNOWN'}`,
      )
    } else if (data?.status === 'OFFLINE') {
      ElMessageBox.alert(data?.hint || '连接失败', '测试结果', { type: 'error' })
    } else {
      ElMessage.info(data?.hint || 'NORMAL 类型无需 IP 探活')
    }
    reload()
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || String(e)
    ElMessage.error('测试失败：' + msg)
  } finally {
    testingIds.value.delete(row.id)
  }
}

function openCreate() {
  editing.value = false
  editingId.value = null
  Object.assign(form, {
    name: '',
    type: 'NORMAL',
    protocol: 'PDF_BROWSER',
    ip: '',
    port: 9100,
    modelSuggestion: 'OTHER',
    enabled: 1,
  })
  dialogVisible.value = true
}

function openEdit(row: SysPrinter) {
  editing.value = true
  editingId.value = row.id ?? null
  Object.assign(form, {
    name: row.name,
    type: row.type,
    protocol: row.protocol || 'PDF_BROWSER',
    ip: row.ip || '',
    port: row.port || 9100,
    modelSuggestion: row.modelSuggestion || 'OTHER',
    enabled: row.enabled ?? 1,
  })
  dialogVisible.value = true
}

function onTypeChange() {
  if (form.type === 'NORMAL') {
    form.protocol = 'PDF_BROWSER'
    form.ip = ''
  } else {
    form.protocol = form.protocol === 'PDF_BROWSER' ? 'ZPL' : form.protocol
  }
}

async function onSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    const payload: SysPrinter = {
      name: form.name,
      type: form.type,
      ip: form.type === 'LABEL' ? form.ip : null,
      port: form.port,
      protocol: form.type === 'LABEL' ? (form.protocol as 'ZPL' | 'TSPL') : 'PDF_BROWSER',
      modelSuggestion: form.modelSuggestion,
      enabled: form.enabled,
    }
    let r: { code?: number; message?: string }
    if (editing.value && editingId.value != null) {
      r = (await E12PrinterService.updatePrinter(editingId.value, payload, 1)) as { code?: number; message?: string }
    } else {
      r = (await E12PrinterService.createPrinter(payload, 1, 1)) as { code?: number; message?: string }
    }
    if (r?.code === 0) {
      ElMessage.success('保存成功')
      dialogVisible.value = false
      reload()
    } else {
      ElMessage.error(r?.message || '保存失败')
    }
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || String(e)
    ElMessage.error('保存失败：' + msg)
  }
}

async function onDelete(row: SysPrinter) {
  if (row.id == null) return
  try {
    const r = (await E12PrinterService.deletePrinter(row.id)) as { code?: number; message?: string }
    if (r?.code === 0) {
      ElMessage.success('删除成功')
      reload()
    } else if (r?.code === 40902) {
      ElMessageBox.alert('打印机已被使用 · 请改为 enabled=0', '无法删除', { type: 'warning' })
    } else {
      ElMessage.error(r?.message || '删除失败')
    }
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || String(e)
    ElMessage.error('删除失败：' + msg)
  }
}

async function toggleEnabled(row: SysPrinter, v: 0 | 1) {
  if (row.id == null) return
  try {
    const r = (await E12PrinterService.updatePrinter(row.id, { ...row, enabled: v }, 1)) as { code?: number; message?: string }
    if (r?.code === 0) {
      ElMessage.success(v === 1 ? '已启用' : '已停用')
      reload()
    } else {
      ElMessage.error(r?.message || '操作失败')
    }
  } catch (e: unknown) {
    const msg = (e as { message?: string })?.message || String(e)
    ElMessage.error('操作失败：' + msg)
  }
}

// === 自动轮询（30s 刷新状态徽章） ===
let pollHandle: number | undefined
onMounted(() => {
  reload()
  pollHandle = window.setInterval(reload, 30_000)
})
onUnmounted(() => {
  if (pollHandle) window.clearInterval(pollHandle)
})
</script>

<style scoped>
.printers-page {
  padding: 16px;
}
</style>
