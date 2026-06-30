<template>
<div class="diagnostic-page" ref="pageRoot">

  <div class="main-content">
    <div class="page-wrap-sm">

      <!-- Start Screen: Module Selection -->
      <div v-if="!started">

        <!-- Page Hero -->
        <div class="diag-hero anim-1">
          <div>
            <div class="page-hero-label page-hero--amber">
              <i class="ri-test-tube-line"></i> 自适应诊断
            </div>
            <h1 style="font-size:2rem;font-weight:800;margin:4px 0 8px">选择诊断课程</h1>
            <p style="color:var(--text-secondary);font-size:0.88rem;margin:0">系统将通过自适应题目精准诊断你的知识掌握情况</p>
          </div>
          <!-- Phase indicator -->
          <div class="phase-indicator">
            <div class="phase-step active">
              <div class="phase-dot"><i class="ri-checkbox-blank-circle-fill"></i></div>
              <span class="phase-label">选择课程</span>
            </div>
            <div class="phase-line"></div>
            <div class="phase-step">
              <div class="phase-dot"><i class="ri-edit-2-line"></i></div>
              <span class="phase-label">作答</span>
            </div>
            <div class="phase-line"></div>
            <div class="phase-step">
              <div class="phase-dot"><i class="ri-bar-chart-box-line"></i></div>
              <span class="phase-label">查看结果</span>
            </div>
          </div>
        </div>

        <div class="diag-profile-section anim-2">
          <div class="diag-profile-head"><i class="ri-user-settings-line"></i> 学习画像上下文</div>
          <div class="diagnostic-profile-grid">
              <div class="form-group">
                <label class="form-label">专业方向</label>
                <select class="form-input" v-model="diagnosticProfile.majorDirection" aria-label="专业方向">
                  <option value="计算机类">计算机类</option>
                  <option value="软件工程">软件工程</option>
                  <option value="电子信息">电子信息</option>
                  <option value="自动化">自动化</option>
                  <option value="数据科学">数据科学</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">学历层次</label>
                <select class="form-input" v-model="diagnosticProfile.educationLevel" aria-label="学历层次">
                  <option value="高职">高职</option>
                  <option value="本科">本科</option>
                  <option value="研究生">研究生</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">当前课程</label>
                <select class="form-input" v-model="diagnosticProfile.currentCourse" aria-label="当前课程">
                  <option value="人工智能基础">人工智能基础</option>
                  <option value="Java Web 开发">Java Web 开发</option>
                  <option value="数字电路基础">数字电路基础</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">学习目标</label>
                <select class="form-input" v-model="diagnosticProfile.learningGoal" aria-label="学习目标">
                  <option value="考试复习">考试复习</option>
                  <option value="项目实践">项目实践</option>
                  <option value="科研入门">科研入门</option>
                  <option value="就业技能">就业技能</option>
                  <option value="竞赛提升">竞赛提升</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">基础水平</label>
                <select class="form-input" v-model="diagnosticProfile.foundationLevel" aria-label="基础水平">
                  <option value="入门">入门</option>
                  <option value="基础">基础</option>
                  <option value="进阶">进阶</option>
                </select>
              </div>
              <div class="form-group">
                <label class="form-label">兴趣方向</label>
                <input class="form-input" v-model="diagnosticProfile.academicInterest" placeholder="如 大模型应用、Web 后端" />
              </div>
            </div>
        </div>

        <!-- Module Grid -->
        <div class="module-grid-v2 anim-2">
          <div
            v-for="m in modules"
            :key="m.key"
            class="module-card-v2"
            :class="{ selected: selectedModule === m.key }"
            :style="`--mod-color: ${m.color}`"
            @click="selectCard(m.key)"
          >
            <div class="module-icon-wrap">
              <div class="icon-badge" :style="`background: ${m.color}18; color: ${m.color}`">
                <i :class="m.icon"></i>
              </div>
            </div>
            <div class="module-card-body">
              <div class="module-name-v2">{{ m.name }}</div>
              <div class="module-desc">{{ m.desc }}</div>
            </div>
            <div class="module-check" v-if="selectedModule === m.key">
              <i class="ri-check-line"></i>
            </div>
          </div>

          <!-- 自定义科目 -->
          <div
            class="module-card-v2 custom-subject-card"
            :class="{ selected: customMode }"
            style="--mod-color: #8b5cf6"
            @click="enableCustomMode"
          >
            <div class="module-icon-wrap">
              <div class="icon-badge" style="background:#8b5cf618;color:#8b5cf6">
                <i class="ri-add-circle-line"></i>
              </div>
            </div>
            <div class="module-card-body">
              <div class="module-name-v2">其他科目</div>
              <div class="module-desc">输入任意科目，AI 为你出题诊断</div>
              <input
                v-if="customMode"
                ref="customInputRef"
                v-model="customSubject"
                class="custom-subject-input"
                type="text"
                placeholder="如：高等数学、大学英语、数据结构…"
                @click.stop
                @keydown.enter="startDiagnostic"
              />
            </div>
            <div class="module-check" v-if="customMode && customSubject.trim()">
              <i class="ri-check-line"></i>
            </div>
          </div>
        </div>

        <div class="anim-3" style="text-align:center;margin-top:32px">
          <button class="btn btn-dark btn-lg" :disabled="!canStart || loading" @click="startDiagnostic">
            <span v-if="loading" class="spinner" style="width:16px;height:16px"></span>
            <template v-else>
              <i class="ri-play-fill"></i> 开始诊断
            </template>
          </button>
          <div v-if="loading" class="loading-hint">
            <p>{{ loadingHint }}</p>
          </div>
        </div>
      </div>

      <!-- Test In Progress -->
      <div v-else>

        <!-- Phase indicator (step 2 active) -->
        <div class="diag-hero anim-1">
          <div style="flex:1">
            <div class="page-hero-label page-hero--amber">
              <i class="ri-test-tube-line"></i> 自适应诊断 · {{ selectedModule }}
            </div>
            <h1 style="font-size:1.6rem;font-weight:800;margin:4px 0 0">正在作答</h1>
          </div>
          <button class="btn btn-outline btn-sm quit-btn" @click="showQuitConfirm = true" :disabled="submitting">
            <i class="ri-logout-box-r-line"></i> 中断诊断
          </button>
        </div>
        <div class="phase-indicator" style="margin-bottom:20px">
          <div class="phase-step done">
            <div class="phase-dot"><i class="ri-check-line"></i></div>
            <span class="phase-label">选择课程</span>
          </div>
          <div class="phase-line done"></div>
          <div class="phase-step active">
            <div class="phase-dot"><i class="ri-edit-2-line"></i></div>
            <span class="phase-label">作答</span>
          </div>
          <div class="phase-line"></div>
          <div class="phase-step">
            <div class="phase-dot"><i class="ri-bar-chart-box-line"></i></div>
            <span class="phase-label">查看结果</span>
          </div>
        </div>

        <!-- Progress bar -->
        <div class="question-progress-bar anim-2">
          <div
            class="question-progress-fill"
            :style="`width: ${progressPercent}%`"
          ></div>
        </div>

        <!-- Question header -->
        <div class="question-header-v2 anim-2">
          <span class="q-counter">
            <span class="q-num">{{ currentQuestionIndex }}</span>
            <span class="q-sep">/</span>
            <span class="q-total">约{{ totalEstimated }}题</span>
          </span>
          <div class="q-header-right">
            <span class="q-timer"><i class="ri-time-line"></i> {{ elapsedDisplay }}</span>
            <span class="badge" :class="difficultyBadgeClass">
              <i :class="difficultyIcon"></i> {{ difficultyLabel }}
            </span>
          </div>
        </div>

        <!-- Question Card -->
        <div class="card question-card-v2 anim-3" ref="questionCardRef">
          <div class="question-content" ref="questionContentRef" v-html="currentQuestion.content"></div>

          <!-- Options (multiple choice) -->
          <div class="option-list-v2" v-if="currentQuestion.options && currentQuestion.options.length">
            <div
              v-for="opt in currentQuestion.options"
              :key="opt.label"
              class="option-item-v2"
              :class="optionClass(opt.label)"
              :tabindex="feedback ? -1 : 0"
              role="button"
              :aria-pressed="selectedAnswer === opt.label"
              @click="selectOption(opt.label)"
              @keydown.enter.prevent="selectOption(opt.label)"
              @keydown.space.prevent="selectOption(opt.label)"
            >
              <span class="option-letter">{{ opt.label }}</span>
              <span class="option-text-v2" v-html="opt.text"></span>
              <i
                v-if="feedback && opt.label === feedback.correctAnswer"
                class="option-result-icon ri-check-circle-fill"
                style="color: var(--success)"
              ></i>
              <i
                v-else-if="feedback && opt.label === selectedAnswer && !feedback.correct"
                class="option-result-icon ri-close-circle-fill"
                style="color: var(--danger)"
              ></i>
            </div>
          </div>

          <!-- Fill / Subjective answer -->
          <div v-else class="fill-answer-v2">
            <label class="fill-label">
              <i class="ri-pencil-line"></i>
              {{ currentQuestion.type === 'subjective' ? '请作答（简答/论述）' : '请输入你的答案' }}
            </label>
            <textarea
              v-if="currentQuestion.type === 'subjective'"
              class="form-input subjective-textarea"
              v-model="fillAnswer"
              placeholder="请写出你的解答，AI 将根据要点评分"
              :disabled="!!feedback"
              rows="5"
            ></textarea>
            <input
              v-else
              class="form-input"
              v-model="fillAnswer"
              placeholder="输入答案后点击提交"
              :disabled="!!feedback"
            />
          </div>
        </div>

        <!-- Submit button (隐藏于反馈态，由「下一题」接管) -->
        <div v-if="!feedback" class="anim-4" style="text-align:center;margin-top:20px">
          <button class="btn btn-dark btn-lg" :disabled="!hasAnswer || submitting" @click="submitAnswer">
            <span v-if="submitting" class="spinner" style="width:16px;height:16px"></span>
            <template v-else>
              <i class="ri-send-plane-fill"></i> 提交答案
            </template>
          </button>
        </div>

        <!-- Feedback -->
        <div v-if="feedback" class="feedback-card anim-4" :class="feedback.correct ? 'feedback-correct' : 'feedback-wrong'">
          <div class="feedback-header">
            <div class="icon-badge icon-badge-sm" :class="feedback.correct ? 'icon-badge-green' : 'icon-badge-red'">
              <i :class="feedback.correct ? 'ri-check-line' : 'ri-close-line'"></i>
            </div>
            <div class="feedback-title">{{ feedback.correct ? '回答正确！' : '回答错误' }}</div>
          </div>
          <div v-if="feedbackIsSubjective && feedback.correctAnswer" class="feedback-answer">
            <span class="feedback-answer-label">参考答案</span>
            <span class="feedback-answer-val">{{ feedback.correctAnswer }}</span>
          </div>
          <div v-else-if="!feedback.correct && feedback.correctAnswer" class="feedback-answer">
            <span class="feedback-answer-label">正确答案</span>
            <span class="feedback-answer-val">{{ feedback.correctAnswer }}</span>
          </div>
          <div v-if="feedback.errorAnalysis && (feedbackIsSubjective || !feedback.correct)" class="feedback-explanation">
            <i class="ri-lightbulb-line"></i> {{ feedback.errorAnalysis }}
          </div>

          <!-- 看完反馈后手动进入下一题 / 查看结果 -->
          <div class="feedback-actions">
            <button v-if="!isLastQuestion" class="btn btn-dark" @click="goToNextQuestion">
              下一题 <i class="ri-arrow-right-line"></i>
            </button>
            <button v-else class="btn btn-dark" @click="goToResult">
              <i class="ri-bar-chart-box-line"></i> 查看诊断结果
            </button>
          </div>
        </div>

        <!-- Quit Confirm Modal -->
        <Teleport to="body">
          <div v-if="showQuitConfirm" class="modal-overlay" @click.self="showQuitConfirm = false">
            <div class="modal-card">
              <div class="modal-header">
                <div class="icon-badge icon-badge-sm" style="background:var(--danger-light);color:var(--danger)">
                  <i class="ri-error-warning-line"></i>
                </div>
                <h3 class="modal-title">确认中断诊断？</h3>
              </div>
              <p class="modal-desc">中断后系统将根据已作答题目生成诊断报告，未作答部分不计入评估。</p>
              <div class="modal-actions">
                <button class="btn btn-outline btn-sm" @click="showQuitConfirm = false">继续作答</button>
                <button class="btn btn-danger btn-sm" :disabled="quitting" @click="quitDiagnostic">
                  <span v-if="quitting" class="spinner" style="width:14px;height:14px"></span>
                  <template v-else><i class="ri-logout-box-r-line"></i> 确认中断</template>
                </button>
              </div>
            </div>
          </div>
        </Teleport>

      </div>

    </div>

    <!-- Conflict Confirm Modal -->
    <Teleport to="body">
      <div v-if="showConflictConfirm" class="modal-overlay" @click.self="showConflictConfirm = false">
        <div class="modal-card">
          <div class="modal-header">
            <div class="icon-badge icon-badge-sm" style="background:var(--accent-light);color:var(--accent)">
              <i class="ri-error-warning-line"></i>
            </div>
            <h3 class="modal-title">存在进行中的诊断</h3>
          </div>
          <p class="modal-desc">你有一个尚未完成的诊断会话，请选择操作：</p>
          <div class="modal-actions">
            <button class="btn btn-outline btn-sm" @click="goToExisting">
              <i class="ri-eye-line"></i> 查看结果
            </button>
            <button class="btn btn-danger btn-sm" :disabled="loading" @click="forceRestart">
              <span v-if="loading" class="spinner" style="width:14px;height:14px"></span>
              <template v-else><i class="ri-refresh-line"></i> 结束并重新开始</template>
            </button>
          </div>
        </div>
      </div>
    </Teleport>

  </div>
