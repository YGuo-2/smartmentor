<template>
<div class="learning-path-detail-page">

  <div class="main-content">
    <div class="page-wrap-sm">

      <div v-if="loading" class="loading-state" style="text-align:center;padding:80px 0">加载中...</div>

      <div v-else-if="error" style="text-align:center;padding:80px 0">
        <p style="color:var(--danger)">{{ error }}</p>
        <button class="btn btn-outline" @click="fetchDetail" style="margin-top:16px">重试</button>
      </div>

      <template v-else>
        <!-- Back link -->
        <div class="back-link-row anim-1">
          <router-link to="/learning" class="btn-back">
            <i class="ri-arrow-left-line"></i> 返回学习路径
          </router-link>
        </div>

        <!-- Header Card -->
        <div class="card card-accent-left-info path-header-card anim-1">
          <div class="card-body path-header-body">
            <div class="path-header-left">
              <div class="page-hero-label" style="--hero-accent:var(--info)">
                <i class="ri-route-line"></i> {{ pathData.module }}
              </div>
              <h2 class="path-title">{{ pathData.title }}</h2>
              <div class="path-stats-strip">
                <span class="path-stat-item">
                  <i class="ri-checkbox-circle-line"></i> {{ pathData.completedNodes }} 已完成
                </span>
                <span class="path-stat-divider">·</span>
                <span class="path-stat-item">
                  <i class="ri-percent-line"></i> {{ completionRate }}% 完成率
                </span>
                <span class="path-stat-divider">·</span>
                <span class="path-stat-item">
                  <i class="ri-timer-line"></i> {{ formatTime(totalTimeSpent) }}
                </span>
                <span class="path-stat-divider">·</span>
                <span class="path-stat-item">
                  <i class="ri-node-tree"></i> {{ pathData.totalNodes }} 个节点
                </span>
              </div>
              <div v-if="pathData.profile" class="path-profile-strip">
                <span v-if="pathData.profile.majorDirection" class="path-profile-chip"><i class="ri-stack-line"></i> {{ pathData.profile.majorDirection }}</span>
                <span v-if="pathData.profile.educationLevel" class="path-profile-chip"><i class="ri-graduation-cap-line"></i> {{ pathData.profile.educationLevel }}</span>
                <span v-if="pathData.profile.currentCourse" class="path-profile-chip"><i class="ri-book-open-line"></i> {{ pathData.profile.currentCourse }}</span>
                <span v-if="pathData.profile.learningGoal" class="path-profile-chip"><i class="ri-flag-line"></i> {{ pathData.profile.learningGoal }}</span>
                <span
                  v-for="rt in (pathData.profile.recommendedResourceTypes || [])"
                  :key="rt"
                  class="path-profile-chip path-profile-chip--res"
                >{{ rt }}</span>
              </div>
              <div class="path-progress-row">
                <div class="progress-bar" style="flex:1">
                  <div class="progress-fill info" :style="{ width: pathData.progress + '%' }"></div>
                </div>
                <span class="path-progress-pct">{{ pathData.progress }}%</span>
                <span class="badge" :class="'badge-' + pathData.status">{{ statusLabel(pathData.status) }}</span>
              </div>
              <div class="path-next-action" v-if="nextActionNode">
                <router-link class="btn btn-dark btn-sm" :to="'/learning/' + props.pathId + '/' + nextActionNode.nodeId">
                  <i class="ri-play-fill"></i> 继续：{{ nextActionNode.knowledgePointName || nextActionNode.knowledgePoint }}
                </router-link>
              </div>
              <div class="path-next-action" v-else-if="pathData.status === 'completed'">
                <router-link class="btn btn-dark btn-sm" to="/diagnostic">
                  <i class="ri-test-tube-line"></i> 再次诊断，生成新路径
                </router-link>
              </div>
            </div>
            <div class="path-header-right">
              <div class="donut donut-lg" :style="`--pct:${pathData.progress};--color:var(--info)`">
                <div class="donut-label">{{ pathData.progress }}%<br><small>完成</small></div>
              </div>
            </div>
          </div>
        </div>

        <!-- Visual Route Map -->
        <section class="route-map-section anim-2">
          <div class="card-header route-map-header">
            <div>
              <h3><i class="ri-route-line" style="color:var(--info)"></i> 路径学情地图</h3>
              <p class="route-map-subtitle">当前学习路径会在完整知识依赖中高亮，右侧面板直接给出下一步动作。</p>
            </div>
            <div class="route-legend">
              <span><i class="legend-pill completed"></i>已完成</span>
              <span><i class="legend-pill current"></i>当前</span>
              <span><i class="legend-pill available"></i>可学习</span>
              <span><i class="legend-pill locked"></i>未解锁</span>
              <span><i class="legend-pill remediation"></i>补救</span>
            </div>
          </div>
          <LearningKnowledgeTree
            title="当前路径学情地图"
            :nodes="routeKnowledgeNodes"
            :edges="routeKnowledgeEdges"
            :path-nodes="sortedNodes"
            :path-id="props.pathId"
            :focus-knowledge-point-id="nextActionKnowledgePointId"
            :loading="knowledgeMapLoading"
            embedded
          />
          <div class="route-inspector" v-if="nextActionNode">
            <div class="route-inspector-main">
              <span class="route-inspector-label">下一步</span>
              <strong>{{ nextActionNode.knowledgePointName || nextActionNode.knowledgePoint || nextActionNode.title }}</strong>
              <span>{{ nodeStatusLabel(nextActionNode.status) }} · {{ formatTime(nextActionNode.estimatedMinutes || nextActionNode.estimatedTime) }}</span>
            </div>
            <router-link class="btn btn-dark btn-sm" :to="'/learning/' + props.pathId + '/' + nextActionNode.nodeId">
              <i class="ri-play-fill"></i> 进入当前节点
            </router-link>
          </div>
        </section>

        <!-- Timeline Card -->
        <div class="card anim-2">
          <div class="card-header">
            <h3><i class="ri-list-check-3" style="color:var(--info)"></i> 节点明细</h3>
            <span class="badge">共 {{ pathData.totalNodes }} 个</span>
          </div>
          <div class="path-timeline">
            <div
              v-for="(node, index) in sortedNodes"
              :key="node.nodeId"
              class="path-node-v2"
              :class="node.status"
              @click="goToNode(node)"
            >
              <!-- Left: dot + line -->
              <div class="node-track">
                <div class="node-dot" :class="node.status">
                  <i v-if="node.status === 'locked'"      class="ri-lock-2-line"></i>
                  <i v-else-if="node.status === 'completed'" class="ri-check-line"></i>
                  <i v-else-if="node.status === 'failed'"    class="ri-close-line"></i>
                  <i v-else-if="node.status === 'in_progress'" class="ri-play-fill"></i>
                  <i v-else class="ri-circle-line"></i>
                </div>
                <div v-if="index < sortedNodes.length - 1" class="node-connector" :class="node.status"></div>
              </div>

              <!-- Right: content -->
              <div class="node-card" :class="node.status">
                <div class="node-card-header">
                  <span class="node-title">{{ node.title }}</span>
                  <span class="badge node-type-badge">{{ typeLabel(node.type) }}</span>
                </div>
                <div class="node-card-meta">
                  <span><i class="ri-book-open-line"></i> {{ node.knowledgePoint }}</span>
                  <span><i class="ri-time-line"></i> {{ formatTime(node.estimatedTime) }}</span>
                  <span class="badge" :class="'badge-' + node.status">{{ nodeStatusLabel(node.status) }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </template>

    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { showToast } from '../composables/state.js'
import LearningKnowledgeTree from '../components/LearningKnowledgeTree.vue'

const props = defineProps({ pathId: { type: String, required: true } })
const router = useRouter()
const loading = ref(true)
const error = ref('')

const pathData = ref({
  pathId: '', title: '', module: '', progress: 0, status: '',
  totalNodes: 0, completedNodes: 0, estimatedTotalTime: 0, nodes: []
})

const sortedNodes = ref([])
const totalTimeSpent = ref(0)
const completionRate = ref(0)
const nextActionNode = ref(null)
const knowledgeMap = ref({ nodes: [], edges: [] })
const knowledgeMapLoading = ref(false)

const nextActionKnowledgePointId = computed(() => {
  return String(nextActionNode.value?.knowledgePointId || nextActionNode.value?.nodeId || '')
})

const routeKnowledgeNodes = computed(() => {
  const mapNodes = Array.isArray(knowledgeMap.value.nodes) ? knowledgeMap.value.nodes : []
  const merged = mapNodes.map(node => ({ ...node }))
  const byId = new Map(merged.map(node => [String(node.id || node.knowledgePointId || node.nodeId), node]))
  for (const node of sortedNodes.value) {
    const kpId = String(node.knowledgePointId || node.nodeId || '')
    if (!kpId) continue
    if (byId.has(kpId)) {
      Object.assign(byId.get(kpId), {
        mastery: node.currentMastery ?? byId.get(kpId).mastery,
        status: node.status,
        reason: node.reason || byId.get(kpId).reason
      })
    } else {
      merged.push({
        id: kpId,
        name: node.knowledgePointName || node.knowledgePoint || node.title || kpId,
        module: node.module || pathData.value.module || '当前路径',
        mastery: node.currentMastery ?? 0,
        masteryLevel: node.status === 'completed' ? 'mastered' : node.status === 'failed' ? 'weak' : 'learning',
        status: node.status,
        reason: node.reason
      })
    }
  }
  return merged
})

const routeKnowledgeEdges = computed(() => {
  const mapEdges = Array.isArray(knowledgeMap.value.edges) ? knowledgeMap.value.edges : []
  if (mapEdges.length) return mapEdges
  const edges = []
  for (let i = 0; i < sortedNodes.value.length - 1; i++) {
    const source = sortedNodes.value[i].knowledgePointId || sortedNodes.value[i].nodeId
    const target = sortedNodes.value[i + 1].knowledgePointId || sortedNodes.value[i + 1].nodeId
    if (source && target) {
      edges.push({ source, target, type: 'path' })
    }
  }
  return edges
})

function formatTime(minutes) {
  if (!minutes && minutes !== 0) return '0分钟'
  if (minutes < 60) return minutes + '分钟'
  const h = Math.floor(minutes / 60)
  const m = minutes % 60
  return m > 0 ? h + '小时' + m + '分钟' : h + '小时'
}
function statusLabel(status) {
  return { active: '进行中', completed: '已完成', paused: '已暂停', in_progress: '进行中' }[status] || status || '未知'
}
function nodeStatusLabel(status) {
  return { locked: '未解锁', pending: '可学习', unlocked: '可学习', available: '可学习', in_progress: '学习中', completed: '已完成', failed: '未通过' }[status] || status || '未知'
}
function typeLabel(type) {
  return { lesson: '学习节点', exercise: '练习', checkpoint: '检查点', remediation: '补救' }[type] || type || '学习节点'
}
function goToNode(node) {
  if (node.status === 'locked') { showToast('该节点尚未解锁', 'warning'); return }
  router.push('/learning/' + props.pathId + '/' + node.nodeId)
}

async function fetchDetail() {
  loading.value = true
  error.value = ''
  try {
    const data = await api.learning.pathDetail(props.pathId)
    pathData.value = data
    sortedNodes.value = [...(data.nodes || [])].sort((a, b) => a.order - b.order)
    nextActionNode.value = sortedNodes.value.find(n => ['in_progress', 'unlocked', 'pending', 'available'].includes(n.status)) || null
    const completed = (data.nodes || []).filter(n => n.status === 'completed')
    completionRate.value = data.totalNodes > 0 ? Math.round((completed.length / data.totalNodes) * 100) : 0
    totalTimeSpent.value = completed.reduce((sum, n) => sum + (n.estimatedTime || 0), 0)
    await fetchKnowledgeMap(data.module)
  } catch (e) {
    error.value = e.message || '加载失败'
    showToast(error.value, 'error')
  } finally { loading.value = false }
}

async function fetchKnowledgeMap(module) {
  knowledgeMapLoading.value = true
  try {
    const params = { depth: 3 }
    if (module) params.module = module
    const data = await api.profile.knowledgeMap(params)
    knowledgeMap.value = {
      nodes: Array.isArray(data?.nodes) ? data.nodes : [],
      edges: Array.isArray(data?.edges) ? data.edges : []
    }
  } catch {
    knowledgeMap.value = { nodes: [], edges: [] }
  } finally {
    knowledgeMapLoading.value = false
  }
}

onMounted(() => { fetchDetail() })
</script>

<style scoped>
/* ===== Back link ===== */
.back-link-row { margin-bottom: 20px; }
.btn-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.85rem;
  color: var(--text-secondary);
  transition: color var(--transition);
  text-decoration: none;
}
.btn-back:hover { color: var(--text); }

