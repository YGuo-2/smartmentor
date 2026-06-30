<template>
  <div ref="playerRef" class="ai-anim-player" :style="`--scene-accent:${current.accent}`">
    <div v-if="asset?.provider" class="ai-anim-provider">
      <span><i class="ri-magic-line"></i>{{ assetLabel }}</span>
      <a
        v-if="asset.videoUrl"
        class="ai-anim-provider-link"
        :href="asset.videoUrl"
        target="_blank"
        rel="noopener noreferrer"
        title="打开生成结果"
      >
        <i class="ri-external-link-line"></i>
      </a>
    </div>

    <div v-if="asset?.videoUrl" class="ai-anim-video">
      <video :src="asset.videoUrl" controls playsinline preload="metadata"></video>
    </div>

    <div v-else class="ai-anim-stage">
      <svg class="ai-anim-svg" viewBox="0 0 760 360" role="img" :aria-label="current.scene || '动画讲解'">
        <defs>
          <marker :id="markerId" markerWidth="8" markerHeight="8" refX="7" refY="4" orient="auto">
            <path d="M 0 0 L 8 4 L 0 8 Z" :fill="current.accent" />
          </marker>
          <filter :id="glowId" x="-40%" y="-40%" width="180%" height="180%">
            <feGaussianBlur stdDeviation="4" result="blur" />
            <feMerge>
              <feMergeNode in="blur" />
              <feMergeNode in="SourceGraphic" />
            </feMerge>
          </filter>
        </defs>

        <rect class="stage-grid" x="0" y="0" width="760" height="360" rx="24" />

        <g class="scene-orbit">
          <circle class="orbit-ring ring-a" cx="380" cy="180" r="122" />
          <circle class="orbit-ring ring-b" cx="380" cy="180" r="82" />
        </g>

        <g class="flow-layer">
          <path
            v-for="(edge, i) in layoutEdges"
            :key="'edge-' + i"
            class="anim-edge"
            :d="edge.path"
            :stroke="current.accent"
            :marker-end="`url(#${markerId})`"
            :style="`--edge-len:${edge.len}`"
          />
          <text
            v-for="(edge, i) in layoutEdges"
            :key="'label-' + i"
            class="anim-edge-label"
            :x="edge.mx"
            :y="edge.my"
          >
            {{ edge.label }}
          </text>
          <circle
            v-for="(edge, i) in layoutEdges"
            :key="'pulse-' + i"
            class="edge-pulse"
            r="5"
            :fill="current.accent"
            :filter="`url(#${glowId})`"
          >
            <animateMotion :dur="`${Math.max(1.6, edge.len / 120)}s`" repeatCount="indefinite" :path="edge.path" />
          </circle>
        </g>

        <g
          v-for="(node, i) in layoutNodes"
          :key="'node-' + node.id"
          class="anim-node"
          :style="`--node-index:${i}`"
        >
          <rect
            class="anim-node-box"
            :x="node.x"
            :y="node.y"
            :width="nodeW"
            :height="nodeH"
            rx="16"
            :stroke="current.accent"
          />
          <circle class="anim-node-dot" :cx="node.x + 18" :cy="node.y + 18" r="5" :fill="current.accent" />
          <text
            class="anim-node-label"
            :x="node.x + nodeW / 2"
            :y="node.y + nodeH / 2 + 4"
            text-anchor="middle"
          >
            {{ node.label }}
          </text>
        </g>

        <g v-if="!layoutNodes.length" class="fallback-visual">
          <circle cx="380" cy="180" r="68" :stroke="current.accent" />
          <path :d="fallbackPath" :stroke="current.accent" />
          <text x="380" y="184" text-anchor="middle">{{ fallbackVisualLabel }}</text>
        </g>
      </svg>

      <div class="scene-copy">
        <div class="scene-badge">场景 {{ currentIndex + 1 }} / {{ sceneCount }}</div>
        <h4>{{ current.scene || '动画讲解' }}</h4>
        <p>{{ current.visual || 'AI 正在把这个知识点拆成可视化过程。' }}</p>
      </div>
    </div>

    <div class="ai-anim-narration">
      <i class="ri-volume-up-line"></i>
      <p>{{ current.narration || '暂无旁白，点击播放查看场景推进。' }}</p>
    </div>

    <div class="ai-anim-controls">
      <button class="anim-ctrl-btn" :disabled="currentIndex === 0" title="上一场景" @click="prev">
        <i class="ri-skip-back-line"></i>
      </button>
      <button class="anim-ctrl-btn anim-ctrl-play" :title="playing ? '暂停' : '播放'" @click="togglePlay">
        <i :class="playing ? 'ri-pause-line' : 'ri-play-fill'"></i>
      </button>
      <button class="anim-ctrl-btn" title="重新播放" @click="replay">
        <i class="ri-replay-line"></i>
      </button>
      <button class="anim-ctrl-btn" :disabled="currentIndex === sceneCount - 1" title="下一场景" @click="next">
        <i class="ri-skip-forward-line"></i>
      </button>

      <div class="scene-progress" aria-hidden="true">
        <span
          v-for="(scene, i) in normalizedScenes"
          :key="'dot-' + i"
          class="scene-dot"
          :class="{ active: i === currentIndex, played: i < currentIndex }"
          @click="goTo(i)"
        ></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { gsap, prefersReducedMotion } from '../lib/gsap'

