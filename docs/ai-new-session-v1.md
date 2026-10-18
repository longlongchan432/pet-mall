# AI 导购「会话管理」设计（新建 / 左侧列表 / 切换 / 删除）

> 需求：对话面板支持「新建会话」，并把历史会话做成可管理的——**左侧可滑动列表**、切回旧会话、彻底删除（带二次确认）；
> 会话标题要突出主题，**从用户问的前三个问题里摘要提取**。
> 前置文档：`docs/ai-memory-v1.md`（三层记忆 L1 会话 / L2 用户长期记忆 / L3 宠物档案）。

## 一、功能范围

| 代号 | 功能 | 说明 |
|------|------|------|
| A | 新建会话 | 头部「＋ 新会话」按钮，隔断当前上下文，从空白开始 |
| B | 左侧会话列表 | 面板左侧展开一列可上下滑动的会话列表（图标 + 标题），标题由前三个问题摘要而来 |
| C | 切回旧会话 | 点列表项切换回去，继续此前的对话 |
| D | 物理删除 | 列表项上的 🗑 + 二次确认弹窗，从 `memory.json` 彻底移除 |

A 单独做时后端可零改动（见 v0 思路）；B/C/D 必须读服务端 `sessions`，且标题要服务端生成，所以要**新增 2 个端点 + 3 个 memory 函数**。

## 二、一句话结论

> 后端：`list_sessions` 返回**从会话前三个用户问题提取的标题**，新增 `GET /api/sessions`、`DELETE /api/sessions/{id}`（Bearer + 归属校验）。
> 前端：`.ai-panel` 由「单列」改为「左侧栏 + 对话区」两列，左侧栏可滑动；复用 `loadHistory()` 切换；删除走 `this.$confirm` 二次确认。
> 改动量：后端约 60 行，前端约 80 行，selftest 约 20 行。

## 三、现状（改之前）

| 位置 | 现状 |
|------|------|
| `ai-service/main.py:21` | 已 `import memory`；`:131` 已有 `_resolve_user_from_header`（Bearer→user_id）可复用 |
| `main.py:53` | `session_id = req.session_id or str(uuid.uuid4())`：空 id → 新建 |
| `main.py:124-128` | `GET /api/chat/history`：按 session_id 回放可见历史（**无归属校验**，靠随机 uuid 不可猜） |
| `ai-service/memory.py:59-75` | `save_session` 写 `sessions[sid]={user_id,messages,updated_at}`，LRU 上限 `MAX_SESSIONS=200` |
| `frontend/.../AiChat.vue:82-88` | `sessionId` 从 localStorage 读，按登录 token 隔离账号 |
| `AiChat.vue:260-275` | `.ai-panel`：`position:fixed; top:80px; right:16px; bottom:16px; width:20vw; min/max 380/560px; display:flex; flex-direction:column` —— **右锚定，所以加宽只会向左生长**，天然适合把列表放左侧 |
| `AiChat.vue:11-14` | 头部：标题 + `×`（flex + space-between） |
| `AiChat.vue:103-117` | `created()`→`loadHistory()` 回放 |
| `AiChat.vue:139-168` | `send()`：`this.controller = new AbortController()`（148）；带 `session_id` POST |
| `AiChat.vue:195-198` | SSE `session` 事件回写 localStorage |

## 四、后端设计

### 4.1 会话标题生成（本次核心新增）

`config.py` 加 `SESSION_TITLE_LEN = 16`；`memory.py` 加：

