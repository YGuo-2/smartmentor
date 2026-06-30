<template>
  <section class="knowledge-map" :class="{ embedded }">
    <header class="map-header">
      <div class="map-heading">
        <div class="map-icon"><i class="ri-node-tree"></i></div>
        <div>
          <h3>{{ title }}</h3>
          <div class="map-meta">
            <span>{{ filteredNodes.length }} 节点</span>
            <span>{{ visibleEdges.length }} 依赖</span>
            <span>{{ averageMastery }} 平均掌握</span>
          </div>
        </div>
      </div>

      <div class="map-actions">
        <div class="map-search">
          <i class="ri-search-line"></i>
          <input v-model.trim="query" type="search" placeholder="搜索知识点" />
        </div>
        <button type="button" class="icon-btn" title="缩小" @click="zoomBy(0.82)">
          <i class="ri-zoom-out-line"></i>
        </button>
        <button type="button" class="icon-btn" title="放大" @click="zoomBy(1.18)">
          <i class="ri-zoom-in-line"></i>
        </button>
        <button type="button" class="icon-btn" title="重置视图" @click="fitView">
          <i class="ri-focus-3-line"></i>
        </button>
      </div>
    </header>

    <div class="status-strip">
      <button
        v-for="option in filterOptions"
        :key="option.value"
        type="button"
        class="status-filter"
        :class="[option.value, { active: statusFilter === option.value }]"
        @click="statusFilter = option.value"
      >
        <i :class="option.icon"></i>
        <span>{{ option.label }}</span>
        <b>{{ option.count }}</b>
      </button>
    </div>

    <div class="map-body">
      <div class="map-stage">
        <div class="stage-topline">
          <div class="stage-route" v-if="stageFocusNode">
            <span>当前焦点</span>
            <strong>{{ stageFocusNode.name }}</strong>
          </div>
          <div class="stage-route muted" v-else>
            <span>{{ query || statusFilter !== 'all' ? '筛选结果' : '薄弱优先' }}</span>
            <strong>{{ query || statusFilter !== 'all' ? `${filteredNodes.length} 个节点` : (priorityNodes[0]?.name || '暂无待处理节点') }}</strong>
          </div>
        </div>

        <div class="stage-canvas" ref="stageRef">
          <div v-if="loading" class="stage-empty">
            <span class="spinner"></span>
            <p>加载学情地图中...</p>
          </div>
          <div v-else-if="!layoutNodes.length" class="stage-empty">
            <i class="ri-node-tree"></i>
            <p>暂无可视化的知识点数据</p>
          </div>

          <svg
            v-show="!loading && layoutNodes.length"
            ref="svgRef"
            class="map-svg"
            :viewBox="viewBox"
            role="img"
            aria-label="交互式学情地图"
          >
            <defs>
              <marker id="knowledge-map-arrow" viewBox="0 0 10 10" refX="8" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse">
                <path d="M 0 0 L 10 5 L 0 10 z" class="map-arrow-head" />
              </marker>
            </defs>

            <g :transform="zoomTransform">
              <path
                v-for="edge in layoutEdges"
                :key="edge.key"
                class="map-edge"
                :class="{ cross: edge.crossModule, active: edgeTouchesSelection(edge) }"
                :d="edge.d"
                marker-end="url(#knowledge-map-arrow)"
              />

              <g
                v-for="node in layoutNodes"
                :key="node.id"
                class="map-node"
                :class="[node.masteryLevel, {
                  selected: selectedNode?.id === node.id,
                  current: node.isCurrent,
                  inPath: node.inPath,
                  locked: node.pathStatus === 'locked'
                }]"
                :transform="`translate(${node.x}, ${node.y})`"
              >
                <foreignObject :width="nodeWidth" :height="nodeHeight">
                  <button
                    xmlns="http://www.w3.org/1999/xhtml"
                    type="button"
                    class="node-card"
                    @click.stop="selectNode(node)"
                    @keydown.enter.prevent="selectNode(node)"
                    @keydown.space.prevent="selectNode(node)"
                  >
                    <span class="node-card-top">
                      <span class="node-title">{{ node.name }}</span>
                      <span v-if="node.inPath" class="node-path-chip">{{ node.pathMark }}</span>
                    </span>
                    <span class="node-card-bottom">
                      <span class="node-status">{{ node.masteryLabel }}</span>
                      <span class="node-percent">{{ node.masteryPercent }}</span>
                    </span>
                    <span class="node-progress">
                      <span :style="{ width: node.masteryPercent }"></span>
                    </span>
                  </button>
                </foreignObject>
              </g>
            </g>
          </svg>
        </div>
      </div>

      <aside class="insight-panel">
        <template v-if="selectedNode">
          <div class="panel-head">
            <div>
              <div class="panel-kicker">{{ selectedNode.module || '课程知识点' }}</div>
              <h4>{{ selectedNode.name }}</h4>
            </div>
            <button type="button" class="panel-close" title="关闭" @click="selectedId = ''">
              <i class="ri-close-line"></i>
            </button>
          </div>

          <div class="focus-score" :class="selectedNode.masteryLevel">
            <div>
              <span>掌握度</span>
              <strong>{{ selectedNode.masteryPercent }}</strong>
            </div>
            <div class="score-track">
              <span :style="{ width: selectedNode.masteryPercent }"></span>
            </div>
          </div>

          <div class="panel-badges">
            <span class="badge" :class="selectedNode.badgeClass">{{ selectedNode.masteryLabel }}</span>
            <span v-if="selectedNode.pathStatusLabel" class="badge badge-info">{{ selectedNode.pathStatusLabel }}</span>
          </div>

          <div class="panel-section" v-if="selectedPrerequisites.length">
            <div class="panel-label">前置知识</div>
            <div class="chip-list">
              <button
                v-for="item in selectedPrerequisites"
                :key="item.id"
                type="button"
                class="node-chip"
                @click="selectById(item.id)"
              >{{ item.name }}</button>
            </div>
          </div>

          <div class="panel-section" v-if="selectedDependents.length">
            <div class="panel-label">影响后续</div>
            <div class="chip-list">
              <button
                v-for="item in selectedDependents"
                :key="item.id"
                type="button"
                class="node-chip"
                @click="selectById(item.id)"
              >{{ item.name }}</button>
            </div>
          </div>

          <div class="panel-section" v-if="selectedNode.reason">
            <div class="panel-label">学习依据</div>
            <p class="panel-note">{{ selectedNode.reason }}</p>
          </div>

          <div class="panel-actions">
            <button
              v-if="canEnterLesson"
              type="button"
              class="btn btn-dark btn-sm"
              @click="enterLesson"
            >
              <i class="ri-play-fill"></i> 进入学习
            </button>
            <button type="button" class="btn btn-outline btn-sm" @click="goLearning">
              <i class="ri-route-line"></i> 学习路径
            </button>
            <button v-if="selectedNode.masteryLevel === 'weak'" type="button" class="btn btn-outline btn-sm" @click="goDiagnostic">
              <i class="ri-test-tube-line"></i> 诊断补弱
            </button>
          </div>
        </template>

        <template v-else>
          <div class="panel-head compact">
            <div>
              <div class="panel-kicker">优先处理</div>
              <h4>薄弱知识点</h4>
            </div>
          </div>
          <div class="priority-list" v-if="priorityNodes.length">
            <button
              v-for="node in priorityNodes"
              :key="node.id"
              type="button"
              class="priority-item"
              :class="node.masteryLevel"
              @click="selectById(node.id)"
            >
              <span>
                <strong>{{ node.name }}</strong>
                <small>{{ node.module || '课程知识点' }}</small>
              </span>
              <b>{{ node.masteryPercent }}</b>
            </button>
          </div>
          <div v-else class="panel-empty">
            <i class="ri-checkbox-circle-line"></i>
            <span>暂无薄弱节点</span>
          </div>
        </template>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import * as d3 from 'd3'

