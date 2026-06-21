import type { Meta, StoryObj } from '@storybook/vue3'
import { ref } from 'vue'
import FigureNumberSearch from '@/components/erp/FigureNumberSearch.vue'
import BomTree from '@/components/erp/BomTree.vue'
import ScanTrigger from '@/components/erp/ScanTrigger.vue'
import MachineLoadBar from '@/components/erp/MachineLoadBar.vue'
import ApprovalChainRenderer from '@/components/erp/ApprovalChainRenderer.vue'
import MoneyAmount from '@/components/erp/MoneyAmount.vue'

const sampleBom = [
  { id: '1', label: '总成 A-001', qty: 1, children: [
    { id: '2', label: '零件 B-002', qty: 2, cost: 120 },
    { id: '3', label: '零件 C-003', qty: 1, cost: 80 },
  ]},
]

const sampleChain = [
  { id: 1, title: '业务员', assignee: '张三', status: 'APPROVED' as const, time: '06-15 09:00' },
  { id: 2, title: '销售主管', assignee: '李四', status: 'PENDING' as const },
  { id: 3, title: '总经理', assignee: '王总', status: 'PENDING' as const },
]

export default {
  title: 'ERP/Custom',
} satisfies Meta

export const FigureSearchBasic: StoryObj = {
  render: () => ({
    components: { FigureNumberSearch },
    setup() {
      const v = ref('')
      return { v }
    },
    template: '<FigureNumberSearch v-model="v" />',
  }),
}

export const FigureSearchDisabled: StoryObj = {
  render: () => ({
    components: { FigureNumberSearch },
    template: '<FigureNumberSearch model-value="DWG-001" disabled />',
  }),
}

export const BomTreeReadonly: StoryObj = {
  render: () => ({
    components: { BomTree },
    setup() { return { sampleBom } },
    template: '<BomTree :data="sampleBom" />',
  }),
}

export const BomTreeWithCost: StoryObj = {
  render: () => ({
    components: { BomTree },
    setup() { return { sampleBom } },
    template: '<BomTree :data="sampleBom" show-cost editable />',
  }),
}

export const ScanTriggerUsb: StoryObj = {
  render: () => ({
    components: { ScanTrigger },
    setup() {
      const code = ref('')
      return { code }
    },
    template: '<ScanTrigger v-model="code" />',
  }),
}

export const MachineLoadGreen: StoryObj = {
  render: () => ({
    components: { MachineLoadBar },
    template: '<MachineLoadBar :percent="55" machine-name="CNC-01" />',
  }),
}

export const MachineLoadYellow: StoryObj = {
  render: () => ({
    components: { MachineLoadBar },
    template: '<MachineLoadBar :percent="78" machine-name="CNC-02" />',
  }),
}

export const MachineLoadRed: StoryObj = {
  render: () => ({
    components: { MachineLoadBar },
    template: '<MachineLoadBar :percent="95" machine-name="CNC-03" />',
  }),
}

export const ApprovalPending: StoryObj = {
  render: () => ({
    components: { ApprovalChainRenderer },
    setup() { return { sampleChain } },
    template: '<ApprovalChainRenderer :nodes="sampleChain" />',
  }),
}

export const ApprovalApproved: StoryObj = {
  render: () => ({
    components: { ApprovalChainRenderer },
    setup() {
      const nodes = sampleChain.map((n) => ({ ...n, status: 'APPROVED' as const }))
      return { nodes }
    },
    template: '<ApprovalChainRenderer :nodes="nodes" />',
  }),
}

export const MoneyCny: StoryObj = {
  render: () => ({
    components: { MoneyAmount },
    setup() {
      const v = ref(1000)
      return { v }
    },
    template: '<MoneyAmount v-model="v" />',
  }),
}

export const MoneyWithTax: StoryObj = {
  render: () => ({
    components: { MoneyAmount },
    setup() {
      const v = ref(50000)
      const tax = ref(true)
      return { v, tax }
    },
    template: '<MoneyAmount v-model="v" v-model:tax-included="tax" currency="CNY" />',
  }),
}
