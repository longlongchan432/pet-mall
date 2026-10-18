"""离线自检：不调真实 LLM、不依赖 Key，用假响应验证 Agent 工具循环逻辑。

运行：python selftest.py
"""
import asyncio
import json
import tempfile
from pathlib import Path

import agent
import config
import mall_tools
import memory


class FakeMessage:
    def __init__(self, content=None, tool_calls=None):
        self.content = content
        self.tool_calls = tool_calls

    def model_dump(self, exclude_none=False):
        out = {"role": "assistant", "content": self.content}
        if self.tool_calls:
            out["tool_calls"] = [
                {"id": c.id, "type": "function",
                 "function": {"name": c.function.name, "arguments": c.function.arguments}}
                for c in self.tool_calls
            ]
        return out


class FakeCall:
    def __init__(self, cid, name, arguments):
        self.id = cid
        self.function = type("F", (), {"name": name, "arguments": arguments})()


class FakeChoice:
    def __init__(self, msg):
        self.message = msg


class FakeResponse:
    def __init__(self, msg):
        self.choices = [FakeChoice(msg)]


class FakeStream:
    def __init__(self, text):
        self._text = text

    async def __aenter__(self):
        return self

    async def __aexit__(self, *a):
        pass

    def __aiter__(self):
        chunks = [
            type("D", (), {"choices": [type("C", (), {"delta": type("Delta", (), {"content": t})()})()]})()
            for t in self._text
        ]
        return _aiter(chunks)


async def _aiter(items):
    for it in items:
        yield it


class FakeCompletions:
    def __init__(self, scripted, final_text):
        self._scripted = scripted
        self._final_text = final_text
        self._n = 0

    async def create(self, **kw):
        if kw.get("stream"):
            return FakeStream(self._final_text)
        msg = self._scripted[min(self._n, len(self._scripted) - 1)]
        self._n += 1
        return FakeResponse(msg)


class FakeClient:
    def __init__(self, scripted, final_text):
        self.chat = type("Chat", (), {"completions": FakeCompletions(scripted, final_text)})()


def test_tool_calling_loop():
    """第一轮模型要求搜商品 → 工具执行 → 第二轮模型给出最终答案。"""
    tool_call_msg = FakeMessage(
        tool_calls=[FakeCall("call_1", "search_products", json.dumps({"keyword": "狗粮"}))]
    )
    final_text = "为您找到3款狗粮，销量都不错，推荐第一款。"

    # 打桩：LLM 客户端
    original_client = agent.AsyncOpenAI
    agent.AsyncOpenAI = lambda **kw: FakeClient([tool_call_msg, FakeMessage(content=final_text)], final_text)

    # 打桩：商城工具
    called = {}

    async def fake_search(keyword):
        called["keyword"] = keyword
        return {"count": 3, "products": [{"id": 1, "name": "幼犬狗粮", "price": 89}]}

    # ponytail: agent 用的是 from import 绑定，必须打桩 agent 模块内的引用
    original_map = agent.build_tool_map
    agent.build_tool_map = lambda token: {
        **original_map(token), "search_products": fake_search
    }

    events = []

    async def run():
        async for kind, payload in agent.run_agent("想买狗粮", [], token=None):
            events.append((kind, payload))

    asyncio.run(run())

    agent.AsyncOpenAI = original_client
    agent.build_tool_map = original_map

    assert called.get("keyword") == "狗粮", "工具未被正确调用"
    assert ("tool", "search_products") in events, "未上报工具事件"
    text = "".join(p for k, p in events if k == "token")
    assert text == final_text, f"最终回答不一致: {text!r}"
    print("PASS: 工具调用循环（ReAct）正常，工具结果被喂回模型并流式输出")


