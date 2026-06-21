<template>
  <div class="visitor-page" :class="{ 'tv-mode': tvMode }">
    <!-- ============ 用户栏 ============ -->
    <div v-if="!tvMode" class="visitor-userbar">
      <span class="user-label">当前用户</span>
      <span class="user-name">{{ displayName }}</span>
      <el-button link type="primary" @click="onLogout">退出登录</el-button>
    </div>

    <!-- ============ Header ============ -->
    <header class="visitor-header">
      <h1>生产进度查询</h1>
      <p class="subtitle">输入订单号、图号、产品名称或客户名称，即可查询生产进度</p>
    </header>

    <!-- ============ Search Box ============ -->
    <section class="search-box">
      <el-input
        v-model="keyword"
        size="large"
        placeholder="🔎  请输入订单号、图号、产品名称或客户名称..."
        clearable
        @keyup.enter="onSearch"
      >
        <template #append>
          <el-button type="primary" :loading="loading" @click="onSearch">搜索</el-button>
        </template>
      </el-input>
      <div class="hot-keywords">
        <span class="hot-label">热门搜索：</span>
        <el-link v-for="kw in hotKeywords" :key="kw" type="primary" :underline="false" @click="onHotKeyword(kw)">
          {{ kw }}
        </el-link>
      </div>
    </section>

    <!-- ============ Result Area ============ -->
    <main v-loading="loading" class="results">
      <!-- 详情视图 -->
      <section v-if="detail" class="detail-view">
        <el-card shadow="never" class="detail-card">
          <template #header>
            <div class="detail-header">
              <div>
                <div class="detail-title">
                  <span class="wo">工单 {{ detail.workorderNo }}</span>
                  <el-tag :type="statusTagType(detail.status)" size="default" effect="dark">
                    {{ statusLabel(detail.status) }}
                  </el-tag>
                </div>
                <div class="detail-meta">
                  <span>产品：<strong>{{ detail.productName || detail.materialCode }}</strong></span>
                  <span v-if="detail.materialCode">料号：{{ detail.materialCode }}</span>
                  <span>数量：<strong>{{ detail.qty }}{{ detail.unit || '件' }}</strong></span>
                  <span v-if="detail.plannedDelivery">计划完成：<strong>{{ formatDate(detail.plannedDelivery) }}</strong></span>
                </div>
              </div>
              <el-button @click="detail = null">返回列表</el-button>
            </div>
          </template>

          <el-progress
            :percentage="Number(detail.progress ?? 0)"
            :stroke-width="18"
            :status="(progressStatus(detail.progress) as 'success' | 'warning' | 'exception' | undefined)"
            class="detail-progress"
          />

          <h3 class="timeline-title">生产时间线</h3>
          <el-timeline class="timeline">
            <el-timeline-item
              v-for="(t, idx) in (detail.timeline as Record<string, unknown>[] | undefined) || []"
              :key="idx"
              :timestamp="formatDate(t.expectedEnd)"
              :type="timelineType(t.status) as 'primary' | 'success' | 'warning' | 'info' | 'danger'"
              :hollow="idx !== activeTimelineIndex(detail.timeline as Record<string, unknown>[])"
              placement="top"
            >
              <div class="timeline-item">
                <span class="step-name">{{ t.name }}</span>
                <el-tag size="small" :type="timelineType(t.status) as 'primary' | 'success' | 'warning' | 'info' | 'danger'" effect="plain">
                  {{ t.statusLabel }}
                </el-tag>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </section>

      <!-- 列表视图（默认 + 搜索结果都走这里） -->
      <section v-else>
        <div class="list-header">
          <h2>
            📋 全厂进行中订单
            <span v-if="!searched" class="muted-count">（共 {{ items.length }} 单）</span>
            <span v-else class="muted-count">（搜索结果：{{ items.length }} 单）</span>
          </h2>
        </div>

        <div v-if="!loading && !items.length" class="empty-tip">
          <el-empty
            :description="searched ? `未找到与「${keyword}」相关的订单，请尝试其他关键词` : '暂无进行中订单'"
          />
        </div>

        <div class="card-grid">
          <el-card
            v-for="row in items"
            :key="String(row.workorderNo ?? row.orderNo)"
            shadow="hover"
            class="progress-card"
            @click="onCardClick(row)"
          >
            <div class="card-title">
              <span class="wo">{{ row.workorderNo ?? row.orderNo }}</span>
              <el-tag :type="statusTagType(row.status)" size="small" effect="dark">
                {{ statusLabel(row.status) }}
              </el-tag>
            </div>
            <div class="meta">
              <span>产品：<strong>{{ row.productName || row.materialCode || '—' }}</strong></span>
              <span v-if="row.materialCode" class="meta-mono">{{ row.materialCode }}</span>
              <span v-if="row.qty">数量：<strong>{{ row.qty }}{{ row.unit || '件' }}</strong></span>
            </div>
            <div class="step-row">
              <span>当前工序：<strong>{{ row.currentStep || '生产中' }}</strong></span>
              <span v-if="row.plannedDelivery" class="plan-end">
                计划完成：<strong>{{ formatDate(row.plannedDelivery) }}</strong>
              </span>
            </div>
            <el-progress
              :percentage="Number(row.progress ?? 0)"
              :stroke-width="14"
              :status="(progressStatus(row.progress) as 'success' | 'warning' | 'exception' | undefined)"
              class="card-progress"
            />
            <div class="card-footer">
              <el-button type="primary" link @click.stop="onCardClick(row)">
                查看详情 <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </el-card>
        </div>

        <!-- 底部提示 -->
        <div v-if="items.length" class="footer-tip">
          💡 提示：输入您的订单号或客户名称可精准查询您的订单进度
        </div>
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import { useBaseStore } from '@/stores/_base'
import { useAuthStore } from '@/stores/auth'
import { unwrapResult } from '@/utils/apiPage'

