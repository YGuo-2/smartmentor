<template>
<div class="chat-page-wrapper">

  <div class="chat-layout" :class="{ 'sidebar-collapsed': !sidebarVisible }">
    <aside class="chat-sidebar-box" :class="{ collapsed: !sidebarVisible }">
      <div class="sidebar-top">
        <button class="btn btn-dark" style="width:100%" @click="startNewSession">+ 新对话</button>
      </div>
      <div class="sidebar-sessions">
        <div
          v-for="session in sessions"
          :key="session.sessionId"
          class="session-item"
          :class="{ active: session.sessionId === currentSessionId }"
          @click="loadSessionMessages(session.sessionId)"
        >
          <div class="session-title">{{ session.title || '未命名对话' }}</div>
          <div class="session-time">{{ formatDate(session.updatedAt) }}</div>
        </div>
        <div v-if="sessions.length === 0" style="text-align:center;padding:24px 12px;color:var(--text-muted);font-size:0.82rem;">
          暂无历史对话
        </div>
      </div>
    </aside>

    <main class="chat-main-area">
      <div class="chat-header-bar">
        <button class="sidebar-toggle" @click="toggleSidebar">
          <span v-if="sidebarVisible">◀</span>
          <span v-else>▶</span>
        </button>
        <div class="chat-ai-info">
          <div class="chat-ai-avatar">AI</div>
          <div>
            <div class="chat-ai-name">SmartMentor</div>
            <div class="chat-ai-status">在线 · AI 课程伴学</div>
          </div>
        </div>
        <button class="new-chat-btn" @click="startNewSession" title="新建对话">
          <i class="ri-add-line"></i>
          <span>新对话</span>
        </button>
      </div>

      <div class="chat-messages-area" ref="messagesContainer">
        <!-- 欢迎界面 -->
        <div v-if="messages.length === 0 && !isStreaming" class="chat-welcome">
          <div class="chat-welcome-avatar">AI</div>
          <h2>你好，我是 SmartMentor</h2>
          <p>我是你的 AI 课程伴学助手，可以帮你理解课程概念、拆解实践任务、分析学习路径。</p>
          <div v-if="profileHint" class="chat-profile-hint">{{ profileHint }}</div>
          <div class="chat-suggestions">
            <button
              v-for="item in welcomeSuggestions"
              :key="item.text"
              class="chat-suggestion-item"
              @click="useSuggestion(item.prompt)"
            >{{ item.text }}</button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          v-for="(msg, index) in messages"
          :key="index"
          class="chat-bubble-row"
          :class="msg.role === 'user' ? 'user' : 'ai'"
        >
          <div v-if="msg.role !== 'user'" class="chat-bubble-avatar">AI</div>
          <div class="chat-bubble" :class="msg.role === 'user' ? 'user' : 'ai'">
            <MarkdownMessage
              class="chat-bubble-content"
              :content="msg.content"
              :streaming="isStreaming && msg.role === 'ai' && index === messages.length - 1"
            />
            <span v-if="isStreaming && msg.role === 'ai' && index === messages.length - 1" class="streaming-cursor"></span>
            <!-- 学习资源卡片 -->
            <div v-if="msg.resources && msg.resources.length" class="chat-resources">
              <div class="chat-resources-title">
                <i :class="msg.resources[0].type === 'search-link' ? 'ri-search-line' : 'ri-vidicon-line'"></i>
                {{ msg.resources[0].type === 'search-link' ? '为你推荐这些学习平台' : '为你找到的学习视频' }}
              </div>
              <a
                v-for="(res, ri) in msg.resources"
                :key="ri"
                class="chat-resource-card"
                :href="res.url"
                target="_blank"
                rel="noopener noreferrer"
              >
                <div class="chat-resource-icon">
                  <i :class="res.type === 'search-link' ? 'ri-external-link-line' : 'ri-play-circle-line'"></i>
                </div>
                <div class="chat-resource-info">
                  <div class="chat-resource-name">{{ res.title }}</div>
                  <div class="chat-resource-meta">
                    <span v-if="res.type !== 'search-link'"><i class="ri-user-line"></i> {{ res.author || '未知UP主' }}</span>
                    <span v-else><i class="ri-bookmark-line"></i> {{ res.description }}</span>
                    <span v-if="res.preferredAuthor" class="chat-resource-badge">权威来源</span>
                    <span v-if="res.playCount"><i class="ri-eye-line"></i> {{ formatPlayCount(res.playCount) }}</span>
                    <span v-if="res.duration"><i class="ri-time-line"></i> {{ res.duration }}</span>
                  </div>
                </div>
                <i class="ri-external-link-line chat-resource-open"></i>
              </a>
            </div>
          </div>
        </div>
        <!-- 等待首个token时显示打字指示器 -->
        <div v-if="isStreaming && !streamingContent" class="chat-bubble-row ai">
          <div class="chat-bubble-avatar">AI</div>
          <div class="typing-dots">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <div class="chat-bottom">
        <div v-if="pendingActionCard" class="chat-action-card">
          <div class="chat-action-icon">
            <i class="ri-compass-3-line"></i>
          </div>
          <div class="chat-action-copy">
            <div class="chat-action-title">看起来你卡在「{{ pendingActionCard.knowledgePointName }}」</div>
            <div class="chat-action-reason">{{ pendingActionCard.reason || '可以用诊断题快速定位薄弱环节' }}</div>
          </div>
          <button class="chat-action-btn" @click="goToDiagnostic(pendingActionCard.module)">
            <i class="ri-test-tube-line"></i>
            去诊断这块
          </button>
        </div>
        <div class="chat-input-box">
          <textarea
            class="chat-textarea"
            v-model="inputText"
            @keydown="handleKeydown"
            placeholder="输入课程问题... (Enter 发送, Shift+Enter 换行)"
            rows="1"
            ref="inputRef"
          ></textarea>
          <button
            v-if="isStreaming"
            class="stop-btn"
            @click="stopStreaming"
            title="停止生成"
          >
            <svg width="16" height="16" viewBox="0 0 24 24" fill="currentColor">
              <rect x="6" y="6" width="12" height="12" rx="2"/>
            </svg>
          </button>
          <button
            v-else
            class="send-btn"
            @click="sendMessage"
            :disabled="!inputText.trim()"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M22 2L11 13M22 2l-7 20-4-9-9-4 20-7z"/>
            </svg>
          </button>
        </div>
        <div class="chat-hint-text">SmartMentor 可能会犯错，请核实重要信息</div>
      </div>
    </main>
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
const sessions = ref([])
const currentSessionId = ref(null)
const messages = ref([])
const inputText = ref('')
const isStreaming = ref(false)
const streamingContent = ref('')
const sidebarVisible = ref(true)
const pendingActionCard = ref(null)
const profileHint = ref('')
const welcomeSuggestions = ref([
  { text: '训练集和测试集有什么区别？', prompt: '机器学习中的训练集和测试集有什么区别？' },
  { text: '设计一个 Java Web 登录接口练习', prompt: '帮我设计一个 Java Web 登录接口的练习任务' },
  { text: '组合逻辑和时序逻辑有什么区别？', prompt: '数字电路里的组合逻辑和时序逻辑有什么区别？' },
  { text: '安排一周学习计划', prompt: '根据我的基础给我安排一周学习计划' }
])
const messagesContainer = ref(null)
const inputRef = ref(null)
let abortController = null
let scrollRAF = null

