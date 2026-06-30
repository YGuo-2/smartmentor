<template>
<div class="report-page" ref="pageRoot">

  <div class="main-content">
    <div class="page-wrap">

      <!-- Hero Header with Period Tabs -->
      <div class="report-hero anim-1">
        <div class="report-hero-left">
          <div class="page-hero-label page-hero--green" style="background:none;padding:0;margin-bottom:4px">
            <i class="ri-line-chart-line"></i> 学习分析
          </div>
          <h1 style="font-size:2rem;font-weight:800;margin:0 0 4px">效果报告</h1>
          <p style="color:var(--text-secondary);font-size:0.88rem;margin:0">{{ periodLabel }} 学习数据汇总</p>
        </div>
        <div class="subnav" style="width:fit-content;margin:0">
          <button class="subnav-item" :class="{ active: selectedPeriod === '7d' }" @click="changePeriod('7d')">7天</button>
          <button class="subnav-item" :class="{ active: selectedPeriod === '30d' }" @click="changePeriod('30d')">30天</button>
          <button class="subnav-item" :class="{ active: selectedPeriod === '90d' }" @click="changePeriod('90d')">90天</button>
        </div>
      </div>

      <!-- Bento 网格 -->
      <div class="bento-grid anim-2">

        <!-- 大格 1：掌握度环形主图 -->
        <div class="bento-item bento-hero bento-gauge">
          <i class="bento-watermark ri-focus-3-line"></i>
          <div class="bento-tag">总掌握度</div>
          <div class="gauge-ring" :style="`--pct:${masteryGaugePct};--gauge-color:${gaugeColor}`">
            <div class="gauge-center">
              <div class="gauge-num" :style="`color:${gaugeColor}`">{{ masteryGaugePct }}<span class="gauge-unit">%</span></div>
            </div>
          </div>
          <div class="gauge-delta" v-if="summary.masteryBefore || summary.masteryAfter">
            <span class="gauge-from">{{ summary.masteryBefore }}%</span>
            <i class="ri-arrow-right-line"></i>
            <span class="gauge-to">{{ summary.masteryAfter }}%</span>
            <span class="gauge-imp" :class="summary.improvementRate >= 0 ? 'up' : 'down'">
              {{ summary.improvementRate >= 0 ? '+' : '' }}{{ summary.improvementRate }}%
            </span>
          </div>
        </div>

        <!-- 大格 2：能力雷达主图 -->
        <div class="bento-item bento-hero bento-radar">
          <div class="bento-tag">能力分布</div>
          <ProfileRadar v-if="radarDimensions.length >= 3" :dimensions="radarDimensions" />
          <div v-else class="empty-hint radar-empty">能力雷达数据不足</div>
        </div>

        <!-- 小格：关键统计 -->
        <div class="bento-item bento-stat">
          <i class="bento-watermark bento-watermark-sm ri-file-list-3-line"></i>
          <div class="bento-stat-num">{{ summary.totalQuestions }}</div>
          <div class="bento-stat-label">完成题目</div>
        </div>
        <div class="bento-item bento-stat">
          <i class="bento-watermark bento-watermark-sm ri-check-double-line"></i>
          <div class="bento-stat-num">{{ summary.averageAccuracy }}<span class="bento-stat-unit">%</span></div>
          <div class="bento-stat-label">平均正确率</div>
        </div>
        <div class="bento-item bento-stat">
          <i class="bento-watermark bento-watermark-sm ri-time-line"></i>
          <div class="bento-stat-num">{{ studyTimeDisplay }}</div>
          <div class="bento-stat-label">学习时长</div>
        </div>

        <!-- 中格：掌握度趋势 -->
        <div class="bento-item bento-trend">
          <i class="bento-watermark ri-line-chart-line"></i>
          <div class="bento-head">
            <h3><i class="ri-bar-chart-grouped-line"></i> 掌握度趋势</h3>
            <span class="bento-meta">{{ masteryCurve.length }} 个记录点</span>
          </div>
          <div v-if="masteryCurve.length === 0" class="empty-hint">暂无趋势数据</div>
          <template v-else>
            <div class="bar-chart-container">
              <div class="bar-chart-y-labels">
                <span>100%</span>
                <span>50%</span>
                <span>0%</span>
              </div>
              <div class="bar-chart">
                <div
                  v-for="(point, idx) in masteryCurve"
                  :key="idx"
                  class="bar-chart-bar success"
                  :style="`height:${point.mastery}%`"
                  :title="`${point.date}: ${point.mastery}%`"
                ></div>
              </div>
            </div>
            <div class="bar-chart-x-labels">
              <span>{{ masteryCurve[0]?.date }}</span>
              <span v-if="masteryCurve.length > 2">{{ masteryCurve[Math.floor(masteryCurve.length / 2)]?.date }}</span>
              <span>{{ masteryCurve[masteryCurve.length - 1]?.date }}</span>
            </div>
          </template>
        </div>

        <!-- 中格：错误消除 -->
        <div class="bento-item bento-errors">
          <i class="bento-watermark ri-eraser-line"></i>
          <div class="bento-head">
            <h3><i class="ri-eraser-line"></i> 错误消除</h3>
            <span class="bento-meta">{{ totalEliminated }}/{{ totalErrors }} 已消除</span>
          </div>
          <div v-if="errorElimination.length === 0" class="empty-hint">暂无错误数据</div>
          <div class="error-elim-item" v-for="item in displayedErrors" :key="item.errorType">
            <div class="error-elim-header">
              <span class="error-elim-name">{{ item.errorType }}</span>
              <span class="badge" :class="getEliminationBadgeClass(item.rate)">{{ item.eliminatedCount }}/{{ item.totalCount }} ({{ item.rate }}%)</span>
            </div>
            <div class="progress-bar error-elim-bar">
              <div class="progress-fill" :class="getEliminationClass(item.rate)" :style="{ width: item.rate + '%' }"></div>
            </div>
          </div>
          <div v-if="hiddenErrorCount > 0" class="error-elim-more">
            另有 {{ hiddenErrorCount }} 类错误已全部消除
          </div>
        </div>

        <!-- 通栏：课程学习效果评估 -->
        <div v-if="courseEffectiveness" class="bento-item bento-course">
          <i class="bento-watermark ri-graduation-cap-line"></i>
          <div class="bento-head">
            <h3><i class="ri-graduation-cap-line"></i> 课程学习效果评估</h3>
            <span class="bento-meta" v-if="courseEffectiveness.currentCourse">{{ courseEffectiveness.currentCourse }}</span>
          </div>
          <div class="ce-metric-grid">
            <div class="ce-metric">
              <div class="ce-metric-num">{{ courseEffectiveness.courseMastery ?? 0 }}%</div>
              <div class="ce-metric-label">当前课程掌握度</div>
            </div>
            <div class="ce-metric">
              <div class="ce-metric-num">{{ courseEffectiveness.pathCompletion ?? 0 }}%</div>
              <div class="ce-metric-label">路径完成度</div>
            </div>
          </div>
          <div v-if="(courseEffectiveness.resourcePreference || []).length" class="ce-pref-row">
            <span class="ce-pref-label">资源使用偏好：</span>
            <span
              v-for="rt in courseEffectiveness.resourcePreference"
              :key="rt"
              class="ce-pref-chip"
            >{{ rt }}</span>
          </div>
        </div>

      </div>

    </div>
  </div>
