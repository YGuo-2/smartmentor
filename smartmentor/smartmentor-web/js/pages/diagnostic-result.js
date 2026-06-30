import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { renderLatex, showToast } from '../state.js'

export const DiagnosticResultPage = {
  name: 'DiagnosticResultPage',
  props: {
    diagnosticId: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const router = useRouter()

    const loading = ref(true)
    const result = ref(null)
    const questions = ref([])
    const weakPoints = ref([])
    const analyzing = ref(false)
    const generating = ref(false)

    const loadResult = async () => {
      try {
        loading.value = true
        const data = await api.diagnostic.detail(props.diagnosticId)
        result.value = data
        questions.value = normalizeQuestions(data.answerRecords || data.questions || [])
        weakPoints.value = data.weakPoints || []

        await nextTick()
        const container = document.querySelector('.question-review-list')
        if (container) {
          renderLatex(container)
        }
      } catch (e) {
        showToast('加载诊断结果失败', 'error')
        console.error(e)
      } finally {
        loading.value = false
      }
    }

    const handleTracing = async () => {
      if (!weakPoints.value.length) {
        showToast('暂无薄弱知识点可分析', 'warning')
        return
      }
      try {
        analyzing.value = true
        const knowledgePointIds = weakPoints.value
          .map(wp => wp.knowledgePointId || wp.kpId)
          .filter(Boolean)
        const { tracingId } = await api.tracing.analyze({
          diagnosticId: props.diagnosticId,
          knowledgePointIds
        })
        router.push(`/tracing/${tracingId}`)
      } catch (e) {
        showToast('溯因分析失败', 'error')
        console.error(e)
      } finally {
        analyzing.value = false
      }
    }

    const handleGeneratePath = async () => {
      try {
        generating.value = true
        const knowledgePointIds = weakPoints.value
          .map(wp => wp.knowledgePointId || wp.kpId)
          .filter(Boolean)
        const tracingData = await api.tracing.analyze({
          diagnosticId: props.diagnosticId,
          knowledgePointIds
        })
        await api.learning.generate({
          tracingId: tracingData.tracingId,
          targetKnowledgePointId: weakPoints.value[0]?.knowledgePointId || weakPoints.value[0]?.kpId || 'review'
        })
        router.push('/learning')
      } catch (e) {
        showToast('生成学习路径失败', 'error')
        console.error(e)
      } finally {
        generating.value = false
      }
    }

    const handleBack = () => {
      router.push('/diagnostic/history')
    }

    const toPercent = (value) => {
      const numeric = Number(value || 0)
      return Math.round(numeric <= 1 ? numeric * 100 : numeric)
    }

    const normalizeQuestions = (items) => items.map(item => ({
      ...item,
      correct: item.correct ?? item.isCorrect,
      userAnswer: item.userAnswer ?? item.studentAnswer,
      difficulty: item.difficulty || 'medium'
    }))

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const d = new Date(dateStr)
      return d.toLocaleString('zh-CN')
    }

    const getMasteryLabel = (level) => {
      const map = {
        'excellent': '优秀',
        'good': '良好',
        'medium': '中等',
        'weak': '薄弱',
        'poor': '较差'
      }
      return map[level] || level
    }

    const getDifficultyLabel = (difficulty) => {
      const map = {
        'easy': '简单',
        'medium': '中等',
        'hard': '困难'
      }
      return map[difficulty] || difficulty
    }

    onMounted(() => {
      loadResult()
    })

    return {
      loading,
      result,
      questions,
      weakPoints,
      analyzing,
      generating,
      handleTracing,
      handleGeneratePath,
      handleBack,
      formatDate,
      getMasteryLabel,
      getDifficultyLabel,
      toPercent
    }
  },
  template: `
    <div class="diagnostic-result-page">
      <nav class="topnav">
        <div class="topnav-brand">SmartMentor</div>
        <div class="topnav-links">
          <router-link to="/dashboard" class="topnav-link">首页</router-link>
          <router-link to="/diagnostic/history" class="topnav-link">历史记录</router-link>
        </div>
      </nav>

      <div class="page-container" v-if="!loading && result">
        <!-- Summary Card -->
        <div class="card result-summary">
          <div class="card-header">
            <h2>诊断结果总览</h2>
          </div>
          <div class="card-body">
            <div class="summary-grid">
              <div class="summary-item">
                <span class="summary-label">模块名称</span>
                <span class="summary-value">{{ result.module }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">诊断时间</span>
                <span class="summary-value">{{ formatDate(result.startTime || result.createdAt) }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">综合得分</span>
                <span class="summary-value score">{{ toPercent(result.accuracy ?? result.score) }}%</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">掌握程度</span>
                <span class="summary-value">
                  <span class="badge" :class="'badge-' + result.masteryLevel">
                    {{ getMasteryLabel(result.masteryLevel) }}
                  </span>
                </span>
              </div>
              <div class="summary-item">
                <span class="summary-label">题目总数</span>
                <span class="summary-value">{{ result.totalQuestions }}</span>
              </div>
              <div class="summary-item">
                <span class="summary-label">正确数量</span>
                <span class="summary-value">{{ result.correctCount }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Questions Detail List -->
        <div class="card">
          <div class="card-header">
            <h2>题目详情</h2>
          </div>
          <div class="card-body question-review-list">
            <div class="question-review" v-for="(q, index) in questions" :key="q.questionId">
              <div class="question-header">
                <span class="question-number">第 {{ index + 1 }} 题</span>
                <span class="badge" :class="q.correct ? 'badge-correct' : 'badge-wrong'">
                  {{ q.correct ? '正确' : '错误' }}
                </span>
                <span class="badge badge-difficulty">{{ getDifficultyLabel(q.difficulty) }}</span>
              </div>
              <div class="question-content" v-html="q.content"></div>
              <div class="question-options" v-if="q.options && q.options.length">
                <div class="option-item" v-for="(opt, oi) in q.options" :key="oi"
                     :class="{
                       'option-correct': opt.key === q.correctAnswer,
                       'option-wrong': opt.key === q.userAnswer && !q.correct
                     }">
                  <span class="option-key">{{ opt.key }}</span>
                  <span class="option-text">{{ opt.text }}</span>
                </div>
              </div>
              <div class="answer-info">
                <span class="user-answer">你的答案：<strong>{{ q.userAnswer }}</strong></span>
                <span class="correct-answer">正确答案：<strong>{{ q.correctAnswer }}</strong></span>
              </div>
              <div class="error-analysis" v-if="q.errorAnalysis && !q.correct">
                <h4>AI 错因分析</h4>
                <div class="analysis-item">
                  <span class="analysis-label">错误类型：</span>
                  <span class="analysis-value">{{ q.errorAnalysis.errorType }}</span>
                </div>
                <div class="analysis-item">
                  <span class="analysis-label">详细解释：</span>
                  <span class="analysis-value">{{ q.errorAnalysis.explanation }}</span>
                </div>
                <div class="analysis-item" v-if="q.errorAnalysis.relatedKnowledge">
                  <span class="analysis-label">关联知识点：</span>
                  <span class="analysis-value">{{ q.errorAnalysis.relatedKnowledge }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Weakness Analysis -->
        <div class="card" v-if="weakPoints.length">
          <div class="card-header">
            <h2>薄弱知识点分析</h2>
          </div>
          <div class="card-body">
            <ul class="weak-points-list">
              <li v-for="(wp, index) in weakPoints" :key="index" class="weak-point-item">
                <div class="weak-point-name">{{ wp.knowledgePoint }}</div>
                <div class="weak-point-detail">
                  <span class="badge badge-mastery">掌握度：{{ wp.mastery }}%</span>
                  <span class="weak-point-prereq" v-if="wp.prerequisite">前置知识：{{ wp.prerequisite }}</span>
                </div>
              </li>
            </ul>
          </div>
        </div>

        <!-- Action Buttons -->
        <div class="action-buttons">
          <button class="btn btn-dark" @click="handleTracing" :disabled="analyzing">
            {{ analyzing ? '分析中...' : '溯因分析' }}
          </button>
          <button class="btn btn-dark" @click="handleGeneratePath" :disabled="generating">
            {{ generating ? '生成中...' : '生成学习路径' }}
          </button>
          <button class="btn btn-outline" @click="handleBack">返回历史</button>
        </div>
      </div>

      <div class="page-container" v-if="loading">
        <div class="loading-state">加载中...</div>
      </div>
    </div>
  `
}
