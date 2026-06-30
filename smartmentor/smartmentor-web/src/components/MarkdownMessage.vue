<template>
  <div class="md-message" ref="rootEl" v-html="html"></div>
</template>

<script setup>
import { ref, computed, watch, onMounted, nextTick } from 'vue'
import { marked } from 'marked'
import { protectLatexForMarkdown, restoreLatexPlaceholders } from '../composables/state.js'

const props = defineProps({
  content: { type: String, default: '' },
  // 流式进行中：未闭合的 mermaid 围栏保持文本，避免对半截代码渲染报错
  streaming: { type: Boolean, default: false }
})

marked.setOptions({ breaks: true, gfm: true })

// mermaid 体积较大，且仅在出现图表时才需要：首次用到时动态加载，给首屏减重
let mermaidMod = null
async function getMermaid() {
  if (mermaidMod) return mermaidMod
  const m = (await import('mermaid')).default
  m.initialize({
    startOnLoad: false,
    theme: 'default',
    securityLevel: 'strict', // AI 输出内容，strict 关闭内联 HTML/JS，防注入
    fontFamily: "-apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif",
    flowchart: { htmlLabels: true, useMaxWidth: true },
    themeVariables: {
      primaryColor: '#eaf2ff',
      primaryBorderColor: '#2b5ea7',
      primaryTextColor: '#1a3a6b',
      lineColor: '#5b8fd4',
      fontSize: '14px'
    }
  })
  mermaidMod = m
  return m
}

const rootEl = ref(null)
let mermaidSeq = 0

// 已闭合的 mermaid 围栏：```mermaid\n...\n```
// 未闭合的（流式中途）不匹配，原样留给后续 marked 当普通文本，闭合后这一轮重渲染才提取
const CLOSED_MERMAID = /```mermaid[ \t]*\r?\n([\s\S]*?)```/g

const html = computed(() => {
  const raw = props.content || ''

  // 1. 先抽取已闭合的 mermaid 块为占位符，避免被 marked / LaTeX 正则破坏
  const diagrams = []
  const withoutMermaid = raw.replace(CLOSED_MERMAID, (_, code) => {
    diagrams.push(code.trim())
    return `\n\n%%MERMAID_${diagrams.length - 1}%%\n\n`
  })

  // 2. 保护 LaTeX → marked → 还原 LaTeX
  const { safe, placeholders } = protectLatexForMarkdown(withoutMermaid)
  let out = restoreLatexPlaceholders(marked.parse(safe), placeholders)

  // 3. mermaid 占位符还原为待渲染容器（marked 可能把它包进 <p>）
  out = out.replace(/(?:<p>)?\s*%%MERMAID_(\d+)%%\s*(?:<\/p>)?/g, (_, idx) => {
    const code = diagrams[Number(idx)]
    if (code == null) return ''
    const encoded = encodeURIComponent(code)
    return `<div class="md-mermaid" data-code="${encoded}"><div class="md-mermaid-loading">图示生成中…</div></div>`
  })

  return out
})

async function renderDiagrams() {
  if (!rootEl.value) return
  const blocks = rootEl.value.querySelectorAll('.md-mermaid:not([data-rendered])')
  if (!blocks.length) return
  const mermaid = await getMermaid()

  for (const el of blocks) {
    const code = decodeURIComponent(el.getAttribute('data-code') || '')
    if (!code) continue
    el.setAttribute('data-rendered', '1')
    try {
      // 先校验语法，避免 mermaid 把错误元素插入 body
      await mermaid.parse(code)
      const { svg } = await mermaid.render(`md-mmd-${mermaidSeq++}`, code)
      el.innerHTML = svg
    } catch (e) {
      // 渲染失败回退为原始代码块，不打断阅读
      el.classList.add('md-mermaid-error')
      el.innerHTML = `<div class="md-mermaid-fallback"><span>图示渲染失败，原始描述：</span><pre>${escapeHtml(code)}</pre></div>`
    }
  }
}

function escapeHtml(s) {
  return String(s).replace(/[&<>"']/g, c => ({
    '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
  }[c]))
}

watch(html, async () => {
  await nextTick()
  renderDiagrams()
})

onMounted(async () => {
  await nextTick()
  renderDiagrams()
})
</script>

<style scoped>
.md-mermaid {
  margin: 1.2em 0;
  display: flex;
  justify-content: center;
  padding: 16px;
  background: #fbfdff;
  border: 1px solid #e2ecf7;
  border-radius: 10px;
  overflow-x: auto;
}
.md-mermaid :deep(svg) {
  max-width: 100%;
  height: auto;
}
.md-mermaid-loading {
  color: #8aa0bd;
  font-size: 0.82rem;
  padding: 12px 0;
}
.md-mermaid-error {
  justify-content: flex-start;
  background: #fff7f5;
  border-color: #f0d2cb;
}
.md-mermaid-fallback {
  width: 100%;
  font-size: 0.8rem;
  color: #b3563b;
}
.md-mermaid-fallback pre {
  margin: 6px 0 0;
  padding: 10px 12px;
  background: #1b2a4a;
  color: #e2e8f0;
  border-radius: 6px;
  overflow-x: auto;
  white-space: pre-wrap;
}
</style>