```python
def _clean_title(text):
    """压缩空白 + 去掉首尾标点/引号，避免标题以「，。？！「」」开头结尾。"""
    t = " ".join((text or "").split())
    return t.strip(' \u3000，。！？!?.,;；:：、\'"“”‘’()（）[]【】<>《》')


def _session_title(messages, limit=config.SESSION_TITLE_LEN):
    """从会话前 3 条用户消息提取主题标题（规则版，零 LLM 成本、确定性、即时）。"""
    qs = [_clean_title(m.get("content")) for m in messages if m.get("role") == "user"]
    qs = [q for q in qs[:3] if q]          # 只看前三个问题，空串剔除
    if not qs:
        return ""                           # 还没问过 → 空标题，前端显示「新会话」
    title = qs[0]                           # 主题通常由第一问确立
    if len(title) < 6:                      # 首问太短（「你好」「在吗」）→ 借后两问补足
        for q in qs[1:]:
            title = (title + " " + q).strip()
            if len(title) >= 6:
                break
    return title[:limit] + "…" if len(title) > limit else title
```

规则版效果示例：`我想买狗粮，有什么好的？` → `我想买狗粮，有什么好的？` (13字，直接用作标题)；
`你好` + `我的订单发货了吗` → `你好 我的订单发货了吗` → `你好 我的订单发货了…`。
首问过短才借用后两问，**符合「从用户前三个问题提取」且不产生任何模型开销**。

> **可选增强（待你决定，默认不做）**：若要图里那种「提炼式」短标题（例如把
> 「我需要把简历改成 Java 方向」提炼成「简历修改（Java方向）」），需要 **LLM 摘要**：
> 会话累计到 3 条用户消息时触发 1 次 LLM 生成 ≤14 字 `title` 并写入 `session.title`，
> 此后不再调用（每会话 1 次，成本有界）。**默认走规则版**；你说要，我再加这一层。

### 4.2 memory.py：`list_sessions` + `delete_session`

```python
def list_sessions(user_id) -> list:
    """当前用户的会话列表，按 updated_at 倒序。无则空表。"""
    sessions = _load()["sessions"]
    uid = str(user_id) if user_id else ""
    out = []
    for sid, s in sessions.items():
        if (s.get("user_id") or "") != uid:
            continue
        msgs = s.get("messages", [])
        out.append({
            "session_id": sid,
            "title": _session_title(msgs),          # 前三个问题摘要
            "updated_at": s.get("updated_at", 0),   # unix 秒；前端格式化
            "message_count": len(msgs),
        })
    out.sort(key=lambda x: x["updated_at"], reverse=True)
    return out


def delete_session(session_id, user_id) -> dict:
    """物理删除；校验归属。返回 {ok, reason}。"""
    sessions = _load()["sessions"]
    s = sessions.get(session_id)
    if not s:
        return {"ok": False, "reason": "not_found"}
    if (s.get("user_id") or "") != (str(user_id) if user_id else ""):
        return {"ok": False, "reason": "forbidden"}
    sessions.pop(session_id, None)
    _save()
    return {"ok": True, "removed": 1}
```

### 4.3 main.py：2 条路由（复用 `_resolve_user_from_header`）

```python
@app.get("/api/sessions")
async def sessions_list(authorization: str = Header(default=None)):
    user_id = await _resolve_user_from_header(authorization)
    return {"code": 200, "data": memory.list_sessions(user_id)}

@app.delete("/api/sessions/{session_id}")
async def session_delete(session_id: str, authorization: str = Header(default=None)):
    user_id = await _resolve_user_from_header(authorization)
    result = memory.delete_session(session_id, user_id)
    if not result["ok"]:
        code = 404 if result["reason"] == "not_found" else 403
        return {"code": code, "message": "无权限或会话不存在"}
    return {"code": 200, "message": "已删除会话", "data": result}
```

- 未登录（token 为空）→ `user_id=None` → 只匹配 `user_id==""` 的会话，逻辑自洽。
- `GET /api/chat/history` 维持原样（不强制归属校验）：切换只回放「列表里拿到的、属于自己的」id，实践中不越权。

## 五、前端设计

### 5.1 面板结构：左列表 + 右对话（实现方式）

`.ai-panel` 是 `position:fixed` 且**右锚定**（`right:16px`）——所以展开左栏时只要把面板整体加宽，对话区宽度不变、整体向左生长。

