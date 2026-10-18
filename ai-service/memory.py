"""记忆层：L1 会话记忆 + L2 用户长期记忆，落地为单个 JSON 文件（原子写）。

设计文档：docs/ai-memory-v1.md
- L1：session_id → 最近 MAX_HISTORY 条对话，重启不丢
- L2：user_id → 模型主动记录的自然语言事实，跨会话注入

ponytail: 单文件 + 全函数同步；asyncio 单线程事件循环内天然串行，无锁即可。
单 worker 演示够用；多 worker/多实例会丢更新 → 换本文件存储后端为 Redis/MySQL。
"""
import json
import os
import time
from pathlib import Path

import config

_EMPTY = {"sessions": {}, "users": {}}
_data = None  # 进程内缓存；None = 未加载


def _path() -> Path:
    return Path(config.MEMORY_FILE)


def _load() -> dict:
    """读盘（进程内缓存一次）；文件损坏/不存在按空数据启动，不阻塞服务。"""
    global _data
    if _data is None:
        try:
            _data = json.loads(_path().read_text(encoding="utf-8"))
        except Exception:
            _data = {}
        if not isinstance(_data, dict):
            _data = {}
        _data.setdefault("sessions", {})
        _data.setdefault("users", {})
    return _data


def _save() -> None:
    """整文件原子替换：先写临时文件再 os.replace，不会留下半截 JSON。"""
    path = _path()
    path.parent.mkdir(parents=True, exist_ok=True)
    tmp = path.with_suffix(path.suffix + ".tmp")
    tmp.write_text(json.dumps(_data, ensure_ascii=False, indent=2), encoding="utf-8")
    os.replace(tmp, path)


# ---- L1 会话记忆 ----


def get_history(session_id: str) -> list:
    """取某会话历史（无则空表）。"""
    if not session_id:
        return []
    return list(_load()["sessions"].get(session_id, {}).get("messages", []))


def save_session(session_id: str, user_id, messages: list) -> None:
    """写入会话历史（裁剪到 MAX_HISTORY，并记录归属用户）。"""
    if not session_id:
        return
    sessions = _load()["sessions"]
    sessions[session_id] = {
        "user_id": str(user_id) if user_id else "",
        "messages": messages[-config.MAX_HISTORY:],
        "updated_at": int(time.time()),
    }
    # LRU 裁剪：会话数超限时丢最旧
    if len(sessions) > config.MAX_SESSIONS:
        oldest = sorted(sessions.items(), key=lambda kv: kv[1].get("updated_at", 0))
        for sid, _ in oldest[: len(sessions) - config.MAX_SESSIONS]:
            sessions.pop(sid, None)
    _save()


def get_session_user(session_id: str):
    """该会话绑定的 user_id（未登录/未知返回 None）。"""
    uid = _load()["sessions"].get(session_id, {}).get("user_id")
    return uid or None


def _clean_title(text) -> str:
    """压缩空白 + 去掉首尾标点/引号，避免标题以「，。？！「」」开头结尾。"""
    t = " ".join((text or "").split())
    return t.strip(' \u3000，。！？!?.,;；:：、\'"“”‘’()（）[]【】<>《》')


def _session_title(messages: list, limit: int = None) -> str:
    """会话标题：从会话前 3 条用户消息提取主题（规则版，零 LLM 成本、确定性）。
    主题通常由第一问确立；首问过短（「你好」）才借后两问补足。"""
    limit = limit or config.SESSION_TITLE_LEN
    qs = [_clean_title(m.get("content")) for m in messages if m.get("role") == "user"]
    qs = [q for q in qs[:3] if q]
    if not qs:
        return ""  # 还没问过 → 空标题，前端显示「新会话」
    title = qs[0]
    if len(title) < 6:
        for q in qs[1:]:
            title = (title + " " + q).strip()
            if len(title) >= 6:
                break
    return title[:limit] + "…" if len(title) > limit else title


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


def delete_session(session_id: str, user_id) -> dict:
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


# ---- L2 用户长期记忆 ----


def get_facts(user_id) -> list:
    """取用户全部长期记忆（原始 dict 列表，注入层只取 text）。"""
    if not user_id:
        return []
    return list(_load()["users"].get(str(user_id), {}).get("facts", []))


def _normalize(text: str) -> str:
    return " ".join((text or "").split()).lower()


def add_fact(user_id, text: str) -> dict:
    """存一条长期记忆；重复（规范化相同或互为子串且短句≥6字）不新增。"""
    text = (text or "").strip()
    if not user_id or not text:
        return {"saved": False, "message": "参数不完整"}
    users = _load()["users"]
    entry = users.setdefault(str(user_id), {"facts": [], "updated_at": int(time.time())})
    norm = _normalize(text)
    for f in entry["facts"]:
        old = _normalize(f.get("text", ""))
        if not old:
            continue
        if old == norm:
            return {"saved": False, "message": "这条信息之前已经记过了", "total": len(entry["facts"])}
        # 互为子串且短句≥6字才视为重复（避免"事实1"被"事实10"误吞）
        if len(old) >= 6 and len(norm) >= 6 and (old in norm or norm in old):
            return {"saved": False, "message": "这条信息之前已经记过了", "total": len(entry["facts"])}
    entry["facts"].append({"text": text, "created_at": int(time.time())})
    # 超限丢最旧
    if len(entry["facts"]) > config.MAX_FACTS:
        entry["facts"] = entry["facts"][-config.MAX_FACTS:]
    entry["updated_at"] = int(time.time())
    _save()
    return {"saved": True, "message": "已记住", "total": len(entry["facts"])}


def forget_facts(user_id, keyword: str = "", clear_all: bool = False) -> dict:
    """删除长期记忆：关键词模糊匹配或全部清空。返回删除条数。"""
    if not user_id:
        return {"removed": 0, "message": "未登录，无记忆可删"}
    users = _load()["users"]
    entry = users.get(str(user_id))
    if not entry:
        return {"removed": 0, "message": "该用户没有记忆"}
    facts = entry.get("facts", [])
    if clear_all:
        removed = len(facts)
        users.pop(str(user_id), None)
    else:
        kw = (keyword or "").strip().lower()
        if not kw:
            return {"removed": 0, "message": "请提供关键词或 all=true"}
        kept = [f for f in facts if kw not in (f.get("text", "") or "").lower()]
        removed = len(facts) - len(kept)
        if removed:
            entry["facts"] = kept
            entry["updated_at"] = int(time.time())
        else:
            return {"removed": 0, "message": f"没有找到包含“{keyword}”的记忆"}
    _save()
    return {"removed": removed, "message": f"已删除 {removed} 条记忆"}


def clear_user(user_id) -> dict:
    """清空某用户长期记忆（REST 用）。"""
    if not user_id:
        return {"removed": 0}
    users = _load()["users"]
    removed = len(users.get(str(user_id), {}).get("facts", []))
    users.pop(str(user_id), None)
    _save()
    return {"removed": removed}


def stats() -> dict:
    """供 /health 或调试：会话数、用户数、记忆条数。"""
    d = _load()
    return {
        "sessions": len(d["sessions"]),
        "users": len(d["users"]),
        "facts": sum(len(u.get("facts", [])) for u in d["users"].values()),
    }


def _reset_for_test():
    """仅供自测：清空进程内缓存（模拟重启）。"""
    global _data
    _data = None