const props = defineProps({
  scenes: { type: Array, default: () => [] },
  asset: { type: Object, default: null },
  interval: { type: Number, default: 6200 }
})

const playerRef = ref(null)
const currentIndex = ref(0)
const playing = ref(false)
let timer = null
let ctx = null
let sceneTl = null

const nodeW = 124
const nodeH = 58
const markerId = 'anim-arrow-' + Math.random().toString(36).slice(2, 8)
const glowId = 'anim-glow-' + Math.random().toString(36).slice(2, 8)

const SCENE_TYPES = [
  { re: /(引入|导入|问题|痛点|背景|场景)/, accent: '#c97742' },
  { re: /(机制|原理|概念|核心|是什么|为什么|拆解)/, accent: '#2f73b8' },
  { re: /(步骤|操作|演示|流程|应用|使用|怎么)/, accent: '#2f8b63' },
  { re: /(结果|反馈|输出|总结|小结|检查|易错|回顾)/, accent: '#9b6a2f' }
]

const fallbackPath = 'M 330 180 C 350 138 410 138 430 180 C 410 222 350 222 330 180 Z'

const normalizedScenes = computed(() => {
  const source = props.asset?.scenes?.length ? props.asset.scenes : props.scenes
  return normalizeScenes(source)
})
const sceneCount = computed(() => Math.max(1, normalizedScenes.value.length))
const current = computed(() => {
  const scene = normalizedScenes.value[currentIndex.value] || normalizedScenes.value[0] || {}
  const type = classifyScene(scene.scene, currentIndex.value)
  return { ...scene, accent: scene.accent || type.accent }
})
const assetLabel = computed(() => {
  if (!props.asset?.provider) return ''
  if (props.asset.status === 'fallback') return '本地实时动画'
  if (props.asset.status === 'ready') return `${props.asset.provider} 已生成`
  return `${props.asset.provider} 生成中`
})
const fallbackVisualLabel = computed(() => {
  const name = current.value.scene || '过程'
  return name.length > 8 ? name.slice(0, 8) : name
})

const diagram = computed(() => current.value.diagram || {})
const nodes = computed(() => Array.isArray(diagram.value.nodes) ? diagram.value.nodes : [])
const edges = computed(() => Array.isArray(diagram.value.edges) ? diagram.value.edges : [])
const layoutNodes = computed(() => layoutDiagramNodes(nodes.value))
const idIndex = computed(() => {
  const map = {}
  layoutNodes.value.forEach((node, index) => { map[node.id] = index })
  return map
})
const layoutEdges = computed(() => layoutDiagramEdges(layoutNodes.value, edges.value, idIndex.value))

function normalizeScenes(source) {
  const list = Array.isArray(source) ? source : []
  const normalized = list.map((item, index) => {
    if (typeof item === 'string') return parseSceneLine(item, index)
    if (!item || typeof item !== 'object') return null
    return {
      scene: String(item.scene || item.title || `场景 ${index + 1}`).trim(),
      narration: String(item.narration || item.voiceover || '').trim(),
      visual: String(item.visual || item.description || '').trim(),
      diagram: normalizeDiagram(item.diagram)
    }
  }).filter(Boolean)
  return normalized.length ? normalized : [{
    scene: '问题引入',
    narration: 'AI 会先把知识点拆成可观察的过程，再逐步展示关键流向。',
    visual: '用节点、连线和运动轨迹表示概念之间的关系。',
    diagram: defaultDiagram('问题', '机制', '应用', '反馈')
  }]
}