const props = defineProps({
  title: { type: String, default: '交互式学情地图' },
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  pathNodes: { type: Array, default: () => [] },
  pathId: { type: [String, Number], default: '' },
  focusKnowledgePointId: { type: String, default: '' },
  loading: { type: Boolean, default: false },
  embedded: { type: Boolean, default: false }
})

const router = useRouter()
const svgRef = ref(null)
const stageRef = ref(null)
const canvasSize = ref({ width: 980, height: 566 })
const selectedId = ref('')
const query = ref('')
const statusFilter = ref('all')
const zoomTransform = ref('translate(0,0) scale(1)')
let zoomBehavior = null
let resizeObserver = null
let fitFrame = 0

const nodeWidth = 176
const nodeHeight = 84
const colGap = 42
const rowGap = 16
const padX = 32
const padY = 32

const pathByKnowledgeId = computed(() => {
  const index = new Map()
  for (const raw of props.pathNodes || []) {
    const kpId = stringValue(raw.knowledgePointId || raw.id)
    const nodeId = stringValue(raw.nodeId)
    if (kpId) index.set(kpId, raw)
    if (nodeId && !index.has(nodeId)) index.set(nodeId, raw)
  }
  return index
})

const normalizedNodes = computed(() => {
  return (props.nodes || [])
    .map(raw => {
      const id = stringValue(raw.id || raw.knowledgePointId || raw.nodeId)
      if (!id) return null
      const pathNode = pathByKnowledgeId.value.get(id)
      const mastery = normalizeMastery(raw.mastery ?? raw.currentMastery)
      const masteryLevel = normalizeMasteryLevel(raw.masteryLevel || raw.status, mastery)
      const pathStatus = stringValue(pathNode?.status)
      const explicitDepth = Number(raw.depth ?? raw.level)
      const isCurrent = pathStatus === 'in_progress'
      const name = stringValue(raw.name || raw.knowledgePointName || raw.knowledgePoint || raw.title || id)
      return {
        ...raw,
        id,
        name,
        module: stringValue(raw.module),
        depth: Number.isFinite(explicitDepth) ? Math.max(0, explicitDepth) : null,
        difficulty: raw.difficulty,
        mastery,
        masteryLevel,
        masteryPercent: Math.round(mastery * 100) + '%',
        masteryLabel: masteryLabel(masteryLevel),
        badgeClass: masteryBadgeClass(masteryLevel),
        pathNode,
        pathNodeId: stringValue(pathNode?.nodeId),
        pathStatus,
        pathStatusLabel: pathStatusLabel(pathStatus),
        inPath: Boolean(pathNode),
        isCurrent,
        pathMark: isCurrent ? '当前' : pathMark(pathStatus),
        reason: stringValue(pathNode?.reason || raw.reason)
      }
    })
    .filter(Boolean)
})

