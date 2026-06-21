<template>
  <div v-loading="loading">
    <h2>FA 首件报告</h2>
    <el-card v-if="report">
      <h3>{{ report.faNo }} 检验报告</h3>
      <p>结论：<ErpStatusTag :status="report.status" /></p>
      <p>检验员：{{ report.inspector }}</p>
      <p>检验时间：{{ report.inspectedAt }}</p>
      <h4>检验项目明细</h4>
      <el-table :data="report.items || []" stripe border>
        <el-table-column prop="itemName" label="项目" />
        <el-table-column prop="standard" label="标准" />
        <el-table-column prop="actual" label="实测" />
        <el-table-column prop="result" label="结果" />
      </el-table>
    </el-card>
    <el-button style="margin-top: 16px" type="primary" @click="exportPdf">导出 PDF</el-button>
    <el-button @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const qualityStore = useQualityStore()
const { data: report, loading } = useDetailLoad<any>((id) => qualityStore.getFaReport(id))

function exportPdf() {
  ElMessage.success('PDF 导出已启动')
}
</script>
