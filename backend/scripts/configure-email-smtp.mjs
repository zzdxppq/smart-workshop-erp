#!/usr/bin/env node
/**
 * 写入 platform email_config（163 SMTP）
 *
 * 用法（勿将授权码提交到 Git）:
 *   API_BASE=https://bts.51xiaoping.com \
 *   SMTP_AUTH_CODE=your-163-auth-code \
 *   SMTP_FROM=your-account@163.com \
 *   node backend/scripts/configure-email-smtp.mjs
 */
const BASE = (process.env.API_BASE || 'http://localhost:9080').replace(/\/$/, '')
const USER = process.env.SMOKE_USER || 'admin'
const PASSWORD = process.env.SMOKE_PASSWORD || '123456'
const AUTH_CODE = process.env.SMTP_AUTH_CODE || process.env.EMAIL_163_AUTH_CODE || ''
const FROM = process.env.SMTP_FROM || process.env.EMAIL_163_FROM || ''

async function login() {
  const res = await fetch(`${BASE}/erp-platform/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ username: USER, password: PASSWORD }),
  })
  const json = await res.json().catch(() => ({}))
  const token = json?.data?.accessToken
  if (!token) throw new Error(`登录失败: ${JSON.stringify(json)}`)
  return token
}

async function main() {
  if (!AUTH_CODE) {
    console.error('请设置环境变量 SMTP_AUTH_CODE 或 EMAIL_163_AUTH_CODE（163 客户端授权码）')
    process.exit(1)
  }
  if (FROM && !FROM.endsWith('@163.com')) {
    console.error('发件地址须为真实 @163.com 邮箱（与授权码对应同一账号）')
    process.exit(1)
  }

  const token = await login()
  const payload = {
    smtpHost: process.env.SMTP_HOST || 'smtp.163.com',
    smtpPort: Number(process.env.SMTP_PORT || 465),
    useSsl: true,
    authCode: AUTH_CODE,
    dailyQuota: 5000,
    quotaWarnThreshold: 0.8,
    logRetentionDays: 90,
    attachmentMaxSizeMb: 10,
    retryPolicy: ['1h', '6h', '24h'],
  }
  if (FROM) payload.fromAddress = FROM

  const res = await fetch(`${BASE}/erp-platform/email/config`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify(payload),
  })
  const json = await res.json().catch(() => ({}))
  if (json.code !== 0) {
    console.error('保存失败:', json)
    process.exit(1)
  }
  console.log(`✅ SMTP 已配置 host=${payload.smtpHost}${FROM ? ` from=${FROM}` : '（发件地址未改，请在管理端填写 @163.com 邮箱）'}`)
}

main()