const nodeIndex = computed(() => {
  const index = new Map()
  for (const node of normalizedNodes.value) index.set(node.id, node)
  return index
})

const normalizedEdges = computed(() => {
  return (props.edges || [])
    .map((raw, index) => {
      const source = stringValue(raw.source || raw.from)
      const target = stringValue(raw.target || raw.to)
      if (!source || !target) return null
      return {
        ...raw,
        source,
        target,
        key: `${source}-${target}-${index}`,
        crossModule: Boolean(raw.crossModule || raw.type === 'cross_module')
      }
    })
    .filter(Boolean)
})

const counts = computed(() => {
  const result = { all: normalizedNodes.value.length, weak: 0, learning: 0, mastered: 0, path: 0 }
  for (const node of normalizedNodes.value) {
    if (node.masteryLevel === 'weak') result.weak++
    if (node.masteryLevel === 'learning') result.learning++
    if (node.masteryLevel === 'mastered') result.mastered++
    if (node.inPath) result.path++
  }
  return result
})

const filterOptions = computed(() => {
  const options = [
    { value: 'all', label: '全部', count: counts.value.all, icon: 'ri-apps-2-line' },
    { value: 'weak', label: '薄弱', count: counts.value.weak, icon: 'ri-alarm-warning-line' },
    { value: 'learning', label: '学习中', count: counts.value.learning, icon: 'ri-loader-4-line' },
    { value: 'mastered', label: '已掌握', count: counts.value.mastered, icon: 'ri-checkbox-circle-line' }
  ]
  if (counts.value.path > 0) {
    options.push({ value: 'path', label: '当前路径', count: counts.value.path, icon: 'ri-route-line' })
  }
  return options
})

const filteredNodes = computed(() => {
  const q = query.value.toLowerCase()
  return normalizedNodes.value.filter(node => {
    if (statusFilter.value === 'path' && !node.inPath) return false
    if (['weak', 'learning', 'mastered'].includes(statusFilter.value) && node.masteryLevel !== statusFilter.value) return false
    if (!q) return true
    return [node.name, node.id, node.module].some(value => stringValue(value).toLowerCase().includes(q))
  })
})

const visibleNodeIds = computed(() => new Set(filteredNodes.value.map(node => node.id)))

const visibleEdges = computed(() => {
  return normalizedEdges.value.filter(edge =>
    visibleNodeIds.value.has(edge.source) && visibleNodeIds.value.has(edge.target))
})

const averageMastery = computed(() => {
  if (!filteredNodes.value.length) return '0%'
  const sum = filteredNodes.value.reduce((total, node) => total + node.mastery, 0)
  return Math.round((sum / filteredNodes.value.length) * 100) + '%'
})

const currentNode = computed(() => normalizedNodes.value.find(node => node.isCurrent) || null)

const priorityNodes = computed(() => {
  return normalizedNodes.value
    .filter(node => node.masteryLevel === 'weak' || node.isCurrent || node.pathStatus === 'failed' || isActionablePathNode(node.pathStatus))
    .sort((a, b) => {
      if (a.isCurrent !== b.isCurrent) return a.isCurrent ? -1 : 1
      if (a.pathStatus === 'failed' !== (b.pathStatus === 'failed')) return a.pathStatus === 'failed' ? -1 : 1
      if (a.inPath !== b.inPath) return a.inPath ? -1 : 1
      return a.mastery - b.mastery
    })
    .slice(0, 6)
})