</div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { api } from '../api/index.js'
import { showToast } from '../composables/state.js'
import { usePageReveal } from '../composables/usePageReveal.js'
import ProfileRadar from '../components/ProfileRadar.vue'

const pageRoot = ref(null)
const selectedPeriod = ref('7d')

const summary = ref({
  totalStudyTime: 0,
  totalQuestions: 0,
  averageAccuracy: 0,
  improvementRate: 0,
  masteryBefore: 0,
  masteryAfter: 0
})

const masteryCurve = ref([])
const errorElimination = ref([])
const abilityRadar = ref([])
const courseEffectiveness = ref(null)
const totalErrors = ref(0)
const totalEliminated = ref(0)
const { replayMotion } = usePageReveal(pageRoot, {
  xRevealSelector: '.error-elim-item, .ce-metric'
})

const periodLabel = computed(() => {
  return { '7d': '最近7天', '30d': '最近30天', '90d': '最近90天' }[selectedPeriod.value] || ''
})

// 大号环形主数字：当前总掌握度
const masteryGaugePct = computed(() => summary.value.masteryAfter || 0)
const gaugeColor = computed(() => {
  const v = masteryGaugePct.value
  if (v >= 80) return 'var(--success)'
  if (v >= 60) return 'var(--accent)'
  if (v >= 40) return 'var(--info)'
  return 'var(--danger)'
})

