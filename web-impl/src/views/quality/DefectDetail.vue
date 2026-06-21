<template>
  <div v-loading="loading">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-header-title">不良品详情</span>
      </template>
    </el-page-header>

    <el-card v-if="defect" style="margin-top: 16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="不良单号">{{ defect.defectNo }}</el-descriptions-item>
        <el-descriptions-item label="工单">{{ defect.workOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ defect.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="不良类型">{{ defect.defectType }}</el-descriptions-item>
        <el-descriptions-item label="原因分类">
          <el-tag v-if="defect.causeCategory" size="small">{{ getCauseCategoryLabel(defect.causeCategory) }}</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="严重度">
          <ErpStatusTag :status="defect.severity" />
        </el-descriptions-item>
        <el-descriptions-item label="数量">{{ defect.qty }}</el-descriptions-item>
        <el-descriptions-item label="总生产数量">{{ defect.totalQty || '-' }}</el-descriptions-item>
        <el-descriptions-item label="PPM不良率">
          {{ defect.defectRatePpm ? defect.defectRatePpm + ' ppm' : '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="责任部门">{{ defect.responsibleDept || '-' }}</el-descriptions-item>
        <el-descriptions-item label="处置方式">
          <el-tag v-if="defect.result === 'REWORK'" type="warning">返工</el-tag>
          <el-tag v-else-if="defect.result === 'SCRAP'" type="danger">报废</el-tag>
          <el-tag v-else-if="defect.result === 'CONCESSION'" type="info">让步接收</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
        <el-descriptions-item label="处置状态">
          <ErpStatusTag :status="defect.dispositionStatus" />
        </el-descriptions-item>
        <el-descriptions-item label="返工次数" v-if="defect.reworkCount">
          {{ defect.reworkCount }} 次
        </el-descriptions-item>
        <el-descriptions-item label="返工工单" v-if="defect.reworkWorkOrderNo">
          {{ defect.reworkWorkOrderNo }}
        </el-descriptions-item>
        <el-descriptions-item label="库存已扣减" v-if="defect.scrapInventoryDeducted">
          <el-tag type="success">已扣减</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="让步审批人" v-if="defect.concessionApproverId">
          {{ defect.concessionApproverId }}
        </el-descriptions-item>
        <el-descriptions-item label="让步审批时间" v-if="defect.concessionApprovedAt">
          {{ defect.concessionApprovedAt }}
        </el-descriptions-item>
      </el-descriptions>

      <!-- V2.1 处置操作面板 -->
      <el-divider content-position="left">处置操作</el-divider>
      <div class="disposition-actions">
        <!-- 待处置状态：选择处置方式 -->
        <template v-if="defect.dispositionStatus === 'PENDING' && !defect.result">
          <el-button type="warning" @click="showActionDialog = true; actionType = 'REWORK'">
            申请返工
          </el-button>
          <el-button type="danger" @click="showActionDialog = true; actionType = 'SCRAP'">
            申请报废
          </el-button>
          <el-button type="info" @click="showActionDialog = true; actionType = 'CONCESSION'">
            申请让步接收
          </el-button>
        </template>

        <!-- 返工：创建工单 -->
        <el-button
          v-if="defect.result === 'REWORK' && !defect.reworkWorkOrderNo"
          type="primary"
          @click="handleCreateReworkWo"
          :loading="processing"
        >
          创建返工工单
        </el-button>

        <!-- 报废：扣减库存 -->
        <el-button
          v-if="defect.result === 'SCRAP' && defect.scrapInventoryDeducted !== 1"
          type="danger"
          @click="handleScrapInventory"
          :loading="processing"
        >
          确认报废扣减库存
        </el-button>

        <!-- 让步接收：审批 -->
        <template v-if="defect.result === 'CONCESSION' && defect.dispositionStatus === 'PENDING'">
          <el-button type="success" @click="handleConcessionApprove(true)" :loading="processing">
            批准让步
          </el-button>
          <el-button type="danger" @click="handleConcessionApprove(false)" :loading="processing">
            驳回让步
          </el-button>
        </template>
      </div>

      <!-- 处置申请对话框 -->
      <el-dialog v-model="showActionDialog" :title="getActionTitle()" width="400px">
        <el-form label-width="100px">
          <el-form-item label="处置数量">
            <el-input-number v-model="actionQty" :min="1" :max="defect.qty" />
          </el-form-item>
          <el-form-item label="责任部门">
            <el-input v-model="actionDept" placeholder="请输入责任部门" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="actionRemark" type="textarea" :rows="2" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showActionDialog = false">取消</el-button>
          <el-button type="primary" @click="handleAddAction" :loading="processing">确认</el-button>
        </template>
      </el-dialog>
    </el-card>

    <!-- 8D 处理历史 -->
    <el-card style="margin-top: 16px" v-if="defect">
      <template #header>
        <span>8D 处理历史</span>
      </template>
      <el-timeline>
        <el-timeline-item
          v-for="(h, idx) in defect.histories || []"
          :key="idx"
          :timestamp="h.createdAt"
          placement="top"
        >
          <el-card shadow="hover">
            <p><strong>{{ h.operatorName || '系统' }}</strong>：{{ h.comment }}</p>
            <p class="timeline-status">{{ h.fromStatus || '新建' }} → {{ h.toStatus }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <div style="margin-top: 16px">
      <el-button @click="$router.back()">返回列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { ElMessage } from 'element-plus'

const qualityStore = useQualityStore()
const { data: defect, loading, reload } = useDetailLoad<any>((id) => qualityStore.getDefect(id))

const processing = ref(false)
const showActionDialog = ref(false)
const actionType = ref('')
const actionQty = ref(1)
const actionDept = ref('')
const actionRemark = ref('')

function getCauseCategoryLabel(code: string): string {
  const map: Record<string, string> = {
    MATERIAL: '材料',
    PROCESS: '工艺',
    EQUIPMENT: '设备',
    HUMAN: '人为',
  }
  return map[code] || code
}

function getActionTitle(): string {
  const map: Record<string, string> = {
    REWORK: '申请返工',
    SCRAP: '申请报废',
    CONCESSION: '申请让步接收',
  }
  return map[actionType.value] || '处置申请'
}

async function handleAddAction() {
  if (!defect.value) return
  processing.value = true
  try {
    await qualityStore.addDefectAction(defect.value.id, {
      actionType: actionType.value,
      qty: actionQty.value,
      responsibleDept: actionDept.value,
      remark: actionRemark.value,
    })
    ElMessage.success('处置申请已提交')
    showActionDialog.value = false
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    processing.value = false
  }
}

async function handleCreateReworkWo() {
  if (!defect.value) return
  processing.value = true
  try {
    await qualityStore.createReworkWo(defect.value.id)
    ElMessage.success('返工工单已创建')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    processing.value = false
  }
}

async function handleScrapInventory() {
  if (!defect.value) return
  processing.value = true
  try {
    await qualityStore.scrapInventory(defect.value.id)
    ElMessage.success('库存已扣减')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    processing.value = false
  }
}

async function handleConcessionApprove(approved: boolean) {
  if (!defect.value) return
  processing.value = true
  try {
    await qualityStore.concessionApprove(defect.value.id, approved)
    ElMessage.success(approved ? '让步已批准' : '让步已驳回')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    processing.value = false
  }
}
</script>

<style scoped>
.disposition-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.page-header-title {
  font-size: 18px;
  font-weight: 600;
}
.timeline-status {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
