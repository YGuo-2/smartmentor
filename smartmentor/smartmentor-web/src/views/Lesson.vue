<template>
<div class="lesson-page">

  <div class="main-content">
    <div class="page-wrap">
      <div v-if="loading" class="lesson-loading" aria-busy="true" aria-live="polite">
        <!-- 折纸鸟母题 + 文案 -->
        <div class="lesson-loading-banner">
          <PaperBird :size="40" />
          <div class="lesson-loading-texts">
            <div class="lesson-loading-title">加载学习节点中…</div>
            <div class="lesson-loading-sub">AI 正在准备讲解、练习与检查点</div>
          </div>
        </div>

        <!-- 骨架屏：贴合真实布局（顶栏 + hero + 双栏） -->
        <div class="skeleton-topbar">
          <span class="sk sk-back"></span>
          <div class="skeleton-tabs">
            <span class="sk sk-tab" v-for="n in 4" :key="n"></span>
          </div>
        </div>
        <div class="skeleton-hero">
          <span class="sk sk-label"></span>
          <span class="sk sk-title"></span>
          <div class="skeleton-badges">
            <span class="sk sk-badge"></span>
            <span class="sk sk-badge"></span>
            <span class="sk sk-badge"></span>
          </div>
        </div>
        <div class="skeleton-grid">
          <div class="skeleton-panel">
            <span class="sk sk-h2"></span>
            <span class="sk sk-line"></span>
            <span class="sk sk-line"></span>
            <span class="sk sk-line short"></span>
            <span class="sk sk-block"></span>
            <span class="sk sk-line"></span>
            <span class="sk sk-line short"></span>
          </div>
          <div class="skeleton-aside">
            <span class="sk sk-h2"></span>
            <span class="sk sk-chat"></span>
            <span class="sk sk-chat short"></span>
            <span class="sk sk-chat"></span>
          </div>
        </div>
      </div>

      <template v-else-if="nodeData">
        <div class="lesson-topbar">
          <button class="btn-back" @click="goBackToPath">
            <i class="ri-arrow-left-line"></i> 返回路径
          </button>
          <div class="step-tabs" aria-label="节点学习步骤">
            <button v-for="step in steps" :key="step.key" class="step-tab"
              :class="{ active: phase === step.key, done: isStepDone(step.key) }"
              @click="jumpToStep(step.key)">
              <i :class="step.icon"></i>{{ step.label }}
            </button>
          </div>
        </div>

        <section class="task-hero">
          <div>
            <div class="page-hero-label page-hero--green">
              <i class="ri-route-line"></i> 路径节点 {{ props.nodeId }}
            </div>
            <h1 class="task-title">{{ nodeData.title }}</h1>
            <div class="task-meta">
              <span class="badge badge-info">{{ knowledgePointName }}</span>
              <span class="badge badge-yellow">{{ nodeData.teachingStrategyLabel || '自适应辅导' }}</span>
              <span class="badge">检查点 {{ Math.round((nodeData.checkpointThreshold || 0.6) * 100) }}% 通过</span>
            </div>
          </div>
          <button class="btn btn-dark" @click="sendTutorMessage(defaultPrompt)">
            <i class="ri-robot-line"></i> 让 AI 带我学
          </button>
        </section>

        <div class="learning-grid" :class="{ 'tutor-hidden': tutorCollapsed }">
          <main class="task-panel">
            <section v-show="phase === 'learn'" ref="lessonContentRef" class="task-section">
              <div class="section-head">
                <h2><i class="ri-book-open-line"></i> 先学会这个节点</h2>
                <div class="section-head-actions">
                  <button class="btn btn-outline btn-sm" :disabled="pptLoading" @click="openPptViewer">
                    <i :class="pptLoading ? 'ri-loader-4-line spin' : 'ri-slideshow-3-line'"></i>
                    {{ pptLoading ? '生成中…' : '演示 PPT' }}
                  </button>
                  <button class="btn btn-outline btn-sm" @click="phase = 'practice'">
                    去练习 <i class="ri-arrow-right-line"></i>
                  </button>
                </div>
              </div>

              <div class="lesson-video-card" :class="{ playing: videoPlaying, empty: !videoResource }">
                <div class="lesson-video-summary">
                  <div class="lesson-video-icon"><i class="ri-video-line"></i></div>
                  <div class="lesson-video-copy">
                    <strong>课程视频讲解</strong>
                    <span>{{ videoTitle }}</span>
                    <small>{{ videoHint }}</small>
                  </div>
                  <div class="lesson-video-actions">
                    <button type="button" class="btn btn-dark btn-sm" :disabled="!videoResource || videoLoading" @click="toggleVideo">
                      <i :class="videoLoading ? 'ri-loader-4-line' : (videoPlaying ? 'ri-arrow-up-s-line' : 'ri-play-circle-line')"></i>
                      {{ videoButtonLabel }}
                    </button>
                    <a v-if="videoResource?.url" class="btn btn-outline btn-sm" :href="videoResource.url" target="_blank" rel="noopener noreferrer">
                      B站打开 <i class="ri-external-link-line"></i>
                    </a>
                  </div>
                </div>
                <div v-if="videoPlaying && videoEmbedUrl" class="lesson-video-player">
                  <iframe
                    :src="videoEmbedUrl"
                    :title="videoResource?.title || '课程视频讲解'"
                    allow="autoplay; fullscreen; encrypted-media; picture-in-picture"
                    scrolling="no"
                  ></iframe>
                </div>
              </div>

              <div v-if="resourceCards.length" class="resource-grid">
                <article v-for="card in resourceCards" :key="card.key"
                  class="resource-card is-clickable"
                  role="button"
                  tabindex="0"
                  @click="openResourceDetail(card)"
                  @keydown.enter="openResourceDetail(card)">
                  <span class="resource-shine" aria-hidden="true"></span>
                  <div class="resource-card-head">
                    <span class="resource-icon">
                      <i :class="card.icon"></i>
                      <span class="resource-status-dot" aria-hidden="true"></span>
                    </span>
                    <div>
                      <div class="resource-type">{{ card.type }}</div>
                      <h3>{{ card.title }}</h3>
                    </div>
                  </div>
                  <div class="rich-text resource-card-summary" v-html="renderMarkdown(card.body || '')"></div>
                  <div class="resource-card-foot">
                    <span v-if="card.key === 'video'"><i class="ri-play-circle-line"></i> 播放视频</span>
                    <span v-else-if="resourceDetailsLoading && !resourceDetails[card.key]">
                      <i class="ri-loader-4-line spin"></i> 详细内容生成中…
                    </span>
                    <span v-else><i class="ri-article-line"></i> 查看详情</span>
                    <i class="ri-arrow-right-up-line"></i>
                  </div>
                </article>
              </div>

              <div v-if="explainStreaming || explainStreamContent" class="lesson-blocks">
                <article class="lesson-block">
                  <h3>核心概念</h3>
                  <div class="rich-text" v-html="renderMarkdown(explainStreamContent || '')"></div>
                  <span v-if="explainStreaming" class="stream-cursor">▋</span>
                  <div v-if="explainStreaming && !explainStreamContent" class="tutor-waiting">
                    <i class="ri-loader-4-line spin"></i> 正在生成讲解…
                  </div>
                </article>
              </div>
              <div v-else-if="contentSections.length" class="lesson-blocks">
                <article v-for="(section, idx) in contentSections" :key="idx" class="lesson-block">
                  <h3>{{ section.title || '学习要点' }}</h3>
                  <div class="rich-text" v-html="renderMarkdown(section.body || section.content || '')"></div>
                </article>
              </div>
              <div v-else-if="explainError" class="empty-inline">
                讲解生成失败，<a href="javascript:void(0)" @click="retryExplanation">点此重新生成</a>。
              </div>
              <div v-else class="empty-inline">讲解准备中…</div>

              <div v-if="keyFormulas.length" class="formula-list">
                <div v-for="(formula, idx) in keyFormulas" :key="idx" class="formula-item">
                  <span>{{ idx + 1 }}</span>
                  <div class="formula-rendered" v-html="renderFormula(formula)"></div>
                </div>
              </div>

              <div v-if="examples.length" class="example-list">
                <article v-for="(example, idx) in examples" :key="idx" class="example-item">
                  <div class="example-title">例题 {{ idx + 1 }}</div>
                  <div class="rich-text" v-html="renderMarkdown(example.question || '')"></div>
                  <ol v-if="example.steps && example.steps.length">
                    <li v-for="(step, sIdx) in example.steps" :key="sIdx" v-html="renderMarkdown(step)"></li>
                  </ol>
                </article>
              </div>
            </section>

            <section v-show="phase === 'practice'" ref="practiceContentRef" class="task-section">
              <div class="section-head">
                <h2><i class="ri-pencil-line"></i> 针对性练习</h2>
                <span class="badge badge-yellow">{{ practiceIndex + 1 }}/{{ exercises.length || 1 }}</span>
              </div>

              <template v-if="currentPractice">
                <div class="question-box">
                  <div class="question-text" v-html="renderMarkdown(currentPractice.content || '')"></div>
                  <button v-for="option in currentPractice.options" :key="optionKey(option)"
                    type="button"
                    class="option-row"
                    :class="optionClass(option)"
                    :disabled="practiceSubmitted"
                    :aria-pressed="selectedPracticeAnswer === optionKey(option)"
                    @pointerdown.prevent="selectPracticeOption(option)"
                    @click.prevent="selectPracticeOption(option)">
                    <span class="option-key">{{ optionKey(option) }}</span>
                    <span class="option-text" v-html="renderMarkdown(optionText(option))"></span>
                  </button>
                </div>

                <div v-if="practiceResult" class="feedback-box" :class="practiceResult.correct ? 'ok' : 'bad'">
                  <strong>{{ practiceResult.correct ? '回答正确' : '回答错误' }}</strong>
                  <span v-if="practiceResult.correctAnswer">正确答案：{{ practiceResult.correctAnswer }}</span>
                  <div v-if="practiceResult.explanation" v-html="renderMarkdown(practiceResult.explanation)"></div>
                  <div v-if="practiceResult.errorAnalysis?.suggestion">{{ practiceResult.errorAnalysis.suggestion }}</div>
                </div>

                <div v-if="practiceIntervention" class="practice-intervention">
                  <div class="practice-intervention-head">
                    <i class="ri-first-aid-kit-line"></i>
                    <strong>连续错题干预</strong>
                  </div>
                  <p>{{ practiceIntervention.message }}</p>
                  <div v-if="practiceIntervention.actions?.length" class="intervention-actions">
                    <span v-for="action in practiceIntervention.actions" :key="action">{{ action }}</span>
                  </div>
                  <div v-if="practiceInterventionContent" class="rich-text" v-html="renderMarkdown(practiceInterventionContent)"></div>
                </div>

                <div class="action-row">
                  <button v-if="!practiceSubmitted" type="button" class="btn btn-dark" :disabled="!selectedPracticeAnswer" @pointerdown.prevent="handleSubmitPracticeAction" @mousedown.prevent="handleSubmitPracticeAction" @click.prevent="handleSubmitPracticeAction">
                    <i class="ri-send-plane-line"></i> 提交练习
                  </button>
                  <button v-else-if="practiceIndex < exercises.length - 1" type="button" class="btn btn-dark" @pointerdown.prevent="handleNextPracticeAction" @mousedown.prevent="handleNextPracticeAction" @click.prevent="handleNextPracticeAction">
                    下一题 <i class="ri-arrow-right-line"></i>
                  </button>
                  <button v-else type="button" class="btn btn-dark" @pointerdown.prevent="handleStartCheckpointAction" @mousedown.prevent="handleStartCheckpointAction" @click.prevent="handleStartCheckpointAction">
                    进入节点检查点 <i class="ri-shield-check-line"></i>
                  </button>
                </div>
              </template>
              <div v-else class="empty-inline">该节点暂时没有练习题，请让 AI 生成一道口头检查题。</div>
            </section>

            <section v-show="phase === 'checkpoint'" ref="checkpointContentRef" class="task-section">
              <div class="section-head">
                <h2><i class="ri-shield-check-line"></i> 节点检查点</h2>
                <span class="badge badge-red" v-if="!checkpointResults">{{ checkpointIndex + 1 }}/{{ exercises.length || 1 }}</span>
              </div>

              <template v-if="!checkpointResults && currentCheckpoint">
                <div class="question-box">
                  <div class="question-text" v-html="renderMarkdown(currentCheckpoint.content || '')"></div>
                  <button v-for="option in currentCheckpoint.options" :key="optionKey(option)"
                    type="button"
                    class="option-row"
                    :class="{ selected: selectedCheckpointAnswer === optionKey(option) }"
                    :aria-pressed="selectedCheckpointAnswer === optionKey(option)"
                    @pointerdown.prevent="selectCheckpointOption(option)"
                    @click.prevent="selectCheckpointOption(option)">
                    <span class="option-key">{{ optionKey(option) }}</span>
                    <span class="option-text" v-html="renderMarkdown(optionText(option))"></span>
                  </button>
                </div>
                <div class="action-row">
                  <button type="button" class="btn btn-dark" :disabled="!selectedCheckpointAnswer || checkpointSubmitting" @pointerdown.prevent="handleSubmitCheckpointAnswerAction" @mousedown.prevent="handleSubmitCheckpointAnswerAction" @click.prevent="handleSubmitCheckpointAnswerAction">
                    {{ checkpointIndex < exercises.length - 1 ? '下一题' : '提交检查点' }}
                    <i class="ri-arrow-right-line"></i>
                  </button>
                </div>
              </template>

              <div v-else-if="checkpointResults" class="checkpoint-result" :class="checkpointResults.passed ? 'passed' : 'failed'">
                <i :class="checkpointResults.passed ? 'ri-checkbox-circle-line' : 'ri-error-warning-line'"></i>
                <h2>{{ checkpointResults.passed ? '检查点通过' : '检查点未通过' }}</h2>
                <div class="score">{{ Math.round(checkpointResults.score || 0) }}分</div>
                <p>{{ checkpointResults.nextAction?.message || checkpointSummary }}</p>
                <div v-if="!checkpointResults.passed && checkpointResults.nextAction?.suggestedActions" class="remedial-actions">
                  <span v-for="action in checkpointResults.nextAction.suggestedActions" :key="action">{{ action }}</span>
                </div>
                <div class="action-row">
                  <router-link class="btn btn-dark" :to="checkpointActionRoute">
                    {{ checkpointActionLabel }}
                    <i class="ri-arrow-right-line"></i>
                  </router-link>
                  <router-link class="btn btn-outline" :to="'/learning/' + props.pathId">查看路径变化</router-link>
                </div>
              </div>
            </section>
          </main>

          <aside class="tutor-panel" v-show="!tutorCollapsed">
            <div class="tutor-head">
              <div>
                <strong>AI 路径伴学</strong>
                <span>已绑定当前节点上下文</span>
              </div>
              <button class="tutor-collapse-btn" @click="tutorCollapsed = true" title="收起 AI 伴学" aria-label="收起 AI 伴学">
                <i class="ri-contract-right-line"></i>
              </button>
            </div>

            <div class="prompt-chips">
              <button v-for="prompt in assistantPrompts" :key="prompt" @click="sendTutorMessage(prompt)">
                {{ prompt }}
              </button>
            </div>

            <div ref="tutorMessagesRef" class="tutor-messages">
              <div v-for="(msg, idx) in tutorMessages" :key="idx" class="tutor-msg" :class="msg.role">
                <div v-html="renderMarkdown(msg.content)"></div>
              </div>
              <div v-if="tutorStreaming && !tutorStreamingContent" class="tutor-waiting">AI 正在思考...</div>
            </div>

            <div class="tutor-input">
              <textarea v-model="tutorInput" rows="2" placeholder="问当前节点的问题..." @keydown="handleTutorKeydown"></textarea>
              <button class="btn btn-dark btn-sm" :disabled="!tutorInput.trim() || tutorStreaming" @click="sendTutorMessage(tutorInput)">
                <i class="ri-send-plane-fill"></i>
              </button>
            </div>
          </aside>

          <!-- 收起后的浮出竖条：点击重新展开 AI 伴学 -->
          <button
            v-show="tutorCollapsed"
            class="tutor-reopen"
            @click="tutorCollapsed = false"
            title="展开 AI 伴学"
            aria-label="展开 AI 伴学"
          >
            <i class="ri-robot-line"></i>
            <span class="tutor-reopen-text">AI 伴学</span>
            <i class="ri-contract-left-line"></i>
          </button>
        </div>
      </template>
    </div>
  </div>

  <!-- 资源详情弹窗 -->
  <Transition name="detail-fade">
    <div v-if="activeResourceCard" class="resource-detail-overlay" @click.self="closeResourceDetail">
      <div class="resource-detail-modal">
        <div class="resource-detail-head">
          <div class="resource-detail-title">
            <span class="resource-icon"><i :class="activeResourceCard.icon"></i></span>
            <div>
              <div class="resource-type">{{ activeResourceCard.type }}</div>
              <h3>{{ activeResourceDetail.title || activeResourceCard.title }}</h3>
            </div>
          </div>
          <button class="resource-detail-close" @click="closeResourceDetail" title="关闭">
            <i class="ri-close-line"></i>
          </button>
        </div>
        <div class="resource-detail-body">
          <div v-if="animationLoading && activeResourceCard.key === 'animationScript' && !activeAnimationScenes.length" class="resource-detail-loading">
            <i class="ri-loader-4-line spin"></i> 正在连接动画生成 AI…
          </div>
          <div v-else-if="activeResourceLoading" class="resource-detail-loading">
            <i class="ri-loader-4-line spin"></i> 正在生成「{{ activeResourceCard.title }}」的详细内容…
          </div>
          <MindMap v-else-if="activeResourceCard.key === 'mindMap'" :markdown="activeResourceContent" />
          <AnimationPlayer
            v-else-if="activeResourceCard.key === 'animationScript' && (activeAnimationScenes.length || animationAsset?.videoUrl)"
            :scenes="activeAnimationScenes"
            :asset="animationAsset" />
          <div v-else class="rich-text" v-html="renderMarkdown(activeResourceContent)"></div>
        </div>
      </div>
    </div>
  </Transition>

  <!-- 个性化演示文稿（reveal.js 在线演示） -->
  <PptViewer
    v-if="pptViewerOpen && slidesDoc"
    :slides-doc="slidesDoc"
    :downloading="pptDownloading"
    @close="closePptViewer"
    @download="downloadPpt" />