// 能力雷达主图数据：[{label,value(0-1),color}]，标签缩短避免溢出
function shortenDim(name) {
  if (!name) return ''
  return name
    .replace(/\s*开发$/, '')
    .replace(/基础$/, '')
    .replace(/\s+/g, ' ')
    .trim()
}
const radarDimensions = computed(() =>
  abilityRadar.value.map(item => ({
    label: shortenDim(item.dimension),
    value: (item.score || 0) / 100,
    color: getRadarColor(item.score)
  }))
)

// 错误消除：按出现次数排序，只显示 top 6，其余折叠为计数
const ERROR_DISPLAY_LIMIT = 6
const displayedErrors = computed(() =>
  [...errorElimination.value]
    .sort((a, b) => (b.totalCount || 0) - (a.totalCount || 0))
    .slice(0, ERROR_DISPLAY_LIMIT)
)
const hiddenErrorCount = computed(() =>
  Math.max(errorElimination.value.length - ERROR_DISPLAY_LIMIT, 0)
)

// 学习时长展示：缺数据时显示占位而非 0分钟
const studyTimeDisplay = computed(() =>
  summary.value.totalStudyTime > 0 ? formatStudyTime(summary.value.totalStudyTime) : '—'
)

function formatStudyTime(minutes) {
  if (!minutes) return '0分钟'
  if (minutes < 60) return minutes + '分钟'
  const hours = Math.floor(minutes / 60)
  const mins = minutes % 60
  return mins > 0 ? hours + '小时' + mins + '分钟' : hours + '小时'
}

function getEliminationClass(rate) {
  if (rate >= 80) return 'elimination-high'
  if (rate >= 50) return 'elimination-mid'
  return 'elimination-low'
}

function getEliminationBadgeClass(rate) {
  if (rate >= 80) return 'badge-green'
  if (rate >= 50) return 'badge-yellow'
  return 'badge-red'
}

function getRadarColor(score) {
  if (score >= 80) return 'var(--success)'
  if (score >= 60) return 'var(--accent)'
  if (score >= 40) return 'var(--info)'
  return 'var(--danger)'
}

async function loadReport() {
  try {
    const data = await api.report.effectiveness({ period: selectedPeriod.value })
    summary.value = normalizeSummary(data.overallSummary || data.summary || {})
    masteryCurve.value = normalizeMasteryCurve(data.masteryCurve || [])
    errorElimination.value = normalizeErrorElimination(data.errorElimination || [])
    abilityRadar.value = normalizeAbilityRadar(data.abilityRadar || [])
    courseEffectiveness.value = data.courseEffectiveness || null
    let errors = 0, eliminated = 0
    errorElimination.value.forEach(item => {
      errors += item.totalCount || 0
      eliminated += item.eliminatedCount || 0
    })
    totalErrors.value = errors
    totalEliminated.value = eliminated
  } catch (e) {
    showToast(e.message || '加载报告失败', 'error')
  } finally {
    replayMotion()
  }
}

function changePeriod(period) {
  selectedPeriod.value = period
  loadReport()
}

function toPercent(value) {
  const numeric = Number(value || 0)
  return Math.round(numeric <= 1 ? numeric * 100 : numeric)
}

function normalizeSummary(raw) {
  return {
    totalStudyTime: Math.round(Number(raw.totalStudyMinutes ?? (Number(raw.totalStudyHours || 0) * 60))),
    totalQuestions: raw.totalQuestions ?? raw.totalQuestionsAnswered ?? 0,
    averageAccuracy: toPercent(raw.averageAccuracy ?? raw.accuracy ?? 0),
    improvementRate: toPercent(raw.improvementRate ?? 0),
    masteryBefore: toPercent(raw.masteryBefore ?? 0),
    masteryAfter: toPercent(raw.masteryAfter ?? 0)
  }
}