</div>
</template>

<script setup>
import { ref, computed, nextTick, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { api } from '../api/index.js'
import { renderLatex, showToast } from '../composables/state.js'
import { usePageReveal } from '../composables/usePageReveal.js'
import { gsap, prefersReducedMotion } from '../lib/gsap.js'

const router = useRouter()
const route = useRoute()

const pageRoot = ref(null)
const { replayMotion } = usePageReveal(pageRoot, { runOnMounted: false })

const modules = [
  { key: '人工智能基础', name: '人工智能基础', icon: 'ri-brain-line', color: '#f59e0b', desc: '搜索、机器学习、智能体' },
  { key: 'Java Web 开发', name: 'Java Web 开发', icon: 'ri-code-box-line', color: '#3b82f6', desc: 'Spring Boot、REST、数据库' },
  { key: '数字电路基础', name: '数字电路基础', icon: 'ri-cpu-line', color: '#22c55e', desc: '逻辑门、触发器、时序电路' }
]

const selectedModule = ref(null)
const customMode = ref(false)
const customSubject = ref('')
const customInputRef = ref(null)
const diagnosticProfile = ref({
  majorDirection: '计算机类',
  educationLevel: '本科',
  currentCourse: '人工智能基础',
  learningGoal: '项目实践',
  foundationLevel: '基础',
  academicInterest: ''
})
const started = ref(false)
const loading = ref(false)
const submitting = ref(false)
const diagnosticId = ref(null)
const currentQuestion = ref(null)
const currentQuestionIndex = ref(1)
const answeredCount = ref(0)
const totalEstimated = ref(8)
const selectedAnswer = ref(null)
const fillAnswer = ref('')
const feedback = ref(null)
const isLastQuestion = ref(false)
const pendingNext = ref(null)
const pendingNextMeta = ref(null)
const questionStartTime = ref(null)
const elapsedSeconds = ref(0)
let timerHandle = null
const questionContentRef = ref(null)
const questionCardRef = ref(null)
const showQuitConfirm = ref(false)
const quitting = ref(false)
const difficultyLabel = ref('')
const difficultyBadgeClass = ref('badge-info')
const difficultyIcon = ref('ri-equal-line')
const loadingHint = ref('AI 正在生成自适应题目...')
let loadingHintTimer = null

const hasAnswer = computed(() => {
  if (feedback.value) return false
  if (currentQuestion.value && currentQuestion.value.options && currentQuestion.value.options.length) {
    return selectedAnswer.value !== null
  }
  return fillAnswer.value.trim() !== ''
})

// 进度：按已完成（已提交反馈）题数占比，避免第 1 题恒显示 0%
const progressPercent = computed(() => {
  const total = totalEstimated.value || 1
  return Math.min(100, Math.round((answeredCount.value / total) * 100))
})

// 本题已用时 mm:ss
const elapsedDisplay = computed(() => {
  const s = elapsedSeconds.value
  const mm = String(Math.floor(s / 60)).padStart(2, '0')
  const ss = String(s % 60).padStart(2, '0')
  return `${mm}:${ss}`
})

function startTimer() {
  stopTimer()
  elapsedSeconds.value = 0
  timerHandle = setInterval(() => {
    elapsedSeconds.value = Math.floor((Date.now() - questionStartTime.value) / 1000)
  }, 1000)
}

function stopTimer() {
  if (timerHandle) {
    clearInterval(timerHandle)
    timerHandle = null
  }
}

// 可开始诊断：选了预定义科目，或自定义模式且填了科目名
const canStart = computed(() =>
  customMode.value ? customSubject.value.trim() !== '' : !!selectedModule.value
)

// 当前反馈是否为主观/填空题（AI 判分，给参考答案而非硬比对）
const feedbackIsSubjective = computed(() =>
  feedback.value && (feedback.value.questionType === 'subjective' || feedback.value.questionType === 'fill')
)

function enableCustomMode() {
  customMode.value = true
  selectedModule.value = null
  nextTick(() => { if (customInputRef.value) customInputRef.value.focus() })
}

function selectCard(key) {
  selectedModule.value = key
  customMode.value = false
}

function applyPrefilledModule() {
  const raw = route.query.module
  const moduleName = Array.isArray(raw) ? raw[0] : raw
  if (!moduleName || !String(moduleName).trim()) {
    return
  }
  const decoded = String(moduleName).trim()
  const knownModule = modules.find(m => m.key === decoded || m.name === decoded)
  if (knownModule) {
    selectedModule.value = knownModule.key
    customMode.value = false
    diagnosticProfile.value.currentCourse = knownModule.key
  } else {
    customMode.value = true
    selectedModule.value = null
    customSubject.value = decoded
    diagnosticProfile.value.currentCourse = decoded
  }
}

function updateDifficulty(difficulty) {
  if (difficulty == null) {
    difficultyLabel.value = '中等'; difficultyBadgeClass.value = 'badge-accent'; difficultyIcon.value = 'ri-equal-line'; return
  }
  const d = typeof difficulty === 'number' ? difficulty : parseFloat(difficulty)
  if (d <= 0.4) {
    difficultyLabel.value = '简单'; difficultyBadgeClass.value = 'badge-success'; difficultyIcon.value = 'ri-seedling-line'
  } else if (d <= 0.7) {
    difficultyLabel.value = '中等'; difficultyBadgeClass.value = 'badge-accent'; difficultyIcon.value = 'ri-equal-line'
  } else {
    difficultyLabel.value = '困难'; difficultyBadgeClass.value = 'badge-danger'; difficultyIcon.value = 'ri-fire-line'
  }
}

const showConflictConfirm = ref(false)
const conflictDiagnosticId = ref(null)

async function startDiagnostic() {
  const effectiveModule = customMode.value ? customSubject.value.trim() : selectedModule.value
  if (!effectiveModule) return
  loading.value = true
  loadingHint.value = 'AI 正在生成自适应题目...'
  const hints = ['正在分析知识点覆盖...', '根据学情调整难度...', '题目生成中，请稍候...']
  let hintIdx = 0
  loadingHintTimer = setInterval(() => {
    loadingHint.value = hints[hintIdx % hints.length]
    hintIdx++
  }, 3000)
  try {
    const res = await api.diagnostic.start({
      module: effectiveModule,
      ...diagnosticProfile.value,
      // 自定义科目时，currentCourse 也用该科目，保证画像上下文一致
      ...(customMode.value ? { currentCourse: effectiveModule } : {})
    })
    enterSession(res)
  } catch (e) {
    const existingId = e.responseData?.diagnosticId
    if (existingId) {
      conflictDiagnosticId.value = existingId
      showConflictConfirm.value = true
    } else {
      showToast(e.message || '启动诊断失败', 'error')
    }
  }
  finally {
    loading.value = false
    clearInterval(loadingHintTimer)
    loadingHintTimer = null
  }
}

function enterSession(res) {
  diagnosticId.value = res.diagnosticId
  currentQuestion.value = res.question
  currentQuestionIndex.value = res.question.questionNumber || 1
  totalEstimated.value = res.totalEstimatedQuestions || 8
  answeredCount.value = 0
  isLastQuestion.value = false
  updateDifficulty(res.question.difficulty)
  started.value = true
  selectedAnswer.value = null
  fillAnswer.value = ''
  feedback.value = null
  questionStartTime.value = Date.now()
  startTimer()
  nextTick(() => {
    if (questionContentRef.value) renderLatex(questionContentRef.value)
    replayMotion()
  })
}

async function forceRestart() {
  showConflictConfirm.value = false
  loading.value = true
  try {
    const effectiveModule = customMode.value ? customSubject.value.trim() : selectedModule.value
    await api.diagnostic.finish({ diagnosticId: conflictDiagnosticId.value })
    const res = await api.diagnostic.start({
      module: effectiveModule,
      ...diagnosticProfile.value,
      ...(customMode.value ? { currentCourse: effectiveModule } : {})
    })
    enterSession(res)
  } catch (e) { showToast(e.message || '重新开始失败', 'error') }
  finally { loading.value = false }
}

function goToExisting() {
  showConflictConfirm.value = false
  router.push('/diagnostic/result/' + conflictDiagnosticId.value)
}

function selectOption(label) {
  if (feedback.value) return
  selectedAnswer.value = label
}

function optionClass(label) {
  if (feedback.value) {
    if (label === feedback.value.correctAnswer) return 'correct'
    if (label === selectedAnswer.value && !feedback.value.correct) return 'wrong'
    return ''
  }
  if (label === selectedAnswer.value) return 'selected'
  return ''
}

async function quitDiagnostic() {
  quitting.value = true
  showQuitConfirm.value = false
  // 先跳转，finish 在后台执行
  api.diagnostic.finish({ diagnosticId: diagnosticId.value }).catch(() => {})
  router.push('/diagnostic/result/' + diagnosticId.value)
}

async function submitAnswer() {
  if (submitting.value) return
  const answer = (currentQuestion.value.options && currentQuestion.value.options.length)
    ? selectedAnswer.value
    : fillAnswer.value.trim()
  if (!answer) return

  submitting.value = true
  const timeSpent = Math.round((Date.now() - questionStartTime.value) / 1000)
  try {
    const res = await api.diagnostic.submit({
      diagnosticId: diagnosticId.value,
      questionId: currentQuestion.value.questionId,
      answer: answer,
      timeSpent
    })

    const result = res.result || {}
    feedback.value = {
      correct: result.isCorrect,
      correctAnswer: result.correctAnswer,
      questionType: result.questionType,
      errorAnalysis: result.errorAnalysis || result.errorDetail
    }
    // 停表：提交后不再计时，等用户看完解析手动进入下一题
    stopTimer()
    answeredCount.value += 1
    isLastQuestion.value = !!res.isFinished
    // 暂存下一题数据，供「下一题」按钮使用（不再自动跳转）
    pendingNext.value = res.isFinished ? null : (res.nextQuestion || null)
    pendingNextMeta.value = {
      currentQuestionIndex: res.currentQuestionIndex,
      totalEstimatedQuestions: res.totalEstimatedQuestions
    }
  } catch (e) { showToast(e.message || '提交答案失败', 'error') }
  finally { submitting.value = false }
}

// 进入下一题：用户看完反馈后手动触发
async function goToNextQuestion() {
  if (!pendingNext.value) return
  const next = pendingNext.value
  const meta = pendingNextMeta.value || {}
  currentQuestion.value = next
  currentQuestionIndex.value = meta.currentQuestionIndex || (currentQuestionIndex.value + 1)
  totalEstimated.value = meta.totalEstimatedQuestions || totalEstimated.value
  updateDifficulty(next.difficulty)
  selectedAnswer.value = null
  fillAnswer.value = ''
  feedback.value = null
  pendingNext.value = null
  questionStartTime.value = Date.now()
  startTimer()
  await nextTick()
  if (questionContentRef.value) renderLatex(questionContentRef.value)
  // 换题轻量过场
  if (!prefersReducedMotion() && questionCardRef.value) {
    gsap.from(questionCardRef.value, { autoAlpha: 0, y: 16, duration: 0.4, ease: 'power2.out', clearProps: 'opacity,visibility,transform' })
  }
}

// 查看诊断结果：最后一题提交后触发，finish 在后台执行
function goToResult() {
  api.diagnostic.finish({ diagnosticId: diagnosticId.value }).catch(() => {})
  router.push('/diagnostic/result/' + diagnosticId.value)
}

onMounted(() => {
  applyPrefilledModule()
  replayMotion()
})

onUnmounted(() => {
  stopTimer()
})
</script>

<style scoped>
/* 入场动画交给 usePageReveal(gsap) 驱动，关掉全局 .anim-* 的 CSS fadeUp 避免双重动画 */
.diagnostic-page :deep(.anim-1),
.diagnostic-page :deep(.anim-2),
.diagnostic-page :deep(.anim-3),
.diagnostic-page :deep(.anim-4),
.diagnostic-page :deep(.anim-5),
.diagnostic-page :deep(.anim-6) {
  animation: none;
}

/* ===== Loading Hint ===== */
.loading-hint {
  margin-top: 16px;
  font-size: 0.84rem;
  color: var(--text-muted);
  animation: fadeInUp 0.3s ease;
}
.loading-hint p {
  margin: 0;
  transition: opacity 0.3s ease;
}

/* ===== Hero / Phase indicator ===== */
.diag-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 20px;
  margin-bottom: 28px;
}

