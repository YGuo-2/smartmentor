<template>
<div class="dashboard-page" ref="pageRoot">
  <!-- Soft Ambient Background (matching global light mode) -->
  <div class="ambient-bg">
    <div class="ambient-orb orb-primary"></div>
    <div class="ambient-orb orb-secondary"></div>
  </div>

  <div class="dashboard-container">

    <!-- Greeting -->
    <div class="hero-greeting anim-stagger">
      <p class="subtitle">{{ todayDate }} <span class="dot-sep">·</span> 今日研读概览</p>
      <h1 class="title">你好，<span class="text-gradient">{{ user?.nickname || user?.username || '同学' }}</span></h1>
    </div>

    <!-- Profile 概览（顶部身份条） -->
    <div class="profile-overview anim-stagger">
      <div class="profile-left">
        <div class="profile-avatar-v2">{{ initials }}</div>
        <div class="profile-info-list">
          <h2 class="profile-name">{{ profile.nickname || '未设置昵称' }}</h2>
          <div class="profile-meta-row">
            <span class="profile-meta-item"><i class="ri-user-line"></i> {{ profile.username || '-' }}</span>
            <span class="profile-meta-item"><i class="ri-mail-line"></i> {{ profile.email || '未绑定' }}</span>
            <span v-if="profile.profileSummary?.educationLevel || profile.grade" class="profile-meta-item"><i class="ri-bookmark-line"></i> {{ profile.profileSummary?.educationLevel || profile.grade }}</span>
            <span v-if="profile.profileSummary?.currentCourse" class="profile-meta-item"><i class="ri-book-open-line"></i> {{ profile.profileSummary.currentCourse }}</span>
            <span v-if="profile.school" class="profile-meta-item"><i class="ri-building-4-line"></i> {{ profile.school }}</span>
          </div>
        </div>
      </div>
      <div class="profile-stats-right">
        <div class="profile-stat">
          <div class="profile-stat-num">{{ profile.stats.totalStudyDays }}</div>
          <div class="profile-stat-label">学习天数</div>
        </div>
        <div class="profile-stat-divider"></div>
        <div class="profile-stat">
          <div class="profile-stat-num">{{ profile.stats.totalQuestions }}</div>
          <div class="profile-stat-label">答题总数</div>
        </div>
        <div class="profile-stat-divider"></div>
        <div class="profile-stat">
          <div class="profile-stat-num accent">{{ (profile.stats.averageMastery * 100).toFixed(0) }}%</div>
          <div class="profile-stat-label">平均掌握度</div>
        </div>
      </div>
    </div>

    <!-- ===== 主区：左主（画像）右辅（路径 + 建议） ===== -->
    <div class="dash-grid anim-stagger">

      <!-- 左主：六维学习画像 -->
      <section class="panel panel--primary">
        <div class="block-head">
          <h3><i class="ri-radar-line"></i> 六维学习画像</h3>
          <button class="ai-rebuild-btn" @click="goOnboarding">
            <i class="ri-magic-line"></i> AI 对话生成画像
          </button>
        </div>
        <div class="radar-and-cards">
          <div class="radar-box">
            <ProfileRadar :dimensions="dimensions" />
          </div>
          <div class="dimension-grid">
            <div class="dimension-card-v2" v-for="dim in dimensions" :key="dim.key"
                 :style="`--val:${dim.value * 100};--dim-color:${dim.color}22`">
              <div class="dimension-card-content">
                <div class="dimension-icon" :style="`color:${dim.color}`">
                  <i :class="getDimensionIcon(dim.key)"></i>
                </div>
                <div class="dimension-score" :style="`color:${dim.color}`">{{ (dim.value * 100).toFixed(0) }}</div>
                <div class="dimension-label">{{ dim.label }}</div>
                <div class="dimension-level" :style="`color:${dim.color};background:${dim.color}14`">{{ getLevel(dim.value) }}</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <!-- 右辅：当前路径 + 建议 -->
      <div class="dash-side">
        <section class="panel" v-if="currentPath">
          <div class="block-head">
            <h3><i class="ri-route-line"></i> 当前攻坚路径</h3>
            <router-link :to="'/learning/' + currentPath.id" class="block-action">继续 <i class="ri-arrow-right-line"></i></router-link>
          </div>
          <div class="path-content">
            <div class="path-info">
              <h3 class="path-title">{{ currentPath.title }}</h3>
              <p class="path-module">{{ currentPath.module }}</p>
            </div>
            <div class="path-progress-bar">
              <div class="path-fill" :style="{ width: currentPath.progress + '%' }"></div>
            </div>
            <div class="path-meta">
              <span class="path-pct">进度：{{ currentPath.progress }}%</span>
            </div>
          </div>
        </section>

        <section class="panel">
          <div class="block-head">
            <h3><i class="ri-focus-3-line"></i> 建议你接下来</h3>
          </div>
          <div class="action-body">
            <div v-if="weakModules.length" class="weak-action-list">
              <div v-for="(w, i) in weakModules" :key="i" class="weak-action-row">
                <div class="weak-action-name"><i class="ri-alert-line"></i> 薄弱：{{ w }}</div>
                <div class="weak-action-btns">
                  <button class="btn-mini btn-mini-primary" @click="goDiagnose(w)">去诊断</button>
                  <button class="btn-mini" @click="goLearn(w)">去学</button>
                </div>
              </div>
            </div>
            <div v-else class="weak-action-empty">
              <p>还没有识别出明显薄弱点。完成一次诊断或和 AI 聊聊，我来帮你定位。</p>
              <div class="weak-action-btns">
                <button class="btn-mini btn-mini-primary" @click="goDiagnose('')">开始诊断</button>
                <button class="btn-mini" @click="goOnboarding">AI 生成画像</button>
              </div>
            </div>
          </div>
        </section>
      </div>
    </div>

    <!-- ===== 次区：画像详情（折叠） ===== -->
    <section class="panel panel--muted anim-stagger">
      <div class="block-head block-head--toggle" @click="showDetails = !showDetails">
        <h3><i class="ri-bar-chart-grouped-line"></i> 画像详情</h3>
        <button class="toggle-btn">
          {{ showDetails ? '收起' : '展开' }}
          <i :class="showDetails ? 'ri-arrow-up-s-line' : 'ri-arrow-down-s-line'"></i>
        </button>
      </div>
      <div v-show="showDetails">
        <ProfileDimensionCharts
          :cognitive-style="dimDetails.cognitiveStyle"
          :learning-behavior="dimDetails.learningBehavior"
          :error-patterns="dimDetails.errorPatterns"
          :subject-profiles="subjectProfiles"
        />
      </div>
    </section>

    <!-- ===== 次区：知识图谱 ===== -->
    <section class="panel panel--muted anim-stagger">
      <div class="block-head">
        <h3><i class="ri-node-tree"></i> 知识图谱</h3>
        <div class="subnav">
          <span
            class="subnav-item"
            v-for="mod in modules"
            :key="mod"
            :class="{ active: selectedModule === mod }"
            @click="selectModule(mod)"
          >{{ mod }}</span>
        </div>
      </div>
      <div class="km-body">
        <div class="knowledge-map-container">
          <div v-if="knowledgeLoading" class="empty-state">加载中...</div>
          <div v-else-if="knowledgeNodes.length === 0" class="empty-state">暂无知识节点数据</div>
          <div v-else class="knowledge-nodes-wrap">
            <div
              class="knowledge-node"
              :class="getNodeClass(node)"
              v-for="node in knowledgeNodes"
              :key="node.id || node.knowledgeId"
            >
              <div class="kn-name">{{ node.name }}</div>
              <div class="kn-mastery">{{ (node.mastery * 100).toFixed(0) }}%</div>
              <div class="kn-status">{{ getNodeStatusLabel(node) }}</div>
            </div>
          </div>
          <div class="knowledge-legend">
            <span class="kl-item"><span class="kl-dot" style="background:#276749"></span> 已掌握 (&gt;70%)</span>
            <span class="kl-item"><span class="kl-dot" style="background:#c5a059"></span> 学习中 (40-70%)</span>
            <span class="kl-item"><span class="kl-dot" style="background:#9b2c2c"></span> 薄弱 (&lt;40%)</span>
            <span class="kl-item"><span class="kl-dot" style="background:#bdbdbd"></span> 未开始</span>
          </div>
        </div>
      </div>
    </section>

  </div>
