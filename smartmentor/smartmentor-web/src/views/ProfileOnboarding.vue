<template>
<div class="onboarding-wrapper">
  <div class="onboarding-card">
    <!-- 顶部 -->
    <div class="onboarding-header">
      <div class="ob-avatar">AI</div>
      <div class="ob-title">
        <h2>AI 学习画像访谈</h2>
        <p>和我聊几句，我来帮你建立个性化学习画像，不用填表</p>
      </div>
      <button class="ob-skip" @click="skip" v-if="!finished">跳过</button>
    </div>

    <!-- 对话区 -->
    <div class="onboarding-messages" ref="messagesContainer">
      <div
        v-for="(msg, index) in messages"
        :key="index"
        class="ob-row"
        :class="msg.role"
      >
        <div v-if="msg.role === 'ai'" class="ob-msg-avatar">AI</div>
        <div class="ob-bubble" :class="msg.role">
          <MarkdownMessage
            v-if="msg.role === 'ai'"
            :content="msg.content"
            :streaming="isStreaming && index === messages.length - 1"
          />
          <span v-else>{{ msg.content }}</span>
        </div>
      </div>

      <!-- 打字指示 -->
      <div v-if="isStreaming && !streamingContent" class="ob-row ai">
        <div class="ob-msg-avatar">AI</div>
        <div class="typing-dots"><span></span><span></span><span></span></div>
      </div>

      <!-- 抽取中提示 -->
      <div v-if="extracting" class="ob-row ai">
        <div class="ob-msg-avatar">AI</div>
        <div class="ob-extracting">
          <i class="ri-loader-4-line spin"></i> 正在分析对话，生成你的学习画像…
        </div>
      </div>

      <!-- 画像生成结果 -->
      <div v-if="finished" class="ob-result">
        <div class="ob-result-icon"><i class="ri-checkbox-circle-fill"></i></div>
        <div class="ob-result-title">画像已生成</div>
        <div v-if="appliedSummary.length" class="ob-result-tags">
          <span v-for="(t, i) in appliedSummary" :key="i" class="ob-tag">{{ t }}</span>
        </div>
        <div v-else class="ob-result-empty">这次没有抽取到新特征，你可以稍后在设置里补充</div>
        <div class="ob-result-actions">
          <button class="btn btn-dark" @click="goProfile">查看我的画像</button>
          <button class="btn btn-ghost" @click="goDashboard">进入学习</button>
        </div>
      </div>
    </div>

    <!-- 输入区 -->
    <div class="onboarding-input" v-if="!finished">
      <textarea
        v-model="inputText"
        @keydown="handleKeydown"
        :disabled="isStreaming || extracting"
        placeholder="说说你的专业、目标，或正在学什么…（Enter 发送）"
        rows="1"
        ref="inputRef"
      ></textarea>
      <button class="ob-finish" @click="finishNow" :disabled="isStreaming || extracting" title="结束访谈并生成画像">
        <i class="ri-check-double-line"></i> 完成
      </button>
      <button class="ob-send" @click="sendMessage" :disabled="!inputText.trim() || isStreaming || extracting">
        <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
        </svg>
      </button>
    </div>
  </div>
</div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { api, streamSse } from '../api/index.js'
import { showToast } from '../composables/state.js'
import MarkdownMessage from '../components/MarkdownMessage.vue'

const router = useRouter()
const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const sessionId = ref(null)
const finished = ref(false)
const extracting = ref(false)
const appliedSummary = ref([])
const messagesContainer = ref(null)
const inputRef = ref(null)
let abortController = null

const FIELD_LABELS = {
  majorDirection: '专业',
  educationLevel: '学历',
  currentCourse: '当前课程',
  learningGoal: '学习目标',
  foundationLevel: '基础水平',
  academicInterest: '兴趣方向',
  learningStyle: '认知风格',
  resourcePreference: '资源偏好',
  weakModulePriority: '薄弱模块'
}
const STYLE_LABELS = { visual: '视觉型', logical: '逻辑型', example: '案例型', formula: '公式型' }

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

