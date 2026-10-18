<template>
  <div class="ai-chat">
    <!-- 悬浮入口按钮 -->
    <div v-if="!open" class="ai-fab" @click="open = true">
      <span class="ai-fab-icon">🐾</span>
      <span class="ai-fab-text">AI 导购</span>
    </div>

    <!-- 对话面板：左侧会话列表 + 右侧对话区 -->
    <div v-else :class="['ai-panel', { 'with-sidebar': showSessions }]">
      <!-- 左侧会话列表（可上下滑动） -->
      <div v-if="showSessions" class="ai-sidebar">
        <div class="ai-sidebar-head">
          <span>会话</span>
          <span class="ai-sidebar-collapse" @click="showSessions = false">«</span>
        </div>
        <div class="ai-sidebar-list">
          <div
            v-for="s in sessionList"
            :key="s.session_id"
            :class="['ai-session-item', { active: s.session_id === sessionId }]"
            @click="switchSession(s.session_id)"
          >
            <span class="ai-session-icon">🐾</span>
            <div class="ai-session-body">
              <div class="ai-session-title">{{ s.title || '新会话' }}</div>
              <div class="ai-session-time">{{ fmtTime(s.updated_at) }}</div>
            </div>
            <span class="ai-session-del" @click.stop="deleteSession(s)">🗑</span>
          </div>
          <div v-if="!sessionList.length" class="ai-empty">还没有历史会话</div>
        </div>
      </div>

      <div class="ai-header">
        <span class="ai-list-btn" @click="toggleSessions">📋</span>
        <span>🐾 智能导购助手</span>
        <span class="ai-header-actions">
          <span class="ai-new" @click="newSession">＋ 新会话</span>
          <span class="ai-close" @click="open = false">×</span>
        </span>
      </div>

      <div class="ai-messages" ref="messages">
        <div v-for="(m, i) in messages" :key="i" :class="['ai-msg', 'ai-msg-' + m.role]">
          <div v-if="m.tool" class="ai-tool">
            <span class="ai-tool-dot"></span>{{ m.tool }}
          </div>
          <template v-else>
            <div v-if="m.content" class="ai-bubble" v-html="fmt(m.content)"></div>
            <div v-if="m.memory" class="ai-memory">🧠 {{ m.memory }}</div>
            <div v-if="m.products && m.products.length" class="ai-cards">
              <div
                v-for="p in m.products"
                :key="p.id"
                class="ai-card"
                @click="goProduct(p.id)"
                :title="'查看 ' + p.name"
              >
                <img class="ai-card-img" :src="p.image" alt="" @error="onImgError"/>
                <div class="ai-card-info">
                  <div class="ai-card-name">{{ p.name }}</div>
                  <div class="ai-card-meta">
                    <span class="ai-card-price">¥{{ p.price }}</span>
                    <span class="ai-card-sales">已售{{ p.sales || 0 }}</span>
                  </div>
                </div>
                <span class="ai-card-go">去看看 ›</span>
              </div>
              <!-- 对比卡片：只有两款商品时渲染为对比布局 -->
              <div v-if="m.isComparison" class="ai-compare">
                <div class="ai-compare-header">
                  <span class="ai-compare-badge">对比</span>
                  <span>以上两款商品对比如下：</span>
                </div>
              </div>
            </div>
          </template>
        </div>
        <div v-if="sending" class="ai-msg ai-msg-assistant">
          <div class="ai-bubble ai-typing">正在思考<span class="dot1">.</span><span class="dot2">.</span><span class="dot3">.</span></div>
        </div>
      </div>

      <!-- 未发送时的快捷问题 -->
      <div v-if="messages.length <= 1" class="ai-suggest">
        <span @click="ask('有什么热门商品推荐吗？')">🔥 热门推荐</span>
        <span @click="ask('我想买狗粮，有什么好的？')">🐶 买狗粮</span>
        <span v-if="isLogin" @click="ask('我的订单发货了吗？')">📦 查订单</span>
      </div>

      <div class="ai-input">
        <input
          v-model="input"
          placeholder="问问我，比如：猫砂哪款好？"
          @keyup.enter="send"
          :disabled="sending"
        />
        <button @click="send" :disabled="sending || !input.trim()">发送</button>
      </div>
    </div>
  </div>
</template>

<script>
// 欢迎语用工厂函数：parseSSE 会对最后一条消息 $set，共享对象引用会被改坏
const greeting = () => ({ role: 'assistant', content: '您好！我是宠旅驿站 AI 导购，可以帮您找商品、查订单，试试下面的问题吧～' })

