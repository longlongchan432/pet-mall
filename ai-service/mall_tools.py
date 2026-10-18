"""工具层：Agent 通过 Function Calling 调用的商城工具，底层 HTTP 调 Spring Boot 接口。"""
import httpx
from config import MALL_API_BASE

_TIMEOUT = httpx.Timeout(8.0)


def _compact_product(p: dict, with_desc: bool = False) -> dict:
    """裁剪商品字段，列表场景不带长描述，控制 token。"""
    out = {
        "id": p.get("id"),
        "name": p.get("name"),
        "price": p.get("price"),
        "sales": p.get("sales"),
        "stock": p.get("stock"),
        "categoryName": p.get("categoryName"),
        "image": p.get("image"),
    }
    if with_desc:
        out["description"] = (p.get("description") or "")[:300]
    return out


async def _get(path: str, params: dict = None, token: str = None) -> dict:
    headers = {"Authorization": f"Bearer {token}"} if token else {}
    # trust_env=False：本机系统代理（Clash 等）会拦截 localhost 请求返回 503，
    # 商城接口是本地回环，必须绕过代理；DeepSeek 客户端不受此设置影响
    async with httpx.AsyncClient(timeout=_TIMEOUT, trust_env=False) as client:
        r = await client.get(f"{MALL_API_BASE}{path}", params=params, headers=headers)
        body = r.json()
    # 业务错误码必须抛出：否则 401 的 data=null 会被上层当成空列表，
    # 未登录查订单会被误报成"没有订单"而不是引导登录
    if body.get("code") != 200:
        raise RuntimeError(body.get("message") or f"HTTP {r.status_code}")
    return body


async def resolve_user_id(token: str):
    """用商城 token 换当前用户ID（长期记忆按用户隔离，不复制 JWT 密钥）。"""
    data = await _get("/user/info", token=token)
    u = data.get("data") or {}
    uid = u.get("id")
    return str(uid) if uid is not None else None


async def search_products(keyword: str) -> str:
    """按关键词搜索上架商品。"""
    data = await _get("/product/public/search", {"keyword": keyword})
    items = data.get("data") or []
    return {"count": len(items), "products": [_compact_product(p) for p in items[:10]]}


async def get_product_detail(product_id: int) -> str:
    """获取单个商品的详细信息（含描述、库存、图片）。"""
    data = await _get(f"/product/public/{product_id}")
    p = data.get("data")
    if not p:
        return {"error": "商品不存在"}
    return _compact_product(p, with_desc=True)


async def recommend_hot_products(category_id: int = None) -> str:
    """按销量推荐热门商品（可限定分类），返回销量最高的 5 件。"""
    params = {"status": 1}
    if category_id is not None:
        params["categoryId"] = category_id
    data = await _get("/product/public/list", params)
    items = data.get("data") or []
    items.sort(key=lambda p: p.get("sales") or 0, reverse=True)
    return {"products": [_compact_product(p) for p in items[:5]]}


async def get_my_orders(token: str, status: int = None) -> str:
    """查询当前登录用户的订单列表。status: 0待支付 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款。"""
    # /order/list 不支持 status 参数，拉全量后本地过滤
    data = await _get("/order/list", token=token)
    orders = data.get("data") or []
    if isinstance(orders, dict):  # 容错
        orders = []
    if status is not None:
        orders = [o for o in orders if o.get("status") == status]
    return {
        "count": len(orders),
        "orders": [
            {
                "id": o.get("id"),
                "orderNo": o.get("orderNo"),
                "status": o.get("status"),
                "totalAmount": o.get("totalAmount"),
                "createTime": o.get("createTime"),
                "items": [
                    {"name": it.get("productName"), "quantity": it.get("quantity")}
                    for it in (o.get("orderItems") or [])
                ],
            }
            for o in orders[:10]
        ],
    }


async def get_order_detail(token: str, order_id: int) -> str:
    """获取当前用户某个订单的详情（含物流、收货信息）。"""
    data = await _get(f"/order/{order_id}", token=token)
    o = data.get("data")
    if not o:
        return {"error": data.get("message") or "订单不存在或无权查看"}
    return {
        "id": o.get("id"),
        "orderNo": o.get("orderNo"),
        "status": o.get("status"),
        "totalAmount": o.get("totalAmount"),
        "address": o.get("address"),
        "receiverName": o.get("receiverName"),
        "receiverPhone": o.get("receiverPhone"),
        "logisticsCompany": o.get("logisticsCompany"),
        "logisticsNo": o.get("logisticsNo"),
        "createTime": o.get("createTime"),
        "payTime": o.get("payTime"),
        "items": [
            {"name": it.get("productName"), "quantity": it.get("quantity"), "price": it.get("price")}
            for it in (o.get("orderItems") or [])
        ],
    }


