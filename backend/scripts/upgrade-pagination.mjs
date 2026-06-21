#!/usr/bin/env node
/**
 * 批量将 el-pagination 升级为 ERP 统一分页样式（销售/采购模块）
 */
import fs from 'node:fs'
import path from 'node:path'

const ROOT = path.resolve(import.meta.dirname, '../../../web-impl/src/views')
const DIRS = ['sales', 'sourcing']

const OLD_LAYOUT = /layout="total, prev, pager, next"/g
const NEW_ATTRS = `:page-sizes="ERP_PAGE_SIZES"\n      :layout="ERP_PAGINATION_LAYOUT"\n      background`

const IMPORT_LINE = "import { ERP_PAGE_SIZES, ERP_PAGINATION_LAYOUT } from '@/constants/pagination'"

let changed = 0

for (const dir of DIRS) {
  const full = path.join(ROOT, dir)
  if (!fs.existsSync(full)) continue
  for (const file of fs.readdirSync(full).filter((f) => f.endsWith('.vue'))) {
    const fp = path.join(full, file)
    let src = fs.readFileSync(fp, 'utf8')
    if (!OLD_LAYOUT.test(src)) continue
    src = src.replace(OLD_LAYOUT, NEW_ATTRS)
    if (!src.includes('ERP_PAGE_SIZES')) {
      const scriptMatch = src.match(/<script setup lang="ts">\n/)
      if (scriptMatch) {
        src = src.replace(
          /<script setup lang="ts">\n/,
          `<script setup lang="ts">\n${IMPORT_LINE}\n`,
        )
      }
    }
    if (!src.includes('class="erp-pagination"') && src.includes('<el-pagination')) {
      src = src.replace(/<el-pagination(\s)/g, '<el-pagination class="erp-pagination"$1')
    }
    fs.writeFileSync(fp, src)
    changed++
    console.log('updated', path.relative(ROOT, fp))
  }
}

console.log(`Done. ${changed} files updated.`)
