<template>
<div class="profile-page">
  <div class="main-content settings-center-wrapper">
    <div class="settings-unified-container">
      
      <!-- Settings Header -->
      <div class="settings-unified-header">
        <div class="su-header-text">
          <h1 class="su-title">设置</h1>
          <p class="su-desc">管理您的基本信息、学习偏好与资源设置。</p>
        </div>
        <button class="su-close-btn" @click="goBack" title="关闭"><i class="ri-close-line"></i></button>
      </div>

      <!-- Settings Body -->
      <div class="settings-unified-body">
        
        <!-- Sidebar -->
        <div class="settings-sidebar">
          <div class="sidebar-menu">
            <div class="sidebar-item" :class="{ active: activeTab === 'basic' }" @click="activeTab = 'basic'">
              <i class="ri-user-settings-line"></i> 基本信息
            </div>
            <div class="sidebar-item" :class="{ active: activeTab === 'study' }" @click="activeTab = 'study'">
              <i class="ri-equalizer-line"></i> 学习偏好
            </div>
            <div class="sidebar-item" :class="{ active: activeTab === 'resources' }" @click="activeTab = 'resources'">
              <i class="ri-folders-line"></i> 资源与模块
            </div>
            <div class="sidebar-item" :class="{ active: activeTab === 'map' }" @click="activeTab = 'map'">
              <i class="ri-node-tree"></i> 学情地图
            </div>
          </div>
        </div>

        <!-- Content Area -->
        <div class="settings-content">
          
          <!-- ================= TAB: BASIC INFO ================= -->
          <div v-show="activeTab === 'basic'" class="tab-pane">
            <div class="tab-header">
              <div class="th-left">
                <h2>基本信息</h2>
                <p>管理您的基础个人信息与当前学习阶段。</p>
              </div>
            </div>
            
            <div class="settings-card">
              <div class="settings-grid">
                <div class="form-group">
                  <label class="form-label">昵称</label>
                  <input class="form-input" v-model="settingsForm.nickname" placeholder="请输入昵称" />
                </div>
                <div class="form-group">
                  <label class="form-label">专业方向</label>
                  <select class="form-input" v-model="settingsForm.majorDirection">
                    <option value="">请选择</option>
                    <option value="计算机类">计算机类</option>
                    <option value="软件工程">软件工程</option>
                    <option value="电子信息">电子信息</option>
                    <option value="自动化">自动化</option>
                    <option value="数据科学">数据科学</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">学历层次</label>
                  <select class="form-input" v-model="settingsForm.educationLevel">
                    <option value="">请选择</option>
                    <option value="高职">高职</option>
                    <option value="本科">本科</option>
                    <option value="研究生">研究生</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">近期重点课程</label>
                  <select class="form-input" v-model="settingsForm.currentCourse">
                    <option value="">请选择</option>
                    <option value="人工智能基础">人工智能基础</option>
                    <option value="Java Web 开发">Java Web 开发</option>
                    <option value="数字电路基础">数字电路基础</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="settings-actions">
              <button class="btn btn-dark" @click="saveSettings" :disabled="saving">
                <i class="ri-save-line"></i> {{ saving ? '保存中...' : '保存设置' }}
              </button>
            </div>
          </div>

          <!-- ================= TAB: STUDY PREFERENCES ================= -->
          <div v-show="activeTab === 'study'" class="tab-pane">
            <div class="tab-header">
              <div class="th-left">
                <h2>学习偏好</h2>
                <p>设置您的学习目标、偏好时段和总体学习风格。</p>
              </div>
            </div>

            <div class="settings-card">
              <div class="settings-grid">
                <div class="form-group">
                  <label class="form-label">学习目标</label>
                  <select class="form-input" v-model="settingsForm.learningGoal">
                    <option value="">请选择</option>
                    <option value="考试复习">考试复习</option>
                    <option value="项目实践">项目实践</option>
                    <option value="科研入门">科研入门</option>
                    <option value="就业技能">就业技能</option>
                    <option value="竞赛提升">竞赛提升</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">基础水平</label>
                  <select class="form-input" v-model="settingsForm.foundationLevel">
                    <option value="">请选择</option>
                    <option value="入门">入门</option>
                    <option value="基础">基础</option>
                    <option value="进阶">进阶</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">兴趣方向</label>
                  <input class="form-input" v-model="settingsForm.academicInterest" placeholder="如 大模型应用、Web 后端" />
                </div>
                <div class="form-group">
                  <label class="form-label">学习风格</label>
                  <select class="form-input" v-model="settingsForm.learningStyle">
                    <option value="">请选择</option>
                    <option value="visual">图表动画型</option>
                    <option value="logical">逻辑推导型</option>
                    <option value="example">案例例题型</option>
                    <option value="formula">公式归纳型</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">每日学习时间（分钟）</label>
                  <input class="form-input" type="number" v-model.number="settingsForm.dailyStudyMinutes" placeholder="如 60" />
                </div>
                <div class="form-group">
                  <label class="form-label">偏好时段</label>
                  <select class="form-input" v-model="settingsForm.preferredTimeSlot">
                    <option value="">请选择</option>
                    <option value="morning">早晨</option>
                    <option value="afternoon">下午</option>
                    <option value="evening">晚上</option>
                    <option value="night">深夜</option>
                  </select>
                </div>
                <div class="form-group">
                  <label class="form-label">学习模式</label>
                  <select class="form-input" v-model="settingsForm.studyMode">
                    <option value="">请选择</option>
                    <option value="intensive">集中突破</option>
                    <option value="balanced">均衡学习</option>
                    <option value="relaxed">轻松巩固</option>
                  </select>
                </div>
              </div>
            </div>

            <div class="settings-actions">
              <button class="btn btn-dark" @click="saveSettings" :disabled="saving">
                <i class="ri-save-line"></i> {{ saving ? '保存中...' : '保存设置' }}
              </button>
            </div>
          </div>

          <!-- ================= TAB: RESOURCES ================= -->
          <div v-show="activeTab === 'resources'" class="tab-pane">
            <div class="tab-header">
              <div class="th-left">
                <h2>资源与模块</h2>
                <p>选择您偏好的学习资源形式及优先复习的模块。</p>
              </div>
            </div>

            <div class="settings-card">
              <div class="section-label" style="margin-top:0">资源偏好</div>
              <div class="knowledge-point-selector mb-4">
                <label v-for="r in resourceOptions" :key="r" class="checkbox-label">
                  <input type="checkbox" :value="r" v-model="settingsForm.resourcePreference" />
                  {{ r }}
                </label>
              </div>

              <div class="section-label">薄弱模块优先级</div>
              <div class="knowledge-point-selector">
                <label v-for="m in modules" :key="m" class="checkbox-label">
                  <input type="checkbox" :value="m" v-model="settingsForm.weakModulePriority" />
                  {{ m }}
                </label>
              </div>

              <div class="section-label subject-section-label">科目画像</div>
              <div v-if="subjectProfiles.length" class="subject-profile-grid">
                <div v-for="subject in subjectProfiles" :key="subject.subject || subject.course" class="subject-profile-item">
                  <div class="subject-profile-head">
                    <span>{{ subject.subject || subject.course }}</span>
                    <strong>{{ subject.masteryPercent ?? Math.round((Number(subject.mastery) || 0) * 100) }}%</strong>
                  </div>
                  <div class="subject-profile-track">
                    <div class="subject-profile-fill" :style="{ width: subjectPercent(subject) + '%' }"></div>
                  </div>
                  <div class="subject-profile-meta">
                    <span>{{ subject.status || '待诊断' }}</span>
                    <span>评估 {{ subject.observedKnowledgePoints || 0 }}/{{ subject.totalKnowledgePoints || 0 }}</span>
                  </div>
                  <div v-if="subjectGaps(subject).length" class="subject-profile-gaps">
                    <span v-for="gap in subjectGaps(subject)" :key="gap">{{ gap }}</span>
                  </div>
                </div>
              </div>
              <div v-else class="subject-profile-empty">暂无科目画像数据</div>
            </div>

            <div class="settings-actions">
              <button class="btn btn-dark" @click="saveSettings" :disabled="saving">
                <i class="ri-save-line"></i> {{ saving ? '保存中...' : '保存设置' }}
              </button>
            </div>
          </div>

          <!-- ================= TAB: LEARNING MAP ================= -->
          <div v-show="activeTab === 'map'" class="tab-pane map-pane">
            <div class="tab-header">
              <div class="th-left">
                <h2>学情地图</h2>
                <p>按课程查看知识依赖、掌握状态和优先补弱点。</p>
              </div>
              <div class="map-toolbar">
                <select class="form-input map-module-select" v-model="knowledgeMapModule" @change="loadKnowledgeMap">
                  <option value="">全部课程</option>
                  <option v-for="m in modules" :key="m" :value="m">{{ m }}</option>
                </select>
                <button class="btn btn-outline btn-sm" @click="loadKnowledgeMap" :disabled="knowledgeMapLoading">
                  <i class="ri-refresh-line"></i> 刷新
                </button>
              </div>
            </div>

            <LearningKnowledgeTree
              title="学生学习情况树"
              :nodes="knowledgeMap.nodes"
              :edges="knowledgeMap.edges"
              :loading="knowledgeMapLoading"
              embedded
            />
          </div>

        </div>
      </div>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { showToast } from '../composables/state.js'