</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { user, showToast } from '../composables/state.js'
import { usePageReveal } from '../composables/usePageReveal.js'
import ProfileRadar from '../components/ProfileRadar.vue'
import ProfileDimensionCharts from '../components/ProfileDimensionCharts.vue'

const router = useRouter()
const pageRoot = ref(null)

const profile = ref({
  nickname: '', username: '', email: '', role: '', grade: '', school: '',
  profileSummary: {},
  stats: { totalStudyDays: 0, totalQuestions: 0, averageMastery: 0 }
})
const initials = ref('')

const dimensions = ref([
  { key: 'knowledgeState',  label: '知识状态', value: 0, color: '#276749' }, // 常春藤绿
  { key: 'errorPattern',    label: '错误模式', value: 0, color: '#9b2c2c' }, // 绯红
  { key: 'learningBehavior',label: '学习行为', value: 0, color: '#2b6cb0' }, // 靛蓝
  { key: 'cognitiveStyle',  label: '认知风格', value: 0, color: '#553c9a' }, // 学者紫
  { key: 'goalProfile',     label: '目标画像', value: 0, color: '#c5a059' }, // 学者金
  { key: 'resourcePreference', label: '资源偏好', value: 0, color: '#285e61' } // 深青
])

// 分维小图明细 + 薄弱模块（行动入口）
const dimDetails = ref({ cognitiveStyle: {}, learningBehavior: {}, errorPatterns: {} })
const weakModules = ref([])
const subjectProfiles = ref([])