</div>
</template>

<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { marked } from 'marked'
import MindMap from '../components/MindMap.vue'
import AnimationPlayer from '../components/AnimationPlayer.vue'
import PptViewer from '../components/PptViewer.vue'
import PaperBird from '../components/PaperBird.vue'
import { api, streamSse, downloadFile } from '../api/index.js'
import {
  protectLatexForMarkdown,
  renderLatex,
  renderLatexString,
  restoreLatexPlaceholders,
  showToast
} from '../composables/state.js'

marked.setOptions({ breaks: true, gfm: true })

const props = defineProps({
  pathId: { type: String, required: true },
  nodeId: { type: String, required: true }
})

const router = useRouter()
const loading = ref(true)
const nodeData = ref(null)
const phase = ref('learn')

const practiceIndex = ref(0)
const selectedPracticeAnswer = ref('')
const practiceSubmitted = ref(false)
const practiceResult = ref(null)
const practiceStartTime = ref(Date.now())
const practiceDone = ref(false)

const checkpointIndex = ref(0)
const selectedCheckpointAnswer = ref('')
const checkpointAnswers = ref([])
const checkpointResults = ref(null)
const checkpointSubmitting = ref(false)
const checkpointStartTime = ref(Date.now())
const lastAction = ref({ key: '', at: 0 })

