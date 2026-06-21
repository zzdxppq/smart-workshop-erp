<template>
  <div v-loading="loading">
    <h2>CMM 检测详情</h2>
    <el-card v-if="cmm">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="检测单号">{{ cmm.cmmNo }}</el-descriptions-item>
        <el-descriptions-item label="设备">{{ cmm.deviceId }}</el-descriptions-item>
        <el-descriptions-item label="料号">{{ cmm.materialCode }}</el-descriptions-item>
        <el-descriptions-item label="检验员">{{ cmm.inspector }}</el-descriptions-item>
        <el-descriptions-item label="状态"><ErpStatusTag :status="cmm.status" /></el-descriptions-item>
        <el-descriptions-item label="时间">{{ cmm.measuredAt }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
    <h3 style="margin-top: 16px">测量数据</h3>
    <el-table :data="cmm?.measurements || []" stripe border>
      <el-table-column prop="dimName" label="维度" />
      <el-table-column prop="nominal" label="标称" />
      <el-table-column prop="value" label="实测" />
      <el-table-column prop="deviation" label="偏差" />
      <el-table-column prop="result" label="结果" />
    </el-table>
    <el-button style="margin-top: 16px" @click="$router.back()">返回</el-button>
  </div>
</template>

<script setup lang="ts">
import { useQualityStore } from '@/stores/quality'
import { useDetailLoad } from '@/composables/useDetailLoad'
import ErpStatusTag from '@/components/erp/ErpStatusTag.vue'

const qualityStore = useQualityStore()
const { data: cmm, loading } = useDetailLoad<any>((id) => qualityStore.getCmm(id))
</script>
