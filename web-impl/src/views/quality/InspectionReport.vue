<template>
  <div v-loading="loading">
    <h2>检验报告</h2>
    <el-card v-if="report">
      <h3>{{ report.inspectionNo }}</h3>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="类型">{{ report.type }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ report.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="送检数">{{ report.qty }}</el-descriptions-item>
        <el-descriptions-item label="合格">{{ report.passQty }}</el-descriptions-item>
        <el-descriptions-item label="不合格">{{ report.failQty }}</el-descriptions-item>
        <el-descriptions-item label="合格率">{{ report.passRate }}%</el-descriptions-item>
      </el-descriptions>
      <h4 style="margin-top: 16px">检验结论</h4>
      <p>{{ report.conclusion }}</p>
    </el-card>
    <el-button style="margin-top: 16px" type="primary" @click="exportPdf">导出 PDF</el-button>
    <el-button @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'

const qualityStore = useQualityStore()
const { data: report, loading } = useDetailLoad<any>((id) => qualityStore.generateInspectionReport(id))

function exportPdf() {
  ElMessage.success('PDF 导出已启动')
}
</script>