/* ===== Header Card ===== */
.path-header-card { margin-bottom: 24px; }
.path-header-body {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 24px;
  align-items: center;
}
.path-title {
  font-size: 1.4rem;
  font-weight: 800;
  margin: 8px 0 12px 0;
  line-height: 1.2;
}
.path-profile-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 10px 0 2px;
}
.path-profile-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-subtle, #f4f6f5);
  border: 1px solid var(--border-color, #e4e8e6);
}
.path-profile-chip--res {
  color: var(--info);
  background: rgba(59, 130, 246, 0.08);
  border-color: rgba(59, 130, 246, 0.25);
}
.path-stats-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 0.82rem;
  color: var(--text-secondary);
  margin-bottom: 14px;
}
.path-stat-item { display: flex; align-items: center; gap: 4px; }
.path-stat-divider { color: var(--border); }
.path-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
}
.path-progress-pct { font-size: 0.9rem; font-weight: 700; white-space: nowrap; }
.path-header-right { flex-shrink: 0; }
.path-next-action { margin-top: 14px; }

/* ===== Progress fill info ===== */
.progress-fill.info { background: var(--info); }

/* ===== Visual Route Map ===== */
.route-map-section {
  margin-bottom: 24px;
  border: 1px solid var(--border);
  border-radius: 12px;
  background: #fff;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}
.route-map-header {
  align-items: flex-start;
  gap: 16px;
  margin: 0;
  padding: 20px 22px;
  border-bottom: 1px solid var(--border);
  background: #fff;
}
.route-map-subtitle {
  margin: 6px 0 0;
  font-size: 0.82rem;
  color: var(--text-muted);
}
.route-legend {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 10px;
  max-width: 420px;
  font-size: 0.74rem;
  color: var(--text-muted);
}
.route-legend span {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  white-space: nowrap;
}
.legend-pill {
  display: inline-block;
  width: 18px;
  height: 8px;
  border-radius: 999px;
  background: var(--border-hover);
}
.legend-pill.completed { background: var(--success); }
.legend-pill.current { background: var(--accent); }
.legend-pill.available { background: var(--info); }
.legend-pill.locked { background: var(--text-muted); opacity: 0.45; }
.legend-pill.remediation { background: var(--danger); }
.route-inspector {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
  background: #fbfcfd;
}
.route-inspector-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  min-width: 0;
  font-size: 0.82rem;
  color: var(--text-secondary);
}
.route-inspector-main strong {
  color: var(--text);
}
.route-inspector-label {
  padding: 3px 8px;
  border-radius: 999px;
  background: var(--accent-light);
  color: var(--accent-hover);
  font-size: 0.72rem;
  font-weight: 700;
}