const currentPath = ref(null)

const modules = ['人工智能基础', 'Java Web 开发', '数字电路基础']
const selectedModule = ref('人工智能基础')
const knowledgeNodes = ref([])
const knowledgeLoading = ref(false)
const showDetails = ref(false) // 画像详情默认折叠，降低首屏密度

const { replayMotion } = usePageReveal(pageRoot, {
  xRevealSelector: '.anim-stagger'
})

const now = new Date()
const todayDate = `${now.toLocaleString('en-US', { month: 'short' })} ${now.getDate()}, ${now.getFullYear()}`

function normalizeScore(value) {
  const numeric = Number(value || 0)
  return numeric > 1 ? numeric / 100 : numeric
}

function toPercent(value) {
  const numeric = Number(value || 0)
  return Math.round(numeric <= 1 ? numeric * 100 : numeric)
}

function getDimensionIcon(key) {
  const icons = {
    errorPattern:   'ri-error-warning-line',
    learningBehavior: 'ri-run-line',
    cognitiveStyle: 'ri-brain-line',
    goalProfile:    'ri-target-line',
    resourcePreference: 'ri-stack-line'
  }
  return icons[key] || 'ri-bar-chart-line'
}

// 把 0-1 的分值翻成可读的等级标签，让卡片相对雷达图有增量信息
function getLevel(value) {
  const v = Number(value) || 0
  if (v >= 0.8) return '优秀'
  if (v >= 0.6) return '良好'
  if (v >= 0.4) return '一般'
  return '待提升'
}

function getNodeClass(node) {
  if (node.status === 'not_started' || node.mastery === 0) return 'not-started'
  if (node.mastery > 0.7) return 'mastered'
  if (node.mastery >= 0.4) return 'learning'
  return 'weak'
}

function getNodeStatusLabel(node) {
  if (node.status === 'not_started' || node.mastery === 0) return '未开始'
  if (node.mastery > 0.7) return '已掌握'
  if (node.mastery >= 0.4) return '学习中'
  return '薄弱'
}