import LearningKnowledgeTree from '../components/LearningKnowledgeTree.vue'

const router = useRouter()
const activeTab = ref('basic') // Changed default to basic to better match settings context

const modules = ['人工智能基础', 'Java Web 开发', '数字电路基础']
const resourceOptions = ['文档', '视频', '代码案例', '论文', '练习', '项目案例']

const settingsForm = ref({
  nickname: '',
  targetSchool: '',
  targetScore: null,
  majorDirection: '',
  educationLevel: '',
  currentCourse: '',
  learningGoal: '',
  foundationLevel: '',
  resourcePreference: [],
  academicInterest: '',
  learningStyle: '',
  dailyStudyMinutes: null,
  preferredTimeSlot: '',
  weakModulePriority: [],
  studyMode: '',
  avatarUrl: ''
})
const saving = ref(false)
const subjectProfiles = ref([])
const knowledgeMap = ref({ nodes: [], edges: [] })
const knowledgeMapLoading = ref(false)
const knowledgeMapModule = ref('')

function goBack() {
  router.back()
}

async function loadProfile() {
  try {
    const [currentUser, data] = await Promise.all([
      api.auth.me().catch(() => ({})),
      api.profile.overview()
    ])

    const behavior = data.dimensions?.learningBehavior || {}
    const cognitive = data.dimensions?.cognitiveStyle || {}
    const goal = data.dimensions?.goalProfile || {}
    settingsForm.value.nickname = currentUser.nickname || ''
    settingsForm.value.targetSchool = goal.targetSchool || ''
    settingsForm.value.targetScore = goal.targetScore || null
    settingsForm.value.majorDirection = goal.majorDirection || currentUser.profile?.majorDirection || ''
    settingsForm.value.educationLevel = goal.educationLevel || currentUser.profile?.educationLevel || ''
    settingsForm.value.currentCourse = goal.currentCourse || currentUser.profile?.currentCourse || ''
    settingsForm.value.learningGoal = goal.learningGoal || currentUser.profile?.learningGoal || ''
    settingsForm.value.foundationLevel = goal.foundationLevel || currentUser.profile?.foundationLevel || ''
    settingsForm.value.resourcePreference = Array.isArray(goal.resourcePreference) ? goal.resourcePreference : parsePreference(currentUser.profile?.resourcePreference)
    settingsForm.value.academicInterest = goal.academicInterest || currentUser.profile?.academicInterest || ''
    settingsForm.value.learningStyle = cognitive.learningStyle || ''
    settingsForm.value.dailyStudyMinutes = behavior.dailyStudyMinutes || null
    settingsForm.value.preferredTimeSlot = behavior.preferredTimeSlot || ''
    settingsForm.value.weakModulePriority = goal.weakModulePriority || []
    settingsForm.value.studyMode = goal.studyMode || ''
    settingsForm.value.avatarUrl = currentUser.avatarUrl || ''
    subjectProfiles.value = Array.isArray(data.subjectProfiles)
      ? data.subjectProfiles
      : (data.dimensions?.knowledgeState?.subjectProfiles || [])
    if (!knowledgeMapModule.value) {
      knowledgeMapModule.value = modules.includes(settingsForm.value.currentCourse)
        ? settingsForm.value.currentCourse
        : ''
    }
  } catch (e) { showToast('加载个人信息失败', 'error') }
}

