# 导购 Agent v1 新功能设计

> 日期: 2026-01-XX  
> 状态: 已确认，开始实施  
> 设计原则: 继承现有两条红线——① 工具即真相源（卡片/金额只能出自真实数据） ② 权限看 token、不信任模型

---

## 功能总览

| # | 功能 | 涉及层 | 一句话 |
|---|---|---|---|
| 1 | 加入购物车 | AI 端 + 后端 | 推荐完直接加购，不动余额不下单 |
| 2 | 宠物档案 | 前端 + 后端 + AI 端 | 用户告诉 AI "我养了只 2 岁金毛"，推荐从此精准 |
| 3 | 商品对比 | AI 端 | 问"A 和 B 哪个好"，AI 调两次详情后对比 |

---

## 一、加入购物车（AI 端）

### 1.1 需求

AI 导购推荐商品后，用户说"加一箱到购物车"，AI 直接调用 `add_to_cart` 工具加入购物车——不动余额、不触发支付、不下单。用户可随时在商城购物车页修改/删除。

**不做下单/支付**。下单是 money path，后续另做。

### 1.2 新增后端 API

```
POST /cart/add    — 增加购物车商品（商品ID + 数量）
GET  /cart/list   — 获取购物车列表（已有？若无则新建）
DELETE /cart/{id} — 删除购物车项（可选，V1 先不做）
```

- 必须登录（`AuthInterceptor` 注入 `userId`）。
- `cart` 表已存在（`CartItem` 实体 + `CartController` + `CartService`），但需要检查是否已有 `add` / `list` 接口。

### 1.3 AI 工具

```json
{
  "name": "add_to_cart",
  "description": "将商品加入购物车（不扣款）。商品必须来自本轮搜索/推荐结果。",
  "parameters": {
    "type": "object",
    "properties": {
      "product_id": {"type": "integer", "description": "商品ID"},
      "quantity": {"type": "integer", "default": 1, "description": "数量，最小 1"}
    },
    "required": ["product_id"]
  }
}
```

### 1.4 安全约束

- 商品 ID 必须来自本轮真实工具结果（走 `recommend_products` 定稿池或 `search_products` 池），**绝不用模型报的 ID**。
- 数量由后端钳制（≥1, 不超过库存）。
- 不动余额、不下单——风险极低。

### 1.5 改动清单

| 文件 | 改动 |
|---|---|
| `backend/src/main/java/com/petmall/controller/CartController.java` | 新增 `POST /cart/add`、`GET /cart/list`（如现有无） |
| `backend/src/main/java/com/petmall/service/CartService.java` | 对应 service 实现 |
| `ai-service/mall_tools.py` | 新增 `add_to_cart(product_id, quantity)` 函数 |
| `ai-service/mall_tools.py` | `TOOL_SCHEMAS` 新增对应 schema |
| `ai-service/mall_tools.py` | `build_tool_map` 注册 |
| `ai-service/agent.py` | 在 `SYSTEM_PROMPT` 中告知模型何时可调用 `add_to_cart` |

---

## 二、宠物档案（前后端 + AI 端）

### 2.1 需求

用户通过对话或前端页面告诉 AI "我养了一只 2 岁的金毛/猫咪"，AI 记录结构化宠物档案。后续推荐自动考虑宠物类型、体型、年龄。

档案通过两种方式维护：
1. **对话式**：用户说"我养了只布偶猫"，AI 调用 `set_pet_profile` 记录。
2. **前端设置页**：`admin` 风格的管理面板或前端个人中心新增"我的宠物"页面。

### 2.2 数据结构

方案 A（推荐）：**独立表 `pet_profile`**

```sql
CREATE TABLE `pet_profile` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `pet_type` varchar(20) NOT NULL COMMENT '宠物类型: dog/cat/bird/fish/other',
  `breed` varchar(50) DEFAULT NULL COMMENT '品种',
  `age_months` smallint DEFAULT NULL COMMENT '年龄（月）',
  `weight_kg` decimal(5,2) DEFAULT NULL COMMENT '体重（kg）',
  `notes` varchar(255) DEFAULT NULL COMMENT '备注（如过敏、特殊饮食）',
  `is_primary` tinyint DEFAULT '0' COMMENT '是否主要宠物',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='宠物档案表';
```

方案 B（备选）：在 `user` 表加字段——但 user 表已有很多字段，且可能有多个宠物，独立表更灵活。

### 2.3 后端 API

```
GET  /api/pet-profile          — 获取当前用户宠物档案列表
POST /api/pet-profile          — 新增/更新宠物档案
PUT  /api/pet-profile/{id}     — 修改
DELETE /api/pet-profile/{id}   — 删除
```

全部需登录，`userId` 从 `AuthInterceptor` 获取，严格校验 `userId` 匹配。

### 2.4 前端改动

- **用户商城前端** (`frontend/`)：在 `User.vue` 或新增页面添加"我的宠物"设置入口。
- **管理后台** (`admin/`)：可选——管理员可查看用户宠物档案（用于客户服务）。V1 不做。

### 2.5 AI 工具

