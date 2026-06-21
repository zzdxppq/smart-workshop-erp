<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面未找到</p>
    <el-button @click="$router.push('/')">返回首页</el-button>
  </div>
</template>

<script setup lang="ts">
</script>

<style scoped>
.not-found { text-align: center; padding: 80px 0; }
.not-found h1 { font-size: 72px; color: #909399; }
</style>