function parseSceneLine(line, index) {
  const text = String(line || '').trim()
  const parts = text.split(/[：:]/)
  const name = parts.length > 1 ? parts.shift().trim() : `场景 ${index + 1}`
  const rest = parts.join('：').trim() || text
  const visualMatch = rest.match(/（画面[：:](.*?)）/)
  return {
    scene: name,
    narration: rest.replace(/（画面[：:].*?）/, '').trim(),
    visual: visualMatch ? visualMatch[1].trim() : rest,
    diagram: defaultDiagram('引入', '分析', '操作', '反馈')
  }
}

function normalizeDiagram(input) {
  const rawNodes = Array.isArray(input?.nodes) ? input.nodes : []
  const rawEdges = Array.isArray(input?.edges) ? input.edges : []
  const safeNodes = rawNodes
    .map((node, index) => ({
      id: String(node?.id || `n${index}`).trim(),
      label: compactLabel(node?.label || node?.name || `节点${index + 1}`)
    }))
    .filter(node => node.id)
    .slice(0, 6)
  const known = new Set(safeNodes.map(node => node.id))
  const safeEdges = rawEdges
    .map(edge => ({
      from: String(edge?.from || '').trim(),
      to: String(edge?.to || '').trim(),
      label: compactLabel(edge?.label || '')
    }))
    .filter(edge => known.has(edge.from) && known.has(edge.to) && edge.from !== edge.to)
    .slice(0, 8)
  return safeNodes.length ? { type: input?.type || 'flow', nodes: safeNodes, edges: safeEdges } : null
}

function defaultDiagram(...labels) {
  const nodes = labels.map((label, index) => ({ id: `n${index}`, label }))
  return {
    type: 'flow',
    nodes,
    edges: nodes.slice(1).map((node, index) => ({ from: nodes[index].id, to: node.id, label: '' }))
  }
}

function compactLabel(value) {
  const text = String(value || '').replace(/\s+/g, '').trim()
  return text.length > 8 ? text.slice(0, 8) : text
}

function classifyScene(name, index) {
  const text = String(name || '')
  return SCENE_TYPES.find(type => type.re.test(text)) || SCENE_TYPES[index % SCENE_TYPES.length]
}

function layoutDiagramNodes(rawNodes) {
  if (!rawNodes.length) return []
  if (rawNodes.length === 1) {
    return [{ ...rawNodes[0], x: 318, y: 151 }]
  }
  const cx = 380
  const cy = 178
  const radius = rawNodes.length <= 4 ? 116 : 132
  return rawNodes.map((node, index) => {
    const angle = (-90 + index * 360 / rawNodes.length) * Math.PI / 180
    return {
      ...node,
      x: cx + Math.cos(angle) * radius - nodeW / 2,
      y: cy + Math.sin(angle) * radius - nodeH / 2
    }
  })
}

function layoutDiagramEdges(ns, rawEdges, indexMap) {
  return rawEdges.map(edge => {
    const fromIdx = indexMap[edge.from]
    const toIdx = indexMap[edge.to]
    if (fromIdx == null || toIdx == null) return null
    const a = ns[fromIdx]
    const b = ns[toIdx]
    const ax = a.x + nodeW / 2
    const ay = a.y + nodeH / 2
    const bx = b.x + nodeW / 2
    const by = b.y + nodeH / 2
    const dx = bx - ax
    const dy = by - ay
    const dist = Math.hypot(dx, dy) || 1
    const ux = dx / dist
    const uy = dy / dist
    const x1 = ax + ux * 44
    const y1 = ay + uy * 28
    const x2 = bx - ux * 44
    const y2 = by - uy * 28
    const cx = (x1 + x2) / 2 - uy * 18
    const cy = (y1 + y2) / 2 + ux * 18
    const path = `M ${x1.toFixed(1)} ${y1.toFixed(1)} Q ${cx.toFixed(1)} ${cy.toFixed(1)} ${x2.toFixed(1)} ${y2.toFixed(1)}`
    return {
      path,
      len: Math.ceil(dist + 36),
      mx: cx,
      my: cy - 10,
      label: edge.label || ''
    }
  }).filter(Boolean)
}

function clearTimer() {
  if (timer) {
    clearTimeout(timer)
    timer = null
  }
}

function scheduleNext() {
  clearTimer()
  if (!playing.value) return
  timer = setTimeout(() => {
    if (currentIndex.value < sceneCount.value - 1) {
      currentIndex.value += 1
      scheduleNext()
    } else {
      playing.value = false
    }
  }, props.interval)
}

function togglePlay() {
  if (playing.value) {
    playing.value = false
    clearTimer()
    sceneTl?.pause()
    return
  }
  if (currentIndex.value === sceneCount.value - 1) currentIndex.value = 0
  playing.value = true
  sceneTl?.restart()
  scheduleNext()
}