const tutorInput = ref('')
const tutorMessages = ref([])
const tutorStreaming = ref(false)
const tutorStreamingContent = ref('')
const tutorSessionId = ref(null)
const tutorMessagesRef = ref(null)
const tutorCollapsed = ref(localStorage.getItem('lessonTutorCollapsed') === '1')

watch(tutorCollapsed, (val) => {
  localStorage.setItem('lessonTutorCollapsed', val ? '1' : '0')
})

const explainStreaming = ref(false)
const explainStreamContent = ref('')
const explainError = ref(false)
let explainLatexTimer = null

const lessonContentRef = ref(null)
const practiceContentRef = ref(null)
const checkpointContentRef = ref(null)
const videoPlaying = ref(false)
const videoLoading = ref(false)
const videoLoadAttempted = ref(false)
const loadedVideoResource = ref(null)
const resourceDetails = ref({})
const resourceDetailsLoading = ref(false)
const activeResourceCard = ref(null)
const animationAsset = ref(null)
const animationLoading = ref(false)

// 个性化演示文稿（PPT）
const pptViewerOpen = ref(false)
const pptLoading = ref(false)
const pptDownloading = ref(false)
const slidesDoc = ref(null)

const steps = [
  { key: 'learn', label: '学习', icon: 'ri-book-open-line' },
  { key: 'practice', label: '练习', icon: 'ri-pencil-line' },
  { key: 'checkpoint', label: '检查点', icon: 'ri-shield-check-line' }
]

const lesson = computed(() => nodeData.value?.lesson || {})
const content = computed(() => nodeData.value?.content || {})
const contentSections = computed(() => (content.value.sections || []).map(section => ({
  ...section,
  body: formatLessonValue(section.body ?? section.content)
})))
const keyFormulas = computed(() => (content.value.keyFormulas || []).map(formatFormulaValue).filter(Boolean))
const examples = computed(() => (content.value.examples || []).map(example => ({
  ...example,
  question: formatLessonValue(example.question || example.problem || example.title),
  steps: normalizeList(example.steps || example.solution).map(formatLessonValue).filter(Boolean)
})))
const exercises = computed(() => (nodeData.value?.exercises || lesson.value.exercises || []).map(normalizeExercise))
const lessonResources = computed(() => nodeData.value?.resources || lesson.value.resources || content.value.resources || {})
const resourceCards = computed(() => buildResourceCards(lessonResources.value))
const videoResource = computed(() => loadedVideoResource.value || nodeData.value?.videoResource || null)
const videoEmbedUrl = computed(() => {
  const url = videoResource.value?.embedUrl || ''
  if (typeof url !== 'string' || !url.startsWith('https://player.bilibili.com/')) return ''
  return withBilibiliQuality(url)
})
const videoTitle = computed(() => {
  if (videoResource.value?.title) return videoResource.value.title
  if (videoLoading.value) return `AI 正在匹配「${knowledgePointName.value}」的课程视频`
  return `暂未匹配到「${knowledgePointName.value}」的课程视频`
})
const videoHint = computed(() => {
  if (videoResource.value?.reason) return videoResource.value.reason
  if (videoLoading.value) return '学习内容已可先看，视频匹配完成后会自动更新。'
  if (videoLoadAttempted.value) return '没有拿到可播放视频，不影响继续学习和练习。'
  return '视频入口会固定显示；匹配成功后可直接在这里播放。'
})
const videoButtonLabel = computed(() => {
  if (videoLoading.value) return '匹配中'
  if (!videoResource.value) return '暂无视频'
  return videoPlaying.value ? '收起视频' : '播放视频'
})

function buildResourceCards(resources) {
  const cards = []
  // 讲解文档卡已移除：完整讲解由页面下方「核心概念」区流式渲染，资源卡只放讲解之外的补充资源，避免重复
  addResourceCard(cards, 'mindMap', '思维导图', 'ri-mind-map',
    resources.mindMap,
    { title: `${knowledgePointName.value}知识结构`, content: `围绕「${knowledgePointName.value}」梳理核心概念、前置基础、应用方法和常见误区。` })
  addResourceCard(cards, 'exercises', '分层练习', 'ri-list-check-3',
    resources.exercises,
    exercises.value.length ? {
      title: `${exercises.value.length}道自适应练习`,
      content: '系统已根据当前节点准备练习题，可在下方练习区完成并获得即时反馈。'
    } : null)
  addResourceCard(cards, 'extendedReading', '拓展阅读', 'ri-book-read-line',
    resources.extendedReading,
    { title: '拓展阅读建议', content: `从课程背景、工程应用和前沿进展三个角度继续拓展「${knowledgePointName.value}」。` })
  addResourceCard(cards, 'practiceCase', '实操案例', 'ri-terminal-box-line',
    resources.practiceCase,
    examples.value[0] ? {
      title: examples.value[0].question || '课程实践案例',
      content: normalizeList(examples.value[0].steps).join('\n')
    } : { title: '课程实践任务', content: `围绕「${knowledgePointName.value}」设计一个小型实践任务，先复现基础流程，再尝试解释结果。` })
  addResourceCard(cards, 'video', '视频推荐', 'ri-video-line',
    videoResource.value ? {
      title: videoTitle.value,
      content: videoHint.value
    } : resources.video,
    null)
  addResourceCard(cards, 'animationScript', '动画讲解', 'ri-movie-2-line',
    resources.animationScript,
    { title: '动画讲解', content: `用“问题场景 -> 核心机制 -> 操作步骤 -> 结果反馈”的分镜动态演示「${knowledgePointName.value}」。` })
  return cards.filter(card => card.body).slice(0, 7)
}

function addResourceCard(cards, key, type, icon, primary, fallback) {
  const normalized = normalizeResource(primary) || normalizeResource(fallback)
  if (!normalized) return
  cards.push({
    key,
    type,
    icon,
    title: normalized.title || type,
    body: normalized.content || normalized.body || normalized.description || ''
  })
}