.phase-indicator {
  display: flex;
  align-items: center;
  gap: 0;
  flex-shrink: 0;
}
.phase-step {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}
.phase-dot {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.85rem;
  background: var(--bg-alt);
  color: var(--text-muted);
  border: 2px solid var(--border);
  transition: all var(--transition);
}
.phase-step.active .phase-dot {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
  box-shadow: 0 0 0 4px rgba(245,158,11,0.15);
}
.phase-step.done .phase-dot {
  background: var(--success);
  color: #fff;
  border-color: var(--success);
}
.phase-label {
  font-size: 0.7rem;
  color: var(--text-muted);
  white-space: nowrap;
}
.phase-step.active .phase-label { color: var(--accent); font-weight: 600; }
.phase-step.done .phase-label { color: var(--success); }
.phase-line {
  width: 40px;
  height: 2px;
  background: var(--border);
  margin-bottom: 18px;
  flex-shrink: 0;
}
.phase-line.done { background: var(--success); }

/* ===== Module Grid ===== */
.diag-profile-section {
  margin-bottom: 22px;
  padding-top: 6px;
  border-top: 1px solid var(--border);
}
.diag-profile-head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.82rem;
  font-weight: 700;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
  margin: 14px 0 14px;
}
.diag-profile-head i { color: var(--accent); }
.diagnostic-profile-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}
.module-grid-v2 {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 8px;
}
.module-card-v2 {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 16px;
  border-radius: var(--radius-md);
  border: 1.5px solid var(--border-light, var(--border));
  cursor: pointer;
  transition: all 0.25s ease;
  background: var(--card-bg-solid);
  position: relative;
  overflow: hidden;
}
.module-card-v2::before {
  content: '';
  position: absolute;
  top: 0; left: 0;
  width: 3px;
  height: 100%;
  background: var(--mod-color, var(--accent));
  transform: scaleY(0);
  transition: transform 0.25s ease;
  transform-origin: top;
}
.module-card-v2:hover { border-color: var(--mod-color, var(--accent)); box-shadow: var(--shadow-sm); }
.module-card-v2:hover::before { transform: scaleY(1); }
.module-card-v2:hover .module-icon-wrap { transform: scale(1.08); }
.module-card-v2.selected {
  border-color: var(--mod-color, var(--accent));
  background: var(--bg-alt);
  box-shadow: var(--shadow-md);
}
.module-card-v2.selected::before { transform: scaleY(1); }
.module-icon-wrap { transition: transform 0.25s ease; flex-shrink: 0; }
.module-card-body { flex: 1; min-width: 0; }
.module-name-v2 { font-size: 0.95rem; font-weight: 700; margin-bottom: 3px; }
.module-desc { font-size: 0.75rem; color: var(--text-muted); line-height: 1.4; }
.custom-subject-input {
  margin-top: 10px;
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  font-size: 0.82rem;
  outline: none;
  background: var(--card-bg-solid, #fff);
  color: var(--text);
}
.custom-subject-input:focus { border-color: #8b5cf6; }
.module-check {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--mod-color, var(--accent));
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8rem;
  flex-shrink: 0;
}

/* ===== Question progress bar ===== */
.question-progress-bar {
  height: 3px;
  background: var(--bg-alt);
  border-radius: var(--radius-full);
  margin-bottom: 20px;
  overflow: hidden;
}
.question-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, var(--accent), #fbbf24);
  border-radius: var(--radius-full);
  transition: width 0.6s ease;
}

