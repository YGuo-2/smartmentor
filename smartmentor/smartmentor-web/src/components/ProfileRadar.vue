<template>
  <div class="radar-wrap">
    <svg :viewBox="`0 0 ${size} ${size}`" class="radar-svg" preserveAspectRatio="xMidYMid meet">
      <!-- 同心网格 -->
      <polygon
        v-for="(ring, ri) in gridRings"
        :key="'ring' + ri"
        :points="ring"
        class="radar-grid"
      />
      <!-- 轴线 -->
      <line
        v-for="(p, i) in axisPoints"
        :key="'axis' + i"
        :x1="center" :y1="center" :x2="p.x" :y2="p.y"
        class="radar-axis"
      />
      <!-- 数据多边形 -->
      <polygon :points="dataPolygon" class="radar-area" />
      <!-- 数据顶点 -->
      <circle
        v-for="(p, i) in dataPoints"
        :key="'dot' + i"
        :cx="p.x" :cy="p.y" r="3.5"
        :fill="items[i].color"
        class="radar-dot"
      />
      <!-- 维度标签 + 分值 -->
      <g v-for="(p, i) in labelPoints" :key="'label' + i">
        <text
          :x="p.x" :y="p.y"
          :text-anchor="p.anchor"
          class="radar-label"
        >{{ items[i].label }}</text>
        <text
          :x="p.x" :y="p.y + 14"
          :text-anchor="p.anchor"
          class="radar-score"
          :fill="items[i].color"
        >{{ Math.round(items[i].value * 100) }}</text>
      </g>
    </svg>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  // [{ label, value(0-1), color }]
  dimensions: { type: Array, default: () => [] }
})

const size = 320
const center = size / 2
const radius = 110 // 数据区半径
const labelRadius = radius + 28

const items = computed(() => {
  const arr = props.dimensions && props.dimensions.length ? props.dimensions : []
  // 至少 3 个点才成形；不足时补默认，避免畸形
  return arr.map(d => ({
    label: d.label || '',
    value: Math.max(0, Math.min(1, Number(d.value) || 0)),
    color: d.color || '#c5a059'
  }))
})

const n = computed(() => items.value.length)

// 每个顶点的角度：从正上方开始顺时针
function angleAt(i) {
  return -Math.PI / 2 + (2 * Math.PI * i) / n.value
}

function pointAt(i, r) {
  const a = angleAt(i)
  return { x: center + r * Math.cos(a), y: center + r * Math.sin(a) }
}

const axisPoints = computed(() => items.value.map((_, i) => pointAt(i, radius)))

const dataPoints = computed(() => items.value.map((d, i) => pointAt(i, radius * d.value)))

const dataPolygon = computed(() =>
  dataPoints.value.map(p => `${p.x.toFixed(1)},${p.y.toFixed(1)}`).join(' ')
)

// 4 层网格环
const gridRings = computed(() => {
  const rings = []
  const levels = 4
  for (let lv = 1; lv <= levels; lv++) {
    const r = (radius * lv) / levels
    const pts = items.value.map((_, i) => {
      const p = pointAt(i, r)
      return `${p.x.toFixed(1)},${p.y.toFixed(1)}`
    }).join(' ')
    rings.push(pts)
  }
  return rings
})

const labelPoints = computed(() => items.value.map((_, i) => {
  const p = pointAt(i, labelRadius)
  let anchor = 'middle'
  if (p.x < center - 8) anchor = 'end'
  else if (p.x > center + 8) anchor = 'start'
  return { x: p.x, y: p.y, anchor }
}))
</script>

<style scoped>
.radar-wrap {
  display: flex;
  justify-content: center;
  padding: 8px;
}
.radar-svg {
  width: 100%;
  max-width: 360px;
  height: auto;
}
.radar-grid {
  fill: none;
  stroke: var(--border, #e2e8f0);
  stroke-width: 1;
}
.radar-axis {
  stroke: var(--border, #e2e8f0);
  stroke-width: 1;
}
.radar-area {
  fill: rgba(197, 160, 89, 0.20);
  stroke: var(--accent-hover, #a88748);
  stroke-width: 2;
  stroke-linejoin: round;
}
.radar-dot {
  stroke: #fff;
  stroke-width: 1.5;
}
.radar-label {
  font-size: 12px;
  font-weight: 600;
  fill: var(--text-secondary, #555);
}
.radar-score {
  font-size: 13px;
  font-weight: 800;
}
</style>
