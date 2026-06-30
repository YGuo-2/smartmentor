import { ref, onMounted, nextTick, watch } from 'vue'
import { useRouter } from 'vue-router'
import { api } from '../api.js'
import { renderLatex, showToast } from '../state.js'

export const LessonPage = {
  name: 'LessonPage',
  props: {
    pathId: { type: String, required: true },
    nodeId: { type: String, required: true }
  },
  setup(props) {
    const router = useRouter()

    const loading = ref(true)
    const nodeData = ref(null)
    const nodeType = ref('')

    // Exercise state
    const currentExerciseIndex = ref(0)
    const selectedAnswer = ref('')
    const submitted = ref(false)
    const submitResult = ref(null)
    const allAnswers = ref([])

    // Checkpoint state
    const checkpointResults = ref(null)
    const lastAction = ref({ key: '', at: 0 })

    const currentExercise = () => {
      if (!nodeData.value || !nodeData.value.exercises) return null
      return normalizeExercise(nodeData.value.exercises[currentExerciseIndex.value], currentExerciseIndex.value)
    }

    async function loadLesson() {
      loading.value = true
      try {
        const data = await api.learning.lesson(props.pathId, props.nodeId)
        nodeData.value = data
        nodeType.value = data.isCheckpoint ? 'checkpoint' : data.type || 'lesson'
        await nextTick()
        renderLatex()
      } catch (e) {
        showToast('加载课程内容失败', 'error')
      } finally {
        loading.value = false
      }
    }

    function resetLessonState() {
      nodeData.value = null
      nodeType.value = ''
      currentExerciseIndex.value = 0
      selectedAnswer.value = ''
      submitted.value = false
      submitResult.value = null
      allAnswers.value = []
      checkpointResults.value = null
      lastAction.value = { key: '', at: 0 }
    }

    function selectOption(option) {
      if (submitted.value) return
      const key = optionKey(option)
      if (key) selectedAnswer.value = key
    }

    function normalizeExercise(exercise, index) {
      const raw = exercise && typeof exercise === 'object' ? exercise : {}
      return {
        ...raw,
        exerciseId: raw.exerciseId || raw.id || `exercise_${index + 1}`,
        content: raw.content || raw.problem || raw.question || raw.title || '',
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
          const optionKey = normalizeOptionKey(key, index)
          return { key: optionKey, label: optionKey, text: stripOptionPrefix(value) || String(value ?? '') }
        }).filter(option => option.key)
      }
      return String(options).split(/\n+/).map((option, index) => normalizeOption(option, index)).filter(option => option.key)
    }

    function normalizeOption(option, index) {
      if (option && typeof option === 'object' && !Array.isArray(option)) {
        const rawKey = option.key || option.label || option.optionKey || option.option || option.id
        const rawText = option.text ?? option.content ?? option.description ?? option.value ?? option.name ?? rawKey
        const key = normalizeOptionKey(rawKey || rawText, index)
        return { ...option, key, label: option.label || key, text: stripOptionPrefix(rawText) || String(rawText ?? '') }
      }
      const text = String(option ?? '')
      const key = normalizeOptionKey(text, index)
      return { key, label: key, text: stripOptionPrefix(text) || text }
    }

    function normalizeOptionKey(value, index) {
      const match = String(value ?? '').trim().match(/^([A-Za-z])(?:[.、):：\s]|$)/)
      return match ? match[1].toUpperCase() : String.fromCharCode(65 + index)
    }

    function stripOptionPrefix(value) {
      return String(value ?? '').trim().replace(/^[A-Za-z][.、):：\s]+/, '').trim()
    }

    function optionKey(option) {
      return String(option?.key || option?.label || '').trim()
    }

    function optionText(option) {
      return option?.text || option?.content || option?.value || ''
    }

    function runActionOnce(key, action) {
      const now = Date.now()
      if (lastAction.value.key === key && now - lastAction.value.at < 350) return
      lastAction.value = { key, at: now }
      action()
    }

    function handleSubmitAnswerAction() {
      runActionOnce('submitAnswer', submitAnswer)
    }

    function handleNextExerciseAction() {
      runActionOnce('nextExercise', nextExercise)
    }

    function handleCompleteAndNextAction() {
      runActionOnce('completeAndNext', completeAndNext)
    }

    function handleCheckpointNextAction() {
      runActionOnce('handleCheckpointNext', handleCheckpointNext)
    }

    async function submitAnswer() {
      if (!selectedAnswer.value) {
        showToast('请先选择一个答案', 'warning')
        return
      }

      const exercise = currentExercise()
      if (!exercise) return

      if (nodeType.value === 'checkpoint') {
        allAnswers.value.push({
          exerciseId: exercise.exerciseId,
          answer: selectedAnswer.value
        })

        if (currentExerciseIndex.value < nodeData.value.exercises.length - 1) {
          currentExerciseIndex.value++
          selectedAnswer.value = ''
          submitted.value = false
          submitResult.value = null
          await nextTick()
          renderLatex()
        } else {
          await submitCheckpoint()
        }
        return
      }

      try {
        const result = await api.learning.submitExercise({
          pathId: props.pathId,
          nodeId: props.nodeId,
          exerciseId: exercise.exerciseId,
          answer: selectedAnswer.value
        })
        submitResult.value = result
        submitted.value = true
        await nextTick()
        renderLatex()
      } catch (e) {
        showToast('提交答案失败', 'error')
      }
    }

    async function submitCheckpoint() {
      try {
        const result = await api.learning.submitCheckpoint({
          pathId: props.pathId,
          nodeId: props.nodeId,
          answers: allAnswers.value
        })
        checkpointResults.value = result
        submitted.value = true
        await nextTick()
        renderLatex()
      } catch (e) {
        showToast('提交检查点失败', 'error')
      }
    }

    function nextExercise() {
      if (submitResult.value && submitResult.value.isComplete) {
        completeAndNext()
        return
      }
      if (currentExerciseIndex.value < nodeData.value.exercises.length - 1) {
        currentExerciseIndex.value++
        selectedAnswer.value = ''
        submitted.value = false
        submitResult.value = null
        nextTick(() => renderLatex())
      }
    }

    function completeAndNext() {
      showToast('已完成，进入下一节', 'success')
      router.push(`/learning/${props.pathId}`)
    }

    function handleCheckpointNext() {
      const nextNodeId = resolveCheckpointNextNodeId(checkpointResults.value)
      if (nextNodeId) {
        router.push(`/learning/${props.pathId}/${nextNodeId}`)
      } else {
        router.push(`/learning/${props.pathId}`)
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
      router.push(`/learning/${props.pathId}`)
    }

    onMounted(() => {
      loadLesson()
    })

    watch(() => [props.pathId, props.nodeId], () => {
      resetLessonState()
      loadLesson()
    })

    return {
      loading,
      nodeData,
      nodeType,
      currentExerciseIndex,
      selectedAnswer,
      submitted,
      submitResult,
      checkpointResults,
      allAnswers,
      currentExercise,
      selectOption,
      optionKey,
      optionText,
      handleSubmitAnswerAction,
      handleNextExerciseAction,
      handleCompleteAndNextAction,
      handleCheckpointNextAction,
      submitAnswer,
      nextExercise,
      completeAndNext,
      handleCheckpointNext,
      goBackToPath
    }
  },
  template: `
    <div class="lesson-container">
      <nav class="topnav">
        <a href="javascript:;" @click="goBackToPath" class="breadcrumb-link">&larr; 返回路径</a>
      </nav>

      <div v-if="loading" class="loading-state">
        <p>加载中...</p>
      </div>

      <!-- Lesson Type -->
      <template v-else-if="nodeType === 'lesson' && nodeData">
        <div class="lesson-header">
          <h1>{{ nodeData.title }}</h1>
          <span class="badge">{{ nodeData.knowledgePoint }}</span>
        </div>

        <div class="lesson-content">
          <div
            v-for="(section, idx) in nodeData.content.sections"
            :key="idx"
            class="lesson-section"
          >
            <h3 v-if="section.title">{{ section.title }}</h3>
            <div v-html="section.body"></div>
          </div>

          <div v-if="nodeData.content.keyFormulas && nodeData.content.keyFormulas.length" class="lesson-section">
            <h3>重点公式</h3>
            <div
              v-for="(formula, idx) in nodeData.content.keyFormulas"
              :key="'f'+idx"
              class="formula-highlight"
            >
              {{ formula }}
            </div>
          </div>

          <div v-if="nodeData.content.examples && nodeData.content.examples.length" class="lesson-section">
            <h3>例题</h3>
            <div
              v-for="(example, idx) in nodeData.content.examples"
              :key="'e'+idx"
              class="example-block"
            >
              <h4>例题 {{ idx + 1 }}</h4>
              <div class="example-question" v-html="example.question"></div>
              <div class="example-steps">
                <p v-for="(step, sIdx) in example.steps" :key="sIdx">
                  <strong>步骤 {{ sIdx + 1 }}:</strong> <span v-html="step"></span>
                </p>
              </div>
              <div v-if="example.answer" class="example-answer">
                <strong>答案:</strong> <span v-html="example.answer"></span>
              </div>
            </div>
          </div>
        </div>

        <div class="lesson-actions">
          <button type="button" class="btn btn-dark" @pointerdown.prevent="handleCompleteAndNextAction" @mousedown.prevent="handleCompleteAndNextAction" @click.prevent="handleCompleteAndNextAction">我学会了</button>
        </div>
      </template>

      <!-- Exercise Type -->
      <template v-else-if="nodeType === 'exercise' && nodeData && nodeData.exercises && nodeData.exercises.length">
        <div class="lesson-header">
          <h1>{{ nodeData.title }}</h1>
          <span class="badge">练习 {{ currentExerciseIndex + 1 }} / {{ nodeData.exercises.length }}</span>
        </div>

        <div class="question-card">
          <div class="question-content" v-html="currentExercise().content"></div>
          <div class="question-options">
            <div
              v-for="option in currentExercise().options"
              :key="optionKey(option)"
              class="option-item"
              :class="{
                selected: selectedAnswer === optionKey(option),
                correct: submitted && submitResult && optionKey(option) === submitResult.correctAnswer,
                wrong: submitted && submitResult && selectedAnswer === optionKey(option) && !submitResult.correct
              }"
              @click="selectOption(option)"
            >
              <span class="option-label">{{ optionKey(option) }}</span>
              <span class="option-text" v-html="optionText(option)"></span>
            </div>
          </div>
        </div>

        <div v-if="submitted && submitResult" class="feedback-box" :class="{ correct: submitResult.correct, wrong: !submitResult.correct }">
          <p class="feedback-result">{{ submitResult.correct ? '回答正确!' : '回答错误' }}</p>
          <p v-if="submitResult.correctAnswer"><strong>正确答案:</strong> {{ submitResult.correctAnswer }}</p>
          <div v-if="submitResult.explanation"><strong>解析:</strong> <span v-html="submitResult.explanation"></span></div>
          <div v-if="submitResult.errorAnalysis"><strong>错因分析:</strong> <span v-html="submitResult.errorAnalysis"></span></div>
        </div>

        <div class="lesson-actions">
          <button v-if="!submitted" type="button" class="btn btn-dark" @pointerdown.prevent="handleSubmitAnswerAction" @mousedown.prevent="handleSubmitAnswerAction" @click.prevent="handleSubmitAnswerAction">提交答案</button>
          <button v-else-if="submitResult && submitResult.isComplete" type="button" class="btn btn-dark" @pointerdown.prevent="handleCompleteAndNextAction" @mousedown.prevent="handleCompleteAndNextAction" @click.prevent="handleCompleteAndNextAction">完成练习</button>
          <button v-else-if="submitted" type="button" class="btn btn-outline" @pointerdown.prevent="handleNextExerciseAction" @mousedown.prevent="handleNextExerciseAction" @click.prevent="handleNextExerciseAction">下一题</button>
        </div>
      </template>

      <!-- Checkpoint Type -->
      <template v-else-if="nodeType === 'checkpoint' && nodeData && nodeData.exercises && nodeData.exercises.length">
        <div class="lesson-header">
          <h1>{{ nodeData.title }}</h1>
          <span class="badge">检查点</span>
          <span v-if="!checkpointResults" class="badge">{{ currentExerciseIndex + 1 }} / {{ nodeData.exercises.length }}</span>
        </div>

        <template v-if="!checkpointResults">
          <div class="question-card">
            <div class="question-content" v-html="currentExercise().content"></div>
            <div class="question-options">
              <div
                v-for="option in currentExercise().options"
                :key="optionKey(option)"
                class="option-item"
                :class="{ selected: selectedAnswer === optionKey(option) }"
                @click="selectOption(option)"
              >
                <span class="option-label">{{ optionKey(option) }}</span>
                <span class="option-text" v-html="optionText(option)"></span>
              </div>
            </div>
          </div>

          <div class="lesson-actions">
            <button type="button" class="btn btn-dark" @pointerdown.prevent="handleSubmitAnswerAction" @mousedown.prevent="handleSubmitAnswerAction" @click.prevent="handleSubmitAnswerAction">
              {{ currentExerciseIndex < nodeData.exercises.length - 1 ? '下一题' : '提交检查点' }}
            </button>
          </div>
        </template>

        <template v-else>
          <div class="feedback-box" :class="{ correct: checkpointResults.passed, wrong: !checkpointResults.passed }">
            <p class="feedback-result">{{ checkpointResults.passed ? '检查点通过!' : '检查点未通过' }}</p>
            <p><strong>得分:</strong> {{ checkpointResults.score }}</p>
            <div v-if="checkpointResults.feedback"><span v-html="checkpointResults.feedback"></span></div>
            <p v-if="!checkpointResults.passed" class="checkpoint-suggestion">建议重新学习相关知识点或重新进行诊断测试。</p>
          </div>

          <div class="lesson-actions">
            <button type="button" class="btn btn-dark" @pointerdown.prevent="handleCheckpointNextAction" @mousedown.prevent="handleCheckpointNextAction" @click.prevent="handleCheckpointNextAction">
              {{ checkpointResults.passed ? '继续学习' : '返回路径' }}
            </button>
          </div>
        </template>
      </template>
    </div>
  `
}