function normalizeResource(resource) {
  if (!resource) return null
  if (typeof resource === 'string') return { content: resource }
  if (Array.isArray(resource)) {
    const content = resource.map(formatLessonValue).filter(Boolean).join('\n')
    return content ? { content } : null
  }
  if (typeof resource === 'object') {
    const content = resource.content || resource.body || resource.description || resource.summary || resource.text
      || (resource.items ? normalizeList(resource.items).join('\n') : '')
      || (resource.nodes ? normalizeList(resource.nodes).join('\n') : '')
      || (resource.steps ? normalizeList(resource.steps).join('\n') : '')
    return {
      title: resource.title || resource.name,
      content: formatLessonValue(content)
    }
  }
  return { content: String(resource) }
}

const activeResourceDetail = computed(() => {
  if (!activeResourceCard.value) return {}
  return resourceDetails.value[activeResourceCard.value.key] || {}
})
const activeResourceLoading = computed(() => {
  if (!activeResourceCard.value) return false
  if (activeResourceCard.value.key === 'video') return false
  return resourceDetailsLoading.value && !activeResourceDetail.value.content
})
const activeResourceContent = computed(() => {
  if (!activeResourceCard.value) return ''
  return activeResourceDetail.value.content || activeResourceCard.value.body || ''
})
// 动画讲解的结构化分镜：优先用后端结构化 scenes，缺失时从 Markdown content 解析兜底
const activeAnimationScenes = computed(() => {
  if (!activeResourceCard.value || activeResourceCard.value.key !== 'animationScript') return []
  const assetScenes = animationAsset.value?.scenes
  if (Array.isArray(assetScenes) && assetScenes.length) return assetScenes
  const scenes = activeResourceDetail.value.scenes
  if (Array.isArray(scenes) && scenes.length) return scenes
  return parseAnimationScenes(activeResourceContent.value)
})

// 解析后端 renderAnimationMarkdown 生成的文本：## 场景N：标题 / **旁白**：… / **画面**：…
function parseAnimationScenes(markdown) {
  if (!markdown) return []
  const blocks = markdown.split(/##\s*场景\s*\d*[：:]?/).map(b => b.trim()).filter(Boolean)
  const scenes = []
  for (const block of blocks) {
    const lines = block.split('\n').map(l => l.trim()).filter(Boolean)
    if (!lines.length) continue
    const sceneName = lines[0].replace(/^[：:]\s*/, '')
    const narrationMatch = block.match(/\*\*旁白\*\*[：:]\s*([\s\S]*?)(?=\*\*画面\*\*|$)/)
    const visualMatch = block.match(/\*\*画面\*\*[：:]\s*([\s\S]*?)$/)
    scenes.push({
      scene: sceneName,
      narration: narrationMatch ? narrationMatch[1].trim() : '',
      visual: visualMatch ? visualMatch[1].trim() : ''
    })
  }
  return scenes
}

function openResourceDetail(card) {
  if (!card) return
  if (card.key === 'video') {
    toggleVideo()
    return
  }
  activeResourceCard.value = card
  if (card.key === 'animationScript') {
    loadAnimationAsset(props.pathId, props.nodeId)
  }
}

function closeResourceDetail() {
  activeResourceCard.value = null
}

async function openPptViewer() {
  if (pptLoading.value) return
  pptLoading.value = true
  try {
    if (!slidesDoc.value) {
      slidesDoc.value = await api.learning.lessonSlides(props.pathId, props.nodeId)
    }
    pptViewerOpen.value = true
  } catch (e) {
    showToast(e.message || '生成演示文稿失败', 'error')
  } finally {
    pptLoading.value = false
  }
}

function closePptViewer() {
  pptViewerOpen.value = false
}

async function downloadPpt() {
  if (pptDownloading.value) return
  pptDownloading.value = true
  try {
    await downloadFile(
      '/learning/lesson/' + props.pathId + '/' + props.nodeId + '/slides.pptx',
      `${knowledgePointName.value}.pptx`)
  } catch (e) {
    showToast(e.message || '下载 .pptx 失败', 'error')
  } finally {
    pptDownloading.value = false
  }
}

async function loadResourceDetails(pathId, nodeId) {
  resourceDetailsLoading.value = true
  try {
    const details = await api.learning.lessonResourceDetails(pathId, nodeId)
    if (props.pathId !== pathId || props.nodeId !== nodeId) return
    resourceDetails.value = details && typeof details === 'object' ? details : {}
  } catch {
    // 详情懒加载失败不阻断学习，卡片回退展示摘要
  } finally {
    if (props.pathId === pathId && props.nodeId === nodeId) {
      resourceDetailsLoading.value = false
    }
  }
}

async function loadAnimationAsset(pathId, nodeId) {
  if (animationLoading.value || animationAsset.value) return
  animationLoading.value = true
  try {
    const asset = await api.learning.lessonAnimation(pathId, nodeId)
    if (props.pathId !== pathId || props.nodeId !== nodeId) return
    animationAsset.value = asset && typeof asset === 'object' ? asset : null
  } catch (e) {
    showToast(e.message || '动画生成接口暂不可用，已使用本地分镜动画', 'warning')
  } finally {
    if (props.pathId === pathId && props.nodeId === nodeId) {
      animationLoading.value = false
    }
  }
}
const currentPractice = computed(() => exercises.value[practiceIndex.value] || null)
const currentCheckpoint = computed(() => exercises.value[checkpointIndex.value] || null)
const practiceIntervention = computed(() => practiceResult.value?.interventionTriggered
  ? practiceResult.value?.intervention
  : null)
const practiceInterventionContent = computed(() => formatInterventionContent(practiceIntervention.value?.remedialContent))
const knowledgePointName = computed(() => {
  const kp = nodeData.value?.knowledgePoint
  if (typeof kp === 'string') return kp
  return kp?.name || nodeData.value?.knowledgePointName || nodeData.value?.title || '当前知识点'
})
const assistantPrompts = computed(() => nodeData.value?.assistantPrompts || [
  `请带我学习${knowledgePointName.value}`,
  `用一道小题检查${knowledgePointName.value}`,
  '我做错了，请给我提示'
])
const defaultPrompt = computed(() => assistantPrompts.value[0] || `请带我学习${knowledgePointName.value}`)
const checkpointSummary = computed(() => checkpointResults.value?.passed
  ? '下一个知识节点已经解锁。'
  : '系统已根据检查点结果生成补救节点。')
const checkpointActionLabel = computed(() => {
  const type = checkpointResults.value?.nextAction?.type
  if (type === 'path_complete') return '再次诊断，生成新路径'
  if (checkpointResults.value?.passed) return '继续下一节点'
  return '进入补救节点'
})
const checkpointActionRoute = computed(() => {
  if (checkpointResults.value?.nextAction?.type === 'path_complete') return '/diagnostic'
  const nextNodeId = resolveCheckpointNextNodeId(checkpointResults.value)
  return nextNodeId ? `/learning/${props.pathId}/${nextNodeId}` : `/learning/${props.pathId}`
})

function renderMarkdown(content) {
  if (!content) return ''
  const { safe, placeholders } = protectLatexForMarkdown(formatLessonValue(content))
  return restoreLatexPlaceholders(marked.parse(safe), placeholders)
}

function renderFormula(formula) {
  const raw = formatFormulaValue(formula).trim()
  if (!raw) return ''
  const normalized = normalizeLatexFormula(raw)
  return renderLatexString(normalized)
}

function normalizeLatexFormula(raw) {
  if (raw.includes('$')) return raw
  const looksLikeLatex = /\\[a-zA-Z]+|[_^{}]/.test(raw)
  if (!looksLikeLatex) return raw
  return `$$${raw}$$`
}

function formatFormulaValue(value) {
  if (value == null) return ''
  if (typeof value === 'string' || typeof value === 'number') return String(value)
  if (Array.isArray(value)) return value.map(formatFormulaValue).filter(Boolean).join('\n')
  if (typeof value === 'object') {
    return value.formula || value.content || value.text || value.name || Object.values(value).map(formatFormulaValue).filter(Boolean).join('：')
  }
  return String(value)
}

function formatLessonValue(value) {
  if (value == null) return ''
  if (typeof value === 'string' || typeof value === 'number') return String(value)
  if (Array.isArray(value)) {
    return value.map((item, index) => {
      const text = formatLessonValue(item)
      return text ? `${index + 1}. ${text}` : ''
    }).filter(Boolean).join('\n')
  }
  if (typeof value === 'object') {
    if (value.mistake || value.correction) {
      return [`误区：${formatLessonValue(value.mistake)}`, `纠正：${formatLessonValue(value.correction)}`].filter(line => !line.endsWith('：')).join('\n')
    }
    if (value.title || value.content || value.text) {
      return [value.title, value.content || value.text].map(formatLessonValue).filter(Boolean).join('\n')
    }
    return Object.entries(value).map(([key, val]) => `${key}：${formatLessonValue(val)}`).join('\n')
  }
  return String(value)
}

function formatInterventionContent(content) {
  if (!content) return ''
  if (typeof content === 'string') return content
  if (Array.isArray(content)) return normalizeList(content).map(formatLessonValue).filter(Boolean).join('\n')
  if (typeof content !== 'object') return String(content)

  const lines = []
  const concept = content.conceptExplanation
  if (concept && typeof concept === 'object') {
    const summary = formatLessonValue(concept.summary)
    const body = formatLessonValue(concept.content)
    if (summary) lines.push(summary)
    if (body) lines.push(body)
  }

  const examples = Array.isArray(content.examples) ? content.examples.slice(0, 1) : []
  examples.forEach((example, index) => {
    const title = formatLessonValue(example.title || `例题 ${index + 1}`)
    const problem = formatLessonValue(example.problem || example.question)
    const solution = formatLessonValue(example.solution || example.answer)
    lines.push([title, problem, solution].filter(Boolean).join('\n'))
  })

  const summary = formatLessonValue(content.summary)
  if (summary) lines.push(summary)
  return lines.filter(Boolean).join('\n\n')
}

function normalizeList(value) {
  if (value == null) return []
  if (Array.isArray(value)) return value
  return String(value).split(/\n+|(?=步骤\s*\d+[:：])|(?=\d+[.、]\s*)/).map(s => s.trim()).filter(Boolean)
}

function normalizeExercise(exercise, index) {
  const raw = exercise && typeof exercise === 'object' ? exercise : {}
  return {
    ...raw,
    exerciseId: raw.exerciseId || raw.id || `exercise_${index + 1}`,
    content: formatLessonValue(raw.content || raw.problem || raw.question || raw.title),
    options: normalizeOptions(raw.options || raw.choices || raw.optionList)
  }
}

function normalizeOptions(options) {
  if (!options) return []
  if (Array.isArray(options)) {
    return options.map((option, index) => normalizeOption(option, index)).filter(option => option.key)
  }
  if (typeof options === 'object') {
    return Object.entries(options).map(([key, value], index) => {
      const text = formatLessonValue(value)
      return {
        key: normalizeOptionKey(key, index),
        label: normalizeOptionKey(key, index),
        text: stripOptionPrefix(text) || text
      }
    }).filter(option => option.key)
  }
  return normalizeList(options).map((option, index) => normalizeOption(option, index)).filter(option => option.key)
}

function normalizeOption(option, index) {
  if (option && typeof option === 'object' && !Array.isArray(option)) {
    const rawKey = option.key || option.label || option.optionKey || option.option || option.id
    const rawText = option.text ?? option.content ?? option.description ?? option.value ?? option.name ?? rawKey
    const text = formatLessonValue(rawText)
    const key = normalizeOptionKey(rawKey || text, index)
    return {
      ...option,
      key,
      label: option.label || key,
      text: stripOptionPrefix(text) || text
    }
  }

  const text = formatLessonValue(option)
  const key = normalizeOptionKey(text, index)
  return {
    key,
    label: key,
    text: stripOptionPrefix(text) || text
  }
}

function normalizeOptionKey(value, index) {
  const text = String(value ?? '').trim()
  const match = text.match(/^([A-Za-z])(?:[.、):：\s]|$)/)
  if (match) return match[1].toUpperCase()
  return String.fromCharCode(65 + index)
}

