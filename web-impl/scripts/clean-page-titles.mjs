/**
 * 批量清理 Vue 页面中面向用户的开发元数据标题（Story / Sprint / V1.3.x 等）
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const root = path.join(path.dirname(fileURLToPath(import.meta.url)), '../src')

const alertReplacements = [
  ['V1.3.7 AD-1：生管视图无厂商下拉框', '生管角色仅能设置自制/委外，不可选择厂商'],
  ['V1.3.7 AD-1：采购视图无工序归属切换', '采购角色仅可选择厂商，不可变更工序归属'],
  ['V1.3.7 AD-2：4 步流程 · 不含\'线下\'按钮', '对账为系统内四步流程，不含线下操作'],
  ['V1.3.7 AD-2：禁止"线下"按钮', '对账流程不含线下操作按钮'],
  ["V1.3.7 AD-2：4 步流程 · 不含\"线下\"按钮", '对账为系统内四步流程，不含线下操作'],
  ['V1.3.7：拍照上传签字扫描件 · 不含"线下"', '支持上传厂商签字扫描件（PDF/JPG/PNG）'],
  ['V1.3.7 AD-3：厂商通知偏好只允许 163 邮箱', '厂商通知仅支持 163 邮箱'],
  ['V1.3.7 AD-3：仅允许 163 邮箱', '通知邮箱须为 163 邮箱'],
  ['V1.3.7 红线 5：作业人员自助只读金额', '作业人员仅可查看本人相关金额'],
]

function cleanHeading(inner) {
  let t = inner.trim()
  // 去掉中英文括号内的 Story / 版本 / Sprint 等
  t = t.replace(/\s*[（(][^）)]*(?:Story|E\d+-S\d*|V1\.3|Sprint|Spec\s|E11-|AC-|sys_workflow|增强|P\d+\s)[^）)]*[）)]/gi, '')
  t = t.replace(/\s*[（(]Story[^）)]*[）)]/gi, '')
  // 去掉 · V1.3.x / · 5 Tab 等后缀
  t = t.replace(/\s*·\s*V1\.3[\d.]*.*$/i, '')
  t = t.replace(/\s*·\s*5 Tab.*$/i, '')
  t = t.replace(/\s*·\s*4 步.*$/i, '')
  t = t.replace(/\s*·\s*Web 辅助.*$/i, '')
  t = t.replace(/\s*·\s*甘特图.*$/i, '')
  t = t.replace(/\s*·\s*3 级结构.*$/i, '')
  t = t.replace(/\s*·\s*FEFO.*$/i, '')
  t = t.replace(/\s*·\s*看板.*$/i, '')
  t = t.replace(/\s*·\s*IQC.*$/i, '')
  t = t.replace(/\s{2,}/g, ' ')
  return t.trim()
}

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf8')
  const orig = content

  for (const [from, to] of alertReplacements) {
    content = content.split(from).join(to)
  }

  content = content.replace(/<h2([^>]*)>([^<]*)<\/h2>/g, (m, attrs, inner) => {
    const cleaned = cleanHeading(inner)
    return cleaned ? `<h2${attrs}>${cleaned}</h2>` : m
  })

  content = content.replace(
    /<span class="title">([^<]*)<\/span>/g,
    (m, inner) => {
      const cleaned = cleanHeading(inner)
      return cleaned ? `<span class="title">${cleaned}</span>` : m
    },
  )

  // WorkflowStats 顶部说明
  content = content.replace(
    /数据源：Sprint[^<]*<code>GET \/workflow\/events\/stats<\/code>[^<]*/,
    '数据来源：审批事件统计接口',
  )

  if (content !== orig) {
    fs.writeFileSync(filePath, content, 'utf8')
    return true
  }
  return false
}

function walk(dir) {
  let count = 0
  for (const name of fs.readdirSync(dir)) {
    const p = path.join(dir, name)
    const stat = fs.statSync(p)
    if (stat.isDirectory()) {
      count += walk(p)
    } else if (name.endsWith('.vue')) {
      if (processFile(p)) count++
    }
  }
  return count
}

const n = walk(root)
console.log(`Updated ${n} vue files`)
