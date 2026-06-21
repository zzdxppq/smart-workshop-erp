<template>
  <el-dialog
    v-model="visible"
    title="全局搜索"
    width="640px"
    destroy-on-close
    class="global-search-dialog"
    @opened="focusInput"
  >
    <el-input
      ref="inputRef"
      v-model="keyword"
      placeholder="搜料号 / 工单 / 客户 / 供应商（Ctrl+K）"
      clearable
      :prefix-icon="Search"
      @input="debouncedSearch"
      @keyup.enter="runSearch"
    />
    <div v-loading="loading" class="results">
      <template v-if="keyword.trim()">
        <section v-if="materialHit">
          <h4>物料</h4>
          <div class="hit-row" @click="go(materialHit.path)">
            <span>{{ materialHit.label }}</span>
            <el-tag size="small">料号</el-tag>
          </div>
        </section>
        <section v-if="workorders.length">
          <h4>工单</h4>
          <div v-for="w in workorders" :key="w.id" class="hit-row" @click="go(w.path)">
            <span>{{ w.label }}</span>
            <el-tag size="small" type="warning">工单</el-tag>
          </div>
        </section>
        <section v-if="customers.length">
          <h4>客户</h4>
          <div v-for="c in customers" :key="c.id" class="hit-row" @click="go(c.path)">
            <span>{{ c.label }}</span>
            <el-tag size="small" type="success">客户</el-tag>
          </div>
        </section>
        <section v-if="vendors.length">
          <h4>供应商</h4>
          <div v-for="v in vendors" :key="v.id" class="hit-row" @click="go(v.path)">
            <span>{{ v.label }}</span>
            <el-tag size="small">厂商</el-tag>
          </div>
        </section>
        <el-empty v-if="!loading && !hasAny" description="无匹配结果" />
      </template>
      <p v-else class="hint">输入关键词后自动搜索；支持料号、工单号、客户名、供应商名</p>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import type { ElInput } from 'element-plus'
import { useBaseStore } from '@/stores/_base'
import { useSourcingStore } from '@/stores/sourcing'
import { E5WorkorderService } from '@/api/generated/services/E5WorkorderService'
import { E2CrmService } from '@/api/generated/services/E2CrmService'
import { parsePageItems, unwrapResult } from '@/utils/apiPage'

const visible = defineModel<boolean>({ default: false })

const router = useRouter()
const api = useBaseStore().api
const sourcingStore = useSourcingStore()
const inputRef = ref<InstanceType<typeof ElInput>>()
const keyword = ref('')
const loading = ref(false)
let debounceTimer: ReturnType<typeof setTimeout> | undefined

interface Hit {
  id?: number
  label: string
  path: string
}

const materialHit = ref<Hit | null>(null)
const workorders = ref<Hit[]>([])
const customers = ref<Hit[]>([])
const vendors = ref<Hit[]>([])

const hasAny = computed(
  () => !!materialHit.value || workorders.value.length || customers.value.length || vendors.value.length,
)

function focusInput() {
  setTimeout(() => inputRef.value?.focus(), 50)
}

function debouncedSearch() {
  clearTimeout(debounceTimer)
  debounceTimer = setTimeout(runSearch, 280)
}

async function runSearch() {
  const kw = keyword.value.trim()
  if (!kw) {
    materialHit.value = null
    workorders.value = []
    customers.value = []
    vendors.value = []
    return
  }
  loading.value = true
  const lower = kw.toLowerCase()
  try {
    materialHit.value = null
    try {
      const m = unwrapResult<{ id?: number; materialCode?: string; materialName?: string }>(
        await api.get('/materials/lookup', { params: { code: kw } }),
      )
      if (m?.id) {
        materialHit.value = {
          id: m.id,
          label: `${m.materialCode ?? kw} · ${m.materialName ?? ''}`,
          path: `/material/detail/${m.id}`,
        }
      }
    } catch {
      /* lookup miss */
    }

    const [woRes, custRes, vendorRes] = await Promise.all([
      E5WorkorderService.listWorkorders(1, 80),
      E2CrmService.listCustomers(1, 80),
      sourcingStore.listVendors({ pageNum: 1, pageSize: 80 }),
    ])

    workorders.value = (parsePageItems(woRes).items as { id?: number; workorderNo?: string; productName?: string }[])
      .filter((w) => (w.workorderNo ?? '').toLowerCase().includes(lower) || (w.productName ?? '').toLowerCase().includes(lower))
      .slice(0, 8)
      .map((w) => ({
        id: w.id,
        label: `${w.workorderNo ?? '—'} · ${w.productName ?? ''}`,
        path: w.id ? `/production/workorder-detail/${w.id}` : '/production/workorders',
      }))

    customers.value = (parsePageItems(custRes).items as { id?: number; name?: string; customerCode?: string }[])
      .filter((c) => (c.name ?? '').toLowerCase().includes(lower) || (c.customerCode ?? '').toLowerCase().includes(lower))
      .slice(0, 8)
      .map((c) => ({
        id: c.id,
        label: `${c.customerCode ?? ''} ${c.name ?? ''}`.trim(),
        path: c.id ? `/sales/customers/${c.id}` : '/sales/customers',
      }))

    vendors.value = (parsePageItems(vendorRes).items as { id?: number; vendorName?: string; vendorCode?: string }[])
      .filter((v) => (v.vendorName ?? '').toLowerCase().includes(lower) || (v.vendorCode ?? '').toLowerCase().includes(lower))
      .slice(0, 8)
      .map((v) => ({
        id: v.id,
        label: `${v.vendorCode ?? ''} ${v.vendorName ?? ''}`.trim(),
        path: '/sourcing/vendors',
      }))
  } finally {
    loading.value = false
  }
}

function go(path: string) {
  visible.value = false
  router.push(path)
}

function open(initial?: string) {
  if (initial) keyword.value = initial
  visible.value = true
  if (initial) runSearch()
}

defineExpose({ open })
</script>

<style scoped>
.results {
  margin-top: 16px;
  min-height: 120px;
  max-height: 420px;
  overflow-y: auto;
}
section {
  margin-bottom: 16px;
}
section h4 {
  margin: 0 0 8px;
  font-size: 12px;
  color: var(--erp-text-secondary);
}
.hit-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  border-radius: 6px;
  cursor: pointer;
}
.hit-row:hover {
  background: var(--erp-color-primary-light);
}
.hint {
  margin: 0;
  color: var(--erp-text-muted);
  font-size: 13px;
}
</style>