async function loadKnowledgeMap() {
  knowledgeMapLoading.value = true
  try {
    const params = { depth: 2 }
    if (knowledgeMapModule.value) params.module = knowledgeMapModule.value
    const data = await api.profile.knowledgeMap(params)
    knowledgeMap.value = {
      nodes: Array.isArray(data?.nodes) ? data.nodes : [],
      edges: Array.isArray(data?.edges) ? data.edges : []
    }
  } catch (e) {
    showToast(e.message || '加载学情地图失败', 'error')
    knowledgeMap.value = { nodes: [], edges: [] }
  } finally {
    knowledgeMapLoading.value = false
  }
}


async function saveSettings() {
  saving.value = true
  try {
    const body = {
      nickname: settingsForm.value.nickname || null,
      targetSchool: settingsForm.value.targetSchool || null,
      targetScore: settingsForm.value.targetScore || null,
      majorDirection: settingsForm.value.majorDirection || null,
      educationLevel: settingsForm.value.educationLevel || null,
      currentCourse: settingsForm.value.currentCourse || null,
      learningGoal: settingsForm.value.learningGoal || null,
      foundationLevel: settingsForm.value.foundationLevel || null,
      resourcePreference: settingsForm.value.resourcePreference.length ? settingsForm.value.resourcePreference : null,
      academicInterest: settingsForm.value.academicInterest || null,
      learningStyle: settingsForm.value.learningStyle || null,
      dailyStudyMinutes: settingsForm.value.dailyStudyMinutes || null,
      preferredTimeSlot: settingsForm.value.preferredTimeSlot || null,
      weakModulePriority: settingsForm.value.weakModulePriority.length ? settingsForm.value.weakModulePriority : null,
      studyMode: settingsForm.value.studyMode || null,
      avatarUrl: settingsForm.value.avatarUrl || null
    }
    await api.profile.updateSettings(body)
    showToast('设置已保存', 'success')
    await loadProfile()
  } catch (e) { showToast(e.message || '保存失败', 'error') }
  finally { saving.value = false }
}

