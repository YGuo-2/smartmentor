import { ref, onMounted, nextTick, watch } from 'vue'
import { api, streamSse } from '../api.js'
import { renderLatex, showToast } from '../state.js'

export const ChatPage = {
  name: 'ChatPage',
  setup() {
    const sessions = ref([])
    const currentSessionId = ref(null)
    const messages = ref([])
    const inputText = ref('')
    const isStreaming = ref(false)
    const sidebarVisible = ref(true)
    const messagesContainer = ref(null)

    const loadSessions = async () => {
      try {
        const res = await api.chat.history({ page: 0, size: 20 })
        sessions.value = res.sessions || []
      } catch (e) {
        showToast('加载会话列表失败', 'error')
      }
    }

    const loadSessionMessages = async (sessionId) => {
      currentSessionId.value = sessionId
      try {
        const res = await api.chat.history({ sessionId })
        messages.value = (res.messages || []).map(m => ({
          role: m.role,
          content: m.content,
          createdAt: m.createdAt
        }))
        await nextTick()
        scrollToBottom()
        renderAllLatex()
      } catch (e) {
        showToast('加载聊天记录失败', 'error')
      }
    }

    const startNewSession = () => {
      currentSessionId.value = null
      messages.value = []
      inputText.value = ''
    }

    const scrollToBottom = () => {
      if (messagesContainer.value) {
        messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
      }
    }

    const renderAllLatex = () => {
      nextTick(() => {
        renderLatex()
      })
    }

    const sendMessage = async () => {
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

      isStreaming.value = true

      const aiMessage = {
        role: 'assistant',
        content: '',
        createdAt: new Date().toISOString()
      }
      messages.value.push(aiMessage)

      try {
        for await (const evt of streamSse('/chat/stream', {
          params: {
            message,
            sessionId: currentSessionId.value || ''
          }
        })) {
          if (evt.type === 'metadata' && evt.data.sessionId) {
            currentSessionId.value = evt.data.sessionId
          } else if (evt.type === 'message') {
            aiMessage.content += evt.data.content || ''
            const idx = messages.value.length - 1
            messages.value[idx] = { ...aiMessage }
            await nextTick()
            scrollToBottom()
          } else if (evt.type === 'done') {
            loadSessions()
            break
          } else if (evt.type === 'error') {
            showToast(evt.data.message || evt.data.error || 'AI 响应出错', 'error')
            break
          }
        }

        isStreaming.value = false
        await nextTick()
        scrollToBottom()
        renderAllLatex()
      } catch (e) {
        isStreaming.value = false
        showToast(e.message || '发送消息失败', 'error')
      }
    }

    const handleKeydown = (e) => {
      if (e.key === 'Enter' && !e.shiftKey) {
        e.preventDefault()
        sendMessage()
      }
    }

    const toggleSidebar = () => {
      sidebarVisible.value = !sidebarVisible.value
    }

    const formatDate = (dateStr) => {
      if (!dateStr) return ''
      const d = new Date(dateStr)
      const month = (d.getMonth() + 1).toString().padStart(2, '0')
      const day = d.getDate().toString().padStart(2, '0')
      const hour = d.getHours().toString().padStart(2, '0')
      const min = d.getMinutes().toString().padStart(2, '0')
      return `${month}-${day} ${hour}:${min}`
    }

    onMounted(() => {
      loadSessions()
    })

    return {
      sessions,
      currentSessionId,
      messages,
      inputText,
      isStreaming,
      sidebarVisible,
      messagesContainer,
      loadSessionMessages,
      startNewSession,
      sendMessage,
      handleKeydown,
      toggleSidebar,
      formatDate
    }
  },
  template: `
    <div class="chat-page">
      <nav class="topnav">
        <div class="topnav-brand">SmartMentor</div>
        <button class="btn btn-outline sidebar-toggle-btn" @click="toggleSidebar">
          {{ sidebarVisible ? '隐藏侧栏' : '显示侧栏' }}
        </button>
      </nav>

      <div class="chat-body">
        <aside class="chat-sidebar" :class="{ hidden: !sidebarVisible }">
          <button class="btn btn-dark new-chat-btn" @click="startNewSession">+ 新对话</button>
          <div class="chat-session-list">
            <div
              v-for="session in sessions"
              :key="session.sessionId"
              class="chat-session-item"
              :class="{ active: session.sessionId === currentSessionId }"
              @click="loadSessionMessages(session.sessionId)"
            >
              <div class="chat-session-title">{{ session.title || '未命名对话' }}</div>
              <div class="chat-session-date">{{ formatDate(session.updatedAt) }}</div>
            </div>
          </div>
        </aside>

        <main class="chat-main">
          <div class="chat-header">
            <h2>AI 课程伴学</h2>
            <span v-if="currentSessionId" class="chat-session-info">会话 ID: {{ currentSessionId }}</span>
          </div>

          <div class="chat-messages" ref="messagesContainer">
            <div
              v-for="(msg, index) in messages"
              :key="index"
              class="chat-msg"
              :class="msg.role === 'user' ? 'user' : 'bot'"
            >
              <div class="chat-msg-content">{{ msg.content }}</div>
            </div>
            <div v-if="isStreaming" class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>

          <div class="chat-input-area">
            <textarea
              class="chat-input"
              v-model="inputText"
              @keydown="handleKeydown"
              placeholder="输入课程问题... (Enter 发送, Shift+Enter 换行)"
              :disabled="isStreaming"
              rows="3"
            ></textarea>
            <button
              class="btn btn-dark chat-send-btn"
              @click="sendMessage"
              :disabled="isStreaming || !inputText.trim()"
            >发送</button>
          </div>
        </main>
      </div>
    </div>
  `
}
