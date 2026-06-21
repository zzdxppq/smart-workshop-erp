<template>
  <div class="login-page">
    <!-- 佰泰胜工业风背景（已去除 AI/平台水印） -->
    <div class="login-bg" aria-hidden="true">
      <div class="login-bg-photo" />

      <!-- 透视网格微动 -->
      <div class="grid-pulse grid-pulse-top" />
      <div class="grid-pulse grid-pulse-bottom" />

      <!-- 左侧机床生产光效（叠在背景图线稿上） -->
      <div class="cnc-live-fx">
        <div class="cnc-light-sweep" />
        <svg class="cnc-energy-svg" viewBox="0 0 520 420" xmlns="http://www.w3.org/2000/svg">
          <defs>
            <linearGradient id="cncFlowBlue" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stop-color="#3d8bff" stop-opacity="0" />
              <stop offset="45%" stop-color="#8ec8ff" stop-opacity="1" />
              <stop offset="55%" stop-color="#ffffff" stop-opacity="0.95" />
              <stop offset="100%" stop-color="#f57c20" stop-opacity="0" />
              <animate attributeName="x1" values="-80%;120%;-80%" dur="2.8s" repeatCount="indefinite" />
              <animate attributeName="x2" values="20%;220%;20%" dur="2.8s" repeatCount="indefinite" />
            </linearGradient>
            <linearGradient id="cncFlowOrange" x1="100%" y1="0%" x2="0%" y2="100%">
              <stop offset="0%" stop-color="#ff9a3c" stop-opacity="0" />
              <stop offset="50%" stop-color="#ffb347" stop-opacity="1" />
              <stop offset="100%" stop-color="#3d8bff" stop-opacity="0" />
              <animate attributeName="y1" values="0%;100%;0%" dur="3.6s" repeatCount="indefinite" />
              <animate attributeName="y2" values="100%;200%;100%" dur="3.6s" repeatCount="indefinite" />
            </linearGradient>
            <filter id="cncGlow" x="-30%" y="-30%" width="160%" height="160%">
              <feGaussianBlur stdDeviation="2.5" result="blur" />
              <feMerge>
                <feMergeNode in="blur" />
                <feMergeNode in="SourceGraphic" />
              </feMerge>
            </filter>
          </defs>
          <g class="cnc-paths cnc-paths-a" filter="url(#cncGlow)">
            <path class="energy-line" d="M40 320 H480" />
            <path class="energy-line" d="M72 80 V320" />
            <path class="energy-line" d="M72 108 H432" />
            <path class="energy-line" d="M280 144 V264" />
            <path class="energy-line" d="M120 268 H320 V320 H120 Z" />
          </g>
          <g class="cnc-paths cnc-paths-b" filter="url(#cncGlow)">
            <path class="energy-line energy-line-alt" d="M56 332 H464" />
            <path class="energy-line energy-line-alt" d="M308 264 V320" />
            <circle class="energy-ring" cx="324" cy="200" r="28" />
            <circle class="energy-ring energy-ring-inner" cx="324" cy="200" r="12" />
          </g>
        </svg>
        <div class="cnc-spindle-glow" />
        <div class="cnc-scan-beam" />
        <div class="cnc-sparks">
          <span v-for="n in 14" :key="n" class="cnc-spark" :style="cncSparkStyle(n)" />
        </div>
        <div class="cnc-status">
          <span class="cnc-status-dot" />
          <span class="cnc-status-text">CNC RUNNING</span>
        </div>
      </div>

      <!-- 右下焊接火花增强 -->
      <div class="spark-layer">
        <span v-for="n in 16" :key="n" class="spark" :style="sparkStyle(n)" />
      </div>

      <div class="bg-vignette" />
    </div>

    <!-- 登录卡片（偏右居中，为左侧机床留出空间） -->
    <div class="login-card-frame">
      <div class="login-card">
        <div class="login-header">
          <div class="brand-icon" aria-hidden="true">
            <svg viewBox="0 0 64 64" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="32" cy="32" r="28" stroke="url(#gearGrad)" stroke-width="2.5" />
              <circle cx="32" cy="32" r="10" fill="url(#gearGrad)" />
              <rect x="29" y="6" width="6" height="10" rx="2" fill="url(#gearGrad)" />
              <rect x="29" y="48" width="6" height="10" rx="2" fill="url(#gearGrad)" />
              <rect x="6" y="29" width="10" height="6" rx="2" fill="url(#gearGrad)" />
              <rect x="48" y="29" width="10" height="6" rx="2" fill="url(#gearGrad)" />
              <rect x="14" y="14" width="8" height="6" rx="1.5" fill="url(#gearGrad)" transform="rotate(-45 18 17)" />
              <rect x="42" y="14" width="8" height="6" rx="1.5" fill="url(#gearGrad)" transform="rotate(45 46 17)" />
              <rect x="14" y="44" width="8" height="6" rx="1.5" fill="url(#gearGrad)" transform="rotate(45 18 47)" />
              <rect x="42" y="44" width="8" height="6" rx="1.5" fill="url(#gearGrad)" transform="rotate(-45 46 47)" />
              <rect x="22" y="22" width="20" height="20" rx="3" stroke="url(#chipGrad)" stroke-width="2" fill="#f0f5ff" />
              <path d="M26 28h12M26 32h12M26 36h8" stroke="url(#chipGrad)" stroke-width="1.5" stroke-linecap="round" />
              <defs>
                <linearGradient id="gearGrad" x1="0" y1="0" x2="64" y2="64">
                  <stop offset="0%" stop-color="#3d7ae8" />
                  <stop offset="100%" stop-color="#1e4d8c" />
                </linearGradient>
                <linearGradient id="chipGrad" x1="22" y1="22" x2="42" y2="42">
                  <stop offset="0%" stop-color="#4a8ef5" />
                  <stop offset="100%" stop-color="#f57c20" />
                </linearGradient>
              </defs>
            </svg>
          </div>
          <h1 class="brand-title">昆山佰泰胜</h1>
          <p class="brand-slogan">数字生产 · 数智管理</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          size="large"
          @submit.prevent="onSubmit"
        >
          <el-form-item prop="username">
            <el-input
              v-model="form.username"
              name="username"
              placeholder="账号"
              :prefix-icon="User"
              clearable
              autofocus
              class="pill-input"
            />
          </el-form-item>

          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              name="password"
              type="password"
              placeholder="密码"
              :prefix-icon="Lock"
              show-password
              class="pill-input"
              @keyup.enter="onSubmit"
            />
          </el-form-item>

          <el-form-item>
            <el-button
              class="login-button"
              :loading="loading"
              native-type="submit"
              @click="onSubmit"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>

          <p class="login-tips">测试管理账号：admin / admin123</p>
        </el-form>

        <p class="login-footer">河南晓评信息科技有限公司 技术支持</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useBaseStore } from '@/stores/_base'