async function loadSessions() {
  try {
    const res = await api.chat.history({ page: 0, size: 20 })
    sessions.value = res.sessions || []
  } catch (e) {
    showToast('加载会话列表失败', 'error')
  }
}

async function loadSessionMessages(sessionId) {
  currentSessionId.value = sessionId
  try {
    const res = await api.chat.history({ sessionId })
    messages.value = (res.messages || []).map(m => ({
      role: m.role === 'student' ? 'user' : 'ai',
      content: m.content,
      resources: m.resources || [],
      createdAt: m.timestamp
    }))
    await nextTick()
    scrollToBottom()
  } catch (e) {
    showToast('加载聊天记录失败', 'error')
  }
}

function startNewSession() {
  currentSessionId.value = null
  messages.value = []
  inputText.value = ''
  pendingActionCard.value = null
}

function useSuggestion(text) {
  inputText.value = text
  sendMessage()
}

function scrollToBottom() {
  if (scrollRAF) return
  scrollRAF = requestAnimationFrame(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
    scrollRAF = null
  })
}

function stopStreaming() {
  if (abortController) {
    abortController.abort()
    abortController = null
  }
  isStreaming.value = false
  streamingContent.value = ''
}

async function sendMessage() {
  const message = inputText.value.trim()
  if (!message || isStreaming.value) return

  inputText.value = ''
  messages.value.push({
    role: 'user',
    content: message,
    createdAt: new Date().toISOString()
  })

  await nextTick()
  scrollToBottom()
  autoResizeInput()

  isStreaming.value = true
  streamingContent.value = ''

  const aiMessage = {
    role: 'ai',
    content: '',
    resources: [],
    createdAt: new Date().toISOString()
  }
  messages.value.push(aiMessage)

  abortController = new AbortController()

  try {
    const msgIdx = messages.value.length - 1

    for await (const evt of streamSse('/chat/stream', {
      params: {
        message,
        sessionId: currentSessionId.value || ''
      },
      signal: abortController.signal
    })) {
      if (evt.type === 'metadata') {
        if (evt.data.sessionId) currentSessionId.value = evt.data.sessionId
        if (evt.data.actionCard) pendingActionCard.value = evt.data.actionCard
      } else if (evt.type === 'resources') {
        aiMessage.resources = evt.data.resources || []
        messages.value.splice(msgIdx, 1, { ...aiMessage })
        scrollToBottom()
      } else if (evt.type === 'message') {
        aiMessage.content += evt.data.content || ''
        streamingContent.value = aiMessage.content
        messages.value.splice(msgIdx, 1, { ...aiMessage })
        scrollToBottom()
      } else if (evt.type === 'done') {
        loadSessions()
        break
      } else if (evt.type === 'error') {
        showToast(evt.data.message || evt.data.error || 'AI 响应出错', 'error')
        break
      }
    }
  } catch (e) {
    if (e.name !== 'AbortError') {
      showToast(e.message || '发送消息失败', 'error')
    }
  } finally {
    isStreaming.value = false
    streamingContent.value = ''
    abortController = null
    await nextTick()
    scrollToBottom()
  }
}