export default {
  name: 'AiChat',
  data() {
    // 会话 id 按登录账号隔离：换账号/退出后重登不复用上一账号的服务端会话历史
    const token = this.$store.state.user.token || ''
    let sessionId = localStorage.getItem('ai_session_id') || ''
    if ((localStorage.getItem('ai_session_token') || '') !== token) {
      sessionId = ''
      localStorage.removeItem('ai_session_id')
      localStorage.removeItem('ai_session_token')
    }
    return {
      open: false,
      input: '',
      messages: [greeting()],
      sending: false,
      sessionId,
      controller: null,
      showSessions: false,   // 左侧会话列表开关
      sessionList: []
    }
  },
  computed: {
    isLogin() {
      return !!this.$store.state.user.token
    }
  },
  created() {
    // 刷新/重开后从 ai-service 回放本会话的可见历史（服务端已过滤内部附注）
    if (this.sessionId) this.loadHistory()
  },
  methods: {
    async loadHistory() {
      try {
        const resp = await fetch(`/ai/api/chat/history?session_id=${encodeURIComponent(this.sessionId)}`)
        const data = await resp.json()
        if (data.code === 200 && Array.isArray(data.data) && data.data.length) {
          this.messages = data.data.map(m => ({ role: m.role, content: m.content }))
          this.scrollBottom()
        }
      } catch (e) { /* 回放失败不影响新对话 */ }
    },
    // 新建会话：掐断在跑的流 + 清本地 id + 回到欢迎语（左栏保持展开）
    newSession() {
      if (this.controller) this.controller.abort()
      this.sessionId = ''
      localStorage.removeItem('ai_session_id')
      localStorage.removeItem('ai_session_token')
      this.input = ''
      this.messages = [greeting()]
    },
    // 左栏开合 + 拉列表
    async toggleSessions() {
      this.showSessions = !this.showSessions
      if (this.showSessions) await this.loadSessions()
    },
    async loadSessions() {
      try {
        const resp = await fetch('/ai/api/sessions', {
          headers: { Authorization: 'Bearer ' + (this.$store.state.user.token || '') }
        })
        const data = await resp.json()
        this.sessionList = (data.code === 200 && data.data) || []
      } catch (e) { this.sessionList = [] }
    },
    // 切回旧会话
    async switchSession(sid) {
      if (this.controller) this.controller.abort()
      this.sessionId = sid
      localStorage.setItem('ai_session_id', sid)
      localStorage.setItem('ai_session_token', this.$store.state.user.token || '')
      this.sending = false
      this.messages = []
      await this.loadHistory()
      if (!this.messages.length) this.messages = [greeting()]
    },
    // 物理删除（二次确认）
    deleteSession(s) {
      const done = async () => {
        try {
          const resp = await fetch('/ai/api/sessions/' + encodeURIComponent(s.session_id), {
            method: 'DELETE',
            headers: { Authorization: 'Bearer ' + (this.$store.state.user.token || '') }
          })
          const data = await resp.json()
          if (data.code === 200) {
            this.sessionList = this.sessionList.filter(x => x.session_id !== s.session_id)
            if (s.session_id === this.sessionId) this.newSession()
          } else {
            this.$message.error(data.message || '删除失败')
          }
        } catch (e) {
          this.$message.error('删除失败，请稍后再试')
        }
      }
      if (this.$confirm) {
        this.$confirm(`确定删除会话「${s.title || '新会话'}」吗？删除后不可恢复。`, '删除确认', {
          confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
        }).then(done).catch(() => {})
      } else if (window.confirm('确定删除该会话？删除后不可恢复。')) {
        done()
      }
    },
    // unix 秒 → MM-DD HH:mm（左栏时间戳）
    fmtTime(ts) {
      const d = new Date((ts || 0) * 1000)
      const p = n => (n < 10 ? '0' : '') + n
      return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
    },
    // 极简 markdown：先转义防 XSS，再把 **加粗** 渲染成 <strong>；换行靠 CSS white-space
    fmt(text) {
      const esc = String(text).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      return esc.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    },
    goProduct(id) {
      this.$router.push(`/product/${id}`)
    },
    onImgError(e) {
      e.target.style.visibility = 'hidden'
    },
    ask(text) {
      this.input = text
      this.send()
    },
    scrollBottom() {
      this.$nextTick(() => {
        const el = this.$refs.messages
        el.scrollTop = el.scrollHeight
      })
    },
    async send() {
      const text = this.input.trim()
      if (!text || this.sending) return
      this.input = ''
      this.messages.push({ role: 'user', content: text })
      this.messages.push({ role: 'assistant', tool: '', pending: true })
      this.sending = true
      this.scrollBottom()

      this.controller = new AbortController()
      try {
        const resp = await fetch('/ai/api/chat/stream', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            message: text,
            session_id: this.sessionId,
            token: this.$store.state.user.token || ''
          }),
          signal: this.controller.signal
        })
        await this.parseSSE(resp.body)
      } catch (e) {
        if (e.name !== 'AbortError') {
          this.fillLast('AI 服务连接失败，请确认 ai-service 已启动（端口 8082）。')
        }
      } finally {
        this.sending = false
        // 新会话首条消息才在后端产生，且当前会话标题会变 → 左栏开着就刷新
        if (this.showSessions) this.loadSessions()
      }
    },
    // 手写 SSE 解析：事件以空行分隔，data 跨行用 \n 拼接
    async parseSSE(stream) {
      const reader = stream.getReader()
      const decoder = new TextDecoder('utf-8')
      let buffer = ''
      let answer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        // sse_starlette 按 SSE 规范用 \r\n 分隔，统一成 \n 才能匹配下方的 '\n\n'
        buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')

        let idx
        while ((idx = buffer.indexOf('\n\n')) !== -1) {
          const raw = buffer.slice(0, idx)
          buffer = buffer.slice(idx + 2)

          let event = 'message'
          const dataLines = []
          raw.split('\n').forEach(line => {
            if (line.startsWith('event:')) event = line.slice(6).trim()
            else if (line.startsWith('data:')) dataLines.push(line.slice(5).replace(/^\s/, ''))
          })
          const data = dataLines.join('\n')

          if (event === 'session') {
            this.sessionId = data
            localStorage.setItem('ai_session_id', data)
            localStorage.setItem('ai_session_token', this.$store.state.user.token || '')
          } else if (event === 'tool') {
            this.$set(this.messages[this.messages.length - 1], 'tool', data)
            this.scrollBottom()
          } else if (event === 'products') {
            try {
              const last = this.messages[this.messages.length - 1]
              this.$set(last, 'products', JSON.parse(data))
            } catch (e) { /* 卡片数据异常不影响文字回答 */ }
          } else if (event === 'memory') {
            this.$set(this.messages[this.messages.length - 1], 'memory', data)
          } else if (event === 'token') {
            answer += data
            // 首个文字到达后把工具状态条换成气泡
            const last = this.messages[this.messages.length - 1]
            if (last.tool) this.$set(last, 'tool', '')
            this.$set(last, 'content', answer)
            this.scrollBottom()
          } else if (event === 'error') {
            this.fillLast(data)
          }
        }
      }
      // 兜底：一条文字都没收到
      if (!answer && this.messages[this.messages.length - 1].tool) {
        this.fillLast('（没有收到回复，请重试）')
      }
    },
    fillLast(content) {
      const last = this.messages[this.messages.length - 1]
      this.$set(last, 'tool', '')
      this.$set(last, 'content', content)
      this.scrollBottom()
    }
  },
  beforeDestroy() {
    if (this.controller) this.controller.abort()
  }
}
</script>