import { unwrapResult } from '@/utils/apiPage'
import { resolveDashboardPath } from '@/utils/roleWorkflowGuide'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const api = useBaseStore().api

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules = reactive<FormRules>({
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
})

/** 机床主轴区域火花 */
function cncSparkStyle(n: number) {
  const x = 48 + ((n * 13) % 28)
  const y = 52 + ((n * 19) % 22)
  const delay = (n * 0.31) % 2.2
  const size = 2 + (n % 3)
  return {
    left: `${x}%`,
    top: `${y}%`,
    animationDelay: `${delay}s`,
    width: `${size}px`,
    height: `${size}px`,
  } as Record<string, string>
}

/** 右下焊接火花粒子 */
function sparkStyle(n: number) {
  const x = 62 + ((n * 19) % 34)
  const y = 58 + ((n * 23) % 36)
  const delay = (n * 0.37) % 2.4
  const size = 2 + (n % 4)
  return {
    left: `${x}%`,
    top: `${y}%`,
    animationDelay: `${delay}s`,
    width: `${size}px`,
    height: `${size}px`,
  } as Record<string, string>
}

onMounted(() => {
  document.documentElement.classList.add('login-route-active')
  if (route.query.expired === '1') {
    ElMessage.warning('登录已过期，请重新登录')
  }
})

onUnmounted(() => {
  document.documentElement.classList.remove('login-route-active')
})

