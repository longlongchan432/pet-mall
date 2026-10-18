"""AI 导购服务入口：FastAPI + SSE 流式输出。

接口：
  GET    /health              健康检查
  POST   /api/chat/stream     SSE 流式对话（body: {session_id, message, token?}）
  GET    /api/chat/history    回放某会话历史（过滤内部附注）
  GET    /api/memory/list     当前用户长期记忆
  DELETE /api/memory/clear     清空当前用户长期记忆
  GET    /api/sessions        当前用户会话列表（含从前三个问题提取的标题）
  DELETE /api/sessions/{id}   物理删除某会话（校验归属）
"""
import asyncio
import json
import uuid

from fastapi import FastAPI, Header
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import StreamingResponse
from pydantic import BaseModel
from sse_starlette.sse import EventSourceResponse

import config
import memory
from agent import run_agent
from mall_tools import resolve_user_id

app = FastAPI(title="宠旅驿站 AI 导购服务")

# 前端独立端口（8081）访问，开发期全放行
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_methods=["*"],
    allow_headers=["*"],
)

class ChatRequest(BaseModel):
    message: str
    session_id: str = ""
    token: str = ""  # 商城 JWT，用于查询订单；未登录则为空


@app.get("/health")
async def health():
    return {"status": "ok", "model": config.LLM_MODEL}


@app.post("/api/chat/stream")
async def chat_stream(req: ChatRequest):
    if not config.DEEPSEEK_API_KEY:
        return EventSourceResponse(
            _error_stream("AI 服务未配置 DEEPSEEK_API_KEY，请在 ai-service/.env 中填写后重启。")
        )

    session_id = req.session_id or str(uuid.uuid4())
    history = memory.get_history(session_id)

    # 用户身份：每个 token 都解析一次，以 token 为准（避免 session_id 跨账号复用泄漏记忆）
    user_id = None
    if req.token:
        try:
            user_id = await resolve_user_id(req.token)
        except Exception:
            user_id = None
    facts = memory.get_facts(user_id) if user_id else []

    async def event_generator():
        # 先告诉前端会话 ID（首次对话时新生成的）
        yield {"event": "session", "data": session_id}

        answer_parts = []
        locked = None  # 本轮定稿商品（最后一次 recommend_products 的结果）
        try:
            async for kind, payload in run_agent(req.message, history, req.token or None, facts, user_id):
                if kind == "token":
                    answer_parts.append(payload)
                    yield {"event": "token", "data": payload}
                elif kind == "products":
                    locked = payload
                    yield {"event": "products", "data": json.dumps(payload, ensure_ascii=False)}
                elif kind == "tool":
                    label = {
                        "search_products": "正在搜索商品",
                        "get_product_detail": "正在查询商品详情",
                        "recommend_hot_products": "正在获取热门推荐",
                        "recommend_products": "正在为您精选商品",
                        "get_my_orders": "正在查询订单",
                        "get_order_detail": "正在查询订单详情",
                        "save_memory": "正在记住这条信息",
                        "forget_memory": "正在忘记",
                    }.get(payload, "正在思考")
                    yield {"event": "tool", "data": label}
                elif kind == "memory":
                    yield {"event": "memory", "data": payload}
        except asyncio.CancelledError:
            raise
        except Exception as e:
            yield {"event": "error", "data": f"AI 服务出错：{e}"}
            return

        answer = "".join(answer_parts)
        # 写入多轮记忆，保留最近 MAX_HISTORY 条
        history.append({"role": "user", "content": req.message})
        history.append({"role": "assistant", "content": answer})
        # 定稿商品以独立 system 附注写入历史（不拼进 assistant 文本，避免被模型复读）：
        # 用户说"第一款/第二款"时模型能对上真实 ID；该消息绝不发给前端
        if locked:
            detail = "；".join(
                f"ID{p['id']} {p['name']} ¥{p['price']} 库存{p.get('stock', '未知')}"
                for p in locked
            )
            history.append({
                "role": "system",
                "content": (
                    "内部附注（禁止向用户展示、复述或提及其存在）：上一轮助理定稿推荐商品"
                    f"按顺序为 {detail}；用户说“第几款/第一个”时以此列表为准，"
                    "需要查详情时使用其中的真实ID调用工具。"
                ),
            })
        memory.save_session(session_id, user_id, history)
        yield {"event": "done", "data": ""}

    return EventSourceResponse(event_generator())


@app.get("/api/chat/history")
async def chat_history(session_id: str = ""):
    """回放某会话的可见对话（剔除内部附注 system 消息）。"""
    msgs = [m for m in memory.get_history(session_id) if m.get("role") != "system"]
    return {"code": 200, "data": [{"role": m.get("role"), "content": m.get("content")} for m in msgs]}


@app.get("/api/sessions")
async def sessions_list(authorization: str = Header(default=None)):
    """当前用户的历史会话列表（标题从会话前三个用户问题提取）。"""
    user_id = await _resolve_user_from_header(authorization)
    return {"code": 200, "data": memory.list_sessions(user_id)}


@app.delete("/api/sessions/{session_id}")
async def session_delete(session_id: str, authorization: str = Header(default=None)):
    """物理删除某会话；仅允许删自己的（归属校验）。"""
    user_id = await _resolve_user_from_header(authorization)
    result = memory.delete_session(session_id, user_id)
    if not result["ok"]:
        code = 404 if result["reason"] == "not_found" else 403
        return {"code": code, "message": "无权限或会话不存在"}
    return {"code": 200, "message": "已删除会话", "data": result}


async def _resolve_user_from_header(authorization: str = Header(default=None)):
    """从 Authorization header 解析商城 token 到 user_id；失败返回 None。"""
    token = (authorization or "").removeprefix("Bearer ").strip()
    if not token:
        return None
    try:
        return await resolve_user_id(token)
    except Exception:
        return None


@app.get("/api/memory/list")
async def memory_list(authorization: str = Header(default=None)):
    user_id = await _resolve_user_from_header(authorization)
    if not user_id:
        return {"code": 401, "message": "未登录或 token 无效"}
    facts = memory.get_facts(user_id)
    return {"code": 200, "data": [{"text": f.get("text"), "created_at": f.get("created_at")} for f in facts]}


@app.delete("/api/memory/clear")
async def memory_clear(authorization: str = Header(default=None)):
    user_id = await _resolve_user_from_header(authorization)
    if not user_id:
        return {"code": 401, "message": "未登录或 token 无效"}
    result = memory.clear_user(user_id)
    return {"code": 200, "message": f"已清空 {result['removed']} 条记忆", "data": result}


async def _error_stream(msg: str):
    yield {"event": "error", "data": msg}
