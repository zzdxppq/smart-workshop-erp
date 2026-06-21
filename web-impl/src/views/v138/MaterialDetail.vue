<template>
  <ErpPageShell title="料号详情" description="查看料号基本信息、工艺路线、图纸与成本数据。">
    <div v-loading="detailLoading">
    <!-- 顶部基本信息 -->
    <el-card v-if="detail.base" class="summary-card">
      <div class="summary-title">{{ detail.base.materialNo }} - {{ detail.base.name }}</div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="料号">{{ detail.base.materialNo }}</el-descriptions-item>
        <el-descriptions-item label="规格">{{ detail.base.spec }}</el-descriptions-item>
        <el-descriptions-item label="单位">{{ detail.base.unit }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.base.category }}</el-descriptions-item>
        <el-descriptions-item label="默认仓库">{{ detail.base.defaultWarehouse }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 7 Tab -->
    <el-tabs v-model="activeTab" class="detail-tabs">
      <!-- Tab 1 基本信息 -->
      <el-tab-pane label="基本信息" name="base">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="料号">{{ detail.base?.materialNo }}</el-descriptions-item>
          <el-descriptions-item label="物料名">{{ detail.base?.name }}</el-descriptions-item>
        </el-descriptions>
      </el-tab-pane>

      <!-- Tab 2 工艺路线 -->
      <el-tab-pane
        label="工艺路线"
        name="process"
        :disabled="!canView('process')"
      >
        <div v-if="canView('process')" style="margin-bottom: 12px">
          <router-link :to="`/material/product-route/${materialId}`">
            <el-button type="primary" link>编辑产品工艺路线 →</el-button>
          </router-link>
        </div>
        <el-table v-if="detail.process?.routes" :data="detail.process.routes" border>
          <el-table-column prop="stepSeq" label="工序" width="80" />
          <el-table-column prop="processNo" label="工艺号" width="120" />
          <el-table-column prop="workcenter" label="工作中心" />
          <el-table-column prop="stdMinutes" label="工时(分)" width="100" />
          <el-table-column prop="equipment" label="设备" />
        </el-table>
        <el-empty v-else description="无权限查看工艺路线" />
      </el-tab-pane>

      <!-- Tab 3 图纸 -->
      <el-tab-pane
        label="图纸"
        name="drawing"
        :disabled="!canView('drawing')"
      >
        <div v-if="detail.drawing?.dwgNo" class="tab-panel">
          <p>图纸号：{{ detail.drawing.dwgNo }}</p>
          <p>版本：{{ detail.drawing.version }}</p>
          <p>状态：
            <ErpStatusTag :status="detail.drawing.status" />
          </p>
          <el-link v-if="detail.drawing.pdfUrl" :href="detail.drawing.pdfUrl" type="primary" target="_blank">
            查看 PDF
          </el-link>
        </div>
        <el-empty v-else description="暂无关联图纸" />
      </el-tab-pane>

      <!-- Tab 4 价格 -->
      <el-tab-pane
        label="价格"
        name="price"
        :disabled="!canView('price')"
      >
        <div v-if="detail.price" class="tab-panel">
          <el-statistic title="当前价" :value="detail.price.currentPrice" :precision="2" prefix="¥" />
          <el-row :gutter="20">
            <el-col :span="6"><el-statistic title="30 天均价" :value="detail.price.avg30d" :precision="2" /></el-col>
            <el-col :span="6"><el-statistic title="30 天最低" :value="detail.price.min30d" :precision="2" /></el-col>
            <el-col :span="6"><el-statistic title="30 天最高" :value="detail.price.max30d" :precision="2" /></el-col>
          </el-row>
        </div>
        <el-empty v-else description="无权限查看价格" />
      </el-tab-pane>

      <!-- Tab 5 材料成本 -->
      <el-tab-pane
        label="材料成本"
        name="cost"
        :disabled="!canView('cost')"
      >
        <div v-if="detail.cost" class="tab-panel">
          <p>材料成本：¥{{ detail.cost.materialCost }}</p>
          <p>报废率：{{ detail.cost.scrapRate }}</p>
          <p>有效成本：¥{{ detail.cost.effectiveCost }}</p>
        </div>
      </el-tab-pane>

      <!-- Tab 6 工时成本 -->
      <el-tab-pane
        label="工时成本"
        name="labor"
        :disabled="!canView('labor')"
      >
        <div v-if="detail.labor" class="tab-panel">
          <p>工时：{{ detail.labor.laborMinutes }} 分</p>
          <p>时薪：¥{{ detail.labor.hourlyRate }}</p>
          <p>工时成本：¥{{ detail.labor.laborCost }}</p>
        </div>
      </el-tab-pane>

      <!-- Tab 7 外协成本 -->
      <el-tab-pane
        label="外协成本"
        name="outsource"
        :disabled="!canView('outsource')"
      >
        <div v-if="detail.outsource" class="tab-panel">
          <p>外协成本：¥{{ detail.outsource.outsourceCost }}</p>
          <p>供应商：{{ detail.outsource.supplier }}</p>
          <p>交期：{{ detail.outsource.leadDays }} 天</p>
        </div>
      </el-tab-pane>
    </el-tabs>
    </div>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { unwrapResult } from '@/utils/apiPage'
import { V138MaterialService } from '@/api/generated/services/V138MaterialService'
import { useAuthStore } from '@/stores/auth'
import { extractRoles } from '@/utils/jwt'
import type { MaterialDetailResponse } from '@/api/generated/models/MaterialDetailResponse'

const route = useRoute()
const authStore = useAuthStore()
const materialId = computed(() => route.params.id as string)

const activeTab = ref('base')

const userRoles = computed<string[]>(() => extractRoles(authStore.token))

const tabPermissions: Record<string, string[]> = {
  // 基本信息：工程师/仓管/采购/管理层都可看
  base:      ['WAREHOUSE', 'PURCHASER', 'ENGINEER', 'PROD_MGR', 'PRODUCTION_MANAGER', 'GM'],
  // 工艺路线：工程师核心职责，必须可见
  process:   ['ENGINEER', 'WAREHOUSE', 'GM'],
  // 图纸：工程师可上传/查看
  drawing:   ['ENGINEER', 'WAREHOUSE', 'SALES', 'GM'],
  // 价格/材料/工时/外协成本：仅财务/管理层可见（工程师无权看金额，G7 权限隔离）
  price:     ['PURCHASER', 'FINANCE', 'GM'],
  cost:      ['PURCHASER', 'FINANCE', 'GM'],
  labor:     ['PURCHASER', 'FINANCE', 'GM'],
  outsource: ['PURCHASER', 'FINANCE', 'GM'],
}

function canView(tab: string): boolean {
  return tabPermissions[tab]?.some(r => userRoles.value.includes(r)) ?? false
}

const detail = ref<MaterialDetailResponse>({})
const detailLoading = ref(false)

async function fetchDetail() {
  detailLoading.value = true
  try {
    detail.value = unwrapResult<MaterialDetailResponse>(
      await V138MaterialService.getMaterialDetail(Number(materialId.value)),
    )
  } catch (e: unknown) {
    ElMessage.error(`加载详情失败：${(e as { message?: string })?.message ?? '未知'}`)
    detail.value = {}
  } finally {
    detailLoading.value = false
  }
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.summary-card {
  margin-bottom: 16px;
}
.summary-title {
  margin-bottom: 12px;
  font-size: 18px;
  font-weight: 600;
  color: var(--erp-text-primary);
}
.detail-tabs {
  margin-top: 16px;
  background: var(--erp-bg-card);
  border-radius: 8px;
  padding: 4px 16px 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}
.detail-tabs :deep(.el-tabs__header) {
  margin: 0 0 12px;
  border-bottom: 2px solid var(--erp-border);
}
.detail-tabs :deep(.el-tabs__nav-wrap)::after {
  height: 1px;
}
.detail-tabs :deep(.el-tabs__item) {
  font-size: 14px;
  font-weight: 500;
  color: var(--erp-text-secondary);
  padding: 0 18px;
  height: 44px;
  line-height: 44px;
}
.detail-tabs :deep(.el-tabs__item.is-active) {
  color: var(--erp-color-primary, #3b82f6);
  font-weight: 600;
}
.detail-tabs :deep(.el-tabs__active-bar) {
  height: 3px;
  border-radius: 2px;
}
.tab-panel {
  padding: 20px;
  margin-top: 4px;
  background: var(--erp-bg-page, #fafbfc);
  border-radius: 6px;
  min-height: 200px;
  color: var(--erp-text-primary);
  border: 1px solid var(--erp-border);
}
.tab-panel p {
  margin: 10px 0;
  font-size: 14px;
  line-height: 1.7;
  color: var(--erp-text-primary);
}
.tab-panel p strong {
  color: var(--erp-color-primary, #3b82f6);
  font-weight: 600;
  margin-right: 6px;
}
</style>