function stripOptionPrefix(value) {
  return String(value ?? '').trim().replace(/^[A-Za-z][.、):：\s]+/, '').trim()
}

function renderCurrentLatex() {
  nextTick(() => {
    renderLatexTextTargets(lessonContentRef.value)
    renderLatexTextTargets(practiceContentRef.value)
    renderLatexTextTargets(checkpointContentRef.value)
  })
}

function renderLatexTextTargets(root) {
  if (!root) return
  const targets = root.querySelectorAll([
    '.rich-text',
    '.question-text',
    '.option-text',
    '.feedback-box > div',
    '.formula-rendered',
    '.example-item li'
  ].join(','))
  targets.forEach(target => renderLatex(target))
}

function optionKey(option) {
  return String(option?.key || option?.label || '').trim()
}

function optionText(option) {
  return option?.text || option?.content || option?.value || ''
}

function selectPracticeOption(option) {
  const key = optionKey(option)
  if (key) selectedPracticeAnswer.value = key
}

function selectCheckpointOption(option) {
  const key = optionKey(option)
  if (key) selectedCheckpointAnswer.value = key
}

function runActionOnce(key, action) {
  const now = Date.now()
  if (lastAction.value.key === key && now - lastAction.value.at < 350) return
  lastAction.value = { key, at: now }
  action()
}

function handleSubmitPracticeAction() {
  runActionOnce('submitPractice', submitPractice)
}

function handleNextPracticeAction() {
  runActionOnce('nextPractice', nextPractice)
}

function handleStartCheckpointAction() {
  runActionOnce('startCheckpoint', startCheckpoint)
}

function handleSubmitCheckpointAnswerAction() {
  runActionOnce('submitCheckpointAnswer', submitCheckpointAnswer)
}

function handleCheckpointNextAction() {
  runActionOnce('handleCheckpointNext', handleCheckpointNext)
}

function optionClass(option) {
  const key = optionKey(option)
  return {
    selected: selectedPracticeAnswer.value === key,
    correct: practiceSubmitted.value && practiceResult.value?.correctAnswer === key,
    wrong: practiceSubmitted.value && selectedPracticeAnswer.value === key && !practiceResult.value?.correct
  }
}

function isStepDone(key) {
  if (key === 'learn') return phase.value !== 'learn'
  if (key === 'practice') return practiceDone.value
  if (key === 'checkpoint') return Boolean(checkpointResults.value)
  return false
}

function jumpToStep(key) {
  if (key === 'checkpoint' && !practiceDone.value) {
    showToast('请先完成节点练习', 'warning')
    return
  }
  phase.value = key
  renderCurrentLatex()
}

function toggleVideo() {
  videoPlaying.value = !videoPlaying.value
}

function withBilibiliQuality(rawUrl) {
  try {
    const url = new URL(rawUrl)
    url.searchParams.set('high_quality', '1')
    url.searchParams.set('quality', '80')
    url.searchParams.set('qn', '80')
    url.searchParams.set('autoplay', url.searchParams.get('autoplay') || '0')
    url.searchParams.set('danmaku', url.searchParams.get('danmaku') || '0')
    return url.toString()
  } catch {
    return rawUrl
  }
}

async function loadLesson() {
  loading.value = true
  try {
    nodeData.value = await api.learning.lesson(props.pathId, props.nodeId)
    loadedVideoResource.value = nodeData.value?.videoResource || null
    videoLoadAttempted.value = Boolean(loadedVideoResource.value)
    if (!loadedVideoResource.value) {
      loadVideoResource(props.pathId, props.nodeId)
    }
    // 讲解走流式：边生成边显示，避免长内容整体超时
    streamExplanation(props.pathId, props.nodeId)
    loadResourceDetails(props.pathId, props.nodeId)
    practiceStartTime.value = Date.now()
    checkpointStartTime.value = Date.now()
    await nextTick()
    renderCurrentLatex()
  } catch (e) {
    showToast(e.message || '加载学习节点失败', 'error')
  } finally {
    loading.value = false
  }
}

async function streamExplanation(pathId, nodeId) {
  explainStreaming.value = true
  explainStreamContent.value = ''
  explainError.value = false
  try {
    for await (const evt of streamSse('/learning/lesson/' + pathId + '/' + nodeId + '/explain-stream')) {
      if (props.pathId !== pathId || props.nodeId !== nodeId) {
        return
      }
      if (evt.type === 'message') {
        explainStreamContent.value += evt.data.content || ''
        scheduleExplainLatex()
      } else if (evt.type === 'error') {
        explainError.value = true
        break
      } else if (evt.type === 'done') {
        break
      }
    }
  } catch (e) {
    if (!explainStreamContent.value) explainError.value = true
  } finally {
    if (props.pathId === pathId && props.nodeId === nodeId) {
      explainStreaming.value = false
      await nextTick()
      renderCurrentLatex()
    }
  }
}

// 流式过程中防抖渲染 LaTeX，避免每个 token 都重渲染
function scheduleExplainLatex() {
  if (explainLatexTimer) clearTimeout(explainLatexTimer)
  explainLatexTimer = setTimeout(() => {
    nextTick(() => renderCurrentLatex())
  }, 300)
}