def test_tool_schemas_valid():
    # recommend_products / save_memory / forget_memory 由 agent 编排层本地执行（不走 HTTP）
    LOCAL_TOOLS = {"recommend_products", "save_memory", "forget_memory"}
    names = {t["function"]["name"] for t in mall_tools.TOOL_SCHEMAS}
    impls = set(mall_tools.build_tool_map("fake-token").keys())
    # 声明 = 实现集合 ∪ 本地工具
    assert names == impls | LOCAL_TOOLS, f"声明与实现不一致: 声明{names} 实现{impls} 本地{LOCAL_TOOLS}"
    # 实现 ⊆ 声明
    assert impls <= names, f"实现不在声明中: {impls - names}"
    print(f"PASS: {len(impls)} 个 HTTP 工具一一对应 + {len(LOCAL_TOOLS)} 个编排层本地工具")


def test_compact_product():
    p = {"id": 1, "name": "x", "price": 10, "sales": 5, "stock": 2,
         "categoryName": "狗粮", "description": "很长" * 200, "image": "a.jpg"}
    small = mall_tools._compact_product(p)
    assert "description" not in small, "列表场景不应带描述"
    detail = mall_tools._compact_product(p, with_desc=True)
    assert len(detail["description"]) <= 300, "描述应被截断"
    print("PASS: 商品字段裁剪正常（控制 token）")


def test_cart_pet_tools_schemas():
    """验证购物车和宠物档案工具 schema 存在且参数正确。"""
    names = {t["function"]["name"] for t in mall_tools.TOOL_SCHEMAS}
    assert "add_to_cart" in names, "缺少 add_to_cart 工具"
    assert "get_cart" in names, "缺少 get_cart 工具"
    assert "delete_cart_item" in names, "缺少 delete_cart_item 工具"
    assert "get_my_pet_profile" in names, "缺少 get_my_pet_profile 工具"
    assert "set_pet_profile" in names, "缺少 set_pet_profile 工具"
    assert "delete_pet_profile" in names, "缺少 delete_pet_profile 工具"

    # 验证参数
    schemas = {t["function"]["name"]: t["function"]["parameters"] for t in mall_tools.TOOL_SCHEMAS}
    assert "product_id" in schemas["add_to_cart"]["required"], "add_to_cart 应要求 product_id"
    assert "pet_type" in schemas["set_pet_profile"]["required"], "set_pet_profile 应要求 pet_type"
    assert "pet_id" in schemas["delete_pet_profile"]["required"], "delete_pet_profile 应要求 pet_id"
    print("PASS: 购物车和宠物档案工具 schema 完整且参数正确")


def _use_temp_memory():
    """把 memory 指向临时文件并清进程缓存，返回临时目录（用完删）。"""
    tmp = tempfile.TemporaryDirectory()
    config.MEMORY_FILE = str(Path(tmp.name) / "memory.json")
    memory._reset_for_test()
    return tmp


def _restore_memory():
    config.MEMORY_FILE = str(Path(config.__file__).parent / "data" / "memory.json")
    memory._reset_for_test()


def test_memory_roundtrip():
    """写会话 → 重置进程缓存（模拟重启）→ 读回一致。"""
    tmp = _use_temp_memory()
    try:
        memory.save_session("s1", 12, [{"role": "user", "content": "想买猫粮"}])
        assert memory.get_history("s1") == [{"role": "user", "content": "想买猫粮"}], "会话写读不一致"
        assert memory.get_session_user("s1") == "12", "会话归属用户未记录"
        memory._reset_for_test()  # 模拟重启
        assert memory.get_history("s1") == [{"role": "user", "content": "想买猫粮"}], "重启后会话丢失"
        print("PASS: 会话记忆落盘，模拟重启后仍可读回")
    finally:
        tmp.cleanup()
        _restore_memory()


