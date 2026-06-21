<template>
  <ErpPageShell title="标签双模式打印" description="V1.3.9 Story 12.4 · 模式一 ZPL/TSPL 直连 · 模式二 A4 PDF 浏览器打印">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="打印参数" shadow="never">
          <el-form label-width="100px">
            <el-form-item label="标签类型" required>
              <el-select v-model="form.type" style="width: 100%" @change="onTypeChange">
                <el-option label="GD 工单码" value="GD" />
                <el-option label="LZ 流转码" value="LZ" />
                <el-option label="SB 设备码" value="SB" />
                <el-option label="WW 委外单码" value="WW" />
                <el-option label="WL 料号" value="WL" />
              </el-select>
            </el-form-item>
            <el-form-item label="二维码内容" required>
              <el-input v-model="form.qrContent" placeholder="如 GD-260614-001" />
            </el-form-item>
            <el-form-item label="明文行">
              <el-input
                v-model="form.linesText"
                type="textarea"
                :rows="5"
                placeholder="每行一条，最多 6 行"
              />
            </el-form-item>
            <el-form-item label="份数">
              <el-input-number v-model="form.copies" :min="1" :max="100" />
            </el-form-item>
            <el-form-item label="备注">
              <el-input v-model="form.remark" maxlength="200" />
            </el-form-item>
            <el-form-item label="打印模式">
              <el-radio-group v-model="printMode">
                <el-radio value="AUTO">自动（有标签机则 ZPL）</el-radio>
                <el-radio value="ZPL_DIRECT">模式一 · ZPL 直连</el-radio>
                <el-radio value="PDF_BROWSER">模式二 · A4 PDF</el-radio>
              </el-radio-group>
            </el-form-item>
            <el-form-item>
              <PrintButton
                :code-type="form.type"
                :code-value="form.qrContent"
                :lines="lines"
                :copies="form.copies"
                :remark="form.remark"
                :force-mode="printMode === 'AUTO' ? 'AUTO' : printMode"
                label="执行打印"
                type="primary"
                :disabled="!form.qrContent"
              />
              <el-button style="margin-left: 8px" @click="$router.push('/admin/print-logs')">
                打印历史
              </el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="标签预览" shadow="never">
          <LabelPreview
            v-if="form.qrContent"
            :key="previewKey"
            :type="form.type"
            :qr-content="form.qrContent"
            :lines="lines"
          />
          <el-empty v-else description="填写二维码内容后预览" />
        </el-card>
        <el-alert
          type="warning"
          :closable="false"
          title="模式一需先在「打印机管理」配置 LABEL 类型设备；无标签机时请选 A4 PDF 模式"
          style="margin-top: 12px"
        />
      </el-col>
    </el-row>
  </ErpPageShell>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import ErpPageShell from '@/components/layout/ErpPageShell.vue'
import LabelPreview from '@/components/label/LabelPreview.vue'
import PrintButton from '@/components/print/PrintButton.vue'
import { useLabelsStore } from '@/stores/labels'

const labelsStore = useLabelsStore()
const previewKey = ref(0)

const form = ref({
  type: 'GD' as 'GD' | 'LZ' | 'SB' | 'WW' | 'WL',
  qrContent: 'GD-260614-001',
  linesText: 'GD-260614-001\n工单：WO20260614001\n工序：P03',
  copies: 1,
  remark: '',
})

const printMode = ref<'AUTO' | 'ZPL_DIRECT' | 'PDF_BROWSER'>('AUTO')

const lines = computed(() =>
  form.value.linesText.split('\n').map((s) => s.trim()).filter(Boolean).slice(0, 6),
)

function onTypeChange(type: string) {
  const t = labelsStore.templates.find((x) => x.type === type)
  if (t?.qrExample) form.value.qrContent = t.qrExample
  previewKey.value++
}

labelsStore.loadTemplates()
</script>