function retryExplanation() {
  streamExplanation(props.pathId, props.nodeId)
}

async function loadVideoResource(pathId, nodeId) {
  videoLoading.value = true
  videoLoadAttempted.value = false
  try {
    const video = await api.learning.lessonVideo(pathId, nodeId)
    if (props.pathId !== pathId || props.nodeId !== nodeId) return
    if (video && video.bvid) {
      loadedVideoResource.value = video
    }
  } catch {
    // 视频是补充资源，失败不打断学习主流程。
  } finally {
    if (props.pathId === pathId && props.nodeId === nodeId) {
      videoLoading.value = false
      videoLoadAttempted.value = true
    }
  }
}

function resetLessonState() {
  nodeData.value = null
  phase.value = 'learn'
  practiceIndex.value = 0
  selectedPracticeAnswer.value = ''
  practiceSubmitted.value = false
  practiceResult.value = null
  practiceStartTime.value = Date.now()
  practiceDone.value = false
  checkpointIndex.value = 0
  selectedCheckpointAnswer.value = ''
  checkpointAnswers.value = []
  checkpointResults.value = null
  checkpointSubmitting.value = false
  checkpointStartTime.value = Date.now()
  tutorInput.value = ''
  tutorMessages.value = []
  tutorStreaming.value = false
  tutorStreamingContent.value = ''
  tutorSessionId.value = null
  explainStreaming.value = false
  explainStreamContent.value = ''
  explainError.value = false
  videoPlaying.value = false
  videoLoading.value = false
  videoLoadAttempted.value = false
  loadedVideoResource.value = null
  resourceDetails.value = {}
  resourceDetailsLoading.value = false
  activeResourceCard.value = null
  animationAsset.value = null
  animationLoading.value = false
  pptViewerOpen.value = false
  pptLoading.value = false
  pptDownloading.value = false
  slidesDoc.value = null
  lastAction.value = { key: '', at: 0 }
}

async function submitPractice() {
  if (!selectedPracticeAnswer.value || !currentPractice.value) return
  try {
    const timeSpentSeconds = Math.round((Date.now() - practiceStartTime.value) / 1000)
    practiceResult.value = await api.learning.submitExercise({
      pathId: props.pathId,
      nodeId: props.nodeId,
      exerciseId: currentPractice.value.exerciseId,
      answer: selectedPracticeAnswer.value,
      solvingSteps: null,
      timeSpentSeconds
    })
    practiceSubmitted.value = true
    renderCurrentLatex()
  } catch (e) {
    showToast(e.message || '提交练习失败', 'error')
  }
}

function nextPractice() {
  practiceIndex.value += 1
  selectedPracticeAnswer.value = ''
  practiceSubmitted.value = false
  practiceResult.value = null
  practiceStartTime.value = Date.now()
  renderCurrentLatex()
}

function startCheckpoint() {
  practiceDone.value = true
  phase.value = 'checkpoint'
  checkpointIndex.value = 0
  selectedCheckpointAnswer.value = ''
  checkpointAnswers.value = []
  checkpointResults.value = null
  checkpointStartTime.value = Date.now()
  renderCurrentLatex()
}

async function submitCheckpointAnswer() {
  if (!selectedCheckpointAnswer.value || !currentCheckpoint.value) return
  checkpointAnswers.value.push({
    exerciseId: currentCheckpoint.value.exerciseId,
    answer: selectedCheckpointAnswer.value
  })

  if (checkpointIndex.value < exercises.value.length - 1) {
    checkpointIndex.value += 1
    selectedCheckpointAnswer.value = ''
    renderCurrentLatex()
    return
  }

  checkpointSubmitting.value = true
  try {
    const totalTimeSeconds = Math.round((Date.now() - checkpointStartTime.value) / 1000)
    checkpointResults.value = await api.learning.submitCheckpoint({
      pathId: props.pathId,
      nodeId: props.nodeId,
      answers: checkpointAnswers.value,
      totalTimeSeconds
    })
    renderCurrentLatex()
  } catch (e) {
    showToast(e.message || '提交检查点失败', 'error')
  } finally {
    checkpointSubmitting.value = false
  }
}

async function sendTutorMessage(rawMessage) {
  const message = String(rawMessage || '').trim()
  if (!message || tutorStreaming.value) return
  tutorInput.value = ''
  tutorMessages.value.push({ role: 'user', content: message })
  const aiMessage = { role: 'ai', content: '' }
  tutorMessages.value.push(aiMessage)
  tutorStreaming.value = true
  tutorStreamingContent.value = ''
  await nextTick()
  scrollTutor()

  const params = new URLSearchParams({
    message,
    sessionId: tutorSessionId.value || '',
    pathId: props.pathId,
    nodeId: props.nodeId
  })

  try {
    const msgIdx = tutorMessages.value.length - 1

    for await (const evt of streamSse('/chat/stream', { params })) {
      if (evt.type === 'metadata' && evt.data.sessionId) tutorSessionId.value = evt.data.sessionId
      if (evt.type === 'message') {
        aiMessage.content += evt.data.content || ''
        tutorStreamingContent.value = aiMessage.content
        tutorMessages.value.splice(msgIdx, 1, { ...aiMessage })
        scrollTutor()
      }
      if (evt.type === 'error') {
        showToast(evt.data.error || evt.data.message || 'AI 响应出错', 'error')
        break
      }
      if (evt.type === 'done') {
        break
      }
    }
  } catch (e) {
    showToast(e.message || 'AI 辅导失败', 'error')
  } finally {
    tutorStreaming.value = false
    tutorStreamingContent.value = ''
    scrollTutor()
  }
}

function scrollTutor() {
  nextTick(() => {
    if (tutorMessagesRef.value) {
      tutorMessagesRef.value.scrollTop = tutorMessagesRef.value.scrollHeight
    }
  })
}

function handleTutorKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendTutorMessage(tutorInput.value)
  }
}

function handleCheckpointNext() {
  if (checkpointResults.value?.nextAction?.type === 'path_complete') {
    router.push('/diagnostic')
    return
  }
  const nextNodeId = resolveCheckpointNextNodeId(checkpointResults.value)
  if (nextNodeId) {
    router.push('/learning/' + props.pathId + '/' + nextNodeId)
  } else {
    showToast('未找到可进入的节点，已打开路径变化', 'warning')
    router.push('/learning/' + props.pathId)
  }
}

function resolveCheckpointNextNodeId(result) {
  const action = result?.nextAction || {}
  return firstText(
    action.nextNodeId,
    action.nodeId,
    action.targetNodeId,
    action.pathNodeId,
    action.nextNode?.nodeId,
    action.nextNode?.id,
    result?.nextNodeId,
    result?.nextNode?.nodeId,
    result?.nextNode?.id,
    result?.currentNodeId,
    result?.remediationNodeId,
    result?.remedialNodeId,
    result?.remediationPlan?.nodeId,
    result?.remediationPlan?.nextNodeId
  )
}

function firstText(...values) {
  for (const value of values) {
    if (value == null) continue
    const text = String(value).trim()
    if (text && text !== 'null' && text !== 'undefined') return text
  }
  return ''
}

function goBackToPath() {
  router.push('/learning/' + props.pathId)
}

onMounted(() => { loadLesson() })

watch(() => [props.pathId, props.nodeId], () => {
  resetLessonState()
  loadLesson()
})
</script>

<style scoped>
/* ===== Loading Skeleton ===== */
.lesson-loading {
  display: flex;
  flex-direction: column;
  gap: 20px;
  animation: lessonLoadingIn 0.3s ease;
}
@keyframes lessonLoadingIn {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}
.lesson-loading-banner {
  display: flex;
  align-items: center;
  gap: 14px;
}
.lesson-loading-title {
  font-size: 1.05rem;
  font-weight: 700;
  color: var(--text);
}
.lesson-loading-sub {
  font-size: 0.82rem;
  color: var(--text-muted);
  margin-top: 2px;
}

/* 骨架块基样式 + shimmer 微光扫过 */
.sk {
  display: block;
  border-radius: 8px;
  background: var(--bg-alt, #f1efe8);
  position: relative;
  overflow: hidden;
}
.sk::after {
  content: '';
  position: absolute;
  inset: 0;
  transform: translateX(-100%);
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 100%
  );
  animation: skShimmer 1.4s ease-in-out infinite;
}
@keyframes skShimmer {
  100% { transform: translateX(100%); }
}

