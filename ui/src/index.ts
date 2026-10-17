import { definePlugin } from '@halo-dev/console-shared'
import { markRaw } from 'vue'
import ConnectTab from './views/ConnectTab.vue'
import { isChinese } from './i18n'

export default definePlugin({
  components: {},
  routes: [],
  extensionPoints: {
    // A "Connect Asyntai" tab on this plugin's own detail page.
    'plugin:self:tabs:create': () => [
      {
        id: 'asyntai-connect',
        label: isChinese() ? '连接 Asyntai' : 'Connect Asyntai',
        component: markRaw(ConnectTab),
        permissions: ['system:configmaps:manage'],
      },
    ],
  },
})
