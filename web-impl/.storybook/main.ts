import type { StorybookConfig } from '@storybook/vue3-vite'
import path from 'path'

const config: StorybookConfig = {
  stories: ['../src/stories/**/*.stories.@(js|jsx|ts|tsx|mdx)'],
  addons: ['@storybook/addon-essentials', '@storybook/addon-links'],
  framework: {
    name: '@storybook/vue3-vite',
    options: {},
  },
  viteFinal: async (cfg) => {
    cfg.resolve = cfg.resolve || {}
    cfg.resolve.alias = {
      ...cfg.resolve.alias,
      '@': path.resolve(__dirname, '../src'),
    }
    return cfg
  },
}

export default config
