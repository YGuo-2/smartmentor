<template>
  <div class="tracing-result-page">

    <div class="main-content" v-if="!loading && result">
      <div class="page-wrap-sm">

        <!-- Hero Summary -->
        <div class="card card-dark tracing-hero anim-1">
          <div class="tracing-hero-glow"></div>
          <div class="tracing-hero-left">
            <div class="page-hero-label" style="color:var(--accent)">
              <i class="ri-mind-map"></i> 知识溯因分析
            </div>
            <div class="hero-stat-number accent" style="font-size:2.5rem;margin:8px 0 4px">
              {{ result.rootCauseCount }} 个根因
            </div>
            <div class="hero-stat-label" style="color:rgba(255,255,255,0.5)">
              共分析 {{ result.analyzedPointCount }} 个薄弱知识点
            </div>
            <div class="tracing-hero-meta">
              <span><i class="ri-node-tree"></i> 深度溯源</span>
              <span v-if="result.isCrossModule"><i class="ri-exchange-line"></i> 跨模块关联</span>
              <span><i class="ri-calendar-line"></i> {{ formatDate(result.createdAt) }}</span>
            </div>
          </div>
          <div class="tracing-hero-right">
            <div class="hero-stats-grid">
              <div class="hero-stat-box">
                <div class="hero-stat-val">{{ result.analyzedPointCount }}</div>
                <div class="hero-stat-desc">薄弱点</div>
              </div>
              <div class="hero-stat-box">
                <div class="hero-stat-val">{{ result.rootCauseCount }}</div>
                <div class="hero-stat-desc">根因</div>
              </div>
              <div class="hero-stat-box">
                <div class="hero-stat-val">{{ maxDepth }}</div>
                <div class="hero-stat-desc">最深层级</div>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Suggestion -->
        <div class="card card-accent-left anim-2" v-if="result.suggestion">
          <div class="card-body">
            <div class="callout-ai-header">
              <div class="icon-badge icon-badge-amber"><i class="ri-robot-line"></i></div>
              <div>
                <div style="font-weight:700;font-size:0.9rem">AI 溯因报告</div>
                <div style="font-size:0.78rem;color:var(--text-muted)">基于知识依赖关系深度分析</div>
              </div>
            </div>
            <p style="white-space:pre-wrap;font-size:0.88rem;line-height:1.8;margin:0;color:var(--text-secondary)">{{ result.suggestion }}</p>
          </div>
        </div>

        <!-- Merged Root Causes -->
        <div class="card anim-3" v-if="mergedRootCauses.length">
          <div class="card-header">
            <h3><i class="ri-alarm-warning-line" style="color:var(--danger)"></i> 根本原因</h3>
            <span class="badge badge-red">{{ mergedRootCauses.length }} 个</span>
          </div>
          <div class="card-body">
            <div class="root-causes-list">
              <div class="root-cause-card" v-for="(rc, idx) in mergedRootCauses" :key="idx">
                <div class="rc-header">
                  <span class="rc-priority">P{{ rc.priority }}</span>
                  <span class="rc-name">{{ rc.knowledgePointName }}</span>
                  <span class="badge badge-gray">{{ rc.module }}</span>
                </div>
                <div class="rc-mastery-row">
                  <div class="rc-mastery-bar">
                    <div class="rc-mastery-fill" :style="{width: Math.round((rc.mastery||0)*100)+'%'}"></div>
                  </div>
                  <span class="rc-mastery-text">掌握度 {{ formatPercent(rc.mastery) }}</span>
                </div>
                <div class="rc-affected" v-if="rc.affectedPointNames && rc.affectedPointNames.length">
                  <span class="rc-affected-label">影响知识点：</span>
                  <span class="badge badge-yellow" v-for="(name, i) in rc.affectedPointNames" :key="i">{{ name }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Tracing Paths -->
        <div class="card anim-4" v-if="tracingResults.length">
          <div class="card-header">
            <h3><i class="ri-route-line" style="color:var(--info)"></i> 溯因路径</h3>
            <span class="badge badge-info">{{ tracingResults.length }} 条链路</span>
          </div>
          <div class="card-body">
            <div class="tracing-path-item" v-for="(tr, idx) in tracingResults" :key="idx">
              <div class="tp-header">
                <span class="tp-target-name">{{ tr.targetPointName }}</span>
                <span class="badge badge-red">掌握度 {{ formatPercent(tr.targetMastery) }}</span>
                <span class="badge badge-gray">深度 {{ tr.depth }}</span>
              </div>
              <div class="tp-chain">
                <div class="tp-node" v-for="(node, ni) in tr.tracingPath" :key="ni"
                     :class="{'tp-node-target': node.isTarget, 'tp-node-root': node.isRootCause}">
                  <div class="tp-node-dot"></div>
                  <div class="tp-node-info">
                    <div class="tp-node-name">
                      {{ node.knowledgePointName }}
                      <span class="badge badge-red" v-if="node.isRootCause" style="font-size:0.65rem">根因</span>
                      <span class="badge badge-yellow" v-if="node.isTarget" style="font-size:0.65rem">目标</span>
                    </div>
                    <div class="tp-node-meta">
                      <span>{{ node.module }}</span>
                      <span>掌握度 {{ formatPercent(node.mastery) }}</span>
                      <span>{{ node.status }}</span>
                    </div>
                  </div>
                </div>
              </div>
              <div class="tp-root-cause" v-if="tr.rootCause">
                <div class="tp-rc-label"><i class="ri-lightbulb-line"></i> 根因分析</div>
                <p>{{ tr.rootCause.reason }}</p>
              </div>
            </div>
          </div>
        </div>

        <!-- Knowledge Graph Visualization -->
        <div class="card anim-5" v-if="graphNodes.length">
          <div class="card-header">
            <h3><i class="ri-bubble-chart-line" style="color:var(--accent)"></i> 知识图谱</h3>
            <span class="badge badge-yellow">{{ graphNodes.length }} 节点</span>
          </div>
          <div class="card-body">
            <div class="graph-container">
              <svg class="graph-svg" :viewBox="svgViewBox">
                <!-- Edges -->
                <g class="graph-edges">
                  <line v-for="(edge, ei) in graphEdgePositions" :key="'e'+ei"
                    :x1="edge.x1" :y1="edge.y1" :x2="edge.x2" :y2="edge.y2"
                    :class="['graph-edge', edge.type === 'cross_module' ? 'edge-cross' : 'edge-normal']"
                  />
                  <!-- Arrowheads -->
                  <polygon v-for="(edge, ei) in graphEdgePositions" :key="'a'+ei"
                    :points="edge.arrowPoints"
                    :class="['graph-arrow', edge.type === 'cross_module' ? 'edge-cross' : 'edge-normal']"
                  />
                </g>
                <!-- Nodes -->
                <g v-for="(node, ni) in graphNodePositions" :key="'n'+ni" class="graph-node-group">
                  <circle :cx="node.x" :cy="node.y" :r="node.r"
                    :class="['graph-node-circle', node.statusClass]"
                  />
                  <text :x="node.x" :y="node.y + node.r + 14" class="graph-node-label">
                    {{ node.shortName }}
                  </text>
                  <text :x="node.x" :y="node.y + 4" class="graph-node-mastery">
                    {{ formatPercent(node.mastery) }}
                  </text>
                </g>
              </svg>
            </div>
            <div class="graph-legend">
              <span class="legend-item"><span class="legend-dot legend-target"></span>目标知识点</span>
              <span class="legend-item"><span class="legend-dot legend-root"></span>根因知识点</span>
              <span class="legend-item"><span class="legend-dot legend-normal"></span>中间节点</span>
              <span class="legend-item"><span class="legend-line legend-cross"></span>跨模块依赖</span>
            </div>
          </div>
        </div>

        <!-- Suggested Learning Path -->
        <div class="card anim-6" v-if="suggestedPath.length">
          <div class="card-header">
            <h3><i class="ri-road-map-line" style="color:var(--success)"></i> 可执行学习路径预览</h3>
            <span class="badge badge-green">{{ suggestedPath.length }} 步</span>
          </div>
          <div class="card-body">
            <div class="learning-path-timeline">
              <div class="lp-step" v-for="(step, si) in suggestedPath" :key="si"
                   :class="'lp-phase-' + getPhaseClass(step.phase)">
                <div class="lp-step-num">{{ step.order }}</div>
                <div class="lp-step-content">
                  <div class="lp-step-header">
                    <span class="lp-step-name">{{ step.knowledgePointName }}</span>
                    <span class="badge" :class="getPhaseBadge(step.phase)">{{ step.phase }}</span>
                  </div>
                  <div class="lp-step-meta">
                    <span><i class="ri-book-2-line"></i> {{ step.module }}</span>
                    <span><i class="ri-time-line"></i> {{ step.estimatedTime }}</span>
                  </div>
                  <div class="lp-step-resources" v-if="step.resources">
                    <span class="lp-resource" v-for="(res, ri) in step.resources" :key="ri">{{ res }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="path-start-panel">
              <div>
                <strong>让 AI 按这条路线带学</strong>
                <p>创建真实学习路径后，系统会进入第一个节点：讲解、练习、检查点和 AI 伴学会按路线推进。</p>
              </div>
              <button class="btn btn-dark" @click="generateAndStartPath" :disabled="generatingPath || aiPending">
                <span v-if="generatingPath" class="spinner" style="width:12px;height:12px;margin-right:6px"></span>
                <i v-else class="ri-play-fill"></i>
                {{ generatingPath ? '正在创建路径...' : '生成路径并开始学习' }}
              </button>
            </div>
          </div>
        </div>

        <!-- Actions -->
        <div class="result-actions">
          <button class="btn btn-dark" @click="generateAndStartPath" :disabled="generatingPath || aiPending">
            <span v-if="generatingPath" class="spinner" style="width:12px;height:12px;margin-right:6px"></span>
            <i v-else class="ri-route-line"></i>
            {{ generatingPath ? '创建中...' : '生成真实学习路径' }}
          </button>
          <button class="btn btn-dark" @click="goToDiagnostic" v-if="result.diagnosticId">
            <i class="ri-bar-chart-box-line"></i> 查看诊断结果
          </button>
          <button class="btn btn-outline" @click="goToHistory">
            <i class="ri-history-line"></i> 返回历史
          </button>
        </div>

      </div>
    </div>

    <div class="main-content" v-if="loading">
      <div class="loading-state">加载溯因结果中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { showToast } from '../composables/state.js'

const props = defineProps({ tracingId: { type: String, required: true } })
const router = useRouter()

const loading = ref(true)
const result = ref(null)
const aiPending = ref(false)
const generatingPath = ref(false)
let pollTimer = null

const tracingResults = computed(() => result.value?.tracingResults || [])
const mergedRootCauses = computed(() => result.value?.mergedRootCauses || [])
const graphNodes = computed(() => result.value?.graphVisualization?.nodes || [])
const graphEdges = computed(() => result.value?.graphVisualization?.edges || [])
const suggestedPath = computed(() => result.value?.suggestedLearningPath || [])

const maxDepth = computed(() => {
  let max = 0
  for (const tr of tracingResults.value) {
    if (tr.depth > max) max = tr.depth
  }
  return max
})

// Graph layout computation
const svgViewBox = computed(() => {
  const nodes = graphNodes.value
  if (!nodes.length) return '0 0 600 300'
  const width = Math.max(600, nodes.length * 120)
  const height = Math.max(300, (maxDepth.value + 1) * 120 + 80)
  return `0 0 ${width} ${height}`
})

const graphNodePositions = computed(() => {
  const nodes = graphNodes.value
  if (!nodes.length) return []

  // Group by depth
  const depthGroups = {}
  for (const node of nodes) {
    const d = node.depth || 0
    if (!depthGroups[d]) depthGroups[d] = []
    depthGroups[d].push(node)
  }

  const positions = []
  const svgWidth = Math.max(600, nodes.length * 120)

  for (const [depth, group] of Object.entries(depthGroups)) {
    const d = parseInt(depth)
    const y = 50 + d * 110
    const spacing = svgWidth / (group.length + 1)
    for (let i = 0; i < group.length; i++) {
      const node = group[i]
      const x = spacing * (i + 1)
      let statusClass = 'node-normal'
      if (node.isRootCause) statusClass = 'node-root'
      else if (node.isTarget) statusClass = 'node-target'
      positions.push({
        ...node,
        x, y,
        r: node.isTarget || node.isRootCause ? 24 : 18,
        statusClass,
        shortName: node.knowledgePointName?.length > 5 ? node.knowledgePointName.substring(0, 5) + '…' : node.knowledgePointName
      })
    }
  }
  return positions
})

const graphEdgePositions = computed(() => {
  const nodeMap = {}
  for (const np of graphNodePositions.value) {
    nodeMap[np.knowledgePointId] = np
  }
  return graphEdges.value.map(edge => {
    const from = nodeMap[edge.from]
    const to = nodeMap[edge.to]
    if (!from || !to) return null
    const dx = to.x - from.x
    const dy = to.y - from.y
    const dist = Math.sqrt(dx * dx + dy * dy) || 1
    const nx = dx / dist
    const ny = dy / dist
    const x1 = from.x + nx * from.r
    const y1 = from.y + ny * from.r
    const x2 = to.x - nx * to.r
    const y2 = to.y - ny * to.r
    // Arrowhead
    const ax = x2 - nx * 8
    const ay = y2 - ny * 8
    const perpX = -ny * 4
    const perpY = nx * 4
    const arrowPoints = `${x2},${y2} ${ax + perpX},${ay + perpY} ${ax - perpX},${ay - perpY}`
    return { x1, y1, x2, y2, arrowPoints, type: edge.type }
  }).filter(Boolean)
})

function formatPercent(val) {
  if (val == null) return '-'
  return (val * 100).toFixed(0) + '%'
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function getPhaseClass(phase) {
  if (phase === '巩固基础') return 'foundation'
  if (phase === '专项突破') return 'practice'
  return 'review'
}

function getPhaseBadge(phase) {
  if (phase === '巩固基础') return 'badge-yellow'
  if (phase === '专项突破') return 'badge-info'
  return 'badge-green'
}

function goToDiagnostic() {
  router.push('/diagnostic/result/' + result.value.diagnosticId)
}

function goToHistory() {
  router.push('/diagnostic/history')
}

async function generateAndStartPath() {
  if (!result.value?.tracingId) {
    showToast('溯因结果尚未加载完成', 'warning')
    return
  }

  generatingPath.value = true
  try {
    const path = await api.learning.generate({
      tracingId: result.value.tracingId,
      mode: 'systematic',
      dailyStudyMinutes: 30
    })
    const nodes = path.nodes || []
    const firstNode = nodes.find(n => ['in_progress', 'unlocked', 'pending', 'available'].includes(n.status)) || nodes[0]
    showToast('学习路径已创建，AI 将按路线带你学习', 'success')
    if (firstNode?.nodeId) {
      router.push(`/learning/${path.pathId}/${firstNode.nodeId}`)
    } else {
      router.push(`/learning/${path.pathId}`)
    }
  } catch (e) {
    if (e.message && e.message.includes('已有进行中的学习路径')) {
      showToast('该知识点已有学习路径，请在学习路径页继续', 'warning')
      router.push('/learning')
    } else {
      showToast(e.message || '生成学习路径失败', 'error')
    }
  } finally {
    generatingPath.value = false
  }
}

async function loadResult() {
  try {
    loading.value = true
    const data = await api.tracing.detail(props.tracingId)
    result.value = data
    if (data.aiAnalysisPending && !pollTimer) {
      aiPending.value = true
      pollTimer = setTimeout(pollForAIResult, 3000)
    } else {
      aiPending.value = false
    }
  } catch (e) {
    showToast('加载溯因结果失败', 'error')
  } finally {
    loading.value = false
  }
}

async function pollForAIResult() {
  try {
    const data = await api.tracing.detail(props.tracingId)
    result.value = data
    if (data.aiAnalysisPending) {
      pollTimer = setTimeout(pollForAIResult, 3000)
    } else {
      aiPending.value = false
      pollTimer = null
    }
  } catch {
    aiPending.value = false
    pollTimer = null
  }
}

onMounted(() => { loadResult() })
onUnmounted(() => { if (pollTimer) clearTimeout(pollTimer) })
</script>

<style scoped>
/* ===== Hero ===== */
.tracing-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: center;
  padding: 36px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}
.tracing-hero-glow {
  position: absolute;
  top: -60px; right: -60px;
  width: 250px; height: 250px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245,158,11,0.18) 0%, transparent 70%);
  pointer-events: none;
}
.tracing-hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
  font-size: 0.8rem;
  color: rgba(255,255,255,0.5);
}
.tracing-hero-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.hero-stats-grid {
  display: flex;
  gap: 20px;
}
.hero-stat-box {
  text-align: center;
  padding: 12px 18px;
  border-radius: var(--radius-sm);
  background: rgba(255,255,255,0.08);
  border: 1px solid rgba(255,255,255,0.1);
}
.hero-stat-val {
  font-size: 1.8rem;
  font-weight: 800;
  color: var(--accent);
}
.hero-stat-desc {
  font-size: 0.72rem;
  color: rgba(255,255,255,0.5);
  margin-top: 4px;
}

