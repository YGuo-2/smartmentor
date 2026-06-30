<template>
  <div class="flow-diagram">
    <svg :viewBox="`0 0 ${width} ${height}`" class="flow-svg" preserveAspectRatio="xMidYMid meet">
      <defs>
        <marker :id="markerId" viewBox="0 0 10 10" refX="9" refY="5"
                markerWidth="7" markerHeight="7" orient="auto-start-reverse">
          <path d="M 0 0 L 10 5 L 0 10 z" :fill="accent" />
        </marker>
      </defs>

      <!-- 边：在两端节点出现后生长 -->
      <g v-for="(edge, i) in layoutEdges" :key="'e' + i" class="flow-edge-group">
        <path
          :d="edge.path"
          class="flow-edge"
          :class="{ show: revealed > Math.max(edge.fromIdx, edge.toIdx) }"
          :stroke="accent"
          :marker-end="`url(#${markerId})`"
          :style="`--len:${edge.len}`"
        />
        <text
          v-if="edge.label"
          :x="edge.mx" :y="edge.my"
          class="flow-edge-label"
          :class="{ show: revealed > Math.max(edge.fromIdx, edge.toIdx) }"
        >{{ edge.label }}</text>
      </g>

      <!-- 节点：依次淡入点亮 -->
      <g v-for="(node, i) in layoutNodes" :key="'n' + node.id"
         class="flow-node" :class="{ show: revealed > i }"
         :style="`--delay:${i * 0.45}s`">
        <rect
          :x="node.x" :y="node.y" :width="nodeW" :height="nodeH" rx="10"
          class="flow-node-box" :stroke="accent"
        />
        <text :x="node.x + nodeW / 2" :y="node.y + nodeH / 2"
              class="flow-node-label" text-anchor="middle" dominant-baseline="central"
        >{{ node.label }}</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'

const props = defineProps({
  // { type, nodes:[{id,label}], edges:[{from,to,label}] }
  diagram: { type: Object, default: () => ({}) },
  accent: { type: String, default: '#c5a059' }
})

// 唯一 marker id，避免多实例 <defs> 冲突
const markerId = 'arrow-' + Math.random().toString(36).slice(2, 8)

const nodeW = 96
const nodeH = 46
const gapX = 46
const gapY = 40
const perRow = 4

const nodes = computed(() => props.diagram?.nodes || [])
const edges = computed(() => props.diagram?.edges || [])

// 横向布局，超过 perRow 换行
const layoutNodes = computed(() => {
  const list = nodes.value
  const cols = Math.min(list.length, perRow)
  return list.map((n, i) => {
    const row = Math.floor(i / perRow)
    const col = i % perRow
    const rowCount = Math.min(list.length - row * perRow, perRow)
    // 行内居中
    const rowWidth = rowCount * nodeW + (rowCount - 1) * gapX
    const startX = (width.value - rowWidth) / 2
    return {
      ...n,
      x: startX + col * (nodeW + gapX),
      y: 24 + row * (nodeH + gapY)
    }
  })
})

const idIndex = computed(() => {
  const m = {}
  layoutNodes.value.forEach((n, i) => { m[n.id] = i })
  return m
})

const rows = computed(() => Math.ceil(nodes.value.length / perRow))
const width = ref(440)
const height = computed(() => 24 + rows.value * nodeH + (rows.value - 1) * gapY + 24)

// 边路径：从源节点边缘连到目标节点边缘
const layoutEdges = computed(() => {
  const ns = layoutNodes.value
  return edges.value.map(e => {
    const fromIdx = idIndex.value[e.from]
    const toIdx = idIndex.value[e.to]
    if (fromIdx == null || toIdx == null) return null
    const a = ns[fromIdx]
    const b = ns[toIdx]
    const ax = a.x + nodeW / 2
    const ay = a.y + nodeH / 2
    const bx = b.x + nodeW / 2
    const by = b.y + nodeH / 2
    // 从中心连线，端点收缩到盒子边缘附近
    const dx = bx - ax
    const dy = by - ay
    const dist = Math.hypot(dx, dy) || 1
    const ux = dx / dist
    const uy = dy / dist
    const pad = 28
    const x1 = ax + ux * pad
    const y1 = ay + uy * pad
    const x2 = bx - ux * pad
    const y2 = by - uy * pad
    const len = Math.hypot(x2 - x1, y2 - y1)
    return {
      path: `M ${x1.toFixed(1)} ${y1.toFixed(1)} L ${x2.toFixed(1)} ${y2.toFixed(1)}`,
      len: Math.ceil(len),
      mx: (x1 + x2) / 2,
      my: (y1 + y2) / 2 - 6,
      label: e.label || '',
      fromIdx,
      toIdx
    }
  }).filter(Boolean)
})

// 逐个点亮节点
const revealed = ref(0)
let timer = null
function play() {
  revealed.value = 0
  clearInterval(timer)
  timer = setInterval(() => {
    if (revealed.value >= nodes.value.length) {
      clearInterval(timer)
      return
    }
    revealed.value++
  }, 450)
}

watch(() => props.diagram, play, { deep: true })
onMounted(play)
onBeforeUnmount(() => clearInterval(timer))
</script>

<style scoped>
.flow-diagram {
  width: 100%;
  display: flex;
  justify-content: center;
}
.flow-svg {
  width: 100%;
  max-width: 460px;
  height: auto;
}

/* 节点 */
.flow-node {
  opacity: 0;
  transform: scale(0.85);
  transform-origin: center;
  transform-box: fill-box;
  transition: opacity 0.45s ease, transform 0.45s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.flow-node.show { opacity: 1; transform: scale(1); }
.flow-node-box {
  fill: rgba(255, 255, 255, 0.1);
  stroke-width: 2;
}
.flow-node-label {
  fill: #fff;
  font-size: 13px;
  font-weight: 700;
}

/* 边：dash 生长 */
.flow-edge {
  fill: none;
  stroke-width: 2.5;
  stroke-dasharray: var(--len);
  stroke-dashoffset: var(--len);
  opacity: 0;
  transition: stroke-dashoffset 0.55s ease, opacity 0.2s ease;
}
.flow-edge.show {
  opacity: 0.9;
  stroke-dashoffset: 0;
}
.flow-edge-label {
  fill: rgba(255, 255, 255, 0.85);
  font-size: 11px;
  font-weight: 600;
  text-anchor: middle;
  opacity: 0;
  transition: opacity 0.4s ease 0.3s;
}
.flow-edge-label.show { opacity: 1; }
</style>
