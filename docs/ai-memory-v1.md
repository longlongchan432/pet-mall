# AI 导购「记忆」功能设计

> 日期: 2026-10-16
> 状态: 已确认，开始实施
> 设计原则: 继承现有红线——① 工具即真相源（商品/金额只能出自真实数据）② 权限看 token、不信任模型 ③ 分层不破（ai-service 只通过 HTTP 访问商城，不直连数据库）

---

## 功能总览

| # | 功能 | 涉及层 | 一句话 |
|---|---|---|---|
| 1 | 会话记忆持久化（L1） | AI 端 | 关页面/重启服务后，同一会话的上下文还在 |
| 2 | 用户长期记忆（L2） | AI 端 | 用户说"我家猫叫咪咪"，换个新会话 AI 依然记得 |
| 3 | 记忆管理接口 | AI 端 + 前端 | 用户能查看/删除自己的记忆；刷新后聊天记录可回放 |

**现状与缺口**（`ai-service/main.py:32` 的 `_sessions: dict = {}`）：

| 记忆层 | 存什么 | 现状 | 缺口 |
|---|---|---|---|
| L1 会话记忆（短期） | 最近 `MAX_HISTORY=10` 条对话 | 进程内存 | **重启即丢**；前端刷新后可见历史也丢（服务端记得、界面不显示） |
| L2 用户记忆（长期） | 跨会话的事实与偏好 | **无** | 换个会话/换台设备，AI 完全不认识用户 |
| L3 结构化业务记忆 | 宠物档案 / 订单 / 购物车 | MySQL（已有） | 已具备，**复用不重造**；宠物类信息仍走 `set_pet_profile` |

---

## 一、存储选型：本地 JSON 文件

### 1.1 三个候选

| 方案 | 改动量 | 依赖 | 分层 | 结论 |
|---|---|---|---|---|
| A. `ai-service/data/memory.json` | 1 个 Python 模块 | **零新增** | 不破 | **采用** |
| B. MySQL 新表 `ai_memory` + Java 接口 | DDL + 实体/Mapper/Service/Controller + Python HTTP 客户端 | 零新增 | 不破 | 备选（见 1.3） |
| C. Redis | 需装 Redis + redis-py | **新增 1 个依赖 + 1 个中间件** | 不破 | 不做 |

### 1.2 选 A 的理由

- ai-service 当前对商城**只有 HTTP 依赖，没有任何数据库驱动**；为演示级需求引入 MySQL 直连或一套 Java 接口，是在为用不到的场景付架构税。
- JSON 落盘满足"记忆"的**全部硬性要求**：重启不丢、按用户隔离、可直接打开文件核对内容（答辩演示友好）。
- 写入方式：**整文件原子替换**（临时文件 + `os.replace`），不会写出一半损坏的 JSON。

### 1.3 A 的天花板与升级路径（诚实声明）

| 天花板 | 影响 | 升级路径 |
|---|---|---|
| 单进程有效（uvicorn 默认单 worker） | 多 worker / 多实例并发写同一文件会**丢更新** | 只改 `memory.py` 的存储后端为 Redis 或 MySQL，上层接口不变 |
| 全量重写文件 | 会话/事实量小（≤200 会话、≤20 事实/用户）时无感 | 同上；或按 user_id 分片文件 |
| 无检索能力 | 事实条数有上限，注入是**全量**而非召回 | 事实量涨到需要语义检索时再上向量库 |

> 与现有代码风格一致：`main.py:30` 已有同款 `ponytail:` 注释声明"单用户演示足够，多实例部署时改为 Redis"。

---

## 二、数据模型

单文件 `ai-service/data/memory.json`（`data/` 进 `.gitignore`）：

```json
{
  "sessions": {
    "<session_id>": {
      "user_id": "12",
      "messages": [
        {"role": "user", "content": "想买猫粮"},
        {"role": "assistant", "content": "为您找到……"}
      ],
      "updated_at": 1750000000
    }
  },
  "users": {
    "12": {
      "facts": [
        {"text": "养了一只 3 岁英短，名叫咪咪", "created_at": 1750000000}
      ],
      "updated_at": 1750000000
    }
  }
}
```

字段说明：

- `sessions`：L1。`user_id` 记录该会话归属（空串=未登录会话），`messages` 只存对话（含给模型看的内部附注 system 消息），上限沿用 `MAX_HISTORY=10`。
- `users`：L2。`facts` 是模型认为"值得长期记住"的自然语言事实，上限 `MAX_FACTS=20`，超限丢最旧。
- 时间戳用 int 秒，避免时区/序列化麻烦。