.skeleton-topbar {
  display: flex;
  align-items: center;
  gap: 16px;
}
.sk-back { width: 96px; height: 34px; border-radius: var(--radius-md); }
.skeleton-tabs { display: flex; gap: 8px; flex-wrap: wrap; }
.sk-tab { width: 84px; height: 34px; border-radius: var(--radius-full, 999px); }

.skeleton-hero {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 4px 0;
}
.sk-label { width: 120px; height: 16px; }
.sk-title { width: 60%; height: 30px; }
.skeleton-badges { display: flex; gap: 8px; }
.sk-badge { width: 88px; height: 22px; border-radius: var(--radius-full, 999px); }

.skeleton-grid {
  display: grid;
  grid-template-columns: 1fr 320px;
  gap: 20px;
}
.skeleton-panel,
.skeleton-aside {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px;
  border: 1px solid var(--border);
  border-radius: var(--radius-lg, 16px);
  background: var(--card-bg-solid);
}
.sk-h2 { width: 45%; height: 22px; margin-bottom: 6px; }
.sk-line { width: 100%; height: 14px; }
.sk-line.short { width: 65%; }
.sk-block { width: 100%; height: 120px; border-radius: var(--radius-md); margin: 6px 0; }
.sk-chat { width: 100%; height: 56px; border-radius: var(--radius-md); }
.sk-chat.short { width: 70%; }

@media (max-width: 900px) {
  .skeleton-grid { grid-template-columns: 1fr; }
  .skeleton-aside { display: none; }
}
@media (prefers-reduced-motion: reduce) {
  .sk::after { animation: none; }
  .lesson-loading { animation: none; }
}

.resource-card.is-clickable {
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transform-style: preserve-3d;
  transition: border-color 0.3s, box-shadow 0.4s cubic-bezier(0.23, 1, 0.32, 1),
              transform 0.4s cubic-bezier(0.23, 1, 0.32, 1);
  display: flex;
  flex-direction: column;
}
.resource-card.is-clickable:hover {
  border-color: var(--accent);
  transform: translateY(-4px) rotateX(6deg) rotateY(-4deg);
  box-shadow:
    0 18px 36px -16px rgba(17, 34, 64, 0.45),
    0 4px 10px -6px rgba(17, 34, 64, 0.25);
}
.resource-card.is-clickable:hover .resource-icon {
  background: var(--accent);
  color: #fff;
}
/* 流光扫过 */
.resource-shine {
  position: absolute;
  top: 0;
  left: -120%;
  width: 55%;
  height: 100%;
  background: linear-gradient(
    90deg,
    transparent,
    rgba(197, 160, 89, 0.35),
    transparent
  );
  transform: skewX(-20deg);
  pointer-events: none;
  z-index: 2;
}
.resource-card.is-clickable:hover .resource-shine {
  left: 150%;
  transition: left 0.8s ease-in-out;
}
/* 图标上的脉冲状态点 */
.resource-icon { position: relative; transition: background 0.3s, color 0.3s; }
.resource-status-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 7px;
  height: 7px;
  background: var(--success);
  border-radius: 50%;
  box-shadow: 0 0 0 2px var(--card-bg-solid);
}
.resource-status-dot::after {
  content: "";
  position: absolute;
  inset: 0;
  background: var(--success);
  border-radius: 50%;
  animation: resourcePulse 2s infinite;
}
@keyframes resourcePulse {
  0% { transform: scale(1); opacity: 0.9; }
  100% { transform: scale(2.8); opacity: 0; }
}
.resource-card-summary {
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}
.resource-card-foot {
  margin-top: 10px;
  padding-top: 8px;
  border-top: 1px dashed var(--border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 0.78rem;
  color: var(--accent);
  font-weight: 500;
}
.resource-card-foot span {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.spin {
  display: inline-block;
  animation: spin 1s linear infinite;
}
@keyframes spin { to { transform: rotate(360deg); } }

.stream-cursor {
  display: inline-block;
  margin-left: 2px;
  color: #5b8def;
  animation: blink 1s step-start infinite;
}
@keyframes blink { 50% { opacity: 0; } }

.resource-detail-overlay {
  position: fixed;
  inset: 0;
  background: rgba(17, 34, 64, 0.42);
  -webkit-backdrop-filter: blur(2px);
  backdrop-filter: blur(2px);
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}
.resource-detail-modal {
  background: var(--card-bg-solid);
  border: 1px solid var(--border);
  border-radius: 14px;
  width: min(760px, 100%);
  max-height: 86vh;
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-xl);
  overflow: hidden;
}
.resource-detail-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--border);
}
.resource-detail-title {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}
.resource-detail-title h3 {
  margin: 2px 0 0;
  font-size: 1.05rem;
}
.resource-detail-close {
  border: none;
  background: var(--bg-alt);
  color: var(--text-secondary);
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  font-size: 1.1rem;
  flex-shrink: 0;
  transition: all 0.2s;
}
.resource-detail-close:hover { background: var(--border); color: var(--text); }
.resource-detail-body {
  padding: 20px;
  overflow-y: auto;
}
.resource-detail-loading {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--text-secondary);
  padding: 28px 8px;
  font-size: 0.9rem;
}
.detail-fade-enter-active, .detail-fade-leave-active { transition: opacity 0.2s ease; }
.detail-fade-enter-from, .detail-fade-leave-to { opacity: 0; }

.lesson-topbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  margin-bottom: 18px;
  flex-wrap: wrap;
}
.btn-back {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.step-tabs {
  display: flex;
  gap: 6px;
  background: var(--bg-alt);
  padding: 4px;
  border-radius: 8px;
  border: 1px solid var(--border);
}
.step-tab {
  border: none;
  background: transparent;
  color: var(--text-secondary);
  padding: 7px 10px;
  border-radius: 6px;
  cursor: pointer;
  display: inline-flex;
  gap: 5px;
  align-items: center;
  font-size: 0.82rem;
}
.step-tab.active { background: var(--card-bg-solid); color: var(--text); box-shadow: var(--shadow-sm); }
.step-tab.done { color: var(--success); }
.task-hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  align-items: center;
  padding: 22px 0;
  border-bottom: 1px solid var(--border);
  margin-bottom: 20px;
}
.task-title { margin: 6px 0 10px; font-size: 1.6rem; line-height: 1.2; }
.task-meta { display: flex; gap: 8px; flex-wrap: wrap; }
.learning-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 22px;
  align-items: start;
  transition: grid-template-columns 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