function goOnboarding() { router.push('/onboarding') }
function goDiagnose() { router.push('/diagnostic') }
function goLearn() { router.push('/learning') }

function normalizeCurrentPath(raw) {
  if (!raw?.pathId) return null
  return {
    ...raw,
    id: raw.pathId,
    title: raw.title || raw.name || '学习路径',
    module: raw.module || '核心课程',
    progress: toPercent(raw.progress),
    totalNodes: raw.totalNodes || 0
  }
}

async function loadProfile() {
  try {
    const [currentUser, data] = await Promise.all([
      api.auth.me().catch(() => ({})),
      api.profile.overview()
    ])
    profile.value.nickname = currentUser.nickname || ''
    profile.value.username = currentUser.username || ''
    profile.value.email = currentUser.email || ''
    profile.value.role = currentUser.role || ''
    profile.value.grade = currentUser.grade || ''
    profile.value.school = currentUser.school || ''
    profile.value.profileSummary = currentUser.profile || {}
    profile.value.stats = {
      totalStudyDays: data.streakDays || 0,
      totalQuestions: data.dimensions?.errorPatterns?.totalErrors || 0,
      averageMastery: Number(data.overallMastery || 0)
    }

    const name = currentUser.nickname || currentUser.username || '用户'
    initials.value = name.substring(0, 2)

    if (data.dimensions) {
      dimensions.value[0].value = normalizeScore(data.dimensions.knowledgeState?.score)
      dimensions.value[1].value = normalizeScore(data.dimensions.errorPatterns?.score)
      dimensions.value[2].value = normalizeScore(data.dimensions.learningBehavior?.score)
      dimensions.value[3].value = normalizeScore(data.dimensions.cognitiveStyle?.score)
      dimensions.value[4].value = normalizeScore(data.dimensions.goalProfile?.score)
      dimensions.value[5].value = normalizeScore(data.dimensions.resourcePreference?.score)

      // 分维小图明细
      dimDetails.value = {
        cognitiveStyle: data.dimensions.cognitiveStyle || {},
        learningBehavior: data.dimensions.learningBehavior || {},
        errorPatterns: data.dimensions.errorPatterns || {}
      }
      subjectProfiles.value = Array.isArray(data.subjectProfiles)
        ? data.subjectProfiles
        : (data.dimensions.knowledgeState?.subjectProfiles || [])

      const subjectWeak = subjectProfiles.value
        .flatMap(item => (Array.isArray(item.gaps) ? item.gaps : [])
          .filter(Boolean)
          .slice(0, 2)
          .map(gap => `${item.subject || item.course} · ${gap}`))
      const wm = data.dimensions.goalProfile?.weakModulePriority
      weakModules.value = subjectWeak.length ? subjectWeak.slice(0, 5) : (Array.isArray(wm) ? wm.filter(Boolean) : [])
    }
  } catch (e) { showToast('加载学情看板失败', 'error') }
}

async function loadDashboard() {
  try {
    const data = await api.report.dashboard()
    currentPath.value = normalizeCurrentPath(data.currentPath)
  } catch (e) { console.error('Failed to load dashboard:', e) }
}

async function loadKnowledgeMap(module) {
  knowledgeLoading.value = true
  try {
    const data = await api.profile.knowledgeMap({ module })
    knowledgeNodes.value = data.nodes || []
  } catch (e) { knowledgeNodes.value = [] }
  finally { knowledgeLoading.value = false }
}

function selectModule(mod) {
  selectedModule.value = mod
  loadKnowledgeMap(mod)
}

onMounted(async () => {
  await Promise.all([loadProfile(), loadDashboard(), loadKnowledgeMap(selectedModule.value)])
  replayMotion()
  maybePromptOnboarding()
})

// 新生若画像仍为空/默认值，引导去对话式画像构建（每会话最多提示一次）
async function maybePromptOnboarding() {
  if (sessionStorage.getItem('onboardingChecked')) return
  sessionStorage.setItem('onboardingChecked', '1')
  // 本机已完成过引导则不再打扰（兜底，避免后端启发式判定偏差导致反复引导）
  if (localStorage.getItem('profileOnboarded')) return
  try {
    const res = await api.profile.buildNeeded()
    if (res?.needed) {
      router.push('/onboarding')
    }
  } catch (e) {
    // 静默失败，不打扰用户
  }
}
</script>

