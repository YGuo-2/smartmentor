<template>
  <div class="dim-charts">
    <!-- 认知风格分布 -->
    <div class="dim-chart-card">
      <div class="dim-chart-title"><i class="ri-brain-line"></i> 认知风格分布</div>
      <div v-if="styleBars.length" class="bar-list">
        <div v-for="b in styleBars" :key="b.key" class="bar-row">
          <span class="bar-label">{{ b.label }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: b.pct + '%', background: b.color }"></div>
          </div>
          <span class="bar-val">{{ b.pct }}%</span>
        </div>
      </div>
      <div v-else class="dim-empty">暂无认知风格数据</div>
    </div>

    <!-- 学习时段偏好 -->
    <div class="dim-chart-card">
      <div class="dim-chart-title"><i class="ri-time-line"></i> 学习时段偏好</div>
      <div v-if="timeBars.length" class="bar-list">
        <div v-for="b in timeBars" :key="b.key" class="bar-row">
          <span class="bar-label">{{ b.label }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: b.pct + '%', background: b.color }"></div>
          </div>
          <span class="bar-val">{{ b.pct }}%</span>
        </div>
      </div>
      <div v-else class="dim-empty">暂无学习时段数据</div>
    </div>

    <!-- 高频错误类型 -->
    <div class="dim-chart-card">
      <div class="dim-chart-title"><i class="ri-error-warning-line"></i> 高频错误类型</div>
      <div v-if="errorBars.length" class="bar-list">
        <div v-for="b in errorBars" :key="b.key" class="bar-row">
          <span class="bar-label" :title="b.label">{{ b.label }}</span>
          <div class="bar-track">
            <div class="bar-fill" :style="{ width: b.pct + '%', background: b.color }"></div>
          </div>
          <span class="bar-val">{{ b.pct }}%</span>
        </div>
      </div>
      <div v-else class="dim-empty">暂无错误模式数据，先做一次诊断吧</div>
    </div>

    <!-- 科目画像 -->
    <div class="dim-chart-card subject-profile-card">
      <div class="dim-chart-title"><i class="ri-book-open-line"></i> 科目画像</div>
      <div v-if="subjectCards.length" class="subject-list">
        <div v-for="s in subjectCards" :key="s.subject" class="subject-row">
          <div class="subject-row-head">
            <span class="subject-name">{{ s.subject }}</span>
            <span class="subject-status">{{ s.status }}</span>
          </div>
          <div class="subject-track">
            <div class="subject-fill" :style="{ width: s.percent + '%' }"></div>
          </div>
          <div class="subject-meta">
            <span>{{ s.percent }}%</span>
            <span>评估 {{ s.observed }}/{{ s.total }}</span>
          </div>
          <div v-if="s.gaps.length" class="subject-gaps">
            <span v-for="gap in s.gaps" :key="gap">{{ gap }}</span>
          </div>
        </div>
      </div>
      <div v-else class="dim-empty">暂无科目画像数据</div>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  cognitiveStyle: { type: Object, default: () => ({}) },
  learningBehavior: { type: Object, default: () => ({}) },
  errorPatterns: { type: Object, default: () => ({}) },
  subjectProfiles: { type: Array, default: () => [] }
})

const STYLE_LABELS = { visual: '视觉型', logical: '逻辑型', example: '案例型', formula: '公式型' }
const STYLE_COLORS = { visual: '#2b6cb0', logical: '#553c9a', example: '#c5a059', formula: '#285e61' }
const TIME_LABELS = { morning: '早晨', afternoon: '下午', evening: '晚上' }
const TIME_COLORS = { morning: '#276749', afternoon: '#c5a059', evening: '#553c9a' }
const ERROR_COLORS = ['#9b2c2c', '#b1503e', '#c5a059', '#7c4a86', '#285e61']

function toPct(v) {
  const n = Number(v) || 0
  // 值可能是 0-1 占比，也可能已是百分数
  return Math.round((n <= 1 ? n * 100 : n))
}

