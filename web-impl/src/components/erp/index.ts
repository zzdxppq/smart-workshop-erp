import type { App } from 'vue'
import FigureNumberSearch from './FigureNumberSearch.vue'
import DrawingPicker from './DrawingPicker.vue'
import DrawingNoCell from './DrawingNoCell.vue'
import BomTree from './BomTree.vue'
import ScanTrigger from './ScanTrigger.vue'
import MachineLoadBar from './MachineLoadBar.vue'
import ApprovalChainRenderer from './ApprovalChainRenderer.vue'
import MoneyAmount from './MoneyAmount.vue'
import ErpStatusTag from './ErpStatusTag.vue'
import ErpPassRate from './ErpPassRate.vue'
import ErpStockLevel from './ErpStockLevel.vue'

export {
  FigureNumberSearch,
  DrawingPicker,
  DrawingNoCell,
  BomTree,
  ScanTrigger,
  MachineLoadBar,
  ApprovalChainRenderer,
  MoneyAmount,
  ErpStatusTag,
  ErpPassRate,
  ErpStockLevel,
}

export function registerErpComponents(app: App) {
  app.component('FigureNumberSearch', FigureNumberSearch)
  app.component('DrawingPicker', DrawingPicker)
  app.component('DrawingNoCell', DrawingNoCell)
  app.component('BomTree', BomTree)
  app.component('ScanTrigger', ScanTrigger)
  app.component('MachineLoadBar', MachineLoadBar)
  app.component('ApprovalChainRenderer', ApprovalChainRenderer)
  app.component('MoneyAmount', MoneyAmount)
  app.component('ErpStatusTag', ErpStatusTag)
  app.component('ErpPassRate', ErpPassRate)
  app.component('ErpStockLevel', ErpStockLevel)
}