左栏**不引入 `.ai-main` 包壳**（那会改动 40 多行模板缩进）：`.ai-sidebar` 是面板的绝对定位子元素（`position:fixed` 的父级天然是它的包含块），贴住面板左缘；聊天列则靠 `.ai-panel.with-sidebar { padding-left: 200px }` 让位。面板保持 `flex-direction: column` 不变。

```css
.ai-panel { /* 原样不动：fixed + flex column + overflow hidden + border-radius 14px */ }
.ai-panel.with-sidebar { width: calc(20vw + 200px); min-width: 580px; max-width: 760px; padding-left: 200px; }
.ai-sidebar { position: absolute; left: 0; top: 0; bottom: 0; width: 200px; ... }
```

模板（只在面板内插入左栏，其余结构原样）：

```html
<div v-else :class="['ai-panel', { 'with-sidebar': showSessions }]">
  <!-- 左侧会话列表（可上下滑动） -->
  <div v-if="showSessions" class="ai-sidebar">
    <div class="ai-sidebar-head"><span>会话</span><span class="ai-sidebar-collapse" @click="showSessions=false">«</span></div>
    <div class="ai-sidebar-list">
      <div v-for="s in sessionList" :key="s.session_id"
           :class="['ai-session-item', { active: s.session_id === sessionId }]"
           @click="switchSession(s.session_id)">
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
  <!-- ai-messages / ai-suggest / ai-input 全部原样不动 -->
</div>
```

### 5.2 方法（新增 / 调整）

```js
const greeting = () => ({ role: 'assistant', content: '您好！我是宠旅驿站 AI 导购，可以帮您找商品、查订单，试试下面的问题吧～' })

// A. 新建会话（掐断在跑的流 + 清本地 id + 回到欢迎语；左栏保持展开）
newSession() {
  if (this.controller) this.controller.abort()
  this.sessionId = ''
  localStorage.removeItem('ai_session_id')
  localStorage.removeItem('ai_session_token')
  this.input = ''
  this.messages = [greeting()]
}

// B. 左栏开合 + 拉列表
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

// C. 切回旧会话（左栏保持展开，方便连续切换）
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

// D. 删除（二次确认）
deleteSession(s) {
  const done = async () => {
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
  }
  // 二次确认：项目已全局注册 ElementUI → this.$confirm；否则降级 window.confirm
  if (this.$confirm) {
    this.$confirm(`确定删除会话「${s.title || '新会话'}」吗？删除后不可恢复。`, '删除确认', {
      confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning'
    }).then(done).catch(() => {})
  } else if (window.confirm('确定删除该会话？删除后不可恢复。')) {
    done()
  }
},

// 时间格式化（unix 秒 → MM-DD HH:mm，用于列表项 hover 提示）
fmtTime(ts) {
  const d = new Date((ts || 0) * 1000)
  const p = n => (n < 10 ? '0' : '') + n
  return `${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
}
```

`data()` 增加 `showSessions: false, sessionList: []`；
`send()` 的 `finally` 里补一句：`if (this.showSessions) this.loadSessions()`——否则新建的会话（第一条消息才在后端产生）不会出现在左栏，且当前会话标题不会刷新。

### 5.3 CSS（左栏 + 列表项）

```css
/* 展开左栏：整体加宽 200px，聊天列靠 padding-left 让位（右锚定 → 向左生长） */
.ai-panel.with-sidebar { width: calc(20vw + 200px); min-width: 580px; max-width: 760px; padding-left: 200px; }