function parsePreference(value) {
  if (!value) return []
  if (Array.isArray(value)) return value
  try {
    const parsed = JSON.parse(value)
    return Array.isArray(parsed) ? parsed : []
  } catch {
    return []
  }
}

function subjectPercent(subject) {
  const raw = subject?.masteryPercent ?? ((Number(subject?.mastery) || 0) * 100)
  return Math.max(0, Math.min(100, Number(raw) || 0))
}

function subjectGaps(subject) {
  return Array.isArray(subject?.gaps) ? subject.gaps.filter(Boolean).slice(0, 4) : []
}

onMounted(async () => {
  await loadProfile()
  await loadKnowledgeMap()
})
</script>

<style scoped>
/* ===== Layout & Container ===== */
.profile-page {
  width: 100%;
  max-width: none;
  margin: 0;
}

/* Override parent padding to allow true full-width */
.main-content.settings-center-wrapper {
  padding: 0 !important;
}

.settings-center-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 0;
  min-height: calc(100vh - 64px);
}

.settings-unified-container {
  width: min(1480px, calc(100vw - 48px));
  max-width: 98vw !important;
  min-height: 700px;
  background: #ffffff;
  border: 1px solid var(--border);
  border-radius: 12px;
  box-shadow: 0 8px 30px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== Header ===== */
.settings-unified-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  padding: 24px 32px;
  border-bottom: 1px solid var(--border);
  background: #ffffff;
}
.su-title {
  font-size: 1.5rem;
  font-weight: 800;
  margin-bottom: 4px;
  color: var(--text);
}
.su-desc {
  font-size: 0.95rem;
  color: var(--text-secondary);
}
.su-close-btn {
  background: none;
  border: none;
  font-size: 1.6rem;
  color: var(--text-muted);
  cursor: pointer;
  padding: 4px;
  transition: color var(--transition-fast);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
}
.su-close-btn:hover {
  color: var(--text);
  background: var(--bg-alt);
}