/* ===== Timeline ===== */
.path-timeline { padding: 8px 0; }

.path-node-v2 {
  display: flex;
  gap: 0;
  cursor: pointer;
}
.path-node-v2.locked { cursor: not-allowed; }

/* Track (dot + line) */
.node-track {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 40px;
  flex-shrink: 0;
}
.node-dot {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1rem;
  flex-shrink: 0;
  border: 2px solid var(--border);
  background: var(--card-bg-solid);
  transition: all var(--transition);
  z-index: 1;
}
.node-dot.completed  { background: var(--success); border-color: var(--success); color: #fff; }
.node-dot.in_progress { background: var(--accent); border-color: var(--accent); color: #fff; animation: pulse-ring 2s infinite; }
.node-dot.failed     { background: var(--danger); border-color: var(--danger); color: #fff; }
.node-dot.available,
.node-dot.unlocked,
.node-dot.pending  { border-color: var(--info); color: var(--info); }
.node-dot.locked     { background: var(--bg-alt); color: var(--text-muted); }

.node-connector {
  flex: 1;
  width: 2px;
  min-height: 16px;
  background: var(--border);
  margin: 2px 0;
}
.node-connector.completed { background: var(--success); }
.node-connector.in_progress { background: var(--accent); }

/* Node card */
.node-card {
  flex: 1;
  margin: 0 0 12px 16px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  transition: all var(--transition);
  background: var(--card-bg-solid);
}
.path-node-v2:hover .node-card:not(.locked) {
  border-color: var(--info);
  box-shadow: var(--shadow-sm);
}
.node-card.in_progress {
  box-shadow: 0 0 0 3px rgba(245,158,11,0.15);
  border-color: var(--accent);
}
.node-card.completed { opacity: 0.75; }
.node-card.locked    { opacity: 0.45; }

.node-card-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}
.node-title { font-size: 0.9rem; font-weight: 600; flex: 1; }
.node-type-badge { font-size: 0.7rem; }
.node-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 0.78rem;
  color: var(--text-muted);
  align-items: center;
}
.node-card-meta i { margin-right: 3px; }

@keyframes pulse-ring {
  0%   { box-shadow: 0 0 0 0 rgba(245,158,11,0.4); }
  70%  { box-shadow: 0 0 0 8px rgba(245,158,11,0); }
  100% { box-shadow: 0 0 0 0 rgba(245,158,11,0); }
}

/* ===== Responsive ===== */
@media (max-width: 640px) {
  .path-header-body { grid-template-columns: 1fr; }
  .path-header-right { display: none; }
  .route-map-header { flex-direction: column; }
  .route-legend { justify-content: flex-start; max-width: none; }
  .route-inspector { align-items: stretch; flex-direction: column; }
}
</style>