/* ===== AI Callout ===== */
.callout-ai-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

/* ===== Root Causes ===== */
.root-causes-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.root-cause-card {
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  border-left: 4px solid var(--danger);
  transition: box-shadow var(--transition);
}
.root-cause-card:hover { box-shadow: var(--shadow-md); }
.rc-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.rc-priority {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px; height: 28px;
  border-radius: 50%;
  background: var(--danger);
  color: #fff;
  font-size: 0.72rem;
  font-weight: 700;
}
.rc-name {
  font-weight: 600;
  font-size: 0.92rem;
}
.rc-mastery-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.rc-mastery-bar {
  flex: 1;
  height: 6px;
  border-radius: 3px;
  background: var(--bg-alt);
  overflow: hidden;
}
.rc-mastery-fill {
  height: 100%;
  border-radius: 3px;
  background: var(--danger);
  transition: width 0.4s ease;
}
.rc-mastery-text {
  font-size: 0.78rem;
  color: var(--text-muted);
  white-space: nowrap;
}
.rc-affected {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.rc-affected-label {
  font-size: 0.78rem;
  color: var(--text-muted);
}

/* ===== Tracing Paths ===== */
.tracing-path-item {
  padding: 16px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  margin-bottom: 12px;
}
.tracing-path-item:last-child { margin-bottom: 0; }
.tp-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}
.tp-target-name {
  font-weight: 600;
  font-size: 0.92rem;
}
.tp-chain {
  position: relative;
  padding-left: 18px;
  border-left: 2px solid var(--border);
  margin-left: 8px;
}
.tp-node {
  position: relative;
  padding: 8px 0 8px 16px;
}
.tp-node-dot {
  position: absolute;
  left: -24px; top: 14px;
  width: 12px; height: 12px;
  border-radius: 50%;
  background: var(--border);
  border: 2px solid var(--card-bg-solid);
}
.tp-node-target .tp-node-dot { background: var(--accent); }
.tp-node-root .tp-node-dot { background: var(--danger); }
.tp-node-name {
  font-size: 0.88rem;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tp-node-meta {
  display: flex;
  gap: 12px;
  font-size: 0.75rem;
  color: var(--text-muted);
  margin-top: 2px;
}
.tp-root-cause {
  margin-top: 12px;
  padding: 12px 14px;
  background: var(--danger-light);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--danger);
}
.tp-rc-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  margin-bottom: 6px;
  color: var(--danger);
}
.tp-root-cause p {
  font-size: 0.84rem;
  color: var(--text-secondary);
  margin: 0;
  line-height: 1.6;
}