/* ===== Question header ===== */
.question-header-v2 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}
.q-counter { display: flex; align-items: baseline; gap: 3px; }
.q-num { font-size: 1.4rem; font-weight: 800; color: var(--accent); }
.q-sep { font-size: 1rem; color: var(--text-muted); }
.q-total { font-size: 0.85rem; color: var(--text-muted); }
.q-header-right { display: flex; align-items: center; gap: 12px; }
.q-timer {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.82rem;
  font-weight: 600;
  color: var(--text-secondary);
  font-variant-numeric: tabular-nums;
}
.q-timer i { color: var(--text-muted); }

/* ===== Question Card ===== */
.question-card-v2 {
  margin-bottom: 20px;
  padding: 24px;
}
.question-content {
  font-size: 0.95rem;
  line-height: 1.8;
  margin-bottom: 20px;
  color: var(--text);
}

/* ===== Option Items ===== */
.option-list-v2 {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.option-item-v2 {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 13px 16px;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: all var(--transition);
  background: var(--card-bg-solid);
}
.option-item-v2:hover {
  border-color: var(--accent);
  transform: translateX(4px);
  box-shadow: var(--shadow-sm);
}
.option-item-v2:focus-visible {
  outline: 2px solid var(--accent);
  outline-offset: 2px;
}
.option-item-v2.selected {
  border-color: var(--accent);
  background: var(--accent-light);
  transform: translateX(4px);
}
.option-item-v2.correct {
  border-color: var(--success);
  background: var(--success-light);
  transform: translateX(4px);
}
.option-item-v2.wrong {
  border-color: var(--danger);
  background: var(--danger-light);
}
.option-letter {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--bg-alt);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.82rem;
  font-weight: 700;
  flex-shrink: 0;
  transition: background var(--transition), color var(--transition);
}
.option-item-v2.selected .option-letter { background: var(--accent); color: #fff; }
.option-item-v2.correct  .option-letter { background: var(--success); color: #fff; }
.option-item-v2.wrong    .option-letter { background: var(--danger);  color: #fff; }
.option-text-v2 { flex: 1; font-size: 0.9rem; line-height: 1.5; }
.option-result-icon { font-size: 1.1rem; flex-shrink: 0; }

/* ===== Fill Answer ===== */
.fill-answer-v2 { display: flex; flex-direction: column; gap: 8px; }
.fill-label { font-size: 0.8rem; color: var(--text-muted); display: flex; align-items: center; gap: 5px; }
.subjective-textarea { resize: vertical; min-height: 110px; line-height: 1.6; font-family: inherit; }

/* ===== Feedback Card ===== */
.feedback-card {
  border-radius: var(--radius-md);
  border: 1px solid var(--border);
  padding: 16px 20px;
  margin-top: 16px;
}
.feedback-card.feedback-correct {
  background: var(--success-light);
  border-color: var(--success);
}
.feedback-card.feedback-wrong {
  background: var(--danger-light);
  border-color: var(--danger);
}
.feedback-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}
.feedback-title {
  font-size: 0.95rem;
  font-weight: 700;
}
.feedback-answer {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
  font-size: 0.85rem;
}
.feedback-answer-label {
  color: var(--text-muted);
  flex-shrink: 0;
}
.feedback-answer-val {
  font-weight: 700;
  color: var(--success);
}
.feedback-explanation {
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.6;
  display: flex;
  gap: 6px;
  align-items: flex-start;
}
.feedback-explanation i { flex-shrink: 0; margin-top: 2px; color: var(--accent); }
.feedback-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
.feedback-actions .btn { min-width: 140px; justify-content: center; }

/* ===== Responsive ===== */
@media (max-width: 768px) {
  .diagnostic-profile-grid { grid-template-columns: 1fr; }
  .module-grid-v2 { grid-template-columns: repeat(2, 1fr); }
  .diag-hero { flex-direction: column; gap: 16px; }
  .phase-indicator { align-self: flex-start; }
  .phase-line { width: 24px; }
}
@media (max-width: 480px) {
  .module-grid-v2 { grid-template-columns: 1fr; }
}

/* ===== Quit Button ===== */
.quit-btn {
  color: var(--danger);
  border-color: var(--danger);
  flex-shrink: 0;
  align-self: center;
}
.quit-btn:hover {
  background: var(--danger);
  color: #fff;
}

/* ===== Modal ===== */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  -webkit-backdrop-filter: blur(2px);
  backdrop-filter: blur(2px);
}
.modal-card {
  background: var(--card-bg-solid);
  border-radius: var(--radius-lg);
  padding: 28px 32px;
  max-width: 400px;
  width: 90%;
  box-shadow: var(--shadow-lg);
}
.modal-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}
.modal-title {
  font-size: 1.05rem;
  font-weight: 700;
  margin: 0;
}
.modal-desc {
  font-size: 0.85rem;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 20px;
}
.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
.btn-danger {
  background: var(--danger);
  color: #fff;
  border: none;
}
.btn-danger:hover {
  opacity: 0.9;
}
</style>
