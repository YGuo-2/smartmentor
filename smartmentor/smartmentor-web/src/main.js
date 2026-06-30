import { createApp } from 'vue'
import App from './App.vue'
import { router } from './router/index.js'
import { loadUser, getAccessToken } from './composables/state.js'
import 'remixicon/fonts/remixicon.css'
import 'katex/dist/katex.min.css'
import './assets/css/base.css'

const app = createApp(App)
app.use(router)
app.mount('#app')

if (getAccessToken()) {
  loadUser()
}