function handleKeydown(e) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function autoResizeInput() {
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
    inputRef.value.style.height = Math.min(inputRef.value.scrollHeight, 120) + 'px'
  }
}

function toggleSidebar() {
  sidebarVisible.value = !sidebarVisible.value
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const month = (d.getMonth() + 1).toString().padStart(2, '0')
  const day = d.getDate().toString().padStart(2, '0')
  const hour = d.getHours().toString().padStart(2, '0')
  const min = d.getMinutes().toString().padStart(2, '0')
  return `${month}-${day} ${hour}:${min}`
}

function formatPlayCount(count) {
  const n = Number(count) || 0
  if (n >= 10000) return (n / 10000).toFixed(1) + '万'
  return String(n)
}

function goToDiagnostic(module) {
  if (!module) return
  pendingActionCard.value = null
  router.push('/diagnostic?module=' + encodeURIComponent(module))
}

async function loadProfileHint() {
  try {
    const overview = await api.profile.overview()
    const goal = overview?.dimensions?.goalProfile || {}
    const overall = overview?.overallProfile || goal.overallProfile || {}
    const major = overall.majorDirection || goal.majorDirection || ''
    const foundation = goal.foundationLevel || ''
    const subjectProfiles = Array.isArray(overview?.subjectProfiles) ? overview.subjectProfiles : []
    if (major || foundation) {
      profileHint.value = `已读取总体画像 · ${major || '专业待确认'} · 基础${foundation || '待确认'}`
    }
    const subjectWithGap = subjectProfiles.find(item => Array.isArray(item.gaps) && item.gaps.some(Boolean))
    if (subjectWithGap) {
      const subject = subjectWithGap.subject || subjectWithGap.course || '这门课'
      const gap = subjectWithGap.gaps.find(item => String(item || '').trim())
      const weakText = `${subject} · ${String(gap).trim()}`
      welcomeSuggestions.value = [
        { text: `帮我攻克${weakText}`, prompt: `帮我攻克${weakText}，先判断我在这门课哪里卡住再讲解` },
        ...welcomeSuggestions.value.slice(1)
      ]
    } else {
      const weakModules = Array.isArray(goal.weakModulePriority) ? goal.weakModulePriority : []
      const weak = weakModules.find(item => String(item || '').trim())
      if (weak) {
        const weakText = String(weak).trim()
        welcomeSuggestions.value = [
          { text: `帮我攻克${weakText}`, prompt: `帮我攻克${weakText}，先判断我卡在哪里再讲解` },
          ...welcomeSuggestions.value.slice(1)
        ]
      }
    }
  } catch (e) {
    profileHint.value = ''
  }
}

