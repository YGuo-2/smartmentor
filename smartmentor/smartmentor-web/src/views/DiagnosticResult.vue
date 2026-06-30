<template>
  <div class="diagnostic-result-page">

    <div class="main-content" v-if="!loading && result">
      <div class="page-wrap-sm">

        <!-- Hero Score Card -->
        <div class="card card-dark result-hero anim-1">
          <div class="result-hero-glow"></div>
          <div class="result-hero-left">
            <div class="page-hero-label" style="color:var(--accent)">
              <i class="ri-bar-chart-box-line"></i> {{ result.module }}
            </div>
            <div class="hero-stat-number accent" style="font-size:3.5rem;margin:8px 0 4px">
              {{ formatPercent(result.accuracy) }}
            </div>
            <div class="hero-stat-label" style="color:rgba(255,255,255,0.5)">正确率</div>
            <div class="result-hero-meta">
              <span><i class="ri-question-line"></i> {{ result.totalQuestions }} 题</span>
              <span><i class="ri-checkbox-circle-line"></i> {{ result.correctCount }} 正确</span>
              <span><i class="ri-calendar-line"></i> {{ formatDate(result.startTime) }}</span>
            </div>
          </div>
          <div class="result-hero-right">
            <div class="result-donut-group">
              <div style="text-align:center">
                <div class="donut donut-lg" :style="`--pct:${formatPct(result.accuracy)};--color:#f59e0b`">
                  <div class="donut-label" style="color:#fff">{{ formatPercent(result.accuracy) }}</div>
                </div>
                <div style="font-size:0.72rem;color:rgba(255,255,255,0.5);margin-top:8px;letter-spacing:1px;text-transform:uppercase">正确率</div>
              </div>
              <div style="text-align:center">
                <div class="donut donut-lg" :style="`--pct:${formatPct(result.overallMastery)};--color:#22c55e`">
                  <div class="donut-label" style="color:#fff">{{ formatPercent(result.overallMastery) }}</div>
                </div>
                <div style="font-size:0.72rem;color:rgba(255,255,255,0.5);margin-top:8px;letter-spacing:1px;text-transform:uppercase">掌握度</div>
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
                <div style="font-weight:700;font-size:0.9rem">AI 学习建议</div>
                <div style="font-size:0.78rem;color:var(--text-muted)">基于本次诊断结果生成</div>
              </div>
              <span v-if="aiPending" class="badge badge-accent" style="margin-left:auto">
                <span class="spinner" style="width:12px;height:12px;margin-right:4px"></span> AI 深度分析中...
              </span>
            </div>
            <p style="white-space:pre-wrap;font-size:0.88rem;line-height:1.8;margin:0;color:var(--text-secondary)">{{ result.suggestion }}</p>
          </div>
        </div>

        <!-- Weak Points Grid -->
        <div class="card anim-3" v-if="weakPoints.length">
          <div class="card-header">
            <h3><i class="ri-focus-3-line" style="color:var(--danger)"></i> 薄弱知识点</h3>
            <span class="badge badge-red">{{ weakPoints.length }} 个</span>
          </div>
          <div class="card-body">
            <div class="weak-points-grid">
              <div
                class="weak-point-card"
                v-for="(wp, index) in weakPoints"
                :key="index"
                :class="getWeakPointClass(wp.mastery)"
              >
                <div class="donut donut-sm" :style="`--pct:${formatPct(wp.mastery)};--color:${getWeakPointColor(wp.mastery)}`">
                  <div class="donut-label">{{ formatPercent(wp.mastery) }}</div>
                </div>
                <div class="weak-point-name">{{ wp.knowledgePointName }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- Question Review -->
        <div class="card anim-4">
          <div class="card-header">
            <h3><i class="ri-file-list-3-line" style="color:var(--info)"></i> 题目详情</h3>
            <span class="badge badge-info">{{ answerRecords.length }} 题</span>
          </div>
          <div class="card-body question-review-list">
            <div class="question-review-v2" v-for="(q, index) in answerRecords" :key="index"
                 :class="q.isCorrect ? 'qr-correct' : 'qr-wrong'">
              <div class="qr-header">
                <div class="qr-num-row">
                  <span class="qr-num">第 {{ q.questionIndex || (index + 1) }} 题</span>
                  <span class="badge" :class="q.isCorrect ? 'badge-green' : 'badge-red'">
                    <i :class="q.isCorrect ? 'ri-check-line' : 'ri-close-line'"></i>
                    {{ q.isCorrect ? '正确' : '错误' }}
                  </span>
                  <span class="badge badge-gray kp-badge">{{ q.knowledgePointName }}</span>
                </div>
              </div>
              <div class="qr-body">
                <div class="question-content" v-html="q.content"></div>
                <div class="answer-compare">
                  <div class="answer-box" :class="q.isCorrect ? 'answer-box-correct' : 'answer-box-wrong'">
                    <div class="answer-box-label">你的答案</div>
                    <div class="answer-box-val">{{ q.studentAnswer }}</div>
                  </div>
                  <div class="answer-box answer-box-correct" v-if="!q.isCorrect">
                    <div class="answer-box-label">正确答案</div>
                    <div class="answer-box-val">{{ q.correctAnswer }}</div>
                  </div>
                </div>
                <div class="error-analysis-v2" v-if="q.errorAnalysis && !q.isCorrect">
                  <div class="error-analysis-title"><i class="ri-lightbulb-line"></i> AI 错因分析</div>
                  <div v-if="q.errorType" style="margin-bottom:4px">
                    <span class="badge badge-yellow">{{ q.errorType }}</span>
                  </div>
                  <p style="font-size:0.85rem;color:var(--text-secondary);margin:0">{{ q.errorAnalysis }}</p>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sticky CTA Bar -->
        <div class="result-actions">
          <button class="btn btn-dark" @click="handleGenerateLearningPath" :disabled="generatingPath || analyzing">
            <span v-if="generatingPath" class="spinner" style="width:12px;height:12px;margin-right:6px"></span>
            <i v-else class="ri-route-line"></i>
            {{ generatingPath ? '生成中...' : '生成路径并开始 AI 带学' }}
          </button>
          <button class="btn btn-dark" @click="handleTracing" :disabled="analyzing">
            <i class="ri-mind-map"></i> {{ analyzing ? '分析中...' : '溯因分析' }}
          </button>
          <button class="btn btn-outline" @click="handleBack">
            <i class="ri-history-line"></i> 返回历史
          </button>
        </div>

      </div>
    </div>

    <div class="main-content" v-if="loading">
      <div class="loading-state">加载中...</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { renderLatex, showToast } from '../composables/state.js'

const props = defineProps({ diagnosticId: { type: String, required: true } })
const router = useRouter()

const loading = ref(true)
const result = ref(null)
const answerRecords = ref([])
const weakPoints = ref([])
const analyzing = ref(false)
const generatingPath = ref(false)
const aiPending = ref(false)
let pollTimer = null

async function loadResult() {
  try {
    loading.value = true
    const data = await api.diagnostic.detail(props.diagnosticId)
    result.value = data
    answerRecords.value = data.answerRecords || []
    weakPoints.value = data.weakPoints || []
    // If AI analysis is still pending, poll for updates
    if (data.aiAnalysisPending && !pollTimer) {
      aiPending.value = true
      pollTimer = setTimeout(pollForAIResult, 3000)
    } else {
      aiPending.value = false
    }
    await nextTick()
    const container = document.querySelector('.question-review-list')
    if (container) renderLatex(container)
  } catch (e) { showToast('加载诊断结果失败', 'error') }
  finally { loading.value = false }
}

async function pollForAIResult() {
  try {
    const data = await api.diagnostic.detail(props.diagnosticId)
    result.value = data
    answerRecords.value = data.answerRecords || []
    weakPoints.value = data.weakPoints || []
    if (data.aiAnalysisPending) {
      pollTimer = setTimeout(pollForAIResult, 3000)
    } else {
      aiPending.value = false
      pollTimer = null
      await nextTick()
      const container = document.querySelector('.question-review-list')
      if (container) renderLatex(container)
    }
  } catch {
    aiPending.value = false
    pollTimer = null
  }
}

async function handleTracing() {
  if (!weakPoints.value.length) { showToast('暂无薄弱知识点可分析', 'warning'); return }
  analyzing.value = true
  try {
    const knowledgePointIds = weakPoints.value.map(wp => wp.knowledgePointId)
    const data = await api.tracing.analyze({ diagnosticId: props.diagnosticId, knowledgePointIds })
    router.push(`/tracing/${data.tracingId}`)
  } catch (e) { showToast('溯因分析失败', 'error') }
  finally { analyzing.value = false }
}

async function handleGenerateLearningPath() {
  if (!weakPoints.value.length) { showToast('暂无薄弱知识点可生成路径', 'warning'); return }
  generatingPath.value = true
  try {
    const knowledgePointIds = weakPoints.value.map(wp => wp.knowledgePointId).filter(Boolean)
    const tracingData = await api.tracing.analyze({ diagnosticId: props.diagnosticId, knowledgePointIds })
    const path = await api.learning.generate({
      tracingId: tracingData.tracingId,
      mode: 'systematic',
      dailyStudyMinutes: 30
    })
    const nodes = path.nodes || []
    const firstNode = nodes.find(n => ['in_progress', 'unlocked', 'pending', 'available'].includes(n.status)) || nodes[0]
    showToast('学习路径已创建，进入 AI 路线带学', 'success')
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

function handleBack() { router.push('/diagnostic/history') }
function formatDate(dateStr) { if (!dateStr) return ''; return new Date(dateStr).toLocaleString('zh-CN') }
function formatPercent(val) { if (val == null) return '-'; return (val * 100).toFixed(0) + '%' }
function formatPct(val) { if (val == null) return 0; return Math.round(val * 100) }
function getMasteryClass(mastery) {
  if (mastery == null) return 'badge-default'
  if (mastery >= 0.8) return 'badge-success'
  if (mastery >= 0.6) return 'badge-info'
  if (mastery >= 0.4) return 'badge-warning'
  return 'badge-danger'
}
function getWeakPointClass(mastery) {
  if (mastery == null || mastery === 0) return 'wp-unstarted'
  if (mastery > 0.7) return 'wp-ok'
  if (mastery >= 0.4) return 'wp-mid'
  return 'wp-low'
}
function getWeakPointColor(mastery) {
  if (mastery == null || mastery === 0) return 'var(--text-muted)'
  if (mastery > 0.7) return 'var(--success)'
  if (mastery >= 0.4) return 'var(--accent)'
  return 'var(--danger)'
}

onMounted(() => { loadResult() })
onUnmounted(() => { if (pollTimer) clearTimeout(pollTimer) })
</script>

<style scoped>
/* 氛围背景锚点：根容器相对定位，内容层浮于背景之上 */
.diagnostic-result-page { position: relative; min-height: calc(100vh - 64px); }
.diagnostic-result-page > .main-content { position: relative; z-index: 1; }

/* ===== Hero ===== */
.result-hero {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 32px;
  align-items: center;
  padding: 36px;
  margin-bottom: 20px;
  position: relative;
  overflow: hidden;
}
.result-hero-glow {
  position: absolute;
  top: -60px; right: -60px;
  width: 250px; height: 250px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(245,158,11,0.18) 0%, transparent 70%);
  pointer-events: none;
}
.result-hero-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-top: 16px;
  font-size: 0.8rem;
  color: rgba(255,255,255,0.5);
}
.result-hero-meta span {
  display: flex;
  align-items: center;
  gap: 5px;
}
.result-hero-right { flex-shrink: 0; }
.result-donut-group {
  display: flex;
  gap: 24px;
  align-items: center;
}

/* ===== AI Callout ===== */
.callout-ai-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 14px;
}

