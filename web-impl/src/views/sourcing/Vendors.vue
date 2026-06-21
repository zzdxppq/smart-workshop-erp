<template>
  <ErpPageShell title="厂商资料" description="E6-S8 · 采购员维护厂商档案；系统经 163 SMTP 发信，厂商接收邮箱不限 163。">
    <template #actions>
      <el-button type="primary" @click="openCreate">新建厂商</el-button>
    </template>

    <el-form :inline="true" style="margin-top: 4px">
      <el-form-item label="厂商名称">
        <el-input v-model="keyword" clearable @keyup.enter="onSearch" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="onSearch">查询</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="items" stripe border>
      <el-table-column prop="vendorCode" label="厂商编码" min-width="120" />
      <el-table-column prop="vendorName" label="厂商名称" min-width="140" />
      <el-table-column prop="contact" label="联系人" width="100" />
      <el-table-column prop="phone" label="电话" width="120">
        <template #default="{ row }">{{ row.phone || '—' }}</template>
      </el-table-column>
      <el-table-column label="接收邮箱" min-width="180">
        <template #default="{ row }">
          <el-tag v-if="row.notifyEmail" :type="row.emailMissing ? 'warning' : 'success'">
            {{ maskEmail(row.notifyEmail) }}
          </el-tag>
          <el-tag v-else type="warning">待补充</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="对账邮箱" min-width="160">
        <template #default="{ row }">
          {{ row.defaultReconEmail ? maskEmail(row.defaultReconEmail) : '同接收邮箱' }}
        </template>
      </el-table-column>
      <el-table-column label="通知渠道" width="110">
        <template #default>
          <el-tag type="info">163 SMTP</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="rating" label="评级" width="80" />
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <ErpStatusTag :status="row.status" :label="vendorStatusLabel(row.status)" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="140" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="pageNum"
      v-model:page-size="pageSize"
      :total="total"
      :page-sizes="ERP_PAGE_SIZES"
      :layout="ERP_PAGINATION_LAYOUT"
      background
      class="erp-pagination"
      style="margin-top: 12px"
      @current-change="onPageChange"
      @size-change="onSearch"
    />

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑厂商' : '新建厂商'" width="560px" destroy-on-close>
      <el-alert type="info" :closable="false" title="系统使用 163 邮箱 SMTP 发送通知；厂商接收/对账邮箱可为任意有效邮箱。" style="margin-bottom: 12px" />
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="厂商名称" prop="vendorName">
          <el-input v-model="form.vendorName" maxlength="100" />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" maxlength="50" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="选填 · 11 位手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="接收邮箱" prop="contactEmail">
          <el-input v-model="form.contactEmail" placeholder="必填 · 接收委外/对账通知" />
        </el-form-item>
        <el-form-item label="对账邮箱" prop="defaultReconEmail">
          <el-input v-model="form.defaultReconEmail" placeholder="选填 · 默认同接收邮箱" />
        </el-form-item>
        <el-form-item label="营业执照">
          <el-upload
            ref="licenseUploadRef"
            :auto-upload="false"
            :limit="1"
            accept="image/*,.pdf"
            :on-change="handleLicenseChange"
            :on-remove="handleLicenseRemove"
            action="#"
          >
            <el-button size="small" type="primary">选择文件</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 JPG/PNG/PDF，不超过 5MB</div>
            </template>
          </el-upload>
          <div v-if="form.businessLicenseUrl" class="license-preview">
            <el-link :href="form.businessLicenseUrl" target="_blank" type="primary">
              查看已上传营业执照
            </el-link>
            <el-tag v-if="form.businessLicenseExpireDate" style="margin-left: 8px" :type="isLicenseExpiringSoon ? 'warning' : 'success'">
              到期: {{ form.businessLicenseExpireDate }}
            </el-tag>
          </div>
        </el-form-item>
        <el-form-item label="执照到期日">
          <el-date-picker
            v-model="form.businessLicenseExpireDate"
            type="date"
            placeholder="选填 · 到期提醒"
            value-format="YYYY-MM-DD"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="加工能力" prop="capabilitiesText">
          <el-input v-model="form.capabilitiesText" placeholder="必填 · 逗号分隔，如 CNC,阳极" />
        </el-form-item>
        <el-form-item label="信用等级">
          <el-select v-model="form.creditLevel" style="width: 120px">
            <el-option label="A" value="A" />
            <el-option label="B" value="B" />
            <el-option label="C" value="C" />
            <el-option label="D" value="D" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editingId" label="合作状态">
          <el-select v-model="form.status" style="width: 140px">
            <el-option label="合作中" value="ACTIVE" />
            <el-option label="暂停" value="SUSPENDED" />
            <el-option label="黑名单" value="BLACKLIST" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveVendor">保存</el-button>
      </template>
    </el-dialog>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import type { FormInstance, FormRules } from 'element-plus'
import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import { useSourcingStore } from '@/stores/sourcing'
import { usePagedList } from '@/composables/usePagedList'
import { unwrapResult } from '@/utils/apiPage'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { vendorStatusLabel } from '@/utils/statusLabels'

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const PHONE_RE = /^1\d{10}$/

const sourcingStore = useSourcingStore()
const keyword = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const formRef = ref<FormInstance>()
const licenseFile = ref<File | null>(null)
const licenseUploadRef = ref()