<style scoped>
/* ===== Base & Ambient ===== */
.dashboard-page {
  position: relative;
  min-height: calc(100vh - 64px);
  padding: 40px 0;
}

/* Soft watercolor glowing orbs matching light theme */
.ambient-bg {
  position: absolute;
  top: 0; left: 0; right: 0; bottom: 0;
  overflow: hidden;
  z-index: 0;
  pointer-events: none;
}
.ambient-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: 0.18; /* 调淡并去掉飘动，减少潜意识噪点 */
}
.orb-primary {
  width: 45vw;
  height: 45vw;
  background: radial-gradient(circle, var(--accent-light) 0%, transparent 60%);
  top: -10%;
  right: -5%;
}
.orb-secondary {
  width: 35vw;
  height: 35vw;
  background: radial-gradient(circle, var(--info-light) 0%, transparent 60%);
  bottom: 0%;
  left: -5%;
  animation-delay: -5s;
}
@keyframes floatOrb {
  0% { transform: translate(0, 0) scale(1); }
  50% { transform: translate(-5%, 5%) scale(1.1); }
  100% { transform: translate(5%, -5%) scale(0.9); }
}

/* ===== Layout ===== */
.dashboard-container {
  max-width: 1300px;
  margin: 0 auto;
  padding: 0 40px;
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  gap: 24px;
}
@media (max-width: 1024px) {
  .dashboard-container { padding: 0 24px; }
}

/* Hero Greeting */
.hero-greeting { padding-bottom: 0; }
.hero-greeting .subtitle {
  font-size: 0.85rem;
  color: var(--accent);
  text-transform: uppercase;
  letter-spacing: 2px;
  margin-bottom: 8px;
  font-weight: 600;
}
.dot-sep { color: var(--text-muted); margin: 0 6px; }
.hero-greeting .title {
  font-size: 2.4rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.02em;
  color: var(--text);
}

/* Glass Cards（保留给可能的实体卡） */
.glass-card {
  background: var(--card-bg);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 28px 32px;
  box-shadow: var(--shadow-sm), var(--glass-border);
  transition: transform 0.3s ease, border-color 0.3s ease, box-shadow 0.3s ease;
}

/* ===== 去卡片化：区块流（无盒子，分隔线 + 留白组织） ===== */
.dash-block {
  padding: 28px 0 4px;
  border-top: 1px solid var(--border);
}

/* ===== 主次分栏布局 ===== */
.dash-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.7fr) minmax(0, 1fr);
  gap: 24px;
  align-items: start;
}
.dash-side {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
@media (max-width: 1024px) {
  .dash-grid { grid-template-columns: 1fr; }
}

/* 卡片面板：用轻盒重新建立层级（替代全去盒导致的"全都一样重"） */
.panel {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: var(--radius-xl);
  padding: 22px 24px;
}
/* 主面板：略微强调，作为视觉焦点 */
.panel--primary {
  box-shadow: var(--shadow-md);
}
/* 次面板：弱化、后退，作为支撑信息 */
.panel--muted {
  background: var(--bg-alt);
  border-color: transparent;
}
.panel .block-head { margin-bottom: 18px; }

/* 折叠头 */
.block-head--toggle { cursor: pointer; user-select: none; }
.toggle-btn {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  background: none;
  border: none;
  color: var(--text-muted);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--transition-fast);
}
.block-head--toggle:hover .toggle-btn { color: var(--accent); }
.block-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.block-head h3 {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 1.1rem;
  font-weight: 700;
  color: var(--text);
  margin: 0;
  padding-left: 12px;
  border-left: 3px solid var(--accent);
  line-height: 1.3;
}
.block-head h3 i { color: var(--accent); font-size: 1.15rem; }
.block-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--accent);
  white-space: nowrap;
}
.block-action:hover { text-decoration: underline; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}
.card-header h2 {
  font-size: 1.15rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}
