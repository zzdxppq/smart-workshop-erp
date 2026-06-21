import type { Preview } from '@storybook/vue3'
import { setup } from '@storybook/vue3'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '../src/styles/tokens/tokens.css'
import { registerErpComponents } from '../src/components/erp'

setup((app) => {
  app.use(ElementPlus)
  registerErpComponents(app)
})

const preview: Preview = {
  parameters: {
    controls: { matchers: { color: /(background|color)$/i, date: /Date$/i } },
  },
}

export default preview