function normalizeMasteryCurve(items) {
  return items.map(item => ({
    ...item,
    mastery: toPercent(item.mastery)
  }))
}

function normalizeErrorElimination(items) {
  return items.map(item => {
    const totalCount = Number(item.totalCount ?? item.countBefore ?? 0)
    const remaining = Number(item.countAfter ?? Math.max(totalCount - Number(item.eliminatedCount || 0), 0))
    const eliminatedCount = Number(item.eliminatedCount ?? Math.max(totalCount - remaining, 0))
    return {
      errorType: item.errorType || '未分类错误',
      totalCount,
      eliminatedCount,
      rate: toPercent(item.rate ?? item.eliminationRate ?? 0)
    }
  }).filter(item => item.totalCount > 0)
}

function normalizeAbilityRadar(raw) {
  if (Array.isArray(raw)) return raw
  const dimensions = raw.dimensions || []
  const values = raw.after || raw.values || []
  return dimensions.map((dimension, index) => ({
    dimension,
    score: toPercent(values[index] ?? 0)
  })).filter(item => item.score > 0)
}

onMounted(() => { loadReport() })
</script>

<style scoped>
/* ===== 课程学习效果评估 ===== */
.ce-metric-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(120px, 1fr));
  gap: 12px;
}
.ce-metric {
  text-align: center;
  padding: 14px 10px;
  border-radius: 12px;
  background: var(--bg-subtle, #f4f6f5);
  border: 1px solid var(--border-color, #e4e8e6);
}
.ce-metric-num {
  font-size: 1.45rem;
  font-weight: 800;
  color: var(--primary);
}
.ce-metric-label {
  margin-top: 4px;
  font-size: 0.74rem;
  color: var(--text-secondary);
}
.ce-pref-row {
  margin-top: 14px;
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}
.ce-pref-label {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-secondary);
}
.ce-pref-chip {
  padding: 4px 11px;
  border-radius: 999px;
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--accent);
  background: var(--accent-light);
  border: 1px solid rgba(197, 160, 89, 0.3);
}
.ce-note {
  margin: 14px 0 0;
  font-size: 0.78rem;
  line-height: 1.6;
  color: var(--text-secondary);
}

/* ===== Hero Header ===== */
.report-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 24px;
  flex-wrap: wrap;
  gap: 16px;
}
.report-hero-left .page-hero-label {
  font-size: 0.72rem;
  font-weight: 700;
  letter-spacing: 2px;
  text-transform: uppercase;
  color: var(--success);
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 6px;
}

/* ===== Bento 网格 ===== */
.bento-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  grid-auto-rows: minmax(80px, auto);
  gap: 14px;
}
.bento-item {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 18px;
  padding: 20px;
  position: relative;
  overflow: hidden;
  transition: transform var(--transition), box-shadow var(--transition), border-color var(--transition);
}
.bento-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 30px -12px rgba(17, 34, 64, 0.25);
  border-color: rgba(197, 160, 89, 0.4);
}
.bento-item > *:not(.bento-watermark) { position: relative; z-index: 1; }

/* 角落图标水印 */
.bento-watermark {
  position: absolute;
  right: -18px;
  bottom: -22px;
  font-size: 7rem;
  line-height: 1;
  color: var(--accent);
  opacity: 0.07;
  pointer-events: none;
  z-index: 0;
  transition: opacity var(--transition), transform var(--transition);
}
.bento-watermark-sm { font-size: 4.2rem; right: -10px; bottom: -14px; }
.bento-item:hover .bento-watermark {
  opacity: 0.12;
  transform: scale(1.05) rotate(-4deg);
}
.bento-tag {
  position: absolute;
  top: 16px;
  left: 20px;
  font-size: 0.72rem;
  letter-spacing: 2px;
  color: var(--text-muted);
  text-transform: uppercase;
  font-weight: 600;
}

/* 大格跨度 */
.bento-hero { grid-column: span 3; grid-row: span 2; }
.bento-gauge {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 18px;
  background:
    radial-gradient(circle at 100% 0%, rgba(197, 160, 89, 0.06), transparent 55%),
    var(--card-bg);
}
.bento-radar {
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: visible;
}
.bento-radar :deep(.radar-svg) {
  overflow: visible;
  max-width: 300px;
}

