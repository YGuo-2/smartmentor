import { ref, reactive } from 'vue';
import { api, clearTokens, getAccessToken } from '../api/index.js';
import katex from 'katex';

export { getAccessToken } from '../api/index.js';

export const user = ref(null);
export const toasts = reactive([]);

export async function loadUser() {
  try {
    user.value = await api.auth.me();
  } catch {
    user.value = null;
  }
  return user.value;
}

export function logout() {
  clearTokens();
  user.value = null;
  window.location.hash = '#/login';
}

export function normalizeRole(role) {
  const value = String(role || '').trim().toLowerCase();
  if (['student', 'role_student', '学生'].includes(value)) return 'student';
  return value;
}

export function isStudentRole(role) {
  return normalizeRole(role) === 'student';
}

export function getUserRole(currentUser = user.value) {
  return normalizeRole(currentUser?.role);
}

export function roleHome(role) {
  return '/dashboard';
}

export function showToast(message, type = 'info') {
  const id = Date.now();
  toasts.push({ id, message, type });
  setTimeout(() => {
    const idx = toasts.findIndex(t => t.id === id);
    if (idx > -1) toasts.splice(idx, 1);
  }, 3000);
}

export function renderLatex(el) {
  if (!el) return;
  let html = el.innerHTML
    .replace(/\\\[([\s\S]*?)\\\]/g, (_, tex) => `$$${tex.trim()}$$`)
    .replace(/\\\(([\s\S]*?)\\\)/g, (_, tex) => `$${tex.trim()}$`);
  html = html.replace(/\$\$([\s\S]*?)\$\$/g, (_, tex) => renderKatex(tex, true));
  html = html.replace(/(^|[^\\])\$([^\n$]+?)\$/g, (_, prefix, tex) => prefix + renderKatex(tex, false));
  el.innerHTML = html;
}

export function renderLatexString(str) {
  if (!str) return str;
  let result = normalizeLatexText(str);
  result = result.replace(/\$\$([\s\S]*?)\$\$/g, (_, tex) => renderKatex(tex, true));
  result = result.replace(/(^|[^\\])\$([^\n$]+?)\$/g, (_, prefix, tex) => prefix + renderKatex(tex, false));
  return result;
}

export function normalizeLatexText(str) {
  if (!str) return str;
  return splitMarkdownFences(String(str)).map(part => {
    if (part.fenced) return part.text;
    return wrapBareFormulaLines(
      part.text
        .replace(/\\\[([\s\S]*?)\\\]/g, (_, tex) => `$$${tex.trim()}$$`)
        .replace(/\\\(([\s\S]*?)\\\)/g, (_, tex) => `$${tex.trim()}$`)
    );
  }).join('');
}

export function protectLatexForMarkdown(content) {
  const placeholders = [];
  const safe = normalizeLatexText(content || '')
    .replace(/\$\$([\s\S]*?)\$\$/g, (match) => {
      placeholders.push(match);
      return `%%LATEX_${placeholders.length - 1}%%`;
    })
    .replace(/(^|[^\\])\$([^\n$]+?)\$/g, (match, prefix) => {
      const formula = match.slice(prefix.length);
      placeholders.push(formula);
      return `${prefix}%%LATEX_${placeholders.length - 1}%%`;
    });

  return { safe, placeholders };
}

export function restoreLatexPlaceholders(html, placeholders) {
  return html.replace(/%%LATEX_(\d+)%%/g, (_, idx) => renderLatexString(placeholders[Number(idx)] || ''));
}

function renderKatex(tex, displayMode) {
  try {
    return katex.renderToString(tex.trim(), { displayMode, throwOnError: false, strict: false });
  } catch {
    return tex;
  }
}

function splitMarkdownFences(text) {
  const parts = [];
  const fenceRegex = /```[\s\S]*?```/g;
  let lastIndex = 0;
  let match;
  while ((match = fenceRegex.exec(text)) !== null) {
    if (match.index > lastIndex) {
      parts.push({ text: text.slice(lastIndex, match.index), fenced: false });
    }
    parts.push({ text: match[0], fenced: true });
    lastIndex = match.index + match[0].length;
  }
  if (lastIndex < text.length) {
    parts.push({ text: text.slice(lastIndex), fenced: false });
  }
  return parts.length ? parts : [{ text, fenced: false }];
}

function wrapBareFormulaLines(text) {
  return text.split('\n').map(line => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.includes('$') || trimmed.startsWith('|')) return line;
    const listMatch = line.match(/^(\s*(?:[-*+]\s+|\d+[.)、]\s+))(.+)$/);
    if (listMatch) {
      const formula = listMatch[2].trim();
      return looksLikeStandaloneFormula(formula) ? `${listMatch[1]}$$${formula}$$` : line;
    }
    if (!looksLikeStandaloneFormula(trimmed)) return line;
    const leading = line.match(/^\s*/)?.[0] || '';
    return `${leading}$$${trimmed}$$`;
  }).join('\n');
}

function looksLikeStandaloneFormula(text) {
  if (/[，。！？；：、]/.test(text)) return false;
  if (!/[=<>≤≥≈]|\\[a-zA-Z]+|[_^{}]/.test(text)) return false;
  if (text.length > 120) return false;
  const mathChars = (text.match(/[A-Za-z0-9_\\^{}+\-*/=<>()[\],.| ]/g) || []).length;
  return mathChars / Math.max(text.length, 1) > 0.7;
}
