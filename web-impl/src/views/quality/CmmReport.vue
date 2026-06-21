<template>
  <div v-loading="loading">
    <h2>CMM 检测报告</h2>
    <el-card v-if="report">
      <h3>{{ report.cmmNo }} 检测报告</h3>
      <p>设备：{{ report.deviceId }}</p>
      <p>状态：<ErpStatusTag :status="report.status" /></p>
      <p>超差数量：<el-tag type="danger">{{ report.outOfTolCount }}</el-tag></p>
      <h4>测量明细</h4>
      <el-table :data="report.measurements || []" stripe border>
        <el-table-column prop="dimName" label="维度" />
        <el-table-column prop="value" label="实测" />
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
const { data: report, loading } = useDetailLoad<any>((id) => qualityStore.getCmmReport(id))

function exportPdf() {
  ElMessage.success('PDF 导出已启动')
}
</script>