const styleBars = computed(() => {
  const dist = props.cognitiveStyle?.styleDistribution || {}
  return Object.keys(STYLE_LABELS)
    .filter(k => dist[k] != null)
    .map(k => ({ key: k, label: STYLE_LABELS[k], pct: toPct(dist[k]), color: STYLE_COLORS[k] }))
})

const timeBars = computed(() => {
  const sp = props.learningBehavior?.studyPattern || {}
  return Object.keys(TIME_LABELS)
    .filter(k => sp[k] != null)
    .map(k => ({ key: k, label: TIME_LABELS[k], pct: toPct(sp[k]), color: TIME_COLORS[k] }))
})

const errorBars = computed(() => {
  const patterns = Array.isArray(props.errorPatterns?.patterns) ? props.errorPatterns.patterns : []
  return patterns
    .slice()
    .sort((a, b) => (Number(b.frequency) || 0) - (Number(a.frequency) || 0))
    .slice(0, 5)
    .map((p, i) => ({
      key: (p.type || '') + i,
      label: p.subType || p.type || '未知错误',
      pct: toPct(p.frequency),
      color: ERROR_COLORS[i % ERROR_COLORS.length]
    }))
})

const subjectCards = computed(() => {
  return (Array.isArray(props.subjectProfiles) ? props.subjectProfiles : [])
    .map(item => ({
      subject: item.subject || item.course || '未命名科目',
      status: item.status || '待诊断',
      percent: Math.max(0, Math.min(100, Number(item.masteryPercent ?? toPct(item.mastery)) || 0)),
      observed: Number(item.observedKnowledgePoints || 0),
      total: Number(item.totalKnowledgePoints || 0),
      gaps: Array.isArray(item.gaps) ? item.gaps.filter(Boolean).slice(0, 3) : []
    }))
})
</script>

<style scoped>
.dim-charts {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}
.dim-chart-card {
  background: var(--card-bg-solid, #fff);
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 12px;
  padding: 16px 18px;
}
.dim-chart-title {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--text, #333);
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
}
.bar-list { display: flex; flex-direction: column; gap: 10px; }
.bar-row { display: flex; align-items: center; gap: 8px; }
.bar-label {
  width: 56px;
  flex-shrink: 0;
  font-size: 0.76rem;
  color: var(--text-secondary, #666);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.bar-track {
  flex: 1;
  height: 8px;
  background: var(--bg-alt, #f0f2f5);
  border-radius: 999px;
  overflow: hidden;
}
.bar-fill {
  height: 100%;
  border-radius: 999px;
  transition: width 0.5s ease;
}
.bar-val {
  width: 34px;
  flex-shrink: 0;
  text-align: right;
  font-size: 0.74rem;
  font-weight: 600;
  color: var(--text-secondary, #666);
}
.dim-empty {
  font-size: 0.78rem;
  color: var(--text-muted, #999);
  padding: 12px 0;
  text-align: center;
}
.subject-profile-card { grid-column: 1 / -1; }
.subject-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 12px;
}
.subject-row {
  border: 1px solid var(--border, #e2e8f0);
  border-radius: 8px;
  padding: 12px;
  background: var(--bg-alt, #f8fafc);
}
.subject-row-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 8px;
}
.subject-name {
  min-width: 0;
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--text, #333);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.subject-status {
  flex-shrink: 0;
  font-size: 0.72rem;
  font-weight: 700;
  color: #285e61;
  background: rgba(40, 94, 97, 0.1);
  border-radius: 999px;
  padding: 3px 7px;
}
.subject-track {
  height: 8px;
  background: #edf2f7;
  border-radius: 999px;
  overflow: hidden;
}
.subject-fill {
  height: 100%;
  border-radius: 999px;
  background: linear-gradient(90deg, #276749, #c5a059);
}
.subject-meta {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  margin-top: 7px;
  font-size: 0.74rem;
  color: var(--text-secondary, #666);
}
.subject-gaps {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}
.subject-gaps span {
  max-width: 100%;
  border-radius: 6px;
  background: rgba(155, 44, 44, 0.08);
  color: #9b2c2c;
  font-size: 0.72rem;
  line-height: 1.2;
  padding: 4px 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
</style>
