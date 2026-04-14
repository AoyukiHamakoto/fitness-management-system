<template>
  <div class="ai-chat-page">
    <el-container class="chat-layout">
      <!-- 左侧历史 -->
      <el-aside width="260px" class="aside">
        <div class="aside-head">
          <span class="aside-title">历史对话</span>
          <el-button type="primary" size="small" @click="startNewSession">
            <el-icon><Plus /></el-icon>
            新对话
          </el-button>
        </div>
        <el-scrollbar class="session-scroll">
          <div
            v-for="s in sessionsSorted"
            :key="s.id"
            class="session-item"
            :class="{ active: s.id === activeSessionId }"
            @click="selectSession(s.id)"
          >
            <div class="session-title">{{ s.title }}</div>
            <div class="session-time">{{ formatSessionTime(s.updatedAt) }}</div>
          </div>
          <el-empty v-if="sessionsSorted.length === 0" description="暂无记录" :image-size="64" />
        </el-scrollbar>
      </el-aside>

      <!-- 右侧主区 -->
      <el-container direction="vertical" class="main-wrap">
        <el-header class="main-header" height="56px">
          <span class="header-text">AI 健身助手</span>
          <el-tag v-if="streaming" type="warning" effect="plain" size="small">正在生成…</el-tag>
        </el-header>

        <el-main class="main-body">
          <el-scrollbar ref="scrollRef" class="msg-scroll">
            <div v-loading="initialLoading" class="msg-inner">
              <template v-if="sortedMessages.length">
                <div
                  v-for="msg in sortedMessages"
                  :key="msg.localId"
                  class="msg-row"
                  :class="msg.role"
                >
                  <div class="bubble">
                    <div class="role-label">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
                    <div v-if="msg.role === 'user'" class="text user-text">{{ msg.content }}</div>
                    <div
                      v-else
                      class="text md-body"
                      v-html="renderAssistant(msg)"
                    />
                    <div v-if="msg.role === 'assistant' && msg.streaming" class="typing">
                      <span /><span /><span />
                    </div>
                  </div>
                  <div class="msg-time">{{ formatMsgTime(msg.ts) }}</div>
                </div>
              </template>
              <el-empty v-else description="开始对话吧，或从左侧选择历史会话" />
            </div>
          </el-scrollbar>
        </el-main>

        <el-footer class="footer" height="auto">
          <div class="input-wrap">
            <el-input
              v-model="inputText"
              type="textarea"
              :rows="3"
              :maxlength="8000"
              show-word-limit
              placeholder="输入问题，支持多行（Shift+Enter 换行）"
              :disabled="streaming"
              @keydown.enter.exact.prevent="onEnterSend"
            />
            <div class="footer-actions">
              <span class="hint">Enter 发送 · 频率限制：每分钟最多 10 次</span>
              <el-button
                type="primary"
                :loading="streaming"
                :disabled="!canSend"
                @click="sendMessage"
              >
                发送
              </el-button>
            </div>
          </div>
        </el-footer>
      </el-container>
    </el-container>
  </div>
</template>

