import { defineConfig, loadEnv } from 'vite'
import vue from '@vitejs/plugin-vue'
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'
import path from 'path'

// 昆山佰泰胜专属 ERP V1.3.7 - Vite 配置
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')

  return {
    plugins: [
      vue(),
      AutoImport({
        resolvers: [ElementPlusResolver()],
        imports: ['vue', 'vue-router', 'pinia'],
        dts: 'src/auto-imports.d.ts',
      }),
      Components({
        resolvers: [ElementPlusResolver()],
        dts: 'src/components.d.ts',
      }),
    ],
    resolve: {
      alias: {
        '@': path.resolve(__dirname, 'src'),
      },
    },
    server: {
      host: '0.0.0.0',
      port: 5173,
      proxy: {
        // Gateway Nacos 路由 · /erp-{service}/...
        '/erp-platform': {
          target: env.VITE_API_TARGET || 'http://localhost:9080',
          changeOrigin: true,
        },
        '/erp-business': {
          target: env.VITE_API_TARGET || 'http://localhost:9080',
          changeOrigin: true,
        },
        '/erp-production': {
          target: env.VITE_API_TARGET || 'http://localhost:9080',
          changeOrigin: true,
        },
      },
    },
    build: {
      target: 'es2020',
      sourcemap: mode !== 'production',
      rollupOptions: {
        output: {
          manualChunks: {
            'vue-vendor': ['vue', 'vue-router', 'pinia'],
            'element-vendor': ['element-plus', '@element-plus/icons-vue'],
            'echarts-vendor': ['echarts', 'vue-echarts'],
            'pdfjs-vendor': ['pdfjs-dist'],
          },
        },
      },
    },
    test: {
      environment: 'jsdom',
      setupFiles: ['./src/test/setup.ts'],
      coverage: {
        provider: 'v8',
        reporter: ['text', 'html'],
        include: ['src/**/*.{vue,ts,tsx}'],
      },
    },
  }
})