/* ===== Weak Points Grid ===== */
.weak-points-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(140px, 1fr));
  gap: 12px;
}
.weak-point-card {
  padding: 16px 12px;
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  text-align: center;
  transition: box-shadow var(--transition);
}
.weak-point-card:hover { box-shadow: var(--shadow-md); }
.weak-point-name { font-size: 0.82rem; font-weight: 500; line-height: 1.3; }
.wp-low  { border-left: 3px solid var(--danger); }
.wp-mid  { border-left: 3px solid var(--accent); }
.wp-ok   { border-left: 3px solid var(--success); }

/* ===== Question Review ===== */
.question-review-v2 {
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  overflow: hidden;
  margin-bottom: 12px;
}
.question-review-v2:last-child { margin-bottom: 0; }
.qr-correct { border-left: 4px solid var(--success); }
.qr-wrong   { border-left: 4px solid var(--danger); }
.qr-header {
  padding: 12px 16px;
  border-bottom: 1px solid var(--border);
  background: var(--bg-alt);
}
.qr-num-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.qr-num { font-size: 0.82rem; font-weight: 600; }
.kp-badge { font-size: 0.72rem; }
.qr-body { padding: 16px; }
.answer-compare {
  display: flex;
  gap: 12px;
  margin: 12px 0;
  flex-wrap: wrap;
}
.answer-box {
  flex: 1;
  min-width: 120px;
  padding: 10px 14px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
}
.answer-box-correct { background: var(--success-light); border-color: var(--success); }
.answer-box-wrong   { background: var(--danger-light);  border-color: var(--danger); }
.answer-box-label { font-size: 0.72rem; color: var(--text-muted); margin-bottom: 4px; }
.answer-box-val   { font-size: 1rem; font-weight: 700; }
.error-analysis-v2 {
  padding: 12px 14px;
  background: var(--accent-light);
  border-radius: var(--radius-sm);
  border-left: 3px solid var(--accent);
}
.error-analysis-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--accent-hover);
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
  .result-hero { grid-template-columns: 1fr; }
  .result-hero-right { display: none; }
  .result-donut-group { justify-content: center; }
}
</style>
