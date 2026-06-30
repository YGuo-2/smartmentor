<template>
<div class="learning-paths-page" ref="pageRoot">

  <div class="main-content">
    <div class="page-wrap">

      <!-- Page Hero -->
      <div class="lp-hero anim-1">
        <div>
          <div class="page-hero-label page-hero--green">
            <i class="ri-route-line"></i> 学习旅程
          </div>
          <h1 style="font-size:2rem;font-weight:800;margin:4px 0 8px;display:flex;align-items:center;gap:10px">
            我的学习路径
            <PaperBird :size="34" color="var(--accent)" />
          </h1>
          <p style="color:var(--text-secondary);font-size:0.88rem;margin:0">
            <template v-if="!loading && paths.length > 0">{{ paths.length }} 条个性化路径，基于诊断结果定制</template>
            <template v-else>完成诊断测试后，AI 将为你量身定制学习路径</template>
          </p>
        </div>
        <router-link to="/diagnostic" class="btn btn-outline btn-sm">
          <i class="ri-test-tube-line"></i> 新建诊断
        </router-link>
      </div>

      <!-- 学习画像条 -->
      <div v-if="profile && !loading && paths.length > 0" class="lp-profile-strip anim-1">
        <span v-if="profile.majorDirection" class="lp-profile-chip"><i class="ri-stack-line"></i> 专业：{{ profile.majorDirection }}</span>
        <span v-if="profile.educationLevel" class="lp-profile-chip"><i class="ri-graduation-cap-line"></i> 学历：{{ profile.educationLevel }}</span>
        <span v-if="profile.currentCourse" class="lp-profile-chip"><i class="ri-book-open-line"></i> 课程：{{ profile.currentCourse }}</span>
        <span v-if="profile.learningGoal" class="lp-profile-chip"><i class="ri-flag-line"></i> 目标：{{ profile.learningGoal }}</span>
        <span
          v-for="rt in (profile.recommendedResourceTypes || [])"
          :key="rt"
          class="lp-profile-chip lp-profile-chip--res"
        ><i class="ri-shapes-line"></i> {{ rt }}</span>
      </div>

      <!-- Loading -->
      <div v-if="loading" class="loading-state" style="text-align:center;padding:80px 0">加载中...</div>

      <!-- Empty state -->
      <div v-else-if="paths.length === 0" class="lp-empty anim-2">
        <div class="lp-empty-icon">
          <PaperBird :size="56" color="var(--primary)" />
        </div>
        <h3 class="lp-empty-title">还没有学习路径</h3>
        <p class="lp-empty-sub">完成诊断测试，AI 将为你量身定制专属学习路径</p>
        <router-link to="/diagnostic" class="btn btn-dark" style="margin-top:4px">
          <i class="ri-play-fill"></i> 开始诊断测试
        </router-link>
      </div>

      <!-- Paths grid -->
      <div v-else>
        <!-- Featured first path -->
        <router-link
          :to="'/learning/' + paths[0].pathId"
          class="card card-tone-green path-featured anim-2"
        >
          <div class="path-featured-body">
            <div class="path-featured-left">
              <div class="path-featured-module">
                <i class="ri-route-line"></i> {{ paths[0].module }}
              </div>
              <h2 class="path-featured-title">{{ paths[0].title }}</h2>
              <div class="path-meta-strip">
                <span class="path-meta-item">
                  <i class="ri-node-tree"></i> {{ paths[0].completedNodes }}/{{ paths[0].totalNodes }} 节点
                </span>
                <span class="path-meta-dot">·</span>
                <span class="path-meta-item">
                  <i class="ri-calendar-line"></i> {{ formatDate(paths[0].createdAt) }}
                </span>
                <span class="path-meta-dot">·</span>
                <span class="badge" :class="statusBadgeClass(paths[0].status)">{{ statusLabel(paths[0].status) }}</span>
              </div>
              <div class="path-progress-row">
                <div class="progress-bar" style="flex:1">
                  <div class="progress-fill path-fill-success" :style="{ width: paths[0].progress + '%' }"></div>
                </div>
                <span class="path-pct">{{ paths[0].progress }}%</span>
              </div>
              <div class="path-featured-cta">
                <span class="btn btn-dark"><i class="ri-play-fill"></i> 继续学习</span>
              </div>
            </div>
            <div class="path-featured-right">
              <div class="donut donut-lg" :style="`--pct:${paths[0].progress};--color:var(--success)`">
                <div class="donut-label">{{ paths[0].progress }}%<br><small>完成</small></div>
              </div>
            </div>
          </div>
        </router-link>

        <!-- Remaining paths grid -->
        <div v-if="paths.length > 1" class="paths-grid anim-3">
          <router-link
            v-for="(path, index) in paths.slice(1)"
            :key="path.pathId"
            :to="'/learning/' + path.pathId"
            class="card card-tone-green path-card-v2"
            :style="`animation-delay: ${0.05 * index}s`"
          >
            <div class="path-card-v2-header">
              <div class="path-module-badge">
                <i class="ri-route-line"></i> {{ path.module }}
              </div>
              <span class="badge" :class="statusBadgeClass(path.status)">{{ statusLabel(path.status) }}</span>
            </div>
            <h4 class="path-card-v2-title">{{ path.title }}</h4>
            <div class="path-card-v2-meta">
              <span><i class="ri-node-tree"></i> {{ path.completedNodes }}/{{ path.totalNodes }}</span>
              <span><i class="ri-calendar-line"></i> {{ formatDate(path.createdAt) }}</span>
            </div>
            <div class="progress-bar path-card-v2-bar">
              <div class="progress-fill path-fill-success" :style="{ width: path.progress + '%' }"></div>
            </div>
            <div class="path-card-v2-footer">
              <span class="path-pct-sm">{{ path.progress }}% 完成</span>
              <span class="path-card-v2-cta">继续学习 <i class="ri-arrow-right-line"></i></span>
            </div>
          </router-link>
        </div>
      </div>

    </div>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { api } from '../api/index.js'