/* 小格统计 */
.bento-stat {
  grid-column: span 2;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
}
.bento-stat-num {
  font-size: 2rem;
  font-weight: 800;
  letter-spacing: -1.5px;
  color: var(--primary);
  line-height: 1;
}
.bento-stat-unit { font-size: 1.1rem; font-weight: 700; margin-left: 1px; }
.bento-stat-label { font-size: 0.78rem; color: var(--text-secondary); }

/* 趋势 / 错误 / 课程 跨度 */
.bento-trend  { grid-column: span 4; }
.bento-errors { grid-column: span 2; }
.bento-course { grid-column: span 6; }

.bento-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 16px;
}
.bento-head h3 {
  display: flex;
  align-items: center;
  gap: 9px;
  font-size: 1rem;
  font-weight: 700;
  color: var(--text);
  margin: 0;
}
.bento-head h3 i { color: var(--accent); font-size: 1.05rem; }
.bento-meta {
  font-size: 0.74rem;
  color: var(--text-muted);
  font-weight: 500;
  white-space: nowrap;
}

/* ===== 掌握度环形 ===== */
.gauge-ring {
  width: 180px;
  height: 180px;
  border-radius: 50%;
  background:
    radial-gradient(closest-side, var(--card-bg) calc(100% - 16px), transparent calc(100% - 15px)),
    conic-gradient(var(--gauge-color) calc(var(--pct) * 1%), var(--bg-alt) 0);
  display: flex;
  align-items: center;
  justify-content: center;
}
.gauge-center { text-align: center; }
.gauge-num {
  font-size: 2.8rem;
  font-weight: 800;
  letter-spacing: -2px;
  line-height: 1;
}
.gauge-unit { font-size: 1.2rem; font-weight: 700; margin-left: 2px; }
.gauge-delta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.9rem;
  font-weight: 600;
}
.gauge-from { color: var(--text-muted); }
.gauge-delta i { color: var(--text-muted); }
.gauge-to { color: var(--text); }
.gauge-imp {
  padding: 3px 9px;
  border-radius: var(--radius-full);
  font-size: 0.8rem;
  font-weight: 700;
}
.gauge-imp.up   { background: var(--success-light); color: var(--success); }
.gauge-imp.down { background: var(--danger-light);  color: var(--danger); }
.radar-empty { padding: 40px 0; }

/* ===== Bar Chart ===== */
.bar-chart-container {
  display: flex;
  gap: 8px;
  height: 120px;
  margin-bottom: 8px;
}
.bar-chart-y-labels {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  font-size: 0.68rem;
  color: var(--text-muted);
  text-align: right;
  padding-bottom: 2px;
}
.bar-chart { flex: 1; height: 100%; }
.bar-chart-x-labels {
  display: flex;
  justify-content: space-between;
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-top: 4px;
}

/* ===== Error Elimination ===== */
.error-elim-item { margin-bottom: 14px; }
.error-elim-item:last-child { margin-bottom: 0; }
.error-elim-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 6px;
}
.error-elim-name { font-size: 0.85rem; font-weight: 500; }
.error-elim-bar { margin: 0; }
.error-elim-more {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
  font-size: 0.78rem;
  color: var(--text-muted);
  text-align: center;
}
.elimination-high { background: var(--success) !important; }
.elimination-mid  { background: var(--accent) !important; }
.elimination-low  { background: var(--danger) !important; }

/* ===== Radar Grid (legacy) ===== */
.radar-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

/* ===== Responsive ===== */
@media (max-width: 920px) {
  .bento-grid { grid-template-columns: repeat(4, 1fr); }
  .bento-hero { grid-column: span 2; }
  .bento-trend { grid-column: span 4; }
  .bento-errors { grid-column: span 4; }
  .bento-course { grid-column: span 4; }
}
@media (max-width: 560px) {
  .bento-grid { grid-template-columns: repeat(2, 1fr); }
  .bento-hero { grid-column: span 2; grid-row: span 1; }
  .bento-stat { grid-column: span 2; }
  .bento-trend, .bento-errors, .bento-course { grid-column: span 2; }
}
</style>
