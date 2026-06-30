import { createApp } from 'vue';
import { router } from './router.js';
import { user, toasts, loadUser } from './state.js';
import { getAccessToken } from './api.js';

const App = {
  template: `
<div id="application">
  <router-view></router-view>
  <div class="toast-container">
    <div v-for="t in toasts" :key="t.id" class="toast" :class="t.type">{{ t.message }}</div>
  </div>
</div>
  `,
  setup() {
    if (getAccessToken()) loadUser();
    return { toasts };
  }
};

const app = createApp(App);
app.use(router);
app.config.globalProperties.$user = user;
app.mount('#app');