async function sendMessage() {
  const message = inputText.value.trim()
  if (!message || isStreaming.value) return

  inputText.value = ''
  messages.value.push({ role: 'user', content: message })
  scrollToBottom()
  autoResize()

  isStreaming.value = true
  streamingContent.value = ''
  const aiMessage = { role: 'ai', content: '' }
  messages.value.push(aiMessage)
  const msgIdx = messages.value.length - 1

  abortController = new AbortController()
  try {
    for await (const evt of streamSse('/chat/stream', {
      params: {
        mode: 'profile_interview',
        message,
        sessionId: sessionId.value || ''
      },
      signal: abortController.signal
    })) {
      if (evt.type === 'metadata') {
        if (evt.data.sessionId) sessionId.value = evt.data.sessionId
      } else if (evt.type === 'message') {
        aiMessage.content += evt.data.content || ''
        streamingContent.value = aiMessage.content
        messages.value.splice(msgIdx, 1, { ...aiMessage })
        scrollToBottom()
      } else if (evt.type === 'done') {
        break
      } else if (evt.type === 'error') {
        showToast(evt.data.message || evt.data.error || 'AI 响应出错', 'error')
        break
      }
    }
  } catch (e) {
    if (e.name !== 'AbortError') showToast(e.message || '发送失败', 'error')
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
    abortController = null
    // 检测访谈结束标记（AI 主动结束）
    await autoDetectFinish(aiMessage, msgIdx)
    scrollToBottom()
  }
}

async function autoDetectFinish(aiMessage, msgIdx) {
  // 放宽检测：容忍空格、全角括号、大小写
  if (!aiMessage.content) return
  const hit = /[\[【]\s*\[?\s*INTERVIEW_DONE\s*\]?\s*[\]】]/i.test(aiMessage.content)
    || aiMessage.content.includes('INTERVIEW_DONE')
  if (!hit) return
  // 剥离标记后展示
  aiMessage.content = aiMessage.content
    .replace(/[\[【]\s*\[?\s*INTERVIEW_DONE\s*\]?\s*[\]】]/ig, '')
    .replace(/INTERVIEW_DONE/g, '')
    .trim()
  messages.value.splice(msgIdx, 1, { ...aiMessage })
  await doExtract()
}

// 用户主动点击「完成访谈」：不依赖 AI 标记，直接抽取写库
async function finishNow() {
  if (extracting.value || finished.value) return
  // 至少要有一轮真实回答才值得抽取
  const hasUserInput = messages.value.some(m => m.role === 'user')
  if (!hasUserInput) {
    showToast('先回答几句，我才能帮你生成画像哦', 'info')
    return
  }
  await doExtract()
}

async function doExtract() {
  if (extracting.value) return
  extracting.value = true
  const conversationText = messages.value
    .map(m => (m.role === 'user' ? '学生：' : '导师：') + m.content)
    .join('\n')
  try {
    const res = await api.profile.buildExtract({ conversationText, overwrite: true })
    appliedSummary.value = summarizeApplied(res?.appliedFields || {})
    // 标记本机已完成引导，避免再次被自动引导
    localStorage.setItem('profileOnboarded', '1')
  } catch (e) {
    showToast('画像生成失败：' + (e.message || '未知错误'), 'error')
  } finally {
    extracting.value = false
    finished.value = true
    scrollToBottom()
  }
}

function summarizeApplied(applied) {
  const tags = []
  for (const [key, val] of Object.entries(applied)) {
    const label = FIELD_LABELS[key] || key
    let display = val
    if (Array.isArray(val)) display = val.join('、')
    else if (key === 'learningStyle') display = STYLE_LABELS[val] || val
    tags.push(`${label}：${display}`)
  }
  return tags
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function autoResize() {
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
  }
}

function skip() { router.push('/dashboard') }
function goProfile() { router.push('/profile') }
function goDashboard() { router.push('/dashboard') }

onMounted(() => {
  // 开场白：由 AI 先问第一个问题
  messages.value.push({
    role: 'ai',
    content: '嗨，我是你的学习伙伴 👋\n\n为了给你定制最合适的学习内容，我想先简单了解你几件事。\n\n先说说看——**你学的是什么专业，现在主要在学哪门课呢？**'
  })
})
</script>