const layoutDepth = computed(() => {
  const depth = new Map()
  const indegree = new Map()
  const outgoing = new Map()

  for (const node of filteredNodes.value) {
    depth.set(node.id, Number.isFinite(node.depth) ? node.depth : 0)
    indegree.set(node.id, 0)
    outgoing.set(node.id, [])
  }

  for (const edge of visibleEdges.value) {
    if (!outgoing.has(edge.source) || !indegree.has(edge.target)) continue
    outgoing.get(edge.source).push(edge.target)
    indegree.set(edge.target, (indegree.get(edge.target) || 0) + 1)
  }

  const queue = filteredNodes.value
    .filter(node => (indegree.get(node.id) || 0) === 0)
    .sort((a, b) => a.name.localeCompare(b.name, 'zh-CN'))
    .map(node => node.id)
  const visited = new Set()

  while (queue.length) {
    const source = queue.shift()
    visited.add(source)
    for (const target of outgoing.get(source) || []) {
      const targetNode = nodeIndex.value.get(target)
      if (!targetNode || Number.isFinite(targetNode.depth)) {
        indegree.set(target, (indegree.get(target) || 0) - 1)
        if ((indegree.get(target) || 0) === 0) queue.push(target)
        continue
      }
      depth.set(target, Math.max(depth.get(target) || 0, (depth.get(source) || 0) + 1))
      indegree.set(target, (indegree.get(target) || 0) - 1)
      if ((indegree.get(target) || 0) === 0) queue.push(target)
    }
  }

  // Cycles stay in their last stable column instead of stretching the map on every pass.
  for (const node of filteredNodes.value) {
    if (!visited.has(node.id) && !Number.isFinite(node.depth) && !depth.has(node.id)) depth.set(node.id, 0)
  }

  return depth
})

const layoutNodes = computed(() => {
  const groups = new Map()
  for (const node of filteredNodes.value) {
    const depth = Number.isFinite(node.depth) ? node.depth : (layoutDepth.value.get(node.id) || 0)
    if (!groups.has(depth)) groups.set(depth, [])
    groups.get(depth).push(node)
  }

  const positioned = []
  const depths = Array.from(groups.keys()).sort((a, b) => a - b)
  for (const depth of depths) {
    const group = groups.get(depth)
      .slice()
      .sort((a, b) => {
        if (a.isCurrent !== b.isCurrent) return a.isCurrent ? -1 : 1
        if (a.inPath !== b.inPath) return a.inPath ? -1 : 1
        if (a.mastery !== b.mastery) return a.mastery - b.mastery
        return a.name.localeCompare(b.name, 'zh-CN')
      })
    group.forEach((node, row) => {
      positioned.push({
        ...node,
        x: padX + depth * (nodeWidth + colGap),
        y: padY + row * (nodeHeight + rowGap)
      })
    })
  }
  return positioned
})

const layoutNodeIndex = computed(() => {
  const index = new Map()
  for (const node of layoutNodes.value) index.set(node.id, node)
  return index
})

const layoutEdges = computed(() => {
  return visibleEdges.value
    .map(edge => {
      const source = layoutNodeIndex.value.get(edge.source)
      const target = layoutNodeIndex.value.get(edge.target)
      if (!source || !target) return null
      const x1 = source.x + nodeWidth
      const y1 = source.y + nodeHeight / 2
      const x2 = target.x
      const y2 = target.y + nodeHeight / 2
      const mid = Math.max(32, (x2 - x1) / 2)
      return {
        ...edge,
        d: `M ${x1} ${y1} C ${x1 + mid} ${y1}, ${x2 - mid} ${y2}, ${x2} ${y2}`
      }
    })
    .filter(Boolean)
})

const viewBox = computed(() => {
  return `0 0 ${canvasSize.value.width} ${canvasSize.value.height}`
})

const graphBounds = computed(() => {
  if (!layoutNodes.value.length) {
    return {
      minX: 0,
      minY: 0,
      maxX: canvasSize.value.width,
      maxY: canvasSize.value.height,
      width: canvasSize.value.width,
      height: canvasSize.value.height
    }
  }
  const minX = Math.min(...layoutNodes.value.map(node => node.x)) - 22
  const minY = Math.min(...layoutNodes.value.map(node => node.y)) - 22
  const maxX = Math.max(...layoutNodes.value.map(node => node.x)) + nodeWidth + 22
  const maxY = Math.max(...layoutNodes.value.map(node => node.y)) + nodeHeight + 22
  return {
    minX,
    minY,
    maxX,
    maxY,
    width: Math.max(1, maxX - minX),
    height: Math.max(1, maxY - minY)
  }
})

const selectedNode = computed(() => {
  if (!selectedId.value) return null
  return layoutNodeIndex.value.get(selectedId.value) || nodeIndex.value.get(selectedId.value) || null
})

const stageFocusNode = computed(() => {
  if (selectedId.value && visibleNodeIds.value.has(selectedId.value)) return selectedNode.value
  if (currentNode.value && visibleNodeIds.value.has(currentNode.value.id)) return currentNode.value
  return null
})

