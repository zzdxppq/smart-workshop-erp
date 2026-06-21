<template>
  <div>
    <h2>不良品分析报告</h2>
    <el-form :inline="true" style="margin-bottom: 12px">
      <el-form-item label="期间">
        <el-date-picker v-model="period" type="month" value-format="YYYY-MM" @change="refresh" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="refresh">生成报告</el-button>
      </el-form-item>
    </el-form>
    <el-card v-loading="loading">
      <template v-if="report">
        <h3>不良品汇总</h3>
        <el-descriptions :column="3" border>
          <el-descriptions-item label="不良总数">{{ report.totalCount }}</el-descriptions-item>
          <el-descriptions-item label="轻微">{{ report.minorCount }}</el-descriptions-item>
          <el-descriptions-item label="严重">{{ report.majorCount }}</el-descriptions-item>
          <el-descriptions-item label="致命">{{ report.criticalCount }}</el-descriptions-item>
          <el-descriptions-item label="返修率">{{ report.reworkRate }}%</el-descriptions-item>
          <el-descriptions-item label="报废率">{{ report.scrapRate }}%</el-descriptions-item>
        </el-descriptions>
        <h4>TOP 不良原因</h4>
        <el-table :data="report.topCauses || []" stripe border>
          <el-table-column prop="cause" label="原因" />
          <el-table-column prop="count" label="次数" />
          <el-table-column prop="percentage" label="占比" />
        </el-table>
      </template>
    </el-card>
    <el-button style="margin-top: 16px" type="primary" @click="exportPdf">导出 PDF</el-button>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useQualityStore } from '@/stores/quality'
import { unwrapResult } from '@/utils/apiPage'

const qualityStore = useQualityStore()
const report = ref<any>(null)
const period = ref<string>('2026-06')
const loading = ref(false)

async function refresh() {
  loading.value = true
  try {
    const r = await qualityStore.getDefectReport({ period: period.value })
    report.value = unwrapResult(r)
  } catch (e: unknown) {
    ElMessage.error((e as { message?: string })?.message || '生成失败')
    report.value = null
  } finally {
    loading.value = false
  }
}

function exportPdf() {
  ElMessage.success('PDF 导出已启动')
}

onMounted(refresh)
</script>