.icon-accent { color: var(--accent); font-size: 1.3rem; }

/* Path Block */
.path-content { display: flex; flex-direction: column; gap: 12px; }
.path-title { font-size: 1.25rem; font-weight: 700; color: var(--text); }
.path-module { color: var(--text-secondary); font-size: 0.95rem; }
.path-progress-bar {
  height: 10px;
  background: var(--bg-alt);
  border-radius: 999px;
  overflow: hidden;
  margin-top: 4px;
  box-shadow: inset 0 1px 2px rgba(17,34,64,0.06);
}
.path-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent) 0%, var(--accent-hover) 100%);
  border-radius: 999px;
  box-shadow: 0 1px 4px rgba(197,160,89,0.4);
  transition: width 1s cubic-bezier(0.16, 1, 0.3, 1);
}
.path-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}
.path-pct { font-size: 0.88rem; font-weight: 500; color: var(--text-muted); }

/* ===== Profile 概览（扁平信息条，无重盒） ===== */
.profile-overview {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
  padding: 18px 22px;
  background: var(--accent-light);
  border-radius: var(--radius-lg, 16px);
}
.profile-hero-body {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  flex-wrap: wrap;
}
.profile-left {
  display: flex;
  align-items: center;
  gap: 20px;
  flex: 1;
}
.profile-avatar-v2 {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: linear-gradient(135deg, rgba(197,160,89,0.28), rgba(17,34,64,0.06));
  border: 3px solid var(--accent);
  font-size: 1.6rem;
  font-weight: 800;
  font-family: var(--font-serif);
  color: var(--accent-hover);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.profile-info-list { flex: 1; }
.profile-name { font-size: 1.3rem; font-weight: 800; margin: 0 0 10px 0; }
.profile-meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 6px;
  align-items: center;
}
.profile-meta-item {
  font-size: 0.82rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}
.profile-stats-right {
  display: flex;
  align-items: center;
  gap: 0;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
}
.profile-stat { padding: 16px 24px; text-align: center; }
.profile-stat-num {
  font-size: 1.6rem;
  font-weight: 800;
  line-height: 1;
  color: var(--text);
}
.profile-stat-num.accent { color: var(--accent); }
.profile-stat-label { font-size: 0.72rem; color: var(--text-muted); margin-top: 4px; white-space: nowrap; }
.profile-stat-divider { width: 1px; background: var(--border); align-self: stretch; }

/* ===== Section Label ===== */
.section-label {
  font-size: 0.95rem;
  font-weight: 700;
  color: var(--text);
  margin-bottom: -4px;
}
.dimension-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}
.ai-rebuild-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--accent);
  background: var(--accent-light);
  color: var(--accent-hover);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.ai-rebuild-btn:hover {
  background: var(--accent-gradient);
  border-color: var(--primary);
  color: #fff;
  transform: translateY(-1px);
}

/* ===== 雷达图 + 维度卡片并排 ===== */
.radar-and-cards {
  display: grid;
  grid-template-columns: 300px 1fr;
  gap: 24px;
  align-items: center;
}
.radar-box {
  padding: 8px;
  overflow: visible;
}
.radar-box :deep(.radar-svg) { overflow: visible; }
@media (max-width: 900px) {
  .radar-and-cards { grid-template-columns: 1fr; }
}

/* ===== Dimension Grid（去盒：无边框扁平格，底部进度填充作数据可视化） ===== */
.dimension-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
}
.dimension-card-v2 {
  padding: 18px 12px 14px;
  border-radius: var(--radius-md);
  text-align: center;
  position: relative;
  overflow: hidden;
  transition: background 0.3s ease;
  background: var(--bg-alt);
}
.dimension-card-v2:hover { background: var(--accent-light); }
.dimension-card-v2::before {
  content: '';
  position: absolute;
  bottom: 0; left: 0; right: 0;
  height: calc(var(--val, 0) * 1%);
  background: var(--dim-color, rgba(245,158,11,0.1));
  z-index: 0;
  transition: height 0.8s ease;
}
.dimension-card-content { position: relative; z-index: 1; }
.dimension-icon { font-size: 1.4rem; margin-bottom: 8px; }
.dimension-score { font-size: 1.5rem; font-weight: 800; line-height: 1; margin-bottom: 6px; }
.dimension-label { font-size: 0.75rem; color: var(--text-secondary); }
.dimension-level {
  display: inline-block;
  margin-top: 8px;
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.02em;
}