<script setup>
import { ref, reactive, computed, watch, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import { useUserStore } from '@/stores/user'

const API_BASE = 'http://localhost:8080/fitness-api'

const userStore = useUserStore()
const md = new MarkdownIt({
  html: false,
  linkify: true,
  breaks: true,
})

const scrollRef = ref(null)
const inputText = ref('')
const streaming = ref(false)
const initialLoading = ref(false)
const activeSessionId = ref('')
let localIdSeq = 0
function nextLocalId() {
  localIdSeq += 1
  return `m-${Date.now()}-${localIdSeq}`
}

/** 当前会话消息（按时间正序存储，展示时再倒序） */
const messages = ref([])

const sessions = reactive([])
const sessionsSorted = computed(() =>
  [...sessions].sort((a, b) => (b.updatedAt || 0) - (a.updatedAt || 0)),
)

const sortedMessages = computed(() =>
  [...messages.value].sort((a, b) => (b.ts || 0) - (a.ts || 0)),
)

const canSend = computed(
  () => !streaming.value && inputText.value.trim().length > 0,
)

function storageKey() {
  const uid = userStore.userInfo?.id || 'guest'
  return `fitness_ai_chat_${uid}`
}

function loadStore() {
  try {
    const raw = localStorage.getItem(storageKey())
    if (!raw) return
    const data = JSON.parse(raw)
    sessions.splice(0, sessions.length, ...(data.sessions || []))
    if (data.activeSessionId) {
      activeSessionId.value = data.activeSessionId
    }
    if (activeSessionId.value && data.messages?.[activeSessionId.value]) {
      messages.value = data.messages[activeSessionId.value]
    } else {
      messages.value = []
    }
  } catch {
    sessions.splice(0, sessions.length)
    messages.value = []
  }
}

function persistStore() {
  if (!activeSessionId.value) return
  const msgMap = {}
  msgMap[activeSessionId.value] = messages.value.map((m) => ({ ...m }))
  try {
    const raw = localStorage.getItem(storageKey())
    const data = raw ? JSON.parse(raw) : { sessions: [], messages: {} }
    data.sessions = [...sessions]
    data.activeSessionId = activeSessionId.value
    data.messages = { ...data.messages, ...msgMap }
    localStorage.setItem(storageKey(), JSON.stringify(data))
  } catch {
    /* ignore */
  }
}

function ensureSession(id) {
  let s = sessions.find((x) => x.id === id)
  if (!s) {
    s = { id, title: '新对话', updatedAt: Date.now() }
    sessions.push(s)
  }
  return s
}

function startNewSession(notify = true) {
  persistStore()
  const id = crypto.randomUUID()
  activeSessionId.value = id
  messages.value = []
  ensureSession(id)
  persistStore()
  if (notify) {
    ElMessage.success('已开始新对话')
  }
}

function selectSession(id) {
  if (id === activeSessionId.value) return
  persistStore()
  activeSessionId.value = id
  try {
    const raw = localStorage.getItem(storageKey())
    const data = raw ? JSON.parse(raw) : {}
    messages.value = data.messages?.[id] ? [...data.messages[id]] : []
  } catch {
    messages.value = []
  }
}

function formatSessionTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getMonth() + 1}/${d.getDate()} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function formatMsgTime(ts) {
  if (!ts) return ''
  const d = new Date(ts)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function renderAssistant(msg) {
  if (!msg.content) return ''
  try {
    return md.render(msg.content)
  } catch {
    return md.utils.escapeHtml(msg.content)
  }
}

function onEnterSend() {
  sendMessage()
}

async function sendMessage() {
  const text = inputText.value.trim()
  if (!text || streaming.value) return
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  if (!activeSessionId.value) {
    startNewSession(false)
  }

  const sid = activeSessionId.value
  const sess = ensureSession(sid)
  const now = Date.now()

  messages.value.push({
    localId: nextLocalId(),
    role: 'user',
    content: text,
    ts: now,
  })
  if (sess.title === '新对话' || sess.title.length < 2) {
    sess.title = text.slice(0, 24) + (text.length > 24 ? '…' : '')
  }
  sess.updatedAt = now
  const assistantMsg = reactive({
    localId: nextLocalId(),
    role: 'assistant',
    content: '',
    ts: now + 1,
    streaming: true,
  })
  messages.value.push(assistantMsg)

  inputText.value = ''
  streaming.value = true
  await nextTick()
  scrollToTop()

  let sseBuffer = ''

  try {
    const res = await fetch(`${API_BASE}/api/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
        Authorization: `Bearer ${userStore.token}`,
      },
      body: JSON.stringify({
        sessionId: sid,
        message: text,
      }),
    })

    const ct = res.headers.get('content-type') || ''
    if (!res.ok) {
      const errText = await res.text()
      let msg = `请求失败 (${res.status})`
      try {
        const j = JSON.parse(errText)
        if (j.msg) msg = j.msg
      } catch {
        if (errText) msg = errText.slice(0, 200)
      }
      throw new Error(msg)
    }
    if (ct.includes('application/json')) {
      const j = await res.json()
      throw new Error(j.msg || '无法建立流式连接')
    }

    const reader = res.body.getReader()
    const decoder = new TextDecoder()
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      sseBuffer += decoder.decode(value, { stream: true })
      const parts = sseBuffer.split('\n\n')
      sseBuffer = parts.pop() || ''
      for (const part of parts) {
        if (!part.trim()) continue
        let eventName = 'message'
        let data = ''
        for (const line of part.split('\n')) {
          if (line.startsWith('event:')) {
            eventName = line.slice(6).trim()
          } else if (line.startsWith('data:')) {
            data += line.slice(5).trim()
          }
        }
        handleSseEvent(eventName, data, assistantMsg)
      }
    }
    assistantMsg.streaming = false
    sess.updatedAt = Date.now()
    persistStore()
  } catch (e) {
    assistantMsg.streaming = false
    const msg = e?.message || '对话失败'
    if (/频繁|限流|10次/.test(msg)) {
      ElMessage.warning(msg)
    } else {
      ElMessage.error(msg)
    }
    if (!assistantMsg.content) {
      assistantMsg.content = '（生成失败，请稍后重试）'
    }
    persistStore()
  } finally {
    streaming.value = false
    await nextTick()
    scrollToTop()
  }
}

function handleSseEvent(event, data, assistantMsg) {
  if (event === 'token' && data) {
    assistantMsg.content += data
  } else if (event === 'error') {
    ElMessage.error(data || '流式输出错误')
  } else if (event === 'done') {
    /* no-op */
  }
}

function scrollToTop() {
  const wrap = scrollRef.value?.wrapRef
  if (wrap) {
    wrap.scrollTop = 0
  }
}

watch(sortedMessages, () => nextTick(() => scrollToTop()))

onMounted(() => {
  initialLoading.value = true
  try {
    loadStore()
    if (!activeSessionId.value) {
      startNewSession(false)
    } else {
      ensureSession(activeSessionId.value)
    }
  } finally {
    initialLoading.value = false
  }
})
</script>

<style scoped lang="scss">
.ai-chat-page {
  height: calc(100vh - 0px);
  min-height: 520px;
  background: #f1f5f9;
}

.chat-layout {
  height: 100%;
  border-radius: 0;
}

.aside {
  background: #fff;
  border-right: 1px solid #e2e8f0;
  display: flex;
  flex-direction: column;
}

.aside-head {
  padding: 14px 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  border-bottom: 1px solid #e2e8f0;
}

.aside-title {
  font-weight: 600;
  color: #0f172a;
}

.session-scroll {
  flex: 1;
  padding: 8px;
}

.session-item {
  padding: 10px 12px;
  border-radius: 10px;
  cursor: pointer;
  margin-bottom: 6px;
  transition: background 0.15s;

  &:hover {
    background: #f8fafc;
  }

  &.active {
    background: #eff6ff;
    border: 1px solid #bfdbfe;
  }

  .session-title {
    font-size: 14px;
    color: #1e293b;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .session-time {
    font-size: 12px;
    color: #94a3b8;
    margin-top: 4px;
  }
}

.main-wrap {
  background: #f8fafc;
}

.main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background: #fff;
  border-bottom: 1px solid #e2e8f0;
}

.header-text {
  font-weight: 600;
  font-size: 16px;
  color: #0f172a;
}

.main-body {
  padding: 0;
  overflow: hidden;
}

.msg-scroll {
  height: 100%;
  padding: 16px 20px;
}

.msg-inner {
  min-height: 200px;
  max-width: 880px;
  margin: 0 auto;
}

.msg-row {
  display: flex;
  flex-direction: column;
  margin-bottom: 20px;

  &.user {
    align-items: flex-end;
  }

  &.assistant {
    align-items: flex-start;
  }

  &.user .bubble {
    background: #e0f2fe;
    border: 1px solid #bae6fd;
    margin-left: auto;
    max-width: 85%;
  }

  &.assistant .bubble {
    background: #fff;
    border: 1px solid #e2e8f0;
    margin-right: auto;
    max-width: 92%;
  }
}

.bubble {
  border-radius: 14px;
  padding: 12px 14px;
  box-shadow: 0 1px 2px rgba(15, 23, 42, 0.06);
}

.role-label {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 6px;
}

.user-text {
  white-space: pre-wrap;
  word-break: break-word;
  color: #0f172a;
  line-height: 1.6;
}

.md-body {
  color: #1e293b;
  line-height: 1.65;
  word-break: break-word;

  :deep(h1),
  :deep(h2),
  :deep(h3) {
    margin: 0.6em 0 0.35em;
    font-size: 1.05em;
  }

  :deep(p) {
    margin: 0.45em 0;
  }

  :deep(ul),
  :deep(ol) {
    padding-left: 1.25em;
    margin: 0.4em 0;
  }

  :deep(code) {
    background: #f1f5f9;
    padding: 2px 6px;
    border-radius: 4px;
    font-size: 0.9em;
  }

  :deep(pre) {
    background: #1e293b;
    color: #e2e8f0;
    padding: 12px;
    border-radius: 8px;
    overflow-x: auto;
  }

  :deep(a) {
    color: #2563eb;
  }
}

.msg-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 6px;
  text-align: right;
}

.msg-row.assistant .msg-time {
  text-align: left;
}

.msg-row.user .msg-time {
  text-align: right;
}

.typing {
  display: inline-flex;
  gap: 4px;
  margin-top: 8px;

  span {
    width: 6px;
    height: 6px;
    border-radius: 50%;
    background: #94a3b8;
    animation: blink 1s infinite ease-in-out;

    &:nth-child(2) {
      animation-delay: 0.15s;
    }

    &:nth-child(3) {
      animation-delay: 0.3s;
    }
  }
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.25;
  }
  40% {
    opacity: 1;
  }
}

.footer {
  background: #fff;
  border-top: 1px solid #e2e8f0;
  padding: 12px 20px 16px;
}

.input-wrap {
  max-width: 880px;
  margin: 0 auto;
}

.footer-actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 10px;
  flex-wrap: wrap;
  gap: 8px;
}

.hint {
  font-size: 12px;
  color: #94a3b8;
}

@media (max-width: 768px) {
  .aside {
    width: 200px !important;
  }
}
</style>