/* ===== Graph ===== */
.graph-container {
  width: 100%;
  overflow-x: auto;
  padding: 12px 0;
}
.graph-svg {
  width: 100%;
  min-height: 280px;
  max-height: 450px;
}
.graph-edge {
  stroke: var(--border-hover);
  stroke-width: 1.5;
}
.graph-arrow {
  fill: var(--border-hover);
}
.edge-cross {
  stroke: var(--accent);
  stroke-dasharray: 4 3;
  fill: var(--accent);
}
.graph-node-circle {
  fill: var(--card-bg-solid);
  stroke: var(--border-hover);
  stroke-width: 2;
  transition: all var(--transition);
}
.graph-node-circle.node-target {
  fill: var(--accent-light);
  stroke: var(--accent);
  stroke-width: 3;
}
.graph-node-circle.node-root {
  fill: var(--danger-light);
  stroke: var(--danger);
  stroke-width: 3;
}
.graph-node-label {
  text-anchor: middle;
  font-size: 10px;
  fill: var(--text-secondary);
}
.graph-node-mastery {
  text-anchor: middle;
  font-size: 9px;
  font-weight: 700;
  fill: var(--text);
}
.graph-legend {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin-top: 12px;
  flex-wrap: wrap;
}
.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-muted);
}
.legend-dot {
  width: 10px; height: 10px;
  border-radius: 50%;
  border: 2px solid;
}
.legend-target { background: var(--accent-light); border-color: var(--accent); }
.legend-root { background: var(--danger-light); border-color: var(--danger); }
.legend-normal { background: var(--card-bg-solid); border-color: var(--border-hover); }
.legend-line {
  width: 20px; height: 2px;
  background: var(--accent);
  border: none;
}
.legend-cross {
  background: repeating-linear-gradient(90deg, var(--accent) 0 4px, transparent 4px 7px);
}