const selectedPrerequisites = computed(() => {
  if (!selectedNode.value) return []
  return normalizedEdges.value
    .filter(edge => edge.target === selectedNode.value.id)
    .map(edge => nodeIndex.value.get(edge.source))
    .filter(Boolean)
    .slice(0, 8)
})

const selectedDependents = computed(() => {
  if (!selectedNode.value) return []
  return normalizedEdges.value
    .filter(edge => edge.source === selectedNode.value.id)
    .map(edge => nodeIndex.value.get(edge.target))
    .filter(Boolean)
    .slice(0, 8)
})

const canEnterLesson = computed(() => {
  if (!props.pathId || !selectedNode.value?.pathNodeId) return false
  return selectedNode.value.pathStatus !== 'locked'
})

function selectNode(node) {
  selectedId.value = node.id
}

function selectById(id) {
  selectedId.value = id
  if (statusFilter.value !== 'all' && !visibleNodeIds.value.has(id)) {
    statusFilter.value = 'all'
  }
}

function edgeTouchesSelection(edge) {
  return selectedId.value && (edge.source === selectedId.value || edge.target === selectedId.value)
}

function enterLesson() {
  if (!canEnterLesson.value) return
  router.push(`/learning/${props.pathId}/${selectedNode.value.pathNodeId}`)
}

function goLearning() {
  router.push('/learning')
}

function goDiagnostic() {
  router.push('/diagnostic')
}

function setupZoom() {
  if (!svgRef.value) return
  zoomBehavior = d3.zoom()
    .scaleExtent([0.42, 2.8])
    .on('zoom', event => {
      zoomTransform.value = event.transform.toString()
    })
  d3.select(svgRef.value).call(zoomBehavior)
}

function updateCanvasSize() {
  const rect = (stageRef.value || svgRef.value)?.getBoundingClientRect()
  if (!rect?.width || !rect?.height) return
  const width = Math.max(320, Math.round(rect.width))
  const height = Math.max(480, Math.round(rect.height))
  if (canvasSize.value.width !== width || canvasSize.value.height !== height) {
    canvasSize.value = { width, height }
  }
}

function scheduleFitView() {
  if (fitFrame) cancelAnimationFrame(fitFrame)
  fitFrame = requestAnimationFrame(() => {
    fitFrame = 0
    fitView()
  })
}

function zoomBy(factor) {
  if (!svgRef.value || !zoomBehavior) return
  d3.select(svgRef.value)
    .transition()
    .duration(180)
    .call(zoomBehavior.scaleBy, factor)
}

function fitView() {
  if (!svgRef.value || !zoomBehavior || !layoutNodes.value.length) return
  updateCanvasSize()
  const { width, height } = canvasSize.value
  const bounds = graphBounds.value
  if (!isFinitePositive(width) || !isFinitePositive(height) || !isFinitePositive(bounds.width) || !isFinitePositive(bounds.height)) {
    return
  }
  const margin = width < 520 ? 20 : 32
  const fitScale = Math.min((width - margin * 2) / bounds.width, (height - margin * 2) / bounds.height)
  const canShowOverview = width >= 900 && fitScale >= 0.58
  const minReadableScale = width < 520 ? 0.72 : 0.78
  const scale = canShowOverview
    ? Math.min(1.08, fitScale || 1)
    : Math.max(minReadableScale, Math.min(1.08, fitScale || 1))
  if (!isFinitePositive(scale)) return
  const focus = selectedNode.value || currentNode.value || priorityNodes.value[0] || layoutNodes.value[0]
  const shouldFocus = !canShowOverview && focus && scale > fitScale + 0.01
  const centerX = shouldFocus ? focus.x + nodeWidth / 2 : bounds.minX + bounds.width / 2
  const centerY = shouldFocus ? focus.y + nodeHeight / 2 : bounds.minY + bounds.height / 2
  let translateX = width / 2 - centerX * scale
  let translateY = height / 2 - centerY * scale

  if (bounds.width * scale <= width - margin * 2) {
    translateX = (width - bounds.width * scale) / 2 - bounds.minX * scale
  } else {
    translateX = clamp(translateX, width - bounds.maxX * scale - margin, margin - bounds.minX * scale)
  }

  if (bounds.height * scale <= height - margin * 2) {
    translateY = (height - bounds.height * scale) / 2 - bounds.minY * scale
  } else {
    translateY = clamp(translateY, height - bounds.maxY * scale - margin, margin - bounds.minY * scale)
  }
  if (!Number.isFinite(translateX) || !Number.isFinite(translateY)) return

  const transform = d3.zoomIdentity.translate(translateX, translateY).scale(scale)
  d3.select(svgRef.value)
    .transition()
    .duration(220)
    .call(zoomBehavior.transform, transform)
}