const route = useRoute()
const router = useRouter()
const api = useBaseStore().api
const auth = useAuthStore()

const displayName = computed(() => auth.user?.realName || auth.user?.username || '用户')

const tvMode = ref(route.query.mode === 'tv')
const keyword = ref('')
const loading = ref(false)
const searched = ref(false)
const items = ref<Record<string, unknown>[]>([])
const detail = ref<Record<string, unknown> | null>(null)

// 热门搜索（脱敏的公共产品名）
const hotKeywords = ['法兰盘', '连接器外壳', '液压阀体']

function formatDate(v: unknown) {
  if (!v) return '—'
  const s = String(v)
  return s.length >= 10 ? s.slice(0, 10) : s
}

function statusLabel(s: unknown) {
  const v = String(s ?? '')
  switch (v) {
    case 'IN_PROGRESS':
    case 'PROCESSING':
      return '进行中'
    case 'FINISHED':
    case 'CLOSED':
    case 'SHIPPED':
    case 'SETTLED':
      return '已完成'
    case 'SCHEDULED':
    case 'PENDING':
    case 'CONFIRMED':
      return '待排产'
    case 'INSPECTING':
    case 'REPORTED':
      return '质检中'
    case 'PARTIAL_SHIPPED':
      return '部分发货'
    case 'PRODUCING':
      return '生产中'
    default:
      return '跟进中'
  }
}

function statusTagType(s: unknown) {
  const label = statusLabel(s)
  switch (label) {
    case '已完成': return 'success'
    case '进行中':
    case '生产中':
    case '质检中': return 'primary'
    case '部分发货': return 'warning'
    case '待排产':
    default: return 'info'
  }
}

function progressStatus(p: unknown) {
  const n = Number(p ?? 0)
  if (n >= 100) return 'success'
  if (n >= 50) return undefined
  return undefined
}

function timelineType(s: unknown) {
  const v = String(s ?? '')
  if (v === 'FINISHED' || v === 'CLOSED') return 'success'
  if (v === 'IN_PROGRESS' || v === 'PROCESSING') return 'primary'
  if (v === 'INSPECTING' || v === 'REPORTED') return 'warning'
  return 'info'
}

function activeTimelineIndex(timeline: Record<string, unknown>[]) {
  const idx = timeline.findIndex(t => t.status === 'IN_PROGRESS' || t.status === 'PROCESSING' || t.status === 'INSPECTING')
  if (idx >= 0) return idx
  return -1
}