/* ===== Body ===== */
.settings-unified-body {
  display: flex;
  flex: 1;
}

/* ===== Sidebar ===== */
.settings-sidebar {
  width: 220px;
  background: #ffffff;
  border-right: 1px solid var(--border);
  padding: 20px 16px;
  flex-shrink: 0;
}
.sidebar-menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.sidebar-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: var(--radius-md);
  color: var(--text-secondary);
  font-weight: 500;
  font-size: 0.95rem;
  cursor: pointer;
  transition: all var(--transition-fast);
}
.sidebar-item i {
  font-size: 1.1rem;
}
.sidebar-item:hover {
  background: var(--primary-light);
  color: var(--primary);
}
.sidebar-item.active {
  background: #1f1f1f;
  color: #ffffff;
}
.sidebar-item.active i {
  color: #ffffff;
}

/* ===== Content Area ===== */
.settings-content {
  flex: 1;
  min-width: 0;
  padding: 40px 40px;
  background: #fdfcf9; /* Very slight off-white to distinguish from header/sidebar, or #fff */
  overflow-y: auto;
}
.tab-header {
  margin-bottom: 32px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
}
.th-left h2 {
  font-size: 1.4rem;
  font-weight: 700;
  margin-bottom: 8px;
  color: var(--text);
}
.th-left p {
  color: var(--text-muted);
  font-size: 0.9rem;
}

.settings-card {
  background: #ffffff; 
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  padding: 24px 28px;
  margin-bottom: 24px;
  width: 100%;
}

.settings-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.mt-4 { margin-top: 24px; }
.mb-4 { margin-bottom: 24px; }

/* ===== Settings Grid ===== */
.settings-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(320px, 1fr));
  gap: 18px 28px;
}

.subject-section-label { margin-top: 28px; }
.subject-profile-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 12px;
}
.subject-profile-item {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  background: #f8fafc;
  min-width: 0;
}
.subject-profile-head,
.subject-profile-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}
.subject-profile-head span {
  min-width: 0;
  font-size: 0.9rem;
  font-weight: 700;
  color: var(--text);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.subject-profile-head strong {
  flex-shrink: 0;
  color: var(--primary);
}
.subject-profile-track {
  height: 8px;
  margin: 10px 0 8px;
  background: #edf2f7;
  border-radius: 999px;
  overflow: hidden;
}
.subject-profile-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #276749, #c5a059);
}
.subject-profile-meta {
  font-size: 0.76rem;
  color: var(--text-secondary);
}
.subject-profile-gaps {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.subject-profile-gaps span {
  max-width: 100%;
  border-radius: 6px;
  background: rgba(155, 44, 44, 0.08);
  color: #9b2c2c;
  font-size: 0.73rem;
  line-height: 1.2;
  padding: 4px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.subject-profile-empty {
  color: var(--text-muted);
  font-size: 0.85rem;
  padding: 14px;
  border: 1px dashed var(--border);
  border-radius: 8px;
  background: #f8fafc;
}
.map-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 10px;
  flex-wrap: wrap;
}
.map-module-select {
  width: 190px;
  margin: 0;
}
.map-pane {
  min-width: 0;
}
.map-pane .tab-header {
  margin-bottom: 18px;
}

/* ===== Responsive ===== */
@media (max-width: 900px) {
  .settings-unified-container { width: 95%; min-height: auto; }
  .settings-unified-body { flex-direction: column; }
  .settings-sidebar { width: 100%; border-right: none; border-bottom: 1px solid var(--border); display: flex; overflow-x: auto; padding: 12px; }
  .sidebar-menu { flex-direction: row; }
  .sidebar-item { white-space: nowrap; }
  .settings-content { padding: 24px 20px; }
  .tab-header { align-items: stretch; flex-direction: column; }
  .map-toolbar { justify-content: flex-start; }
}

@media (max-width: 768px) {
  .settings-grid { grid-template-columns: 1fr; }
}

</style>