def test_memory_dedupe_and_cap():
    """重复事实只存一条；超上限丢最旧。"""
    tmp = _use_temp_memory()
    try:
        r1 = memory.add_fact("7", "喜欢国产猫粮")
        r2 = memory.add_fact("7", "喜欢国产猫粮")          # 完全重复
        r3 = memory.add_fact("7", " 喜欢国产猫粮 ")        # 空白规范化后重复
        r4 = memory.add_fact("7", "我喜欢国产猫粮和猫砂")   # 互为子串 → 重复
        assert r1["saved"] is True
        assert r2["saved"] is False and r3["saved"] is False and r4["saved"] is False, "去重失效"
        assert len(memory.get_facts("7")) == 1, f"重复事实未去重: {memory.get_facts('7')}"
        for i in range(config.MAX_FACTS + 5):
            memory.add_fact("7", f"事实{i}")
        facts = memory.get_facts("7")
        assert len(facts) == config.MAX_FACTS, f"上限失效: {len(facts)}"
        assert facts[0]["text"] == "事实5", f"未丢最旧: {facts[0]}"
        assert facts[-1]["text"] == f"事实{config.MAX_FACTS + 4}", "最新事实不在末尾"
        print("PASS: 记忆去重与上限裁剪正常")
    finally:
        tmp.cleanup()
        _restore_memory()


def test_memory_forget():
    """关键词删除 + 全清。"""
    tmp = _use_temp_memory()
    try:
        memory.add_fact("7", "养了一只3岁英短")
        memory.add_fact("7", "偏好国产粮")
        memory.add_fact("7", "住在上海")
        r = memory.forget_facts("7", keyword="英短")
        assert r["removed"] == 1, f"关键词删除条数不对: {r}"
        texts = {f["text"] for f in memory.get_facts("7")}
        assert "养了一只3岁英短" not in texts, "关键词删除未命中"
        r2 = memory.forget_facts("7", clear_all=True)
        assert r2["removed"] == 2, f"全清条数不对: {r2}"
        assert memory.get_facts("7") == [], "全清后仍有残留"
        print("PASS: 记忆关键词删除与全清正常")
    finally:
        tmp.cleanup()
        _restore_memory()


def test_memory_injection_and_local_tools():
    """run_agent 传入 facts 时注入 messages；打桩 LLM 调 save_memory 后落盘。"""
    tmp = _use_temp_memory()
    try:
        captured = {}

        class RecordingCompletions(FakeCompletions):
            async def create(self, **kw):
                captured["messages"] = kw["messages"]
                return await super().create(**kw)

        class RecordingClient(FakeClient):
            def __init__(self, scripted, final_text):
                self.chat = type("Chat", (), {
                    "completions": RecordingCompletions(scripted, final_text)
                })()

        tool_call_msg = FakeMessage(
            tool_calls=[FakeCall("call_1", "save_memory", json.dumps({"fact": "偏好国产粮"}, ensure_ascii=False))]
        )
        final_text = "好的，已经记住啦。"

        original_client = agent.AsyncOpenAI
        agent.AsyncOpenAI = lambda **kw: RecordingClient([tool_call_msg, FakeMessage(content=final_text)], final_text)

        events = []

        async def run():
            async for kind, payload in agent.run_agent(
                "我喜欢国产粮", [], token=None,
                facts=[{"text": "养了一只3岁英短，名叫咪咪"}], user_id="12"):
                events.append((kind, payload))

        asyncio.run(run())
        agent.AsyncOpenAI = original_client

        # 注入断言：system 消息里应出现长期记忆文本
        sys_msgs = [m for m in captured.get("messages", []) if m.get("role") == "system"]
        assert any("咪咪" in m.get("content", "") for m in sys_msgs), "长期记忆未注入 system"
        # 落盘断言：save_memory 被本地执行
        facts = memory.get_facts("12")
        assert any("国产粮" in f.get("text", "") for f in facts), f"save_memory 未落盘: {facts}"
        # 事件断言：save_memory 成功会 yield memory 事件
        assert ("memory", "已记住") in events, f"未上报记忆事件: {events}"
        print("PASS: 长期记忆注入 + save_memory 本地执行落盘")
    finally:
        tmp.cleanup()
        _restore_memory()