function replay() {
  currentIndex.value = 0
  playing.value = true
  sceneTl?.restart()
  scheduleNext()
}

function prev() {
  if (currentIndex.value > 0) currentIndex.value -= 1
  if (playing.value) scheduleNext()
}

function next() {
  if (currentIndex.value < sceneCount.value - 1) currentIndex.value += 1
  if (playing.value) scheduleNext()
}

function goTo(index) {
  currentIndex.value = index
  if (playing.value) scheduleNext()
}

async function animateScene() {
  await nextTick()
  if (!playerRef.value) return
  sceneTl?.kill()
  const reduceMotion = prefersReducedMotion()
  ctx?.revert()
  ctx = gsap.context(() => {
    sceneTl = gsap.timeline({ paused: !playing.value, defaults: { ease: 'power3.out' } })
    sceneTl
      .fromTo('.scene-copy', { autoAlpha: 0, y: 16 }, { autoAlpha: 1, y: 0, duration: reduceMotion ? 0 : 0.45 }, 0)
      .fromTo('.ai-anim-narration', { autoAlpha: 0, y: 8 }, { autoAlpha: 1, y: 0, duration: reduceMotion ? 0 : 0.4 }, 0.08)
      .fromTo('.anim-node', { autoAlpha: 0, scale: 0.74, transformOrigin: '50% 50%' }, {
        autoAlpha: 1,
        scale: 1,
        duration: reduceMotion ? 0 : 0.52,
        stagger: reduceMotion ? 0 : 0.14
      }, 0.14)
      .fromTo('.anim-edge', { strokeDashoffset: 'var(--edge-len)', autoAlpha: 0 }, {
        strokeDashoffset: 0,
        autoAlpha: 1,
        duration: reduceMotion ? 0 : 0.7,
        stagger: reduceMotion ? 0 : 0.12
      }, 0.45)
      .fromTo('.anim-edge-label', { autoAlpha: 0, y: 4 }, {
        autoAlpha: 1,
        y: 0,
        duration: reduceMotion ? 0 : 0.32,
        stagger: reduceMotion ? 0 : 0.08
      }, 0.75)
      .fromTo('.fallback-visual', { autoAlpha: 0, scale: 0.8, transformOrigin: '50% 50%' }, {
        autoAlpha: 1,
        scale: 1,
        duration: reduceMotion ? 0 : 0.5
      }, 0.2)
    if (!reduceMotion) {
      sceneTl.to('.ring-a', { rotation: 360, transformOrigin: '50% 50%', duration: 12, repeat: -1, ease: 'none' }, 0)
      sceneTl.to('.ring-b', { rotation: -360, transformOrigin: '50% 50%', duration: 9, repeat: -1, ease: 'none' }, 0)
    }
  }, playerRef.value)
}

watch(currentIndex, animateScene)
watch(() => props.scenes, () => {
  currentIndex.value = 0
  playing.value = false
  clearTimer()
  animateScene()
}, { deep: true })
watch(() => props.asset, () => {
  currentIndex.value = 0
  playing.value = false
  clearTimer()
  animateScene()
}, { deep: true })

onMounted(() => {
  playing.value = sceneCount.value > 1 && !props.asset?.videoUrl
  animateScene()
  scheduleNext()
})

onBeforeUnmount(() => {
  clearTimer()
  sceneTl?.kill()
  ctx?.revert()
})
</script>