async function loadDefaultList() {
  loading.value = true
  try {
    const r = unwrapResult<{ list?: Record<string, unknown>[] }>(
      await api.get('/visitor/progress/list', { params: { limit: 23 } }),
    )
    items.value = r.list ?? []
  } catch (e: unknown) {
    ElMessage.warning((e as { message?: string })?.message || '加载默认列表失败')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  if (!keyword.value.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  loading.value = true
  searched.value = true
  detail.value = null
  try {
    const r = unwrapResult<{ list?: Record<string, unknown>[] }>(
      await api.get('/visitor/progress/search', { params: { keyword: keyword.value.trim() } }),
    )
    items.value = r.list ?? []
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '搜索失败')
    items.value = []
  } finally {
    loading.value = false
  }
}

async function onHotKeyword(kw: string) {
  keyword.value = kw
  await onSearch()
}

async function onLogout() {
  await auth.logoutRemote()
  router.push({ name: 'Login' })
}

async function onCardClick(row: Record<string, unknown>) {
  const woNo = (row.workorderNo ?? row.orderNo) as string | undefined
  if (!woNo) return
  loading.value = true
  try {
    const r = unwrapResult<Record<string, unknown>>(
      await api.get('/visitor/progress/detail', { params: { workorderNo: woNo } }),
    )
    detail.value = r
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '加载详情失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadDefaultList()
})
</script>

<style scoped>
/* ===== 工业风（深色主题 + 蓝色主调） ===== */
.visitor-page {
  min-height: 100vh;
  padding: 32px 24px;
  background:
    linear-gradient(135deg, #0b1220 0%, #1a2332 60%, #0b1220 100%);
  color: #e6edf6;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, -apple-system, sans-serif;
  position: relative;
}
.visitor-userbar {
  position: absolute;
  top: 16px;
  right: 24px;
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 13px;
  color: #94a3b8;
}
.visitor-userbar .user-label { color: #64748b; }
.visitor-userbar .user-name {
  color: #e6edf6;
  font-weight: 600;
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.visitor-userbar :deep(.el-button) {
  color: #60a5fa !important;
  font-size: 13px;
}
.visitor-header { text-align: center; margin-bottom: 28px; padding-top: 8px; }
.visitor-header h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.04em;
  text-shadow: 0 2px 12px rgba(59, 130, 246, 0.4);
}
.subtitle {
  color: #94a3b8;
  margin: 10px 0 0;
  font-size: 14px;
}

/* 搜索 */
.search-box { max-width: 760px; margin: 0 auto 28px; }
.search-box :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(59, 130, 246, 0.4);
  box-shadow: none;
  border-radius: 10px;
  padding: 4px 12px;
}
.search-box :deep(.el-input__inner) {
  color: #e6edf6;
  font-size: 16px;
}
.search-box :deep(.el-input__inner::placeholder) { color: #64748b; }
.search-box :deep(.el-input-group__append) {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  border: 0;
}
.search-box :deep(.el-input-group__append .el-button) {
  background: transparent;
  color: #fff;
  font-weight: 600;
  font-size: 15px;
}
.hot-keywords {
  display: flex; align-items: center; gap: 12px; flex-wrap: wrap;
  margin-top: 12px; padding: 0 4px; font-size: 13px;
}
.hot-label { color: #64748b; }
.hot-keywords :deep(.el-link) { color: #60a5fa !important; }

/* 列表区 */
.results { max-width: 1200px; margin: 0 auto; }
.list-header h2 {
  font-size: 20px; color: #e6edf6; font-weight: 600;
  margin: 0 0 20px; padding-left: 4px;
  border-left: 4px solid #3b82f6;
}
.muted-count { font-size: 14px; color: #94a3b8; font-weight: 400; margin-left: 6px; }

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 18px;
}

/* 卡片 · 工业风深色卡 */
.progress-card {
  background: linear-gradient(160deg, #1e293b 0%, #131c2b 100%) !important;
  border: 1px solid rgba(59, 130, 246, 0.25) !important;
  border-radius: 12px !important;
  color: #e6edf6;
  cursor: pointer;
  transition: all 0.25s;
}
.progress-card:hover {
  transform: translateY(-2px);
  border-color: #3b82f6 !important;
  box-shadow: 0 8px 24px rgba(59, 130, 246, 0.18);
}
.progress-card :deep(.el-card__body) { padding: 18px 20px; }

.card-title {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 10px;
}
.card-title .wo {
  font-size: 17px; font-weight: 700; color: #ffffff;
  font-family: 'JetBrains Mono', 'SF Mono', monospace;
  letter-spacing: 0.02em;
}
.meta {
  display: flex; flex-wrap: wrap; gap: 12px 16px; font-size: 13px; color: #94a3b8;
  margin-bottom: 10px;
}
.meta strong { color: #e6edf6; font-weight: 600; margin-left: 4px; }
.meta-mono { font-family: 'JetBrains Mono', monospace; color: #60a5fa; }

.step-row {
  display: flex; justify-content: space-between; gap: 12px; flex-wrap: wrap;
  font-size: 13px; color: #94a3b8; margin: 8px 0;
}
.step-row strong { color: #cbd5e1; margin-left: 4px; }
.plan-end strong { color: #fbbf24; }

.card-progress { margin: 12px 0 8px; }
.card-progress :deep(.el-progress__text) { color: #cbd5e1 !important; font-size: 12px; }
.card-progress :deep(.el-progress-bar__outer) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}

.card-footer {
  display: flex; justify-content: flex-end; align-items: center;
  margin-top: 8px; padding-top: 10px;
  border-top: 1px dashed rgba(59, 130, 246, 0.2);
}
.card-footer :deep(.el-button) {
  color: #60a5fa !important;
  font-size: 13px;
}

/* 详情 */
.detail-card {
  background: linear-gradient(160deg, #1e293b 0%, #131c2b 100%) !important;
  border: 1px solid rgba(59, 130, 246, 0.3) !important;
  border-radius: 14px !important;
  color: #e6edf6;
}
.detail-card :deep(.el-card__header) {
  border-bottom: 1px solid rgba(59, 130, 246, 0.2);
  padding: 20px 24px;
}
.detail-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.detail-title {
  display: flex; align-items: center; gap: 12px; margin-bottom: 10px;
}
.detail-title .wo {
  font-size: 22px; font-weight: 700; color: #ffffff;
  font-family: 'JetBrains Mono', monospace;
}
.detail-meta { display: flex; flex-wrap: wrap; gap: 16px; font-size: 13px; color: #94a3b8; }
.detail-meta strong { color: #e6edf6; margin-left: 4px; }
.detail-progress { margin: 8px 0 24px; }
.detail-progress :deep(.el-progress__text) { color: #ffffff !important; font-weight: 600; }
.detail-progress :deep(.el-progress-bar__outer) {
  background-color: rgba(255, 255, 255, 0.08) !important;
}
.detail-card :deep(.el-button) {
  background: rgba(59, 130, 246, 0.15);
  border: 1px solid rgba(59, 130, 246, 0.3);
  color: #93c5fd;
}
.detail-card :deep(.el-button:hover) {
  background: rgba(59, 130, 246, 0.25);
  color: #ffffff;
}

.timeline-title {
  font-size: 15px; font-weight: 600; color: #cbd5e1;
  margin: 16px 0 12px; padding-left: 8px;
  border-left: 3px solid #3b82f6;
}
.timeline { padding-left: 6px; }
.timeline :deep(.el-timeline-item__timestamp) { color: #94a3b8; font-size: 12px; }
.timeline-item {
  display: flex; align-items: center; gap: 10px;
  color: #e6edf6; font-size: 14px;
}
.step-name { font-weight: 500; }

/* 空状态 */
.empty-tip { padding: 40px 0; }
.empty-tip :deep(.el-empty__description p) { color: #94a3b8; }

.footer-tip {
  text-align: center; color: #64748b;
  padding: 24px 0 0;
  font-size: 13px;
}

/* TV 大屏模式 */
.tv-mode { font-size: 1.15rem; }
.tv-mode .visitor-header h1 { font-size: 38px; }
.tv-mode .card-grid { grid-template-columns: repeat(auto-fill, minmax(440px, 1fr)); gap: 24px; }
</style>