const form = ref({
  vendorName: '',
  contactName: '',
  contactPhone: '',
  contactEmail: '',
  defaultReconEmail: '',
  businessLicenseUrl: '',
  businessLicenseExpireDate: '',
  capabilitiesText: '',
  creditLevel: 'C',
  status: 'ACTIVE',
})

const rules: FormRules = {
  vendorName: [{ required: true, message: '请输入厂商名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactEmail: [
    { required: true, message: '接收邮箱必填', trigger: 'blur' },
    { pattern: EMAIL_RE, message: '邮箱格式错误', trigger: 'blur' },
  ],
  contactPhone: [
    {
      validator: (_r, v, cb) => {
        if (!v || PHONE_RE.test(v)) cb()
        else cb(new Error('应为 11 位手机号'))
      },
      trigger: 'blur',
    },
  ],
  defaultReconEmail: [
    {
      validator: (_r, v, cb) => {
        if (!v || EMAIL_RE.test(v)) cb()
        else cb(new Error('对账邮箱格式错误'))
      },
      trigger: 'blur',
    },
  ],
  capabilitiesText: [{ required: true, message: '请填写加工能力', trigger: 'blur' }],
}

const { items, loading, pageNum, pageSize, total, reload } = usePagedList<any>((params) =>
  sourcingStore.listVendors({ keyword: keyword.value || undefined, ...params }),
)

function maskEmail(email: string) {
  const at = email.indexOf('@')
  if (at <= 0) return email
  const local = email.slice(0, at)
  const domain = email.slice(at)
  const head = local.length <= 3 ? local : local.slice(0, 3) + '***'
  return head + domain
}

function isLicenseExpiringSoon() {
  if (!form.value.businessLicenseExpireDate) return false
  const expireDate = new Date(form.value.businessLicenseExpireDate)
  const thirtyDaysLater = new Date()
  thirtyDaysLater.setDate(thirtyDaysLater.getDate() + 30)
  return expireDate <= thirtyDaysLater
}

function handleLicenseChange(file: any) {
  licenseFile.value = file.raw
}

function handleLicenseRemove() {
  licenseFile.value = null
}

function resetForm() {
  form.value = {
    vendorName: '',
    contactName: '',
    contactPhone: '',
    contactEmail: '',
    defaultReconEmail: '',
    businessLicenseUrl: '',
    businessLicenseExpireDate: '',
    capabilitiesText: '',
    creditLevel: 'C',
    status: 'ACTIVE',
  }
}

function openCreate() {
  editingId.value = null
  resetForm()
  dialogVisible.value = true
}

function openEdit(row: any) {
  editingId.value = row.id
  form.value = {
    vendorName: row.vendorName ?? '',
    contactName: row.contact ?? row.contactName ?? '',
    contactPhone: row.phone ?? row.contactPhone ?? '',
    contactEmail: row.notifyEmail ?? row.contactEmail ?? '',
    defaultReconEmail: row.defaultReconEmail ?? '',
    businessLicenseUrl: row.businessLicenseUrl ?? '',
    businessLicenseExpireDate: row.businessLicenseExpireDate ?? '',
    capabilitiesText: parseCapabilities(row.capabilitiesJson),
    creditLevel: row.rating ?? row.creditLevel ?? 'C',
    status: row.status ?? 'ACTIVE',
  }
  dialogVisible.value = true
}

function parseCapabilities(json?: string) {
  if (!json) return ''
  try {
    const arr = JSON.parse(json)
    return Array.isArray(arr) ? arr.join(',') : ''
  } catch {
    return ''
  }
}

function buildPayload() {
  const capabilities = form.value.capabilitiesText
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
  return {
    vendorName: form.value.vendorName.trim(),
    contactName: form.value.contactName.trim(),
    contactPhone: form.value.contactPhone.trim() || undefined,
    contactEmail: form.value.contactEmail.trim(),
    defaultReconEmail: form.value.defaultReconEmail.trim() || undefined,
    businessLicenseExpireDate: form.value.businessLicenseExpireDate || undefined,
    capabilities,
    creditLevel: form.value.creditLevel,
    status: form.value.status,
  }
}

async function saveVendor() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  saving.value = true
  try {
    const payload = buildPayload()
    if (editingId.value) {
      unwrapResult(await sourcingStore.updateVendor(editingId.value, payload))
      // 上传营业执照
      if (licenseFile.value) {
        const fd = new FormData()
        fd.append('file', licenseFile.value)
        await sourcingStore.uploadBusinessLicense(editingId.value, fd)
        ElMessage.success('营业执照已上传')
      }
      ElMessage.success('厂商资料已更新')
    } else {
      const result = unwrapResult<{ id?: number; data?: { id?: number } }>(
        await sourcingStore.createVendor(payload),
      )
      const newId = result?.id || result?.data?.id
      if (newId && licenseFile.value) {
        const fd = new FormData()
        fd.append('file', licenseFile.value)
        await sourcingStore.uploadBusinessLicense(newId, fd)
        ElMessage.success('营业执照已上传')
      }
      ElMessage.success('厂商已创建')
    }
    dialogVisible.value = false
    onPageChange()
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

function onSearch() {
  pageNum.value = 1
  reload({ keyword: keyword.value || undefined })
}
function onPageChange() {
  reload({ keyword: keyword.value || undefined })
}

onMounted(onSearch)
</script>