onMounted(() => {
  loadSessions()
  loadProfileHint()
})
</script>

<style scoped>
.chat-page-wrapper {
  height: calc(100vh - 64px);
  margin-top: 64px;
  display: flex;
  flex-direction: column;
}
.chat-layout {
  flex: 1;
  display: grid;
  grid-template-columns: 280px 1fr;
  overflow: hidden;
  transition: grid-template-columns 0.3s ease;
}
.chat-layout.sidebar-collapsed {
  grid-template-columns: 0 1fr;
}
.chat-sidebar-box {
  border-right: 1px solid var(--border);
  background: var(--card-bg-solid);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: width 0.3s ease, min-width 0.3s ease;
}
.chat-sidebar-box.collapsed {
  width: 0;
  min-width: 0;
  border-right: none;
}
.sidebar-top {
  padding: 16px;
  border-bottom: 1px solid var(--border);
}
.sidebar-sessions {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}
.session-item {
  padding: 12px 14px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 4px;
  transition: background 0.2s;
}
.session-item:hover { background: var(--bg-alt); }
.session-item.active { background: var(--accent-light); }
.session-title {
  font-size: 0.85rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.session-time {
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-top: 4px;
}

.chat-main-area {
  display: flex;
  flex-direction: column;
  min-width: 0;
  overflow: hidden;
}
.chat-header-bar {
  padding: 12px 20px;
  border-bottom: 1px solid var(--border);
  display: flex;
  align-items: center;
  gap: 12px;
  background: var(--card-bg-solid);
}
.sidebar-toggle {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 1px solid var(--border);
  background: var(--bg-alt);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.75rem;
  color: var(--text-secondary);
  transition: all 0.2s;
}
.sidebar-toggle:hover {
  border-color: var(--border-hover);
  background: var(--card-bg-solid);
}
.chat-ai-info {
  display: flex;
  align-items: center;
  gap: 10px;
}
.chat-ai-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.82rem;
}
.chat-ai-name {
  font-weight: 600;
  font-size: 0.9rem;
}
.chat-ai-status {
  font-size: 0.72rem;
  color: var(--success);
}
.new-chat-btn {
  margin-left: auto;
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid var(--border);
  background: var(--bg-alt);
  color: var(--text-secondary);
  font-size: 0.78rem;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}
.new-chat-btn:hover {
  border-color: var(--accent);
  background: var(--accent-light);
  color: var(--text);
}
.new-chat-btn i {
  font-size: 0.95rem;
}

