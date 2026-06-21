<template>
  <ErpPageShell title="人事管理" description="员工档案与薪酬（考勤打卡请在 Android APP）。">
    <el-tabs v-model="tab">
      <el-tab-pane label="员工档案" name="employee">
        <el-alert
          type="info"
          :closable="false"
          show-icon
          title="员工档案列表；WiFi/蓝牙考勤打卡为 APP 功能。"
          style="margin-bottom: 12px"
        />
        <el-button type="primary" @click="$router.push('/hr/employees')">员工列表</el-button>
        <el-button @click="$router.push('/admin/payroll')">薪酬核算</el-button>
      </el-tab-pane>
      <el-tab-pane label="绩效" name="performance">
        <el-empty description="绩效管理模块开发中" />
      </el-tab-pane>
      <el-tab-pane label="招聘" name="recruitment">
        <el-empty description="招聘模块开发中" />
      </el-tab-pane>
    </el-tabs>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'

const tab = ref('employee')
</script>