<style scoped>
.ai-fab {
  position: fixed;
  top: 82px;
  right: 16px;
  z-index: 9999;
  background: linear-gradient(135deg, #ff9a56, #ff6a3d);
  color: #fff;
  border-radius: 24px;
  padding: 12px 20px;
  cursor: pointer;
  box-shadow: 0 6px 20px rgba(255, 106, 61, 0.4);
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 15px;
  transition: transform 0.2s;
}
.ai-fab:hover { transform: translateY(-2px); }
.ai-fab-icon { font-size: 18px; }

.ai-panel {
  position: fixed;
  top: 80px;
  right: 16px;
  bottom: 16px;
  z-index: 9999;
  width: 20vw;
  min-width: 380px;
  max-width: 560px;
  background: #fff;
  border-radius: 14px;
  box-shadow: 0 10px 40px rgba(0, 0, 0, 0.18);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
/* 展开左栏：整体加宽 200px，聊天列靠 padding-left 让位（右锚定 → 向左生长） */
.ai-panel.with-sidebar {
  width: calc(20vw + 200px);
  min-width: 580px;
  max-width: 760px;
  padding-left: 200px;
}
/* 左侧会话列表：绝对定位贴面板左缘 */
.ai-sidebar {
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 200px;
  background: #fafafa;
  border-right: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
}
.ai-sidebar-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 12px;
  font-size: 14px;
  font-weight: 600;
  border-bottom: 1px solid #f0f0f0;
}
.ai-sidebar-collapse { cursor: pointer; }
.ai-sidebar-list { flex: 1; overflow-y: auto; padding: 6px; }
.ai-session-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  cursor: pointer;
}
.ai-session-item:hover { background: #f0f0f0; }
.ai-session-item.active { background: #fff3ec; }
.ai-session-item.active .ai-session-title { font-weight: 600; color: #ff6a3d; }
.ai-session-icon { flex: none; font-size: 13px; }
.ai-session-body { flex: 1; min-width: 0; }
.ai-session-title {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ai-session-time { font-size: 11px; color: #aaa; margin-top: 2px; }
.ai-session-del { flex: none; font-size: 13px; opacity: 0; transition: opacity .15s; }
.ai-session-item:hover .ai-session-del { opacity: .6; }
.ai-session-del:hover { opacity: 1; }
.ai-empty { padding: 24px 12px; text-align: center; color: #bbb; font-size: 13px; }
.ai-header {
  background: linear-gradient(135deg, #ff9a56, #ff6a3d);
  color: #fff;
  padding: 14px 16px;
  font-weight: 600;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.ai-close { font-size: 22px; cursor: pointer; line-height: 1; }
.ai-list-btn { cursor: pointer; }
.ai-header-actions { display: flex; align-items: center; gap: 12px; }
.ai-new { font-size: 12px; cursor: pointer; opacity: .95; }

.ai-messages {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  background: #f7f8fa;
}
.ai-msg { margin-bottom: 12px; display: flex; }
.ai-msg-user { justify-content: flex-end; }
.ai-msg-assistant { flex-direction: column; align-items: flex-start; }
.ai-bubble {
  max-width: 80%;
  padding: 9px 13px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.55;
  white-space: pre-wrap;
  word-break: break-word;
}
.ai-msg-user .ai-bubble {
  background: #ff6a3d;
  color: #fff;
  border-bottom-right-radius: 3px;
}
.ai-msg-assistant .ai-bubble {
  background: #fff;
  color: #333;
  border: 1px solid #eee;
  border-bottom-left-radius: 3px;
}
.ai-bubble ::v-deep strong { color: #ff6a3d; }
.ai-memory {
  margin-top: 6px;
  font-size: 12px;
  color: #9a866b;
  background: #faf5ee;
  border: 1px solid #f0e4d2;
  border-radius: 10px;
  padding: 3px 10px;
}

/* 商品卡片：窄面板内横排小卡，整卡点击跳详情 */
.ai-cards {
  width: 100%;
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.ai-card {
  display: flex;
  align-items: center;
  gap: 10px;
  background: #fff;
  border: 1px solid #eee;
  border-radius: 10px;
  padding: 8px;
  cursor: pointer;
  transition: box-shadow 0.15s, border-color 0.15s;
}
.ai-card:hover {
  border-color: #ffb089;
  box-shadow: 0 4px 14px rgba(255, 106, 61, 0.18);
}
.ai-card-img {
  width: 64px;
  height: 64px;
  flex: none;
  object-fit: cover;
  border-radius: 8px;
  background: #f5f5f5;
}
.ai-card-info { flex: 1; min-width: 0; }
.ai-card-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ai-card-meta {
  margin-top: 4px;
  display: flex;
  align-items: baseline;
  gap: 10px;
}
.ai-card-price { color: #ff4d2d; font-weight: 700; font-size: 15px; }
.ai-card-sales { color: #999; font-size: 12px; }
.ai-card-go { flex: none; color: #ff6a3d; font-size: 12px; }
.ai-tool {
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}
.ai-tool-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #ff9a56;
  animation: blink 1s infinite;
}
@keyframes blink { 50% { opacity: 0.3; } }
.ai-typing { color: #999; }
.ai-typing .dot1 { animation: blink 1s infinite; }
.ai-typing .dot2 { animation: blink 1s 0.2s infinite; }
.ai-typing .dot3 { animation: blink 1s 0.4s infinite; }

.ai-suggest {
  display: flex;
  gap: 8px;
  padding: 8px 14px 0;
  flex-wrap: wrap;
  background: #f7f8fa;
}
.ai-suggest span {
  font-size: 12px;
  padding: 5px 10px;
  background: #fff;
  border: 1px solid #ffd1ba;
  color: #ff6a3d;
  border-radius: 14px;
  cursor: pointer;
}
.ai-suggest span:hover { background: #fff3ec; }

.ai-input {
  display: flex;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid #f0f0f0;
}
.ai-input input {
  flex: 1;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 9px 12px;
  font-size: 14px;
  outline: none;
}
.ai-input input:focus { border-color: #ff9a56; }
.ai-input button {
  background: #ff6a3d;
  color: #fff;
  border: none;
  border-radius: 8px;
  padding: 0 16px;
  cursor: pointer;
  font-size: 14px;
}
.ai-input button:disabled { background: #ccc; cursor: not-allowed; }
</style>