/* AI 伴学收起：右列收成细窄轨，左侧内容自适应铺满变大 */
.learning-grid.tutor-hidden {
  grid-template-columns: minmax(0, 1fr) 46px;
  gap: 12px;
}
.task-panel, .tutor-panel {
  border: 1px solid var(--border);
  background: var(--card-bg-solid);
  border-radius: 8px;
  min-width: 0;
}
.task-section { padding: 22px; }
.section-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
  margin-bottom: 18px;
}
.section-head h2 { font-size: 1.05rem; margin: 0; display: flex; align-items: center; gap: 8px; }
.section-head-actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.lesson-block, .example-item, .question-box {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 14px;
}
.lesson-block h3 { margin: 0 0 10px; font-size: 0.95rem; }
.resource-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-bottom: 14px;
  perspective: 1200px;
}
.resource-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 14px;
  min-width: 0;
  background: var(--card-bg-solid);
}
.resource-card-head {
  display: flex;
  gap: 10px;
  align-items: flex-start;
  margin-bottom: 10px;
}
.resource-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  background: var(--accent-light);
  color: var(--accent-hover);
  flex-shrink: 0;
}
.resource-type {
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-bottom: 2px;
}
.resource-card h3 {
  margin: 0;
  font-size: 0.92rem;
  line-height: 1.35;
}
.lesson-video-card {
  border: 1px solid var(--border);
  background: linear-gradient(180deg, rgba(59, 130, 246, 0.08), rgba(255, 255, 255, 0.92));
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}
.lesson-video-card.playing { background: var(--card-bg-solid); }
.lesson-video-card.empty { background: rgba(148, 163, 184, 0.08); }
.lesson-video-summary {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
}
.lesson-video-icon {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: grid;
  place-items: center;
  color: #fff;
  background: var(--text);
  font-size: 1.1rem;
}
.lesson-video-copy {
  min-width: 0;
  display: grid;
  gap: 4px;
}
.lesson-video-copy strong { font-size: 0.94rem; }
.lesson-video-copy span {
  color: var(--text);
  font-size: 0.9rem;
  overflow-wrap: anywhere;
}
.lesson-video-copy small {
  color: var(--text-muted);
  line-height: 1.5;
  overflow-wrap: anywhere;
}
.lesson-video-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
  justify-content: flex-end;
}
.lesson-video-player {
  margin-top: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  overflow: hidden;
  background: #000;
  aspect-ratio: 16 / 9;
}
.lesson-video-player iframe {
  width: 100%;
  height: 100%;
  display: block;
  border: 0;
}
.rich-text,
.question-text,
.feedback-box,
.tutor-msg {
  overflow-wrap: anywhere;
  word-break: break-word;
}
.rich-text {
  font-size: 0.9rem;
  line-height: 1.8;
  color: var(--text-secondary);
  max-width: 100%;
}
.rich-text :deep(p),
.rich-text :deep(li),
.rich-text :deep(ol),
.rich-text :deep(ul),
.tutor-msg :deep(p),
.tutor-msg :deep(li),
.tutor-msg :deep(ol),
.tutor-msg :deep(ul) {
  max-width: 100%;
  overflow-wrap: anywhere;
}
.rich-text :deep(ol),
.rich-text :deep(ul),
.tutor-msg :deep(ol),
.tutor-msg :deep(ul),
.question-text :deep(ol),
.question-text :deep(ul),
.feedback-box :deep(ol),
.feedback-box :deep(ul) {
  margin: 8px 0 8px 0;
  padding-left: 1.4rem;
  list-style-position: outside;
}
.rich-text :deep(li),
.tutor-msg :deep(li),
.question-text :deep(li),
.feedback-box :deep(li) {
  padding-left: 0.25rem;
  margin: 4px 0;
}
.rich-text :deep(.katex-display),
.tutor-msg :deep(.katex-display),
.question-text :deep(.katex-display) {
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
}
.rich-text :deep(pre),
.rich-text :deep(code),
.tutor-msg :deep(pre),
.tutor-msg :deep(code) {
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.formula-list { display: grid; gap: 8px; margin: 16px 0; }
.formula-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: var(--accent-light);
  border-radius: 8px;
  min-width: 0;
}
.formula-item span {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: var(--accent);
  color: #fff;
  display: grid;
  place-items: center;
  font-size: 0.72rem;
}
.formula-rendered {
  min-width: 0;
  max-width: 100%;
  overflow-x: auto;
  overflow-y: hidden;
  color: var(--text);
  line-height: 1.7;
}
.formula-rendered :deep(.katex-display) {
  margin: 0;
}
.formula-rendered :deep(.katex) {
  font-size: 1.04rem;
}
.example-title { font-weight: 700; font-size: 0.86rem; margin-bottom: 8px; color: var(--accent-hover); }
.example-item ol {
  margin: 10px 0 0;
  padding-left: 1.45rem;
}
.example-item li {
  padding-left: 0.25rem;
  margin: 5px 0;
}
.question-text { font-size: 0.95rem; line-height: 1.8; margin-bottom: 14px; }
.option-row {
  width: 100%;
  border: 1px solid var(--border);
  background: var(--card-bg-solid);
  border-radius: 8px;
  padding: 12px 14px;
  margin-bottom: 9px;
  display: flex;
  gap: 12px;
  text-align: left;
  cursor: pointer;
  min-width: 0;
  overflow-wrap: anywhere;
}
.option-row:hover:not(:disabled), .option-row.selected { border-color: var(--accent); background: var(--accent-light); }
.option-row.correct { border-color: var(--success); background: var(--success-light); }
.option-row.wrong { border-color: var(--danger); background: var(--danger-light); }
.option-key {
  width: 26px;
  height: 26px;
  border-radius: 50%;
  background: var(--bg-alt);
  display: grid;
  place-items: center;
  font-weight: 700;
  flex-shrink: 0;
}
.feedback-box {
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 14px;
  display: grid;
  gap: 6px;
  font-size: 0.88rem;
}
.feedback-box.ok { background: var(--success-light); border: 1px solid var(--success); }
.feedback-box.bad { background: var(--danger-light); border: 1px solid var(--danger); }
.practice-intervention {
  border: 1px solid #f59e0b;
  background: #fffbeb;
  border-radius: 8px;
  padding: 14px 16px;
  margin-bottom: 14px;
  display: grid;
  gap: 10px;
  color: #78350f;
  overflow-wrap: anywhere;
}
.practice-intervention-head {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 0.92rem;
}
.practice-intervention p {
  margin: 0;
  line-height: 1.6;
  color: #92400e;
}
.intervention-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.intervention-actions span {
  border: 1px solid #fbbf24;
  background: #fff7ed;
  border-radius: 999px;
  padding: 5px 10px;
  font-size: 0.78rem;
  color: #78350f;
}
.action-row { display: flex; gap: 10px; justify-content: flex-end; flex-wrap: wrap; }
.checkpoint-result {
  text-align: center;
  display: grid;
  justify-items: center;
  gap: 10px;
  padding: 36px 20px;
}
.checkpoint-result > i { font-size: 3rem; }
.checkpoint-result.passed > i { color: var(--success); }
.checkpoint-result.failed > i { color: var(--danger); }
.checkpoint-result h2 { margin: 0; font-size: 1.35rem; }
.score { font-size: 2rem; font-weight: 800; color: var(--accent); }
.remedial-actions { display: flex; gap: 8px; flex-wrap: wrap; justify-content: center; }
.remedial-actions span { background: var(--bg-alt); border: 1px solid var(--border); border-radius: 999px; padding: 5px 10px; font-size: 0.78rem; }
.empty-inline { color: var(--text-muted); font-size: 0.9rem; padding: 18px; border: 1px dashed var(--border); border-radius: 8px; }
.tutor-panel {
  position: sticky;
  top: 80px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: calc(100vh - 104px);
}
.tutor-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--border);
}
.tutor-head strong { display: block; font-size: 0.95rem; }
.tutor-head span { display: block; color: var(--text-muted); font-size: 0.76rem; margin-top: 2px; }
.tutor-head i { font-size: 1.3rem; color: var(--accent); }
.tutor-collapse-btn {
  border: 1px solid var(--border);
  background: var(--bg-alt);
  color: var(--text-secondary);
  width: 30px;
  height: 30px;
  border-radius: 8px;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 1.05rem;
  flex-shrink: 0;
  transition: all 0.2s;
}
.tutor-collapse-btn:hover { border-color: var(--accent); color: var(--accent); }
/* 收起后的浮出竖条 */
.tutor-reopen {
  position: sticky;
  top: 80px;
  align-self: start;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  padding: 14px 8px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--card-bg-solid);
  color: var(--text-secondary);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  box-shadow: var(--shadow-sm);
  transition: all 0.2s;
}
.tutor-reopen:hover { border-color: var(--accent); color: var(--accent); }
.tutor-reopen i:first-child { color: var(--accent); font-size: 1.2rem; }
.tutor-reopen .tutor-reopen-text {
  writing-mode: vertical-rl;
  letter-spacing: 2px;
}
.prompt-chips { display: flex; flex-wrap: wrap; gap: 8px; }
.prompt-chips button {
  border: 1px solid var(--border);
  background: var(--bg-alt);
  border-radius: 999px;
  padding: 6px 10px;
  font-size: 0.76rem;
  cursor: pointer;
  max-width: 100%;
  overflow-wrap: anywhere;
}
.tutor-messages {
  height: 360px;
  overflow-y: auto;
  display: flex;
  flex-direction: column;
  gap: 10px;
  padding: 4px;
}
.tutor-msg {
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 0.84rem;
  line-height: 1.7;
  min-width: 0;
}
.tutor-msg.user { align-self: flex-end; max-width: min(88%, 300px); background: var(--text); color: #fff; }
.tutor-msg.ai { align-self: flex-start; max-width: min(94%, 320px); background: var(--bg-alt); color: var(--text); }
.tutor-waiting { color: var(--text-muted); font-size: 0.82rem; padding: 8px; }
.tutor-input { display: grid; grid-template-columns: minmax(0, 1fr) auto; gap: 8px; align-items: end; }
.tutor-input textarea {
  resize: none;
  border: 1px solid var(--border);
  border-radius: 8px;
  padding: 9px 10px;
  font: inherit;
  outline: none;
  min-width: 0;
  max-width: 100%;
  overflow-wrap: anywhere;
}
.tutor-input textarea:focus { border-color: var(--accent); }
@media (max-width: 980px) {
  .learning-grid { grid-template-columns: 1fr; }
  .tutor-panel { position: static; }
}
@media (max-width: 640px) {
  .task-hero { align-items: flex-start; flex-direction: column; }
  .task-section { padding: 16px; }
  .step-tabs { width: 100%; overflow-x: auto; }
  .resource-grid { grid-template-columns: 1fr; }
  .lesson-video-summary { grid-template-columns: auto minmax(0, 1fr); }
  .lesson-video-actions { grid-column: 1 / -1; justify-content: flex-start; }
}
</style>