```json
[
  {
    "name": "get_my_pet_profile",
    "description": "查询当前用户的宠物档案。推荐商品前必查，了解用户的养宠情况。",
    "parameters": {}
  },
  {
    "name": "set_pet_profile",
    "description": "设置或更新用户的宠物档案信息。当用户主动提供宠物信息时调用。",
    "parameters": {
      "type": "object",
      "properties": {
        "pet_type": {"type": "string", "description": "宠物类型: dog/cat/bird/fish/other"},
        "breed": {"type": "string", "description": "品种，如 金毛/布偶猫"},
        "age_months": {"type": "integer", "description": "年龄（月）"},
        "weight_kg": {"type": "number", "description": "体重（kg）"},
        "notes": {"type": "string", "description": "备注"}
      }
    }
  },
  {
    "name": "delete_pet_profile",
    "description": "删除宠物档案。",
    "parameters": {
      "type": "object",
      "properties": {
        "pet_id": {"type": "integer", "description": "档案ID"}
      },
      "required": ["pet_id"]
    }
  }
]
```

### 2.6 Agent 集成

- `SYSTEM_PROMPT` 新增：推荐商品前必须先调 `get_my_pet_profile`，根据宠物类型/年龄推荐对应商品。
- `SYSTEM_PROMPT` 新增：用户主动提供宠物信息时，调用 `set_pet_profile`。
- `mall_tools.py`：新增 3 个工具函数。
- 宠物档案属于"读用户私有数据"——必须 token 校验，后端严格隔离。

### 2.7 改动清单

| 文件 | 改动 |
|---|---|
| `sql/petmall.sql` | 新增 `pet_profile` 建表语句 |
| `backend/.../entity/PetProfile.java` | 新实体 |
| `backend/.../mapper/PetProfileMapper.java` | 新 Mapper |
| `backend/.../resources/mapper/PetProfileMapper.xml` | 新 XML |
| `backend/.../controller/PetProfileController.java` | CRUD API |
| `backend/.../service/PetProfileService.java` | 服务层 |
| `frontend/src/views/User.vue` (或新增) | "我的宠物"设置页 |
| `frontend/src/api/index.js` | pet-profile API 函数 |
| `frontend/src/router/index.js` | 路由 |
| `ai-service/mall_tools.py` | 3 个工具函数 + schema |
| `ai-service/agent.py` | SYSTEM_PROMPT 更新 |

---

## 三、商品对比（AI 端）

### 3.1 需求

用户问"A 和 B 哪个好"或"这两款猫粮有什么区别"，AI 自动获取两款商品的详情进行对比，输出结构化对比回复。

### 3.2 实现方式

**方案 A（优先）：仅 prompt + 复用现有工具**

ReAct 循环已支持连续调用多个工具。模型可以连续调两次 `get_product_detail`，拿到结果后输出对比。只需在 `SYSTEM_PROMPT` 中明确指导行为。

新增一个前端渲染组件——"对比卡片"（两个商品横排展示，逐项对比）。

**方案 B（备选）：新增 `compare_products` 工具**

如果 A 方案不够稳定（模型有时不主动调对比），可以新增专用工具：

```json
{
  "name": "compare_products",
  "description": "对比两款商品。调用前必须先分别调用 get_product_detail 获取两款商品的详细信息。",
  "parameters": {
    "type": "object",
    "properties": {
      "product_ids": {"type": "array", "items": {"type": "integer"}, "description": "要对比的商品ID列表（仅2个）"}
    },
    "required": ["product_ids"]
  }
}
```

这个工具只做前端渲染编排——从本轮候选池中取两个商品详情，回传给前端渲染对比卡片。不改变 AI 决策逻辑。

**V1 采用方案 A**。如果实际测试发现模型对比行为不可靠，再切换到方案 B。

### 3.3 前端改动

- `frontend/src/components/AiChat.vue`：新增"对比卡片"样式组件，两个商品横排，显示名称、价格、销量、核心卖点。

### 3.4 Agent 改动

- `SYSTEM_PROMPT` 新增对比指导：
  - 当用户要求对比时，先调 `get_product_detail` 获取每个商品的完整信息。
  - 从真实结果中提取对比维度（价格、销量、库存、描述），客观说明差异。
  - 不要编造商品属性。

### 3.5 改动清单

| 文件 | 改动 |
|---|---|
| `ai-service/agent.py` | `SYSTEM_PROMPT` 新增对比指导 |
| `frontend/src/components/AiChat.vue` | 新增对比卡片样式 |
| `frontend/src/assets/css/common.css` (或组件内) | 对比卡片 CSS |

---

## 四、实施顺序

| 阶段 | 内容 | 预计工作量 |
|---|---|---|
| 阶段 1 | 购物车（后端 API + AI 工具） | ~2h |
| 阶段 2 | 宠物档案（数据库 + 后端 + AI 工具） | ~3h |
| 阶段 3 | 商品对比（AI prompt + 前端） | ~1h |
| 阶段 4 | 前端宠物档案设置页 | ~2h |
| 阶段 5 | 全链路测试 | ~1h |

建议按 1→2→3→4→5 顺序，但前端部分（4）可与阶段 1 的 backend 并行。

---

## 五、测试策略

每个工具新增后，在 `selftest.py` 中增加对应单元测试：
- 购物车：模拟 `add_to_cart` 调用，断言调用后端正确 URL。
- 宠物档案：模拟 `get_my_pet_profile` / `set_pet_profile`，验证参数。

---

## 六、已知风险 & 备注

1. **购物车幂等**：同一商品多次加购应增加数量而非重复条目。
2. **宠物档案多宠物**：用户可能养多只宠物，`pet_profile` 表支持一对多。
3. **商品对比的 prompt 稳定性**：如果 ReAct 循环下模型不主动调对比，需升级到方案 B。
4. **所有写操作工具必须 token 校验**，权限看后端，不信任模型。