async function onSubmit() {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  loading.value = true
  try {
    const body = unwrapResult<{
      accessToken?: string
      refreshToken?: string
      roles?: string[]
      menuPaths?: string[]
      permissions?: string[]
      user?: { username?: string; realName?: string; roleCodes?: string[] }
    }>(await api.post('/auth/login', form))
    const token = body.accessToken
    if (!token) {
      ElMessage.error('登录失败：服务端未返回 accessToken')
      return
    }
    auth.setToken(token)
    if (body.refreshToken) {
      auth.saveRefreshToken(body.refreshToken)
    }
    const roles = body.roles?.length
      ? body.roles
      : body.user?.roleCodes?.length
        ? body.user.roleCodes
        : []
    auth.setUser({
      username: body.user?.username || form.username,
      realName: body.user?.realName,
      roles,
    })
    auth.setMenuAccess(body.menuPaths, body.permissions)
    ElMessage.success('登录成功')
    router.push(resolveDashboardPath(roles))
  } catch (e: unknown) {
    const err = e as { response?: { data?: { msg?: string; message?: string } }; message?: string }
    const msg =
      err?.response?.data?.msg ||
      err?.response?.data?.message ||
      err?.message ||
      '登录失败：未知错误'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  --steel-blue: #3b66f5;
  --industrial-orange: #e87c34;
  --text-muted: #9aa3b0;
  --card-inner: #ffffff;

  position: relative;
  box-sizing: border-box;
  height: 100vh;
  height: 100dvh;
  max-height: 100dvh;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #121a26;
  /* 左侧留空给机床视觉，右侧适度留白，避免登录框贴边 */
  padding: 24px clamp(24px, 6vw, 96px) 24px clamp(24px, min(32vw, 480px), 32vw);
}

.login-bg {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
  contain: paint;
}

.login-bg-photo {
  position: absolute;
  inset: 0;
  background:
    url('/images/login-bg.png') center center / cover no-repeat,
    linear-gradient(125deg, #141c28 0%, #1a2433 100%);
}

.bg-vignette {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 70% 65% at 72% 48%, rgba(15, 22, 34, 0.08) 0%, rgba(15, 22, 34, 0.45) 72%),
    linear-gradient(90deg, rgba(15, 22, 34, 0.15) 0%, transparent 38%, rgba(15, 22, 34, 0.25) 100%);
}

/* 透视网格微动 */
.grid-pulse {
  position: absolute;
  left: 0;
  right: 0;
  height: 28%;
  opacity: 0.35;
  background-image:
    linear-gradient(rgba(80, 200, 255, 0.18) 1px, transparent 1px),
    linear-gradient(90deg, rgba(80, 200, 255, 0.18) 1px, transparent 1px);
  background-size: 48px 48px;
  transform: perspective(420px) rotateX(58deg);
  transform-origin: center top;
  animation: grid-drift 8s ease-in-out infinite;
}

.grid-pulse-top {
  top: 0;
  mask-image: linear-gradient(180deg, rgba(0, 0, 0, 0.85) 0%, transparent 100%);
}

.grid-pulse-bottom {
  bottom: 0;
  transform-origin: center bottom;
  transform: perspective(420px) rotateX(-58deg);
  mask-image: linear-gradient(0deg, rgba(0, 0, 0, 0.85) 0%, transparent 100%);
  animation-delay: -4s;
}

@keyframes grid-drift {
  0%, 100% { background-position: 0 0; opacity: 0.28; }
  50% { background-position: 24px 24px; opacity: 0.42; }
}

/* 左侧机床生产光效 */
.cnc-live-fx {
  position: absolute;
  left: -2%;
  bottom: 6%;
  width: min(58vw, 660px);
  aspect-ratio: 520 / 420;
  pointer-events: none;
  mix-blend-mode: screen;
  opacity: 0.92;
}

.cnc-energy-svg {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.cnc-paths .energy-line {
  fill: none;
  stroke: url(#cncFlowBlue);
  stroke-width: 1.6;
  stroke-linecap: round;
  stroke-dasharray: 10 28;
  animation: cnc-energy-flow 2.4s linear infinite;
}

.cnc-paths-b .energy-line {
  stroke: url(#cncFlowOrange);
  animation-duration: 3.1s;
  animation-direction: reverse;
}

.cnc-paths .energy-line-alt {
  stroke-dasharray: 6 22;
  opacity: 0.75;
}

.cnc-paths .energy-ring {
  fill: none;
  stroke: url(#cncFlowBlue);
  stroke-width: 1.2;
  stroke-dasharray: 6 14;
  animation: cnc-energy-flow 3.2s linear infinite;
}

.cnc-paths .energy-ring-inner {
  stroke: url(#cncFlowOrange);
  stroke-width: 1;
  stroke-dasharray: 4 10;
  animation-duration: 2.2s;
  animation-direction: reverse;
}

.cnc-paths-b {
  animation: cnc-path-pulse 2.8s ease-in-out infinite;
}

@keyframes cnc-energy-flow {
  to { stroke-dashoffset: -152; }
}

@keyframes cnc-path-pulse {
  0%, 100% { opacity: 0.55; }
  50% { opacity: 1; }
}

.cnc-light-sweep {
  position: absolute;
  inset: -10% -20%;
  background: linear-gradient(
    115deg,
    transparent 38%,
    rgba(120, 185, 255, 0.12) 46%,
    rgba(255, 180, 90, 0.22) 50%,
    rgba(120, 185, 255, 0.12) 54%,
    transparent 62%
  );
  animation: cnc-sweep 5.5s ease-in-out infinite;
}

@keyframes cnc-sweep {
  0%, 100% { transform: translateX(-35%) skewX(-8deg); opacity: 0.35; }
  45% { transform: translateX(35%) skewX(-8deg); opacity: 0.85; }
}

.cnc-spindle-glow {
  position: absolute;
  left: 58%;
  top: 42%;
  width: 18%;
  height: 18%;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(100, 180, 255, 0.55) 0%, rgba(255, 140, 50, 0.25) 45%, transparent 70%);
  animation: cnc-spindle-pulse 1.6s ease-in-out infinite;
  filter: blur(2px);
}

@keyframes cnc-spindle-pulse {
  0%, 100% { transform: scale(0.85); opacity: 0.5; }
  50% { transform: scale(1.15); opacity: 1; }
}

.cnc-scan-beam {
  position: absolute;
  left: 22%;
  top: 62%;
  width: 38%;
  height: 2px;
  background: linear-gradient(90deg, transparent, #6eb5ff, #fff, #ffb347, transparent);
  box-shadow: 0 0 12px 2px rgba(110, 181, 255, 0.65);
  animation: cnc-scan 2.8s ease-in-out infinite;
  opacity: 0.85;
}

@keyframes cnc-scan {
  0%, 100% { transform: translateY(0); opacity: 0.35; }
  50% { transform: translateY(-52px); opacity: 1; }
}

.cnc-sparks {
  position: absolute;
  left: 38%;
  top: 58%;
  width: 28%;
  height: 22%;
}

.cnc-spark {
  position: absolute;
  border-radius: 50%;
  background: #ffb347;
  box-shadow: 0 0 6px 2px rgba(255, 140, 40, 0.9), 0 0 14px 4px rgba(255, 100, 20, 0.45);
  animation: cnc-spark-flicker 1.5s ease-in-out infinite;
}

.cnc-spark:nth-child(3n) { background: #ff6b1a; animation-duration: 1.1s; }
.cnc-spark:nth-child(3n + 1) { width: 2px !important; height: 2px !important; animation-duration: 1.9s; }

@keyframes cnc-spark-flicker {
  0%, 100% { opacity: 0.1; transform: scale(0.4); }
  40% { opacity: 1; transform: scale(1); }
  70% { opacity: 0.35; transform: scale(0.65); }
}

.cnc-status {
  position: absolute;
  left: 8%;
  bottom: 2%;
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 11px;
  letter-spacing: 0.22em;
  color: rgba(140, 200, 255, 0.75);
  text-shadow: 0 0 8px rgba(80, 160, 255, 0.5);
}

.cnc-status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: #4ade80;
  box-shadow: 0 0 8px #4ade80;
  animation: cnc-status-blink 1.2s ease-in-out infinite;
}

@keyframes cnc-status-blink {
  0%, 100% { opacity: 0.45; }
  50% { opacity: 1; }
}

/* 右下焊接火花 */
.spark-layer {
  position: absolute;
  inset: 0;
}

.spark {
  position: absolute;
  border-radius: 50%;
  background: #ffb347;
  box-shadow: 0 0 6px 2px rgba(255, 140, 40, 0.85), 0 0 14px 4px rgba(255, 100, 20, 0.45);
  animation: spark-float 2.2s ease-in-out infinite;
}

.spark:nth-child(3n) { background: #ff6b1a; animation-duration: 1.4s; }
.spark:nth-child(3n + 1) { animation-duration: 2.8s; }

@keyframes spark-float {
  0%, 100% { opacity: 0.15; transform: translateY(0) scale(0.5); }
  35% { opacity: 1; transform: translateY(-8px) scale(1); }
  70% { opacity: 0.4; transform: translateY(-16px) scale(0.7); }
}

@media (prefers-reduced-motion: reduce) {
  .cnc-live-fx,
  .cnc-live-fx *,
  .spark-layer,
  .grid-pulse {
    animation: none !important;
  }
  .cnc-live-fx { opacity: 0.45; }
}

/* 金属拉丝边框卡片 */
.login-card-frame {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 400px;
  margin: 0;
  flex-shrink: 0;
  padding: 5px;
  border-radius: 14px;
  background: linear-gradient(
    160deg,
    #f0f3f8 0%,
    #ffffff 22%,
    #c5cdd8 48%,
    #ffffff 72%,
    #b8c0cc 100%
  );
  box-shadow:
    0 28px 70px rgba(0, 0, 0, 0.42),
    0 0 0 1px rgba(255, 255, 255, 0.55) inset,
    0 2px 0 rgba(255, 255, 255, 0.65) inset;
}

.login-card {
  background: var(--card-inner);
  border-radius: 10px;
  padding: 36px 32px 24px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.98);
}

.login-header {
  text-align: center;
  margin-bottom: 26px;
}

.brand-icon {
  width: 60px;
  height: 60px;
  margin: 0 auto 14px;
}

.brand-icon svg {
  width: 100%;
  height: 100%;
  filter: drop-shadow(0 4px 10px rgba(59, 102, 245, 0.25));
}

.brand-title {
  margin: 0 0 6px;
  font-size: 24px;
  font-weight: 800;
  color: #1a2332;
  letter-spacing: 0.04em;
}

.brand-slogan {
  margin: 0;
  font-size: 13px;
  color: var(--text-muted);
  letter-spacing: 0.06em;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 16px;
}

.login-form :deep(.el-form-item__error) {
  padding-top: 4px;
}

.pill-input :deep(.el-input__wrapper) {
  border-radius: 50px;
  padding: 5px 18px;
  min-height: 44px;
  box-shadow: 0 0 0 1px #e2e8f0 inset;
  background: #fafbfc;
  transition: box-shadow 0.2s, background 0.2s;
}

.pill-input :deep(.el-input__wrapper:hover),
.pill-input :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px var(--steel-blue) inset;
  background: #fff;
}

.pill-input :deep(.el-input__prefix .el-icon) {
  color: #a8b0bc;
}

.login-button {
  width: 100%;
  height: 46px;
  border: none;
  border-radius: 50px;
  font-size: 16px;
  font-weight: 600;
  letter-spacing: 0.12em;
  color: #fff;
  background: linear-gradient(90deg, var(--steel-blue) 0%, var(--industrial-orange) 100%);
  box-shadow: 0 8px 22px rgba(59, 102, 245, 0.42);
  transition: transform 0.15s, box-shadow 0.15s, filter 0.15s;
}

.login-button:hover,
.login-button:focus {
  filter: brightness(1.05);
  box-shadow: 0 10px 28px rgba(232, 124, 52, 0.45);
  transform: translateY(-1px);
}

.login-button:active {
  transform: translateY(0);
}

.login-tips {
  margin: 6px 0 0;
  text-align: center;
  font-size: 12px;
  color: var(--text-muted);
}

.login-footer {
  margin: 18px 0 0;
  padding-top: 14px;
  border-top: 1px solid #eef1f5;
  text-align: center;
  font-size: 11px;
  color: #b0b8c4;
}


@media (max-width: 480px) {
  .login-page {
    padding: 48px 16px 16px;
    align-items: flex-start;
    justify-content: center;
  }

  .login-bg-photo {
    background-position: 35% center;
  }

  .cnc-live-fx {
    left: -18%;
    bottom: 2%;
    width: 130vw;
    opacity: 0.65;
  }

  .cnc-status {
    display: none;
  }

  .login-card {
    padding: 28px 22px 20px;
  }

  .brand-title {
    font-size: 22px;
  }
}
</style>

<style>
html.login-route-active,
html.login-route-active body {
  overflow: hidden;
  overscroll-behavior: none;
}
</style>
