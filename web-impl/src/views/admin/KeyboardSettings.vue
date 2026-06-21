<template>
  <div class="keyboard-settings">
    <h2>全局快捷键设置</h2>
    <p class="hint">修改后保存在浏览器 localStorage，立即生效。</p>
    <el-table :data="bindings" stripe border>
      <el-table-column prop="label" label="操作" />
      <el-table-column prop="keys" label="快捷键">
        <template #default="{ row }">
          <el-input v-model="row.keys" size="small" style="width: 160px" />
        </template>
      </el-table-column>
      <el-table-column prop="action" label="动作 ID" />
    </el-table>
    <div style="margin-top: 16px">
      <el-button type="primary" @click="save">保存</el-button>
      <el-button @click="reset">恢复默认</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { useKeyboardShortcuts, type ShortcutBinding } from '@/composables/useKeyboardShortcuts'

const { bindings, saveBindings, resetBindings, DEFAULT_BINDINGS } = useKeyboardShortcuts()

function save() {
  saveBindings([...bindings.value] as ShortcutBinding[])
  ElMessage.success('快捷键已保存')
}

function reset() {
  resetBindings()
  ElMessage.info('已恢复默认')
}
</script>

<style scoped>
.keyboard-settings { padding: 16px; }
.hint { color: var(--erp-text-muted); font-size: 14px; }
</style>