# ---- 购物车 ----
async def add_to_cart(token: str, product_id: int, quantity: int = 1) -> str:
    """将商品加入购物车（不扣款）。"""
    if quantity < 1:
        quantity = 1
    try:
        async with httpx.AsyncClient(timeout=_TIMEOUT, trust_env=False) as client:
            resp = await client.post(
                f"{MALL_API_BASE}/cart/add",
                json={"productId": product_id, "quantity": quantity},
                headers={"Authorization": f"Bearer {token}"},
            )
            resp.raise_for_status()
            result = resp.json()
        if result.get("code") == 200:
            return {"success": True, "message": result.get("message", "已加入购物车")}
        raise RuntimeError(result.get("message") or f"HTTP {resp.status_code}")
    except RuntimeError:
        raise
    except Exception as e:
        raise RuntimeError(f"加购失败: {e}")


async def get_cart(token: str) -> str:
    """查询当前用户的购物车商品列表。"""
    data = await _get("/cart/list", token=token)
    items = data.get("data") or []
    return {
        "count": len(items),
        "items": [
            {
                "id": it.get("id"),
                "productId": it.get("productId"),
                "productName": it.get("productName"),
                "price": it.get("price"),
                "quantity": it.get("quantity"),
                "productImage": it.get("productImage"),
                "merchantName": it.get("merchantName"),
            }
            for it in items
        ],
    }


async def delete_cart_item(token: str, cart_id: int) -> str:
    """删除购物车中的一件商品。"""
    async with httpx.AsyncClient(timeout=_TIMEOUT, trust_env=False) as client:
        resp = await client.delete(
            f"{MALL_API_BASE}/cart/{cart_id}",
            headers={"Authorization": f"Bearer {token}"},
        )
        resp.raise_for_status()
        result = resp.json()
    if result.get("code") == 200:
        return {"success": True, "message": result.get("message", "已删除")}
    raise RuntimeError(result.get("message") or f"HTTP {resp.status_code}")


# ---- 宠物档案 ----
async def get_my_pet_profile(token: str) -> str:
    """查询当前用户的宠物档案。"""
    data = await _get("/pet-profile/list", token=token)
    items = data.get("data") or []
    return {
        "count": len(items),
        "profiles": [
            {
                "id": p.get("id"),
                "petType": p.get("petType"),
                "breed": p.get("breed"),
                "ageMonths": p.get("ageMonths"),
                "weightKg": p.get("weightKg"),
                "notes": p.get("notes"),
                "isPrimary": p.get("isPrimary"),
            }
            for p in items
        ],
    }


async def set_pet_profile(
    token: str,
    pet_type: str,
    breed: str = None,
    age_months: int = None,
    weight_kg: float = None,
    notes: str = None,
    is_primary: bool = None,
) -> str:
    """设置或更新用户的宠物档案。"""
    body = {
        "petType": pet_type,
        "breed": breed,
        "ageMonths": age_months,
        "weightKg": weight_kg,
        "notes": notes,
        "isPrimary": 1 if is_primary else 0,
    }
    async with httpx.AsyncClient(timeout=_TIMEOUT, trust_env=False) as client:
        resp = await client.post(
            f"{MALL_API_BASE}/pet-profile",
            json=body,
            headers={"Authorization": f"Bearer {token}"},
        )
        resp.raise_for_status()
        result = resp.json()
    if result.get("code") == 200:
        return {"success": True, "message": result.get("message", "宠物档案已保存")}
    raise RuntimeError(result.get("message") or f"HTTP {resp.status_code}")


async def delete_pet_profile(token: str, pet_id: int) -> str:
    """删除宠物档案。"""
    async with httpx.AsyncClient(timeout=_TIMEOUT, trust_env=False) as client:
        resp = await client.delete(
            f"{MALL_API_BASE}/pet-profile/{pet_id}",
            headers={"Authorization": f"Bearer {token}"},
        )
        resp.raise_for_status()
        result = resp.json()
    if result.get("code") == 200:
        return {"success": True, "message": result.get("message", "已删除")}
    raise RuntimeError(result.get("message") or f"HTTP {resp.status_code}")