function syncFocus() {
  const focus = stringValue(props.focusKnowledgePointId)
  if (focus && nodeIndex.value.has(focus)) {
    selectedId.value = focus
    return
  }
  const current = normalizedNodes.value.find(node => node.isCurrent)
  if (!selectedId.value && current) {
    selectedId.value = current.id
  }
}

function stringValue(value) {
  if (value === undefined || value === null) return ''
  const text = String(value).trim()
  return text === 'null' || text === 'undefined' ? '' : text
}

function clamp(value, min, max) {
  if (min > max) return value
  return Math.max(min, Math.min(max, value))
}

function isFinitePositive(value) {
  return Number.isFinite(value) && value > 0
}

function normalizeMastery(value) {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return 0
  return Math.max(0, Math.min(1, numeric > 1 ? numeric / 100 : numeric))
}

function normalizeMasteryLevel(value, mastery) {
  const raw = stringValue(value)
  if (['mastered', 'completed'].includes(raw)) return 'mastered'
  if (['learning', 'in_progress', 'unlocked', 'pending', 'available'].includes(raw)) return 'learning'
  if (['weak', 'failed'].includes(raw)) return 'weak'
  if (mastery >= 0.8) return 'mastered'
  if (mastery >= 0.5) return 'learning'
  if (mastery > 0) return 'weak'
  return 'unknown'
}

function masteryLabel(level) {
  return {
    mastered: '已掌握',
    learning: '学习中',
    weak: '薄弱',
    unknown: '未开始'
  }[level] || '未开始'
}

function masteryBadgeClass(level) {
  return {
    mastered: 'badge-green',
    learning: 'badge-yellow',
    weak: 'badge-red',
    unknown: 'badge-muted'
  }[level] || 'badge-muted'
}

function pathStatusLabel(status) {
  return {
    completed: '路径已完成',
    in_progress: '当前学习',
    unlocked: '可学习',
    pending: '可学习',
    available: '可学习',
    locked: '未解锁',
    failed: '待补救'
  }[status] || ''
}

function pathMark(status) {
  return {
    completed: '✓',
    locked: '锁',
    failed: '!',
    unlocked: '可学',
    pending: '可学',
    available: '可学'
  }[status] || '路径'
}

function isActionablePathNode(status) {
  return ['unlocked', 'pending', 'available'].includes(status)
}

watch(() => [props.nodes, props.edges, props.pathNodes, props.focusKnowledgePointId], async () => {
  syncFocus()
  await nextTick()
  scheduleFitView()
}, { deep: true })

watch(filteredNodes, () => {
  if (selectedId.value && !visibleNodeIds.value.has(selectedId.value)) {
    selectedId.value = ''
  }
  nextTick(scheduleFitView)
})

watch(canvasSize, () => {
  nextTick(scheduleFitView)
})

watch(() => counts.value.path, pathCount => {
  if (statusFilter.value === 'path' && pathCount === 0) {
    statusFilter.value = 'all'
  }
})

onMounted(async () => {
  updateCanvasSize()
  if (typeof ResizeObserver !== 'undefined' && stageRef.value) {
    resizeObserver = new ResizeObserver(() => {
      updateCanvasSize()
    })
    resizeObserver.observe(stageRef.value)
  }
  setupZoom()
  syncFocus()
  await nextTick()
  scheduleFitView()
})

onBeforeUnmount(() => {
  if (fitFrame) cancelAnimationFrame(fitFrame)
  resizeObserver?.disconnect()
  if (svgRef.value) {
    d3.select(svgRef.value).on('.zoom', null)
  }
})
</script>