<style scoped>
.ai-anim-player {
  display: grid;
  gap: 14px;
  color: var(--text);
}
.ai-anim-provider {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 0.78rem;
  color: var(--text-secondary);
}
.ai-anim-provider span,
.ai-anim-provider-link {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.ai-anim-provider-link {
  color: var(--accent);
  text-decoration: none;
}
.ai-anim-video {
  border-radius: 8px;
  overflow: hidden;
  background: #0f172a;
  aspect-ratio: 16 / 9;
}
.ai-anim-video video {
  width: 100%;
  height: 100%;
  display: block;
}
.ai-anim-stage {
  position: relative;
  min-height: 360px;
  border-radius: 8px;
  overflow: hidden;
  background:
    radial-gradient(circle at 18% 18%, color-mix(in srgb, var(--scene-accent) 26%, transparent), transparent 34%),
    radial-gradient(circle at 85% 20%, rgba(255, 255, 255, 0.12), transparent 28%),
    linear-gradient(135deg, #112240 0%, #1e3a68 100%);
}
.ai-anim-svg {
  width: 100%;
  min-height: 320px;
  display: block;
}
.stage-grid {
  fill: rgba(255, 255, 255, 0.035);
}
.orbit-ring {
  fill: none;
  stroke: color-mix(in srgb, var(--scene-accent) 34%, transparent);
  stroke-width: 1.5;
  stroke-dasharray: 7 9;
  transform-box: fill-box;
}
.ring-b {
  stroke: rgba(255, 255, 255, 0.18);
}
.anim-edge {
  fill: none;
  stroke-width: 3;
  stroke-linecap: round;
  stroke-dasharray: var(--edge-len);
  stroke-dashoffset: var(--edge-len);
  opacity: 0;
}
.anim-edge-label {
  fill: rgba(255, 255, 255, 0.86);
  font-size: 12px;
  font-weight: 700;
  paint-order: stroke;
  stroke: rgba(17, 34, 64, 0.8);
  stroke-width: 4px;
  text-anchor: middle;
  opacity: 0;
}
.edge-pulse {
  opacity: 0.74;
}
.anim-node {
  opacity: 0;
  transform-box: fill-box;
}
.anim-node-box {
  fill: rgba(255, 255, 255, 0.1);
  stroke-width: 2;
  backdrop-filter: blur(8px);
}
.anim-node-dot {
  opacity: 0.9;
}
.anim-node-label {
  fill: #fff;
  font-size: 15px;
  font-weight: 800;
}
.fallback-visual {
  opacity: 0;
}
.fallback-visual circle,
.fallback-visual path {
  fill: none;
  stroke-width: 3;
}
.fallback-visual text {
  fill: #fff;
  font-size: 20px;
  font-weight: 800;
}
.scene-copy {
  position: absolute;
  left: 22px;
  right: 22px;
  bottom: 18px;
  display: grid;
  gap: 8px;
  color: #fff;
  pointer-events: none;
}
.scene-badge {
  width: fit-content;
  padding: 4px 10px;
  border-radius: 999px;
  background: color-mix(in srgb, var(--scene-accent) 26%, rgba(17, 34, 64, 0.6));
  border: 1px solid color-mix(in srgb, var(--scene-accent) 56%, transparent);
  font-size: 0.72rem;
  font-weight: 800;
}
.scene-copy h4 {
  margin: 0;
  font-size: 1.15rem;
  line-height: 1.25;
}
.scene-copy p {
  margin: 0;
  max-width: 640px;
  font-size: 0.86rem;
  line-height: 1.65;
  color: rgba(255, 255, 255, 0.86);
}
.ai-anim-narration {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 13px 14px;
  border-radius: 8px;
  background: color-mix(in srgb, var(--scene-accent) 9%, var(--card-bg-solid));
  border: 1px solid color-mix(in srgb, var(--scene-accent) 28%, var(--border));
}
.ai-anim-narration i {
  margin-top: 2px;
  color: var(--scene-accent);
  flex-shrink: 0;
}
.ai-anim-narration p {
  margin: 0;
  font-size: 0.9rem;
  line-height: 1.7;
}
.ai-anim-controls {
  display: flex;
  align-items: center;
  gap: 8px;
}
.anim-ctrl-btn {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card-bg-solid);
  color: var(--text);
  display: grid;
  place-items: center;
  cursor: pointer;
  font-size: 1.1rem;
  transition: border-color 0.2s, color 0.2s, transform 0.2s;
}
.anim-ctrl-btn:hover:not(:disabled) {
  border-color: var(--scene-accent);
  color: var(--scene-accent);
  transform: translateY(-1px);
}
.anim-ctrl-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.anim-ctrl-play {
  background: var(--text);
  border-color: var(--text);
  color: #fff;
}
.anim-ctrl-play:hover:not(:disabled) {
  color: #fff;
  border-color: var(--text);
}
.scene-progress {
  display: flex;
  align-items: center;
  gap: 7px;
  margin-left: 6px;
}
.scene-dot {
  width: 10px;
  height: 10px;
  border-radius: 999px;
  background: var(--border);
  cursor: pointer;
  transition: width 0.25s, background 0.25s;
}
.scene-dot.played {
  background: color-mix(in srgb, var(--scene-accent) 55%, var(--border));
}
.scene-dot.active {
  width: 26px;
  background: var(--scene-accent);
}
@media (max-width: 640px) {
  .ai-anim-stage {
    min-height: 320px;
  }
  .scene-copy {
    left: 16px;
    right: 16px;
    bottom: 14px;
  }
  .scene-copy p {
    font-size: 0.8rem;
  }
  .ai-anim-controls {
    flex-wrap: wrap;
  }
}
@media (prefers-reduced-motion: reduce) {
  .edge-pulse,
  .orbit-ring {
    animation: none;
  }
}
</style>