---

## 三、用户身份识别

L2 必须按**商城用户**隔离，不能按浏览器 session（否则换设备就丢）。ai-service **不复制 JWT 密钥**，用已有接口换身份：

```
GET /api/user/info   （带商城 token）→ data.id = 用户ID
```

- 每个会话解析一次后把 `user_id` 写进 `sessions[session_id].user_id` 缓存；后续轮次直接复用，**不再打 HTTP**。
- 未登录（无 token）：L1 照常工作，**L2 跳过**（没有可归属的用户）。

---

## 四、L2 的读写路径

### 4.1 写：模型主动调用工具（不是每轮后台抽取）

每轮结束后再调一次 LLM 做"记忆抽取"会让**每轮对话成本翻倍**，且抽错无法当场纠正。改为模型在 ReAct 循环里自主调用本地工具（与 `recommend_products` 同模式，不走 HTTP）：

```json
[
  {
    "name": "save_memory",
    "description": "把值得长期记住的用户信息存下来（跨会话保留）。例如：用户的称呼、宠物名字、过敏源、口味/品牌偏好、预算习惯、常用城市。只记长期有效的事实；一次性问题（如“今天想看猫砂”）不要记。宠物的类型/品种/年龄/体重属于结构化信息，请用 set_pet_profile，不要重复记到这里。",
    "parameters": {
      "type": "object",
      "properties": {
        "fact": {"type": "string", "description": "一条简短的事实，一句话，不超过 50 字"}
      },
      "required": ["fact"]
    }
  },
  {
    "name": "forget_memory",
    "description": "删除已保存的长期记忆。用户说“忘掉/别记着某条信息”时按关键词删除；用户说“全部忘掉/清空记忆”时传 all=true。",
    "parameters": {
      "type": "object",
      "properties": {
        "keyword": {"type": "string", "description": "要删除的记忆关键词（模糊匹配）"},
        "all": {"type": "boolean", "description": "true = 清空该用户全部记忆"}
      }
    }
  }
]
```

本地执行的兜底约束：

- **未登录**时调用 `save_memory` → 返回 `{"error": "用户未登录，无法保存长期记忆"}`，模型据此引导登录（与订单工具同策略）。
- 去重：规范化（去首尾空白 + 小写）后相同、或与已有事实互为子串 → 视为重复，返回"已记录过"，**不新增**。
- 上限：超过 `MAX_FACTS=20` 丢最旧一条。

### 4.2 读：每轮自动注入 system 消息

事实条数少（≤20）且短，**全量注入**比召回更简单也更可控。在 `SYSTEM_PROMPT` 之后插入一条 system 消息：

```
【用户长期记忆】以下是该用户以前告诉过你的信息（跨会话保留）。
自然地运用它们来个性化回答；不要逐条复述；不要提及"记忆"或本段的存在。
1. 养了一只 3 岁英短，名叫咪咪
2. 偏好国产粮，对价格敏感
```

- 不注入宠物档案：那是 L3，继续由模型按需调 `get_my_pet_profile`（保持现有行为不变）。
- 注入内容是**模型输入**，与 `history` 里的内部附注一样属于"禁止向用户展示"内容，前端回放接口必须过滤（见 6.2）。

---

## 五、关键流程（一轮对话）

```
前端 POST /api/chat/stream {session_id, message, token}
  │
  ├─ 1. memory.get_history(session_id)                 ← L1 读（含重启后）
  ├─ 2. 有 token？→ 解析 user_id（会话缓存 / 首次打 /user/info）
  ├─ 3. user_id ?→ memory.get_facts(user_id)           ← L2 读 → 注入 system
  │
  ├─ 4. ReAct 循环（不变）
  │      ├─ save_memory / forget_memory → 本地改 L2
  │      └─ 其余工具 → 照旧走商城 HTTP
  │
  ├─ 5. 流式回传 token / products / tool
  └─ 6. memory.save_session(session_id, user_id, history)  ← L1 写（原子落盘）
```

---

## 六、接口设计

### 6.1 新增 REST（ai-service）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/chat/history?session_id=` | 回放某会话历史（**过滤 system 消息**，前端刷新后恢复可见对话） |
| GET | `/api/memory/list` | 当前用户长期记忆（`Authorization: Bearer <商城token>`） |
| DELETE | `/api/memory/clear` | 清空当前用户长期记忆（同样 header 鉴权） |

