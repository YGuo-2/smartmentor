import { ref, reactive } from 'vue';
import { api, clearTokens } from './api.js';

export const user = ref(null);
export const toasts = reactive([]);

export async function loadUser() {
  try {
    user.value = await api.auth.me();
  } catch {
    user.value = null;
  }
}

export function logout() {
  clearTokens();
  user.value = null;
  window.location.hash = '#/login';
}

export function showToast(message, type = 'info') {
  const id = Date.now();
  toasts.push({ id, message, type });
  setTimeout(() => {
    const idx = toasts.findIndex(t => t.id === id);
    if (idx > -1) toasts.splice(idx, 1);
  }, 3000);
}

// Render LaTeX in a given element
export function renderLatex(el) {
  if (!el || !window.katex) return;
  // Process display math $$...$$
  el.innerHTML = el.innerHTML.replace(/\$\$([^$]+)\$\$/g, (_, tex) => {
    try { return window.katex.renderToString(tex.trim(), { displayMode: true }); } catch { return tex; }
  });
  // Process inline math $...$
  el.innerHTML = el.innerHTML.replace(/\$([^$]+)\$/g, (_, tex) => {
    try { return window.katex.renderToString(tex.trim(), { displayMode: false }); } catch { return tex; }
  });
}