# ---- Function Calling 工具声明（OpenAI 格式）----
TOOL_SCHEMAS = [
    {
        "type": "function",
        "function": {
            "name": "search_products",
            "description": "按关键词搜索商城中的商品（如狗粮、猫砂、牵引绳等）。用户想找某类商品时使用。",
            "parameters": {
                "type": "object",
                "properties": {"keyword": {"type": "string", "description": "搜索关键词"}},
                "required": ["keyword"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_product_detail",
            "description": "查询某个商品的详细信息（描述、库存、价格）。用户询问具体商品时使用。",
            "parameters": {
                "type": "object",
                "properties": {"product_id": {"type": "integer", "description": "商品ID"}},
                "required": ["product_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "recommend_hot_products",
            "description": "推荐销量最高的热门商品，可限定商品分类。用户想要推荐/不知道买什么时使用。",
            "parameters": {
                "type": "object",
                "properties": {"category_id": {"type": "integer", "description": "可选，分类ID"}},
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "recommend_products",
            "description": (
                "【定稿/锁单工具】向用户推荐或列举具体商品前必须调用。"
                "提交本轮用 search_products / recommend_hot_products / get_product_detail "
                "查到的真实商品ID（最多6个，按推荐优先级排序）。系统校验后回传这些商品的"
                "权威信息（名称、价格等必须照抄），并向用户展示商品卡片。"
                "禁止提交未在查询结果中出现的ID。"
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "product_ids": {
                        "type": "array",
                        "items": {"type": "integer"},
                        "description": "本轮已查到的真实商品ID列表，按推荐优先级排序，最多6个",
                    }
                },
                "required": ["product_ids"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_my_orders",
            "description": "查询当前登录用户自己的订单。用户问“我的订单/买了什么/发货了吗”时使用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "status": {
                        "type": "integer",
                        "description": "订单状态筛选：0待支付 1待发货 2待收货 3已完成 4已取消 5退款中 6已退款，不传则查全部",
                    }
                },
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_order_detail",
            "description": "查询用户某个订单的详情，含物流单号、收货地址、商品明细。需要订单ID。",
            "parameters": {
                "type": "object",
                "properties": {"order_id": {"type": "integer", "description": "订单ID"}},
                "required": ["order_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "add_to_cart",
            "description": "将商品加入购物车（不扣款、不生成订单）。用户说「加入购物车/放进购物车」时使用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "product_id": {"type": "integer", "description": "商品ID"},
                    "quantity": {"type": "integer", "description": "数量（默认1）", "default": 1},
                },
                "required": ["product_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_cart",
            "description": "查询当前用户的购物车商品列表。用户问「我购物车里有什么」时使用。",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "delete_cart_item",
            "description": "从购物车中删除一件商品。用户说「把XX从购物车删掉」时使用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "cart_id": {"type": "integer", "description": "购物车商品ID（从 get_cart 获取）"},
                },
                "required": ["cart_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "get_my_pet_profile",
            "description": "查询当前用户的宠物档案。推荐商品前应先查此工具了解用户养了什么宠物，以提供更精准推荐。",
            "parameters": {"type": "object", "properties": {}},
        },
    },
    {
        "type": "function",
        "function": {
            "name": "set_pet_profile",
            "description": "设置或更新用户的宠物档案。用户主动提供宠物信息（如「我养了一只金毛犬，2岁」）时调用。",
            "parameters": {
                "type": "object",
                "properties": {
                    "pet_type": {"type": "string", "description": "宠物类型: dog(狗)/cat(猫)/bird(鸟)/fish(鱼)/other(其他)"},
                    "breed": {"type": "string", "description": "品种"},
                    "age_months": {"type": "integer", "description": "年龄（月）"},
                    "weight_kg": {"type": "number", "description": "体重（kg）"},
                    "notes": {"type": "string", "description": "备注（如过敏、特殊饮食）"},
                    "is_primary": {"type": "boolean", "description": "是否主要宠物"},
                },
                "required": ["pet_type"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "delete_pet_profile",
            "description": "删除宠物档案。",
            "parameters": {
                "type": "object",
                "properties": {
                    "pet_id": {"type": "integer", "description": "宠物档案ID（从 get_my_pet_profile 获取）"},
                },
                "required": ["pet_id"],
            },
        },
    },
    {
        "type": "function",
        "function": {
            "name": "save_memory",
            "description": "把值得长期记住的用户信息存下来（跨会话保留）。例如：用户的称呼、宠物名字、过敏源、口味/品牌偏好、预算习惯、常用城市。只记长期有效的事实；一次性问题（如“今天想看猫砂”）不要记。宠物的类型/品种/年龄/体重属于结构化信息，请用 set_pet_profile，不要重复记到这里。",
            "parameters": {
                "type": "object",
                "properties": {
                    "fact": {"type": "string", "description": "一条简短的事实，一句话，不超过50字"}
                },
                "required": ["fact"]
            }
        },
    },
    {
        "type": "function",
        "function": {
            "name": "forget_memory",
            "description": "删除已保存的长期记忆。用户说“忘掉/别记着某条信息”时按关键词删除；用户说“全部忘掉/清空记忆”时传 all=true。",
            "parameters": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string", "description": "要删除的记忆关键词（模糊匹配）"},
                    "all": {"type": "boolean", "description": "true = 清空该用户全部记忆"}
                }
            }
        },
    },
]

# 名称 → 实现。需要登录的工具通过 token 闭包注入。
def build_tool_map(token: str) -> dict:
    return {
        "search_products": search_products,
        "get_product_detail": get_product_detail,
        "recommend_hot_products": recommend_hot_products,
        "recommend_products": lambda **kw: None,  # 由 agent.py 本地执行，不走 HTTP
        "get_my_orders": lambda **kw: get_my_orders(token, **kw),
        "get_order_detail": lambda order_id: get_order_detail(token, order_id),
        "add_to_cart": lambda **kw: add_to_cart(token, **kw),
        "get_cart": lambda: get_cart(token),
        "delete_cart_item": lambda cart_id: delete_cart_item(token, cart_id),
        "get_my_pet_profile": lambda: get_my_pet_profile(token),
        "set_pet_profile": lambda **kw: set_pet_profile(token, **kw),
        "delete_pet_profile": lambda pet_id: delete_pet_profile(token, pet_id),
        "save_memory": lambda **kw: None,  # 由 agent.py 本地执行，不走 HTTP
        "forget_memory": lambda **kw: None,  # 由 agent.py 本地执行，不走 HTTP
    }
