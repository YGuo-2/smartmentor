<template>
  <div class="ppt-overlay" @click.self="emitClose">
    <div class="ppt-modal">
      <div class="ppt-toolbar">
        <div class="ppt-toolbar-title">
          <i class="ri-slideshow-3-line"></i>
          <span>{{ deckTitle }}</span>
          <span v-if="degraded" class="ppt-degraded" title="AI 生成不可用，已用课程素材兜底">兜底大纲</span>
        </div>
        <div class="ppt-toolbar-actions">
          <button type="button" class="ppt-btn" :disabled="downloading" @click="emitDownload">
            <i :class="downloading ? 'ri-loader-4-line spin' : 'ri-download-2-line'"></i>
            {{ downloading ? '导出中…' : '下载 .pptx' }}
          </button>
          <button type="button" class="ppt-btn ppt-btn-ghost" @click="emitClose" title="关闭">
            <i class="ri-close-line"></i>
          </button>
        </div>
      </div>

      <div ref="deckRef" class="reveal ppt-deck">
        <div class="slides">
          <section v-for="(slide, idx) in slides" :key="idx" :data-type="slide.type">
            <!-- 封面 -->
            <template v-if="slide.type === 'cover'">
              <div class="slide-cover">
                <div class="slide-accent"></div>
                <h1>{{ slide.title || deckTitle }}</h1>
                <p v-if="slide.subtitle" class="slide-subtitle">{{ slide.subtitle }}</p>
                <p v-if="audience" class="slide-audience">{{ audience }}</p>
              </div>
            </template>

            <!-- 目录 / 小结 / 内容（列表型） -->
            <template v-else-if="slide.type === 'agenda' || slide.type === 'summary' || slide.type === 'content'">
              <h2 class="slide-title">{{ slide.title }}</h2>
              <ul class="slide-list">
                <li v-for="(item, i) in listItems(slide)" :key="i">{{ item }}</li>
              </ul>
            </template>

            <!-- 代码 -->
            <template v-else-if="slide.type === 'code'">
              <h2 class="slide-title">{{ slide.title }}</h2>
              <p v-if="slide.explain" class="slide-explain">{{ slide.explain }}</p>
              <pre class="slide-code"><code>{{ slide.code }}</code></pre>
            </template>

            <!-- 公式 -->
            <template v-else-if="slide.type === 'formula'">
              <h2 class="slide-title">{{ slide.title }}</h2>
              <div class="slide-formula" v-html="renderFormula(slide.latex)"></div>
              <p v-if="slide.explain" class="slide-explain slide-explain-center">{{ slide.explain }}</p>
            </template>

            <!-- 实操案例 -->
            <template v-else-if="slide.type === 'case'">
              <h2 class="slide-title">{{ slide.title }}</h2>
              <div v-if="slide.scenario" class="slide-scenario">{{ slide.scenario }}</div>
              <ol class="slide-steps">
                <li v-for="(step, i) in (slide.steps || [])" :key="i">{{ step }}</li>
              </ol>
            </template>

            <template v-else>
              <h2 class="slide-title">{{ slide.title }}</h2>
              <ul class="slide-list">
                <li v-for="(item, i) in listItems(slide)" :key="i">{{ item }}</li>
              </ul>
            </template>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import Reveal from 'reveal.js'
import 'reveal.js/reveal.css'
import 'reveal.js/theme/white.css'
import { renderLatexString } from '../composables/state.js'

const props = defineProps({
  slidesDoc: { type: Object, required: true },
  downloading: { type: Boolean, default: false }
})
const emit = defineEmits(['close', 'download'])

const deckRef = ref(null)
let deck = null

const meta = computed(() => props.slidesDoc?.meta || {})
const deckTitle = computed(() => meta.value.title || '演示文稿')
const audience = computed(() => meta.value.audience || '')
const degraded = computed(() => Boolean(props.slidesDoc?.degraded))
const slides = computed(() => Array.isArray(props.slidesDoc?.slides) ? props.slidesDoc.slides : [])

function listItems(slide) {
  return slide.points || slide.bullets || []
}

function renderFormula(latex) {
  const raw = String(latex || '').trim()
  if (!raw) return ''
  const wrapped = raw.includes('$') ? raw : `$$${raw}$$`
  return renderLatexString(wrapped)
}

function emitClose() {
  emit('close')
}
function emitDownload() {
  emit('download')
}

onMounted(async () => {
  await nextTick()
  deck = new Reveal(deckRef.value, {
    embedded: true,
    hash: false,
    controls: true,
    progress: true,
    slideNumber: 'c/t',
    transition: 'slide',
    width: 960,
    height: 600,
    margin: 0.06
  })
  await deck.initialize()
})

