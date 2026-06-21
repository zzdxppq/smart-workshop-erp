import { ref, watch, onMounted } from 'vue'

export type ThemeMode = 'light' | 'dark' | 'industrial' | 'system'

const STORAGE_KEY = 'erp-theme-mode'

const mode = ref<ThemeMode>((localStorage.getItem(STORAGE_KEY) as ThemeMode) || 'system')

function applyTheme(m: ThemeMode) {
  const root = document.documentElement
  if (m === 'system') {
    root.removeAttribute('data-theme')
  } else {
    root.setAttribute('data-theme', m)
  }
}

watch(mode, (m) => {
  localStorage.setItem(STORAGE_KEY, m)
  applyTheme(m)
}, { immediate: true })

export function useTheme() {
  onMounted(() => applyTheme(mode.value))

  function setTheme(m: ThemeMode) {
    mode.value = m
  }

  function toggleTheme() {
    mode.value = mode.value === 'light' ? 'industrial' : 'light'
  }

  return { mode, setTheme, toggleTheme }
}