<style scoped>
.onboarding-wrapper {
  min-height: calc(100vh - 64px);
  margin-top: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: linear-gradient(135deg, #f5f7fb 0%, #eef1f8 100%);
}
.onboarding-card {
  width: 100%;
  max-width: 720px;
  height: min(80vh, 720px);
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 18px;
  box-shadow: 0 12px 40px rgba(30, 50, 90, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.onboarding-header {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--border);
}
.ob-avatar {
  width: 44px; height: 44px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-weight: 700;
}
.ob-title h2 { font-size: 1.1rem; margin: 0; }
.ob-title p { font-size: 0.82rem; color: var(--text-secondary); margin: 2px 0 0; }
.ob-skip {
  margin-left: auto;
  background: none; border: none;
  color: var(--text-muted); font-size: 0.82rem; cursor: pointer;
}
.ob-skip:hover { color: var(--text); }

.onboarding-messages {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.ob-row { display: flex; align-items: flex-start; gap: 10px; }
.ob-row.user { justify-content: flex-end; }
.ob-msg-avatar {
  width: 30px; height: 30px; border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff; display: flex; align-items: center; justify-content: center;
  font-size: 0.65rem; font-weight: 700; flex-shrink: 0;
}
.ob-bubble {
  max-width: 78%;
  padding: 12px 16px;
  border-radius: 14px;
  font-size: 0.9rem;
  line-height: 1.7;
}
.ob-bubble.ai {
  background: #f7f9fc;
  border: 1px solid var(--border);
  border-bottom-left-radius: 4px;
  color: #3b3b3b;
}
.ob-bubble.user {
  background: #eef6ff;
  border: 1px solid #c7dff7;
  color: #14365d;
  border-bottom-right-radius: 4px;
}

.ob-result {
  align-self: center;
  text-align: center;
  padding: 24px;
  background: #f7fbff;
  border: 1px solid #d4e7fb;
  border-radius: 14px;
  width: 100%;
  max-width: 480px;
}
.ob-result-icon { font-size: 2.4rem; color: #22c55e; }
.ob-result-title { font-size: 1.05rem; font-weight: 700; margin: 6px 0 14px; }
.ob-result-tags { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-bottom: 18px; }
.ob-tag {
  font-size: 0.78rem;
  padding: 5px 12px;
  border-radius: 999px;
  background: #eaf2ff;
  color: #1e4d8c;
  border: 1px solid #c5d9f0;
}
.ob-result-empty { font-size: 0.84rem; color: var(--text-secondary); margin-bottom: 18px; }
.ob-result-actions { display: flex; gap: 10px; justify-content: center; }
.btn-ghost {
  background: transparent;
  border: 1px solid var(--border);
  color: var(--text-secondary);
}

.onboarding-input {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 16px 24px;
  border-top: 1px solid var(--border);
}
.onboarding-input textarea {
  flex: 1;
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 10px 14px;
  font-size: 0.9rem;
  line-height: 1.5;
  resize: none;
  outline: none;
  max-height: 120px;
  font-family: inherit;
}
.onboarding-input textarea:focus { border-color: var(--accent); }
.ob-send {
  width: 40px; height: 40px;
  border-radius: 10px; border: none;
  background: var(--text); color: #fff;
  display: flex; align-items: center; justify-content: center;
  cursor: pointer; flex-shrink: 0;
}
.ob-send:disabled { opacity: 0.4; cursor: not-allowed; }
.ob-finish {
  height: 40px;
  padding: 0 14px;
  border-radius: 10px;
  border: 1px solid var(--accent, #667eea);
  background: #eef1fb;
  color: var(--accent, #667eea);
  font-size: 0.82rem;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.ob-finish:hover:not(:disabled) {
  background: var(--accent, #667eea);
  color: #fff;
}
.ob-finish:disabled { opacity: 0.4; cursor: not-allowed; }

.ob-extracting {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  background: #f7f9fc;
  border: 1px solid var(--border);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
  font-size: 0.86rem;
  color: var(--text-secondary);
}
.spin { animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

.typing-dots {
  display: flex; gap: 4px;
  padding: 12px 16px;
  background: #f7f9fc;
  border: 1px solid var(--border);
  border-radius: 14px;
  border-bottom-left-radius: 4px;
}
.typing-dots span {
  width: 6px; height: 6px;
  background: var(--text-muted);
  border-radius: 50%;
  animation: dotsAnim 1.2s infinite;
}
.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }
@keyframes dotsAnim {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-4px); }
}
</style>