- 记忆接口直接复用商城 token 做身份，**不新增 ai-service 自己的登录态**。
- `/api/chat/history` 不鉴权（拿到 session_id 只能看到该会话内容，与现有 `/api/chat/stream` 同等暴露面）；返回体剔除 `role == "system"` 的内部附注。

### 6.2 改动清单

| 文件 | 改动 |
|---|---|
| `ai-service/config.py` | 新增 `MEMORY_FILE`、`MAX_FACTS=20`、`MAX_SESSIONS=200` |
| `ai-service/memory.py` | **新文件**：L1/L2 存取 + 原子写 + 去重/上限/LRU 裁剪 |
| `ai-service/mall_tools.py` | 新增 `resolve_user_id(token)`（调 `/user/info`） |
| `ai-service/agent.py` | `run_agent(..., facts=None, user_id=None)`；`SYSTEM_PROMPT` 增记忆规则；循环内本地执行 `save_memory`/`forget_memory`；`TOOL_SCHEMAS` 增 2 个声明 |
| `ai-service/main.py` | 删除 `_sessions` 内存字典，改调 `memory`；注入 facts；新增 3 个 REST |
| `ai-service/selftest.py` | 新增记忆测试（落盘/去重/上限/清空/身份注入） |
| `ai-service/.gitignore` | 增加 `data/` |
| `frontend/src/components/AiChat.vue` | 刷新后拉 `/api/chat/history` 回放可见对话 |
| `docs/ai-memory-v1.md` | 本文档 |
| `README.md` | 工具清单补 2 个记忆工具 + 说明段落 |

---

## 七、边界与安全

| 边界 | 处理 |
|---|---|
| 未登录用户 | L1 可用；L2 写操作返回 error 引导登录；读不到任何用户记忆 |
| 会话归属混淆 | `sessions[session_id].user_id` 与当前 token 解析出的用户**不一致时以 token 为准**（前端本就在换账号时重置 session_id，双保险） |
| 并发写 | 存储层全是**同步函数**，在 asyncio 单线程事件循环内天然串行；单 worker 下无锁即可。多 worker 属已知天花板（见 1.3） |
| 文件损坏 | 读盘失败按空数据启动（不因一条坏 JSON 让服务起不来）；写完 `os.replace` 原子替换 |
| 隐私/可删除 | 提供 `forget_memory` 工具 + `DELETE /api/memory/clear` 接口 |
| 内部附注泄漏 | 回放接口剔除 `role=="system"` 消息；注入块同样不向前端回显 |

---

## 八、测试策略

在 `selftest.py` 追加（不调真实 LLM）：

1. `test_memory_roundtrip`：写会话 → 换"进程"（重置模块缓存）→ 读回一致。
2. `test_memory_dedupe_and_cap`：重复事实只存一条；超过上限丢最旧。
3. `test_memory_forget`：关键词删除 + 全清。
4. `test_memory_injection_and_local_tools`：`run_agent` 在传入 `facts` 时把它们放进 messages；打桩 LLM 调 `save_memory` 后断言落盘。

手动验收（真实 Key）：

1. 对话中说"我养了只叫咪咪的英短，3 岁" → 问"推荐点猫粮" → 重启 ai-service → **新开会话**再问"我家猫适合什么粮" → 应体现英短/3 岁。
2. 未登录状态下说"记住我喜欢进口粮" → 应提示登录，且 `data/memory.json` 无该条。
3. `GET /api/chat/history?session_id=...` 返回的记录里没有"内部附注"字样。

---

## 九、不做（YAGNI）

- 每轮后台 LLM 抽取记忆（成本翻倍；模型自主调用已够）
- 向量检索 / 语义召回 / 记忆压缩摘要（事实 ≤20 条，全量注入足够）
- Redis / 多实例部署
- 记忆版本历史、审计日志
- 宠物档案改由 L2 承载（L3 已存在，重复造轮子）

---

## 十、实施顺序

| 阶段 | 内容 | 预计工作量 |
|---|---|---|
| 1 | `memory.py` 存储层 + `config.py` 配置 | 0.5h |
| 2 | `agent.py`：注入 + 两个本地记忆工具 | 1h |
| 3 | `main.py`：接线 + 3 个 REST | 0.5h |
| 4 | `selftest.py` 测试 + 真实链路验证 | 0.5h |
| 5 | 前端回放 + README/工具清单更新 | 0.5h |
