<template>
  <div v-loading="loading">
    <el-page-header @back="$router.back()" title="返回">
      <template #content>
        <span class="page-header-title">FA 首件详情</span>
      </template>
    </el-page-header>

    <el-card v-if="fa" style="margin-top: 16px">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="首件单号">{{ fa.faNo }}</el-descriptions-item>
        <el-descriptions-item label="工单号">{{ fa.workOrderNo }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ fa.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="工序">{{ fa.processName }}</el-descriptions-item>
        <el-descriptions-item label="检验员">{{ fa.inspector }}</el-descriptions-item>
        <el-descriptions-item label="工程师">{{ fa.engineer }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <ErpStatusTag :status="fa.status" />
        </el-descriptions-item>
        <el-descriptions-item label="检验时间">{{ fa.inspectedAt }}</el-descriptions-item>
        <el-descriptions-item label="品检签字" v-if="fa.inspectorSignedAt">
          {{ fa.inspectorSignedAt }}
        </el-descriptions-item>
        <el-descriptions-item label="工程师签字" v-if="fa.engineerSignedAt">
          {{ fa.engineerSignedAt }}
        </el-descriptions-item>
        <el-descriptions-item label="驳回原因" v-if="fa.rejectReason" :span="2">
          <el-tag type="danger">{{ fa.rejectReason }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="返工次数" v-if="fa.reworkCount">
          {{ fa.reworkCount }} 次
        </el-descriptions-item>
        <el-descriptions-item label="PDF报告" :span="2">
          <el-link v-if="fa.pdfUrl" :href="fa.pdfUrl" target="_blank">查看报告</el-link>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- V2.1 双签操作面板 -->
      <el-divider content-position="left">双签操作</el-divider>
      <div class="sign-actions">
        <!-- 待检验状态：品检员开始检验 -->
        <el-button
          v-if="fa.status === 'PENDING_INSPECT'"
          type="primary"
          @click="handleInspectorSign"
          :loading="signing"
        >
          品检员签字
        </el-button>

        <!-- 待双签状态：工程师终签 -->
        <template v-if="fa.status === 'PENDING_SIGN'">
          <el-button type="success" @click="handleEngineerSign(true)" :loading="signing">
            工程师签字（通过）
          </el-button>
          <el-button type="danger" @click="showRejectDialog = true" :loading="signing">
            驳回并要求返工
          </el-button>
        </template>

        <!-- 返工状态：重新提交 -->
        <el-button
          v-if="fa.status === 'REWORK'"
          type="warning"
          @click="handleResubmit"
          :loading="signing"
        >
          重新提交检验
        </el-button>
      </div>

      <!-- 驳回原因对话框 -->
      <el-dialog v-model="showRejectDialog" title="驳回原因" width="400px">
        <el-input
          v-model="rejectReason"
          type="textarea"
          :rows="3"
          placeholder="请输入驳回原因"
        />
        <template #footer>
          <el-button @click="showRejectDialog = false">取消</el-button>
          <el-button type="danger" @click="handleReject" :loading="signing">确认驳回</el-button>
        </template>
      </el-dialog>
    </el-card>

    <el-card style="margin-top: 16px" v-if="fa">
      <template #header>
        <span>检验项目（8维度）</span>
      </template>
      <el-table :data="fa.items || []" stripe border>
        <el-table-column prop="dimension" label="维度" width="100" />
        <el-table-column prop="itemName" label="项目名称" min-width="120" />
        <el-table-column prop="standard" label="标准" min-width="120" />
        <el-table-column prop="tolerance" label="公差" width="80" />
        <el-table-column prop="measuredValue" label="实测值" width="100" />
        <el-table-column prop="passed" label="结果" width="80">
          <template #default="{ row }">
            <el-tag :type="row.passed ? 'success' : 'danger'">
              {{ row.passed ? '合格' : '不合格' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <div style="margin-top: 16px">
      <el-button @click="$router.back()">返回列表</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'
import { ElMessage } from 'element-plus'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const qualityStore = useQualityStore()
const { data: fa, loading, reload } = useDetailLoad<any>((id) => qualityStore.getFa(id))

const signing = ref(false)
const showRejectDialog = ref(false)
const rejectReason = ref('')

async function handleInspectorSign() {
  if (!fa.value) return
  signing.value = true
  try {
    await qualityStore.inspectorSignFa(fa.value.id)
    ElMessage.success('品检员签字成功')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '签字失败')
  } finally {
    signing.value = false
  }
}

async function handleEngineerSign(passed: boolean) {
  if (!fa.value) return
  signing.value = true
  try {
    await qualityStore.engineerSignFa(fa.value.id)
    ElMessage.success('工程师签字成功')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '签字失败')
  } finally {
    signing.value = false
  }
}

async function handleReject() {
  if (!fa.value) return
  signing.value = true
  try {
    await qualityStore.reworkFa(fa.value.id, rejectReason.value)
    ElMessage.success('已驳回，等待返工')
    showRejectDialog.value = false
    rejectReason.value = ''
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '驳回失败')
  } finally {
    signing.value = false
  }
}

async function handleResubmit() {
  if (!fa.value) return
  signing.value = true
  try {
    await qualityStore.resubmitFa(fa.value.id)
    ElMessage.success('已重新提交检验')
    await reload()
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    signing.value = false
  }
}
</script>

<style scoped>
.sign-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}
.page-header-title {
  font-size: 18px;
  font-weight: 600;
}
</style>