<style scoped>
.knowledge-map {
  width: 100%;
  min-width: 0;
  border: 1px solid #d9e0e8;
  border-radius: 10px;
  background: #ffffff;
  overflow: hidden;
  box-shadow: 0 12px 30px rgba(17, 34, 64, 0.06);
}
.knowledge-map.embedded {
  border: 0;
  border-radius: 0;
  box-shadow: none;
}
.map-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid #e3e8ee;
  background: #ffffff;
}
.map-heading,
.map-actions {
  display: flex;
  align-items: center;
  min-width: 0;
}
.map-heading { gap: 12px; }
.map-icon {
  width: 40px;
  height: 40px;
  display: grid;
  place-items: center;
  border-radius: 8px;
  background: #e9f2fb;
  color: var(--info);
  font-size: 1.15rem;
  flex-shrink: 0;
}
.map-heading h3 {
  margin: 0;
  color: var(--text);
  font-size: 1rem;
  line-height: 1.25;
}
.map-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 5px;
  color: var(--text-muted);
  font-size: 0.76rem;
}
.map-actions {
  justify-content: flex-end;
  gap: 8px;
  flex-wrap: wrap;
}
.map-search {
  width: 230px;
  height: 36px;
  display: flex;
  align-items: center;
  gap: 7px;
  padding: 0 10px;
  border: 1px solid #d9e0e8;
  border-radius: 8px;
  background: #f8fafc;
}
.map-search i { color: var(--text-muted); }
.map-search input {
  width: 100%;
  min-width: 0;
  border: 0;
  outline: none;
  background: transparent;
  color: var(--text);
  font-size: 0.84rem;
}
.icon-btn {
  width: 36px;
  height: 36px;
  display: inline-grid;
  place-items: center;
  border: 1px solid #d9e0e8;
  border-radius: 8px;
  background: #fff;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.16s ease;
}
.icon-btn:hover {
  color: var(--info);
  border-color: rgba(43, 108, 176, 0.42);
  background: #f2f7fc;
}
.status-strip {
  display: flex;
  gap: 8px;
  padding: 12px 20px;
  border-bottom: 1px solid #e3e8ee;
  background: #f8fafc;
  overflow-x: auto;
}
.status-filter {
  min-width: 112px;
  height: 38px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 0 12px;
  border: 1px solid #d9e0e8;
  border-radius: 8px;
  background: #fff;
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 700;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.16s ease;
}
.status-filter b {
  color: var(--text);
  font-size: 0.78rem;
}
.status-filter:hover,
.status-filter.active {
  border-color: rgba(43, 108, 176, 0.45);
  color: var(--text);
  background: #eef6ff;
}
.status-filter.weak.active {
  border-color: rgba(155, 44, 44, 0.32);
  background: #fff1f1;
}
.status-filter.mastered.active {
  border-color: rgba(39, 103, 73, 0.32);
  background: #effaf5;
}
.status-filter.learning.active {
  border-color: rgba(197, 160, 89, 0.38);
  background: #fff8ea;
}
.map-body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  min-height: 620px;
}
.map-stage {
  min-width: 0;
  background: #f6f8fb;
  border-right: 1px solid #e3e8ee;
}
.stage-topline {
  height: 54px;
  display: flex;
  align-items: center;
  padding: 0 18px;
  border-bottom: 1px solid #e3e8ee;
  background: #ffffff;
}
.stage-route {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
  font-size: 0.82rem;
}
.stage-route span {
  color: var(--text-muted);
  font-weight: 700;
}
.stage-route strong {
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.stage-route.muted strong { color: var(--text-secondary); }
.stage-canvas {
  position: relative;
  min-height: 566px;
  background:
    linear-gradient(#eef2f7 1px, transparent 1px),
    linear-gradient(90deg, #eef2f7 1px, transparent 1px);
  background-size: 28px 28px;
  overflow: hidden;
}
.map-svg {
  width: 100%;
  height: 566px;
  display: block;
  cursor: grab;
}
.map-svg:active { cursor: grabbing; }
.map-edge {
  fill: none;
  stroke: #b2bfcc;
  stroke-width: 2.2;
  stroke-linecap: round;
  opacity: 0.76;
}
.map-edge.cross {
  stroke: var(--accent);
  stroke-dasharray: 7 6;
}
.map-edge.active {
  stroke: var(--info);
  stroke-width: 3.3;
  opacity: 1;
}
.map-arrow-head { fill: #b2bfcc; }
.map-node {
  overflow: visible;
}
.map-node.locked {
  opacity: 0.58;
}
.node-card {
  width: 100%;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 8px;
  padding: 12px 13px 11px;
  border: 1px solid #d9e0e8;
  border-left: 5px solid var(--text-muted);
  border-radius: 8px;
  background: #ffffff;
  color: var(--text);
  cursor: pointer;
  text-align: left;
  box-shadow: 0 8px 18px rgba(17, 34, 64, 0.08);
  transition: border-color 0.16s ease, box-shadow 0.16s ease, transform 0.16s ease;
}
.map-node.mastered .node-card { border-left-color: var(--success); }
.map-node.learning .node-card { border-left-color: var(--accent); }
.map-node.weak .node-card { border-left-color: var(--danger); }
.map-node.unknown .node-card { border-left-color: #9aa7b5; }
.map-node.inPath .node-card {
  border-color: rgba(43, 108, 176, 0.45);
}
.map-node.current .node-card {
  border-color: rgba(197, 160, 89, 0.88);
  box-shadow: 0 0 0 4px rgba(197, 160, 89, 0.14), 0 10px 24px rgba(17, 34, 64, 0.12);
}
.map-node.selected .node-card,
.node-card:hover,
.node-card:focus {
  border-color: rgba(43, 108, 176, 0.72);
  box-shadow: 0 0 0 4px rgba(43, 108, 176, 0.12), 0 12px 26px rgba(17, 34, 64, 0.12);
  outline: none;
}
.node-card-top,
.node-card-bottom {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}
.node-title {
  min-width: 0;
  color: var(--text);
  font-size: 0.82rem;
  font-weight: 800;
  line-height: 1.25;
  word-break: break-word;
}
.node-path-chip {
  flex-shrink: 0;
  border-radius: 6px;
  padding: 2px 6px;
  background: #e9f2fb;
  color: var(--info);
  font-size: 0.68rem;
  font-weight: 800;
}
.node-status,
.node-percent {
  color: var(--text-secondary);
  font-size: 0.72rem;
  font-weight: 800;
}
.node-percent { color: var(--text); }
.node-progress {
  display: block;
  height: 6px;
  border-radius: 999px;
  background: #e6ebf1;
  overflow: hidden;
}
.node-progress span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--text-muted);
}
.map-node.mastered .node-progress span { background: var(--success); }
.map-node.learning .node-progress span { background: var(--accent); }
.map-node.weak .node-progress span { background: var(--danger); }
.insight-panel {
  min-width: 0;
  padding: 20px;
  background: #ffffff;
}
.panel-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}
.panel-head.compact { margin-bottom: 14px; }
.panel-kicker {
  color: var(--info);
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 1px;
}
.panel-head h4 {
  margin: 7px 0 0;
  color: var(--text);
  font-size: 1.05rem;
  line-height: 1.35;
}
.panel-close {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border: 0;
  border-radius: 7px;
  background: #f1f4f8;
  color: var(--text-muted);
  cursor: pointer;
  flex-shrink: 0;
}
.panel-close:hover { color: var(--text); }
.focus-score {
  margin: 18px 0 12px;
  padding: 14px;
  border-radius: 8px;
  border: 1px solid #e1e7ee;
  background: #f8fafc;
}
.focus-score > div:first-child {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 0.82rem;
}
.focus-score strong {
  color: var(--text);
  font-size: 1.2rem;
}
.score-track {
  height: 8px;
  margin-top: 10px;
  border-radius: 999px;
  background: #e3e9ef;
  overflow: hidden;
}
.score-track span {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--text-muted);
}
.focus-score.mastered .score-track span { background: var(--success); }
.focus-score.learning .score-track span { background: var(--accent); }
.focus-score.weak .score-track span { background: var(--danger); }
.panel-badges,
.panel-actions,
.chip-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.panel-section {
  margin-top: 18px;
}
.panel-label {
  margin-bottom: 8px;
  color: var(--text-muted);
  font-size: 0.72rem;
  font-weight: 900;
  letter-spacing: 1px;
}
.node-chip {
  max-width: 100%;
  display: inline-flex;
  padding: 6px 9px;
  border: 1px solid #d9e0e8;
  border-radius: 7px;
  background: #fff;
  color: var(--text-secondary);
  font-size: 0.75rem;
  cursor: pointer;
}
.node-chip:hover {
  color: var(--info);
  border-color: rgba(43, 108, 176, 0.45);
  background: #f2f7fc;
}
.panel-note {
  margin: 0;
  color: var(--text-secondary);
  font-size: 0.8rem;
  line-height: 1.7;
}
.panel-actions {
  margin-top: 20px;
}
.priority-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.priority-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px;
  border: 1px solid #d9e0e8;
  border-left: 4px solid var(--text-muted);
  border-radius: 8px;
  background: #fff;
  cursor: pointer;
  text-align: left;
}
.priority-item:hover {
  border-color: rgba(43, 108, 176, 0.45);
  background: #f8fbff;
}
.priority-item.weak { border-left-color: var(--danger); }
.priority-item.learning { border-left-color: var(--accent); }
.priority-item.mastered { border-left-color: var(--success); }
.priority-item span {
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.priority-item strong {
  color: var(--text);
  font-size: 0.82rem;
  line-height: 1.3;
}
.priority-item small {
  color: var(--text-muted);
  font-size: 0.72rem;
}
.priority-item b {
  color: var(--text);
  font-size: 0.88rem;
  flex-shrink: 0;
}
.panel-empty,
.stage-empty {
  display: grid;
  place-content: center;
  justify-items: center;
  gap: 10px;
  color: var(--text-muted);
  text-align: center;
}
.stage-empty {
  position: absolute;
  inset: 0;
  font-size: 0.88rem;
}
.stage-empty i,
.panel-empty i {
  color: var(--info);
  font-size: 2rem;
}

@media (max-width: 1120px) {
  .map-body {
    grid-template-columns: 1fr;
  }
  .map-stage {
    border-right: 0;
  }
  .insight-panel {
    border-top: 1px solid #e3e8ee;
  }
}

@media (max-width: 760px) {
  .map-header {
    align-items: stretch;
    flex-direction: column;
  }
  .map-actions {
    justify-content: flex-start;
  }
  .map-search {
    width: 100%;
  }
  .status-filter {
    min-width: 104px;
  }
  .stage-canvas,
  .map-svg {
    min-height: 500px;
    height: 500px;
  }
}
</style>