onBeforeUnmount(() => {
  if (deck) {
    try { deck.destroy() } catch { /* reveal 实例已释放 */ }
    deck = null
  }
})
</script>

<style scoped>
.ppt-overlay {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.62);
  -webkit-backdrop-filter: blur(3px);
  backdrop-filter: blur(3px);
  z-index: 240;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.ppt-modal {
  background: var(--card-bg-solid);
  border: 1px solid var(--border);
  border-radius: 14px;
  width: min(1040px, 100%);
  max-height: 92vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-xl);
  overflow: hidden;
}
.ppt-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
}
.ppt-toolbar-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: var(--text);
  min-width: 0;
}
.ppt-toolbar-title > span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ppt-toolbar-title i { color: var(--accent); font-size: 1.2rem; }
.ppt-degraded {
  flex-shrink: 0;
  font-size: 0.7rem;
  font-weight: 500;
  color: #92400e;
  background: #fef3c7;
  border-radius: 999px;
  padding: 2px 8px;
}
.ppt-toolbar-actions { display: flex; gap: 8px; flex-shrink: 0; }
.ppt-btn {
  border: 1px solid var(--accent);
  background: var(--accent);
  color: #fff;
  border-radius: 8px;
  padding: 7px 14px;
  cursor: pointer;
  font-size: 0.84rem;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  transition: opacity 0.18s;
}
.ppt-btn:hover:not(:disabled) { opacity: 0.88; }
.ppt-btn:disabled { opacity: 0.6; cursor: default; }
.ppt-btn-ghost {
  background: var(--bg-alt);
  color: var(--text-secondary);
  border-color: var(--border);
  padding: 7px 10px;
}
.ppt-deck {
  width: 100%;
  height: min(70vh, 640px);
  background: #fff;
}
.spin { display: inline-block; animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* reveal section 内容样式（不 scoped 到 .reveal 内部，用 :deep） */
.ppt-deck :deep(.slide-title) {
  color: #1e3a8a;
  font-size: 1.7rem;
  margin: 0 0 0.6em;
  text-align: left;
  border-left: 5px solid #2563eb;
  padding-left: 14px;
}
.ppt-deck :deep(.slide-list) {
  text-align: left;
  font-size: 1.15rem;
  line-height: 1.9;
  color: #1f2937;
  padding-left: 1.4em;
}
.ppt-deck :deep(.slide-list li) { margin: 0.25em 0; }
.ppt-deck :deep(.slide-cover) {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  text-align: left;
  padding: 0 8%;
}
.ppt-deck :deep(.slide-cover h1) {
  color: #1e3a8a;
  font-size: 2.6rem;
  margin: 0 0 0.3em;
}
.ppt-deck :deep(.slide-accent) {
  width: 110px;
  height: 6px;
  background: #2563eb;
  border-radius: 3px;
  margin-bottom: 24px;
}
.ppt-deck :deep(.slide-subtitle) { color: #475569; font-size: 1.3rem; margin: 0; }
.ppt-deck :deep(.slide-audience) { color: #94a3b8; font-size: 0.95rem; margin-top: 18px; }
.ppt-deck :deep(.slide-explain) {
  text-align: left;
  color: #6b7280;
  font-size: 1rem;
  margin: 0 0 0.7em;
}
.ppt-deck :deep(.slide-explain-center) { text-align: center; margin-top: 1em; }
.ppt-deck :deep(.slide-code) {
  text-align: left;
  background: #0f172a;
  color: #e2e8f0;
  border-radius: 10px;
  padding: 18px 20px;
  font-size: 0.92rem;
  line-height: 1.6;
  overflow: auto;
  max-height: 64%;
}
.ppt-deck :deep(.slide-code code) { font-family: Consolas, Monaco, monospace; }
.ppt-deck :deep(.slide-formula) {
  background: #f1f5f9;
  border-radius: 10px;
  padding: 28px;
  margin: 0.4em 0;
  text-align: center;
  font-size: 1.3rem;
  overflow-x: auto;
}
.ppt-deck :deep(.slide-scenario) {
  text-align: left;
  background: #f1f5f9;
  border-radius: 10px;
  padding: 14px 18px;
  color: #1f2937;
  font-size: 1.05rem;
  margin-bottom: 0.8em;
}
.ppt-deck :deep(.slide-steps) {
  text-align: left;
  font-size: 1.12rem;
  line-height: 1.9;
  color: #1f2937;
  padding-left: 1.4em;
}
</style>