.chat-messages-area {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 欢迎界面 */
.chat-welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 60px 24px;
  max-width: 600px;
  margin: auto;
}
.chat-welcome-avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 1.4rem;
  margin-bottom: 20px;
}
.chat-welcome h2 {
  font-size: 1.3rem;
  margin-bottom: 8px;
}
.chat-welcome p {
  color: var(--text-secondary);
  font-size: 0.9rem;
  line-height: 1.6;
  margin-bottom: 12px;
}
.chat-profile-hint {
  margin-bottom: 22px;
  font-size: 0.78rem;
  color: var(--text-secondary);
  background: var(--bg-alt);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 7px 12px;
}
.chat-suggestions {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  width: 100%;
  margin-bottom: 32px;
}
.chat-suggestion-item {
  padding: 12px 16px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--card-bg-solid);
  font-size: 0.82rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  line-height: 1.4;
}
.chat-suggestion-item:hover {
  border-color: var(--accent);
  background: var(--accent-light);
  color: var(--text);
}

/* 消息气泡 */
.chat-bubble-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}
.chat-bubble-row.user {
  justify-content: flex-end;
}
.chat-bubble-row.ai {
  justify-content: flex-start;
}
.chat-bubble-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.7rem;
  flex-shrink: 0;
}
.chat-bubble {
  max-width: 76%;
  padding: 14px 18px;
  border-radius: 16px;
  font-size: 0.9rem;
  line-height: 1.75;
  word-break: break-word;
  min-width: 0;
  overflow-wrap: anywhere;
}
.chat-bubble.user {
  background: #eef6ff;
  border: 1px solid #c7dff7;
  color: #14365d;
  border-bottom-right-radius: 4px;
}
.chat-bubble.ai {
  background: var(--card-bg-solid);
  border: 1px solid var(--border);
  color: var(--text);
  padding: 24px 30px;
  border-bottom-left-radius: 4px;
}

/* AI 回复内容排版 */
.chat-bubble-content {
  white-space: normal;
  font-size: 0.92rem;
  line-height: 2;
  letter-spacing: 0.02em;
  color: inherit;
  font-family: -apple-system, 'PingFang SC', 'Noto Sans SC', 'Microsoft YaHei', sans-serif;
  max-width: 100%;
  min-width: 0;
  overflow-wrap: anywhere;
}
.chat-bubble.ai .chat-bubble-content {
  color: #3b3b3b;
}
.chat-bubble.user .chat-bubble-content strong,
.chat-bubble.user .chat-bubble-content h1,
.chat-bubble.user .chat-bubble-content h2,
.chat-bubble.user .chat-bubble-content h3,
.chat-bubble.user .chat-bubble-content h4,
.chat-bubble.user .chat-bubble-content h5,
.chat-bubble.user .chat-bubble-content h6,
.chat-bubble.user .chat-bubble-content li::marker {
  color: #14365d;
}
.chat-bubble-content > *:first-child {
  margin-top: 0;
}
.chat-bubble-content > *:last-child {
  margin-bottom: 0;
}
.chat-bubble-content p {
  margin: 0 0 1.1em 0;
  line-height: 2;
}
.chat-bubble-content p:last-child {
  margin-bottom: 0;
}
.chat-bubble-content strong {
  font-weight: 700;
  color: #d64b2c;
}