import { showToast } from '../composables/state.js'
import { usePageReveal } from '../composables/usePageReveal.js'
import PaperBird from '../components/PaperBird.vue'

const pageRoot = ref(null)
const paths = ref([])
const profile = ref(null)
const loading = ref(true)
const { replayMotion } = usePageReveal(pageRoot, {
  xRevealSelector: '.path-card-v2',
  runOnMounted: false
})

async function fetchPaths() {
  try {
    loading.value = true
    const res = await api.learning.paths({ status: 'active' })
    paths.value = res.paths || res.records || []
    profile.value = res.profile || null
  } catch (e) {
    showToast('加载学习路径失败', 'error')
  } finally {
    loading.value = false
    replayMotion()
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

function statusLabel(status) {
  return { active: '进行中', completed: '已完成', paused: '已暂停', in_progress: '进行中' }[status] || status || '进行中'
}

function statusBadgeClass(status) {
  return { active: 'badge-info', completed: 'badge-green', paused: 'badge-yellow', in_progress: 'badge-accent' }[status] || 'badge-default'
}

onMounted(() => { fetchPaths() })
</script>

<style scoped>
/* 氛围背景锚点：根容器相对定位，内容层浮于背景之上 */
.learning-paths-page { position: relative; }
.learning-paths-page > .main-content { position: relative; z-index: 1; }

/* ===== Hero ===== */
.learning-paths-page :deep(.anim-1),
.learning-paths-page :deep(.anim-2),
.learning-paths-page :deep(.anim-3),
.learning-paths-page :deep(.anim-4),
.learning-paths-page :deep(.anim-5),
.learning-paths-page :deep(.anim-6) {
  animation: none;
}

.lp-profile-strip {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 4px 0 18px;
}
.lp-profile-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 12px;
  border-radius: 999px;
  font-size: 0.76rem;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--bg-subtle, #f4f6f5);
  border: 1px solid var(--border-color, #e4e8e6);
}
.lp-profile-chip--res {
  color: var(--success);
  background: rgba(34, 197, 94, 0.08);
  border-color: rgba(34, 197, 94, 0.25);
}

.lp-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 16px;
  margin-bottom: 28px;
}

/* ===== Empty State ===== */
.lp-empty {
  text-align: center;
  padding: 80px 32px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.lp-empty-icon {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: var(--accent-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 2.2rem;
  color: var(--accent-hover);
  margin-bottom: 8px;
}
.lp-empty-title { font-size: 1.2rem; font-weight: 700; margin: 0; }
.lp-empty-sub { font-size: 0.88rem; color: var(--text-secondary); margin: 0; max-width: 320px; }

/* ===== Featured Path Card ===== */
.path-featured {
  display: block;
  text-decoration: none;
  margin-bottom: 20px;
  background: linear-gradient(135deg, rgba(34,197,94,0.07) 0%, var(--card-bg-solid) 55%);
  border-top: 3px solid var(--success);
  transition: all var(--transition);
}
.path-featured:hover { box-shadow: var(--shadow-md); transform: translateY(-2px); }
.path-featured-body {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: center;
  padding: 28px 32px;
}
.path-featured-module {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--success);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}
.path-featured-title {
  font-size: 1.5rem;
  font-weight: 800;
  margin: 0 0 14px 0;
  color: var(--text);
  line-height: 1.2;
}
.path-meta-strip {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  font-size: 0.82rem;
  color: var(--text-secondary);
  margin-bottom: 14px;
}
.path-meta-item { display: flex; align-items: center; gap: 4px; }
.path-meta-dot { color: var(--border); }
.path-progress-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}
.path-pct { font-size: 0.9rem; font-weight: 700; white-space: nowrap; color: var(--success); }
.path-fill-success { background: linear-gradient(90deg, var(--success), #86efac) !important; }
.path-featured-right { flex-shrink: 0; }
.path-featured-cta { pointer-events: none; }

/* ===== Paths Grid (remaining) ===== */
.paths-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 16px;
}
.path-card-v2 {
  display: flex;
  flex-direction: column;
  gap: 10px;
  text-decoration: none;
  color: var(--text);
  transition: all var(--transition);
}
.path-card-v2:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}
.path-card-v2:hover .path-card-v2-cta { color: var(--success); }
.path-card-v2-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.path-module-badge {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--success);
  display: flex;
  align-items: center;
  gap: 4px;
}
.path-card-v2-title {
  font-size: 0.95rem;
  font-weight: 700;
  margin: 0;
  line-height: 1.3;
}
.path-card-v2-meta {
  display: flex;
  gap: 14px;
  font-size: 0.78rem;
  color: var(--text-muted);
}
.path-card-v2-meta span { display: flex; align-items: center; gap: 4px; }
.path-card-v2-bar { margin: 2px 0; }
.path-card-v2-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.path-pct-sm { font-size: 0.78rem; color: var(--text-muted); }
.path-card-v2-cta {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
  transition: color var(--transition);
}

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .path-featured-body { grid-template-columns: 1fr; }
  .path-featured-right { display: none; }
  .lp-hero { flex-direction: column; align-items: flex-start; }
}
</style>
