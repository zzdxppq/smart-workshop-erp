/**
 * Vitest 全局 setup
 * - Element Plus 组件按需 auto-import
 * - 抑制 console.warn
 */
import { vi } from 'vitest'

// 避免 jsdom 缺失方法的报警
if (typeof window !== 'undefined') {
  if (!window.matchMedia) {
    window.matchMedia = vi.fn().mockImplementation((query: string) => ({
      matches: false,
      media: query,
      onchange: null,
      addListener: vi.fn(),
      removeListener: vi.fn(),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      dispatchEvent: vi.fn(),
    }))
  }
}