def test_session_title():
    """会话标题：从前三个用户问题提取（规则版，无 LLM 调用）。"""
    m = lambda role, text: {"role": role, "content": text}
    # 首问够长 → 直接作标题（首尾标点被剥离）
    assert memory._session_title([m("user", "我想买狗粮，有什么好的？")]) == "我想买狗粮，有什么好的"
    # 首问过短（寒暄）→ 借后两问补足
    t = memory._session_title([m("user", "你好"), m("user", "我的订单发货了吗")])
    assert t.startswith("你好") and "订单" in t, f"过短首问未兜底: {t!r}"
    # 限定前三个问题：第 4 问不得进标题
    t = memory._session_title([m("user", "帮我看看狗粮"), m("user", "第二问"),
                               m("user", "第三问"), m("user", "第四条问题XYZ")])
    assert t == "帮我看看狗粮", f"标题未限定前三问: {t!r}"
    # 超长截断到 SESSION_TITLE_LEN + 省略号
    t = memory._session_title([m("user", "我" * 40)])
    assert len(t) == config.SESSION_TITLE_LEN + 1 and t.endswith("…"), f"截断不对: {t!r}"
    # 无用户消息 → 空标题
    assert memory._session_title([m("assistant", "您好"), m("system", "内部附注")]) == "", "无用户消息应为空标题"
    print("PASS: 会话标题从前三个用户问题提取正常")


def test_session_ownership():
    """会话列表按归属过滤；跨用户删除被拒；未登录自洽；删除后列表收缩。"""
    tmp = _use_temp_memory()
    try:
        memory.save_session("s-a1", 12, [{"role": "user", "content": "第一个会话"}])
        memory.save_session("s-a2", 12, [{"role": "user", "content": "第二个会话"}])
        memory.save_session("s-b1", 13, [{"role": "user", "content": "第三个会话"}])
        memory.save_session("s-anon", None, [{"role": "user", "content": "未登录会话"}])

        assert len(memory.list_sessions("12")) == 2, "user12 会话数不对"
        assert len(memory.list_sessions("13")) == 1, "user13 会话数不对"
        assert {s["session_id"] for s in memory.list_sessions("12")} == {"s-a1", "s-a2"}, "user12 列表串号"
        assert [s["session_id"] for s in memory.list_sessions(None)] == ["s-anon"], "未登录用户列表不对"
        assert {s["title"] for s in memory.list_sessions("12")} == {"第一个会话", "第二个会话"}, "列表标题不对"

        # 越权删别人的 → forbidden，且不改变任何数据
        r = memory.delete_session("s-b1", "12")
        assert r["ok"] is False and r["reason"] == "forbidden", f"越权删除未被拒: {r}"
        assert len(memory.list_sessions("13")) == 1, "越权删除动了别人的会话"
        # 删不存在的 → not_found
        assert memory.delete_session("nope", "12")["reason"] == "not_found"

        # 删自己的 → 成功 + 列表收缩 + 历史物理消失
        assert memory.delete_session("s-a1", "12")["ok"] is True
        assert [s["session_id"] for s in memory.list_sessions("12")] == ["s-a2"], "删除后列表未收缩"
        assert memory.get_history("s-a1") == [], "物理删除后历史仍在"
        # 未登录用户能删自己的
        assert memory.delete_session("s-anon", None)["ok"] is True, "未登录用户删自己的会话失败"
        print("PASS: 会话归属过滤 + 越权删除被拒 + 物理删除生效")
    finally:
        tmp.cleanup()
        _restore_memory()


if __name__ == "__main__":
    test_tool_schemas_valid()
    test_compact_product()
    test_tool_calling_loop()
    test_cart_pet_tools_schemas()
    test_memory_roundtrip()
    test_memory_dedupe_and_cap()
    test_memory_forget()
    test_memory_injection_and_local_tools()
    test_session_title()
    test_session_ownership()
    print("\n全部自检通过 [OK]")