/* 标题 — 藏蓝色，教材感 */
.chat-bubble-content h1 {
  font-size: 1.3em;
  font-weight: 800;
  color: #1a3a6b;
  margin: 1.8em 0 0.9em 0;
  padding: 0.3em 0.6em;
  background: linear-gradient(90deg, #eaf2ff 0%, transparent 100%);
  border-left: 4px solid #2b5ea7;
  border-radius: 0 4px 4px 0;
}
.chat-bubble-content h2 {
  font-size: 1.15em;
  font-weight: 700;
  color: #1e4d8c;
  margin: 1.5em 0 0.75em 0;
  padding-bottom: 0.3em;
  border-bottom: 2px solid #c5d9f0;
}
.chat-bubble-content h3 {
  font-size: 1.05em;
  font-weight: 700;
  color: #2a5a9e;
  margin: 1.3em 0 0.6em 0;
  padding-left: 0.5em;
  border-left: 3px solid #5b8fd4;
}
.chat-bubble-content h4,
.chat-bubble-content h5,
.chat-bubble-content h6 {
  font-size: 0.98em;
  font-weight: 650;
  color: #2f5f9a;
  margin: 1.1em 0 0.5em 0;
}

/* 列表 */
.chat-bubble-content ul,
.chat-bubble-content ol {
  margin: 0.7em 0 1.1em 0;
  padding-left: 1.6em;
}
.chat-bubble-content li {
  margin-bottom: 0.5em;
  line-height: 1.9;
}
.chat-bubble-content li:last-child {
  margin-bottom: 0;
}
.chat-bubble-content li > ul,
.chat-bubble-content li > ol {
  margin: 0.3em 0 0.3em 0;
}
.chat-bubble-content li::marker {
  color: #2b5ea7;
  font-weight: 600;
}

/* 行内代码 */
.chat-bubble-content code {
  background: #f0f5ff;
  color: #1e4d8c;
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.84em;
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  word-break: break-all;
  border: 1px solid #dce8f5;
}

/* 代码块 */
.chat-bubble-content pre {
  background: #1b2a4a;
  color: #e2e8f0;
  padding: 16px 18px;
  border-radius: 8px;
  overflow-x: auto;
  margin: 1em 0;
  font-size: 0.82em;
  line-height: 1.7;
  border: 1px solid #2d4a7a;
  max-width: 100%;
  white-space: pre-wrap;
}
.chat-bubble-content pre code {
  background: none;
  padding: 0;
  color: inherit;
  font-size: inherit;
  word-break: normal;
  border: none;
}

/* 引用 — 教材提示框风格 */
.chat-bubble-content blockquote {
  border-left: 4px solid #2b5ea7;
  margin: 1em 0;
  padding: 0.6em 1em;
  background: #f5f8fd;
  border-radius: 0 6px 6px 0;
  color: #4a5f7a;
}
.chat-bubble-content blockquote p {
  margin: 0.3em 0;
}

/* 分割线 */
.chat-bubble-content hr {
  border: none;
  border-top: 2px solid #e2ecf7;
  margin: 1.5em 0;
}

/* 表格 */
.chat-bubble-content table {
  border-collapse: collapse;
  margin: 1em 0;
  font-size: 0.85em;
  width: 100%;
  overflow-x: auto;
  display: block;
}
.chat-bubble-content thead {
  background: #eaf2ff;
}
.chat-bubble-content th {
  border: 1px solid #c5d9f0;
  padding: 9px 14px;
  font-weight: 700;
  text-align: left;
  color: #1a3a6b;
}
.chat-bubble-content td {
  border: 1px solid #dce8f5;
  padding: 8px 14px;
  color: #3b3b3b;
}
.chat-bubble-content tr:nth-child(even) {
  background: #f8fbff;
}

/* LaTeX 公式间距 */
.chat-bubble-content .katex-display {
  margin: 1em 0;
  overflow-x: auto;
  overflow-y: hidden;
  padding: 8px 2px;
  max-width: 100%;
}
.chat-bubble-content .katex {
  max-width: 100%;
  white-space: normal;
}

/* 流式输出闪烁光标 */
.streaming-cursor {
  display: inline-block;
  width: 2px;
  height: 1em;
  background: var(--accent, #667eea);
  margin-left: 2px;
  vertical-align: text-bottom;
  animation: cursorBlink 0.8s step-end infinite;
}

/* 学习资源卡片 */
.chat-resources {
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.chat-resources-title {
  font-size: 0.78rem;
  font-weight: 700;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
}
.chat-resource-card {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-alt);
  text-decoration: none;
  color: inherit;
  transition: all 0.2s;
}
.chat-resource-card:hover {
  border-color: var(--accent);
  background: var(--accent-light);
  transform: translateY(-1px);
}
.chat-resource-icon {
  font-size: 1.6rem;
  color: #fb7299;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}
.chat-resource-info {
  flex: 1;
  min-width: 0;
}
.chat-resource-name {
  font-size: 0.85rem;
  font-weight: 600;
  color: var(--text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chat-resource-meta {
  margin-top: 4px;
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 0.72rem;
  color: var(--text-muted);
}
.chat-resource-meta span {
  display: inline-flex;
  align-items: center;
  gap: 3px;
}
.chat-resource-badge {
  color: var(--success);
  font-weight: 600;
}
.chat-resource-open {
  color: var(--text-muted);
  flex-shrink: 0;
}
@keyframes cursorBlink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 输入区域 */
.chat-bottom {
  padding: 16px 24px;
  border-top: 1px solid var(--border);
  background: var(--card-bg-solid);
  display: flex;
  flex-direction: column;
}
.chat-input-box {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: var(--bg-alt);
  border: 1px solid var(--border);
  border-radius: 12px;
  padding: 8px 12px;
  transition: border-color 0.2s;
}
.chat-input-box:focus-within {
  border-color: var(--accent);
}
.chat-textarea {
  flex: 1;
  border: none;
  background: transparent;
  font-size: 0.9rem;
  line-height: 1.5;
  resize: none;
  outline: none;
  max-height: 120px;
  font-family: inherit;
  color: var(--text);
}
.chat-textarea::placeholder {
  color: var(--text-muted);
}
.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: var(--text);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
}
.send-btn:hover:not(:disabled) {
  opacity: 0.85;
  transform: scale(1.05);
}
.send-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}
.stop-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: #ef4444;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: all 0.2s;
  animation: stopPulse 2s ease-in-out infinite;
}
.stop-btn:hover {
  background: #dc2626;
  transform: scale(1.05);
}
@keyframes stopPulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(239, 68, 68, 0.4); }
  50% { box-shadow: 0 0 0 6px rgba(239, 68, 68, 0); }
}
.chat-hint-text {
  text-align: center;
  font-size: 0.7rem;
  color: var(--text-muted);
  margin-top: 8px;
}
.chat-action-card {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  padding: 12px 14px;
  border: 1px solid var(--border);
  border-radius: 10px;
  background: var(--bg-alt);
}
.chat-action-icon {
  width: 34px;
  height: 34px;
  border-radius: 9px;
  background: var(--accent-light);
  color: var(--accent);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 1rem;
}
.chat-action-copy {
  flex: 1;
  min-width: 0;
}
.chat-action-title {
  font-size: 0.86rem;
  font-weight: 700;
  color: var(--text);
}
.chat-action-reason {
  margin-top: 2px;
  font-size: 0.74rem;
  color: var(--text-muted);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.chat-action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  border: none;
  border-radius: 8px;
  background: var(--text);
  color: #fff;
  padding: 8px 12px;
  font-size: 0.78rem;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}
.chat-action-btn:hover {
  opacity: 0.88;
}

/* 打字指示器 */
.typing-dots {
  display: flex;
  gap: 4px;
  padding: 12px 16px;
  background: var(--card-bg-solid);
  border: 1px solid var(--border);
  border-radius: 16px;
  border-bottom-left-radius: 4px;
}
.typing-dots span {
  width: 6px;
  height: 6px;
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

@media (max-width: 768px) {
  .chat-layout { grid-template-columns: 1fr; }
  .chat-sidebar-box { position: fixed; left: 0; top: 64px; bottom: 0; width: 280px; z-index: 50; }
  .chat-sidebar-box.collapsed { transform: translateX(-100%); }
  .chat-suggestions { grid-template-columns: 1fr; }
  .chat-action-card { align-items: flex-start; flex-wrap: wrap; }
  .chat-action-btn { width: 100%; justify-content: center; }
}
</style>