/* ===== Learning Path ===== */
.learning-path-timeline {
  display: flex;
  flex-direction: column;
  gap: 0;
}
.lp-step {
  display: flex;
  gap: 14px;
  padding: 14px 0;
  border-bottom: 1px solid var(--border);
}
.lp-step:last-child { border-bottom: none; }
.lp-step-num {
  width: 32px; height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.82rem;
  flex-shrink: 0;
  color: #fff;
  background: var(--text-muted);
}
.lp-phase-foundation .lp-step-num { background: var(--accent); }
.lp-phase-practice .lp-step-num { background: var(--info); }
.lp-phase-review .lp-step-num { background: var(--success); }
.lp-step-content { flex: 1; }
.lp-step-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 4px;
}
.lp-step-name {
  font-weight: 600;
  font-size: 0.9rem;
}
.lp-step-meta {
  display: flex;
  gap: 14px;
  font-size: 0.78rem;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.lp-step-meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}
.lp-step-resources {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.lp-resource {
  font-size: 0.72rem;
  padding: 2px 8px;
  border-radius: var(--radius-full);
  background: var(--bg-alt);
  color: var(--text-secondary);
}
.path-start-panel {
  margin-top: 16px;
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: var(--success-light);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.path-start-panel strong {
  font-size: 0.92rem;
}
.path-start-panel p {
  margin: 4px 0 0;
  font-size: 0.8rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

/* ===== Sticky CTA ===== */
.result-actions {
  position: sticky;
  bottom: 24px;
  display: flex;
  gap: 12px;
  justify-content: center;
  padding: 14px 24px;
  background: rgba(238,238,240,0.9);
  -webkit-backdrop-filter: blur(12px);
  backdrop-filter: blur(12px);
  border-radius: var(--radius-full);
  border: 1px solid var(--border);
  box-shadow: var(--shadow-lg);
  width: fit-content;
  margin: 24px auto 0;
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .tracing-hero { grid-template-columns: 1fr; }
  .tracing-hero-right { display: none; }
  .hero-stats-grid { justify-content: center; }
  .path-start-panel { align-items: stretch; flex-direction: column; }
}
</style>