/* ===== 行动入口 ===== */
.action-body .weak-action-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.weak-action-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 14px;
  background: var(--bg-alt, #f8fafc);
  border: 1px solid var(--border);
  border-radius: 10px;
}
.weak-action-name {
  font-size: 0.86rem;
  font-weight: 600;
  color: var(--text);
  display: flex;
  align-items: center;
  gap: 6px;
}
.weak-action-name i { color: #f44336; }
.weak-action-btns { display: flex; gap: 8px; flex-shrink: 0; }
.weak-action-empty p { color: var(--text-secondary); font-size: 0.88rem; margin-bottom: 12px; }
.btn-mini {
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--card-bg-solid, #fff);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}
.btn-mini:hover { border-color: var(--accent); color: var(--text); }
.btn-mini-primary {
  background: var(--accent, #667eea);
  border-color: var(--accent, #667eea);
  color: #fff;
}
.btn-mini-primary:hover { color: #fff; opacity: 0.9; }

/* ===== Knowledge Nodes ===== */
.knowledge-nodes-wrap { display: flex; flex-wrap: wrap; gap: 10px; margin-bottom: 16px; }
.knowledge-node {
  padding: 10px 14px;
  border-radius: 10px;
  border: 1px solid var(--border);
  text-align: center;
  min-width: 96px;
}
.knowledge-node.mastered    { background: rgba(39,103,73,0.10);  border-color: rgba(39,103,73,0.4); }
.knowledge-node.learning    { background: rgba(197,160,89,0.12); border-color: rgba(197,160,89,0.5); }
.knowledge-node.weak        { background: rgba(155,44,44,0.10);  border-color: rgba(155,44,44,0.4); }
.knowledge-node.not-started { background: var(--bg-alt); border-color: var(--border); opacity: 0.7; }
.kn-name   { font-size: 0.85rem; font-weight: 600; margin-bottom: 3px; }
.kn-mastery{ font-size: 0.78rem; opacity: 0.85; }
.kn-status { font-size: 0.7rem; margin-top: 3px; opacity: 0.7; }
.knowledge-legend { display: flex; flex-wrap: wrap; gap: 14px; margin-top: 8px; }
.kl-item { font-size: 0.78rem; display: flex; align-items: center; gap: 5px; color: var(--text-secondary); }
.kl-dot { width: 10px; height: 10px; border-radius: 3px; display: inline-block; flex-shrink: 0; }

/* Utils */
.empty-state { text-align: center; color: var(--text-muted); padding: 20px; font-size: 0.9rem; }

/* GSAP/Reveal Fallback Staggering */
.anim-stagger {
  opacity: 0;
  transform: translateY(20px);
  animation: fadeUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}
.dashboard-container > :nth-child(1) { animation-delay: 0.05s; }
.dashboard-container > :nth-child(2) { animation-delay: 0.12s; }
.dashboard-container > :nth-child(3) { animation-delay: 0.19s; }
.dashboard-container > :nth-child(4) { animation-delay: 0.26s; }
.dashboard-container > :nth-child(5) { animation-delay: 0.33s; }
.dashboard-container > :nth-child(6) { animation-delay: 0.40s; }
.dashboard-container > :nth-child(7) { animation-delay: 0.47s; }

@keyframes fadeUp {
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 768px) {
  .hero-greeting .title { font-size: 2rem; }
  .profile-hero-body { flex-direction: column; }
  .profile-left { flex-direction: column; }
  .profile-stats-right { width: 100%; }
  .dimension-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