/* 左侧会话列表：绝对定位贴面板左缘 */
.ai-sidebar { position: absolute; left: 0; top: 0; bottom: 0; width: 200px; background: #fafafa; border-right: 1px solid #f0f0f0; display: flex; flex-direction: column; }
.ai-sidebar-head { display: flex; align-items: center; justify-content: space-between; padding: 14px 12px; font-size: 14px; font-weight: 600; border-bottom: 1px solid #f0f0f0; }
.ai-sidebar-collapse { cursor: pointer; }
.ai-sidebar-list { flex: 1; overflow-y: auto; padding: 6px; }   /* ← 上下滑动查看会话 */
.ai-session-item { display: flex; align-items: center; gap: 8px; padding: 8px 10px; border-radius: 8px; cursor: pointer; }
.ai-session-item:hover { background: #f0f0f0; }
.ai-session-item.active { background: #fff3ec; }
.ai-session-item.active .ai-session-title { font-weight: 600; color: #ff6a3d; }
.ai-session-icon { flex: none; font-size: 13px; }
.ai-session-body { flex: 1; min-width: 0; }
.ai-session-title { font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.ai-session-time { font-size: 11px; color: #aaa; margin-top: 2px; }   /* ← 时间戳 */
.ai-session-del { flex: none; font-size: 13px; opacity: 0; transition: opacity .15s; }
.ai-session-item:hover .ai-session-del { opacity: .6; }
.ai-session-del:hover { opacity: 1; }
.ai-empty { padding: 24px 12px; text-align: center; color: #bbb; font-size: 13px; }

/* 头部：📋 开关 + 「＋ 新会话」+ × */
.ai-list-btn { cursor: pointer; }
.ai-header-actions { display: flex; align-items: center; gap: 12px; }
.ai-new { font-size: 12px; cursor: pointer; opacity: .95; }
```

列表项显示**标题 + 时间戳**（`fmtTime(updated_at)` → `MM-DD HH:mm`），标题单行省略、时间戳小字灰色。
`.ai-session-body` 的 `min-width: 0` 是长标题 ellipsis 生效的必要条件。

### 5.4 核心语义（务必一致）

| 层 | 新建会话后 | 切回旧会话后 |
|----|-----------|--------------|
| L1 会话上下文 | 全新会话（0 历史） | 恢复该会话的历史 |
| L2 长期记忆 | 保留 | 保留 |
| L3 宠物档案 | 保留 | 保留 |

即：新建/切换只动 L1；L2/L3 始终跟随用户。

### 5.5 实现坑（已知点）

1. **`greeting` 用工厂函数**，不要共享对象：`parseSSE` 里有 `this.$set(last,'content',…)`，欢迎语是唯一一条消息时会成为 `last`，共享引用会被改坏。
2. **切换/删除前先 `controller.abort()`**，避免在跑的流继续往 `messages` 塞字、或写进旧会话。
3. **删的是当前会话时**，删除后调用 `newSession()` 复位面板，否则 `sessionId` 指向已不存在的会话。
4. **删除图标 `@click.stop`**，否则触发整行 `switchSession`。
5. **`.ai-main` 必须 `min-width: 0`**，否则 flex 子项不肯收缩，长标题的 `ellipsis` 失效、把面板撑破。
6. **`.ai-panel` 由 column 改 row 后**，原有 header/messages/suggest/input 必须整体包进 `.ai-main`，否则会与左栏并排错位。

## 六、改动清单

| 文件 | 改动 |
|------|------|
| `ai-service/config.py` | 加 `SESSION_TITLE_LEN = 16` |
| `ai-service/memory.py` | 新增 `_clean_title`、`_session_title`、`list_sessions(user_id)`、`delete_session(session_id, user_id)` |
| `ai-service/main.py` | 新增 `GET /api/sessions`、`DELETE /api/sessions/{id}` |
| `ai-service/selftest.py` | 新增 2 个用例：标题提取（前 3 问/过短兜底/截断）、跨用户归属（越权删 403、删除后列表收缩） |
| `frontend/src/components/AiChat.vue` | `.ai-panel` 改 row + `.ai-main` 包壳 + `.ai-sidebar` 左栏模板；`greeting()` 工厂；`newSession/toggleSessions/loadSessions/switchSession/deleteSession/fmtTime`；`data` 加 `showSessions`、`sessionList`；`send()` finally 补刷新；CSS ~20 行 |
| `README.md` | AI 导购段落补一句：支持新建/左侧列表/切换/删除会话，标题自动摘要 |

预估：后端 ~60 行，前端 ~80 行，selftest ~20 行。

## 七、边界与安全

- **归属校验在后端**：`list_sessions` 按 user_id 过滤；`delete_session` 比对 `session.user_id` 与 token 解析出的 user_id，不符返回 403。换账号/未登录都只能动自己的会话。
- **未登录会话**：`user_id==""`，删/列都只命中自己的空用户会话，自洽。
- **`/api/chat/history` 不强制归属**：随机 uuid 不可猜，且切换只用列表里属于自己的 id（已知天花板）。
- **删除是物理删除**：从 `memory.json` 彻底移除、不可恢复，二次确认就是挡误操作的。
- **标题不含敏感计算**：纯字符串提取，无 LLM 调用，不引入新成本或注入面。

## 八、验证

### 自动化（selftest，离线，扩内存临时文件）
- 标题：3 条用户消息 `["我想买狗粮", "有什么好的", "预算100"]` → 标题 = `我想买狗粮`；`["你好", "我的订单发货了吗"]` → 标题以 `你好` 开头且含第二问；17 字长问 → 截断 16 字 + `…`；无用户消息 → `""`。
- 归属：user `12` 存 2 会话、user `13` 存 1 → `list_sessions("12")` 返 2、`delete_session(sid13,"12")` → `forbidden`、`delete_session(sidA,"12")` → `ok=True` 且列表变 1。

### 手验（curl + 点击，后端 8080 + ai-service 8082 需运行）
1. 点「📋」→ 左栏展开、面板向左加宽，空态显示「还没有历史会话」。
2. 聊一句 → 左栏出现该项，标题 = 该问题（≤16 字），图标 🐾，当前项高亮加粗。
3. 点「＋ 新会话」→ 再聊一句 → 左栏 A 之上多出新会话 B（updated_at 更大），A 标题不变。
4. 点 A → 面板回放 A 的对话（切回成功），左栏保持展开。
5. 在 B 里问「你还记得我的偏好吗」→ 能答出 L2 持久事实（L2 跨会话保留）。
6. 点 A 的 🗑 → 弹二次确认 → 确认 → A 从列表消失；若删的是当前会话，面板复位到欢迎语。
7. `GET /api/chat/history?session_id=A` 返回空（物理删除生效）。
8. 流式中点「＋ 新会话」→ 输出立刻停住、面板清空、半截对话未落盘（重查 A 条数不变）。
9. 会话多到超出左栏高度 → 左栏可上下滑动。

## 九、不做（真 YAGNI，本期明确砍掉）

- 会话重命名 / 搜索 / 置顶。
- 自动把上一会话总结进 L2（交给模型主动 `save_memory`）。
- 跨端/跨设备会话同步（单文件 JSON 单机）。
- 给 `/api/chat/history` 补 token 归属校验（低风险，uuid 不可猜）。
- **LLM 摘要标题**（见 4.1 可选增强）：默认规则版；要做的话你点个头。

## 十、实施顺序

1. `config.py` 加 `SESSION_TITLE_LEN`。
2. `memory.py`：`_clean_title` + `_session_title` + `list_sessions` + `delete_session`。
3. `main.py`：2 条路由。
4. `selftest.py`：标题 + 归属用例，`python selftest.py` 全绿。
5. `AiChat.vue`：面板改 row + `.ai-main` 包壳 + 左栏模板 + 方法 + CSS + `send()` 刷新。
6. `curl` 跑第八节 1-9 步；前端 `vue-cli-service` HMR 编译通过。
7. `README.md` 补一句。
