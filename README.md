# 宠旅驿站 - 宠物用品电商系统

一个集「用户商城 + 商家管理 + 人工审核 + AI 导购」于一体的宠物用品电商平台，采用前后端分离架构，4 个后端服务共享 1 个 MySQL 数据库。

## 架构总览

```
┌─────────────┐  ┌─────────────┐  ┌──────────────┐  ┌────────────┐
│  前端商城    │  │  后台管理    │  │  AI 导购服务 │  │  后端 API  │
│  Vue 2.6    │  │  Vue 2.6    │  │  FastAPI     │  │  Spring    │
│  :8081      │  │  :8083      │  │  :8082       │  │  Boot      │
└──────┬──────┘  └──────┬──────┘  └──────┬───────┘  │  :8080   │
       │                │                │          └────┬─────┘
       └────────────────┴────────────────┴───────────────┘
                                              │
                                        ┌─────▼─────┐
                                        │  MySQL 8  │
                                        │ pet_mall  │
                                        └───────────┘
```

| 服务 | 技术栈 | 端口 | 说明 |
|------|--------|------|------|
| 后端 API | Java Spring Boot 2.7.18 + JDK 1.8 | 8080 | RESTful 业务接口，context-path `/api` |
| 前端商城 | Vue 2.6 + Element UI + Vuex | 8081 | 面向消费者（C 端）的购物商城 |
| 后台管理 | Vue 2.6 + Element UI + ECharts | 8083 | 面向商家和平台管理员 |
| AI 导购 | Python FastAPI + DeepSeek LLM | 8082 | 基于 Function Calling 的智能推荐对话 |
| 数据库 | MySQL 8 | 3306 | 共享 pet_mall 数据库 |
| 图片文件服务器 | Ubuntu 26.04 + nginx | 虚拟机上 80 | 上传图存这里：后端 SFTP 写，nginx 直出 `/upload/`；URL 仍是相对路径 `/upload/<uuid>.png` |

> 文件服务器是可选依赖：`FILE_STORAGE_TYPE=local` 时图片存本机 `petmall/upload/`，其余服务照常跑（见「快速启动 · 后端 API」）。

## 功能清单

### C 端（前端商城）

| 功能 | 说明 |
|------|------|
| 用户认证 | 注册 / 登录 / JWT Token |
| 商品浏览 | 首页、分类筛选、商品详情、搜索 |
| 购物车 | 添加商品、修改数量、删除、清空 |
| 下单购买 | 使用账户余额支付（余额充值→审核→支付下单） |
| 订单管理 | 订单列表、详情、物流跟踪 |
| 账户管理 | 个人信息、修改密码、查看余额 |
| 宠物档案 | 设置宠物类型/品种/年龄/体重，AI 导购据此个性化推荐 |
| 评价系统 | 对已完成订单进行评价，评价需审核后方可展示 |

### 商家端（后台管理）

| 功能 | 说明 |
|------|------|
| 商品管理 | 商品列表、新增、编辑、上下架 |
| 订单管理 | 查看订单、发货操作 |

### 管理员端（后台管理）

| 功能 | 说明 |
|------|------|
| 充值审核 | 审核用户充值申请（通过/拒绝） |
| 评价审核 | 审核用户评价（通过/拒绝） |
| 用户管理 | 查看所有用户列表 |
| 商家管理 | 查看商家列表 |
| 商品管理 | 全平台商品查看 |
| 订单管理 | 全平台订单查看 |
| 分类管理 | 商品分类增删改查 |
| 数据统计 | ECharts 数据可视化仪表盘 |

### AI 导购（对话机器人）

| 功能 | 说明 |
|------|------|
| 商品搜索 | 按关键词搜索真实商品 |
| 热门推荐 | 按品类推荐热门商品 |
| 宠物档案推荐 | 根据用户宠物档案给出个性化推荐 |
| 商品对比 | 逐项对比两款商品的规格、价格、销量等 |
| 购物车操作 | 将商品加入购物车 / 查看购物车 / 删除商品 |
| 订单查询 | 查询历史订单和物流状态 |

## 数据库

```sql
-- 建表
mysql -u root -p < sql/petmall.sql
```

共 10 张表：

| 表名 | 说明 |
|------|------|
| `user` | 用户（含普通用户、商家、管理员） |
| `category` | 商品分类 |
| `product` | 商品 |
| `cart` | 购物车 |
| `order` | 订单 |
| `order_item` | 订单明细 |
| `payment` | 支付记录 |
| `recharge_record` | 充值记录（审核流） |
| `review` | 商品评价（审核流） |
| `pet_profile` | 宠物档案（AI 推荐数据） |

## 快速启动

### 前置条件

- JDK 1.8+（后端）
- Node.js 14+（前端 / 管理后台）
- Python 3.10+（AI 服务）
- MySQL 8
- Docker + Docker Compose（只有走容器化部署时才需要，本地开发用不到）

### 1. 数据库

```bash
mysql -u root -p < sql/petmall.sql
```

修改后端配置 `backend/src/main/resources/application.yml` 中的 MySQL 连接信息（如需改账号密码）：

```yaml
spring:
  datasource:
    username: root
    password: your_mysql_password   # 示例占位，部署前请改为独立强密码，切勿使用弱密码或与账号密码相同
    url: jdbc:mysql://localhost:3306/pet_mall?useSSL=false&characterEncoding=utf8
```

### 2. 后端 API

```bash
cd backend
# 使用 Maven 编译并启动
mvn spring-boot:run
# 或 IDE 直接运行 PetTravelStationApplication.java
```

默认启动在 `http://localhost:8080`，上下文路径 `/api`。

**上传图片的存储**默认走 Ubuntu 文件服务器（SFTP 写 + 该机 nginx 直出 `/upload/`），所以启动前要给它凭据，否则会**启动即失败**（`SftpImageStore` 启动自检，报错里点名缺哪个变量）：

```bash
export FILE_SFTP_USER=petmall
export FILE_SFTP_KEY=/path/to/petmall_rsa          # 必须是 RSA 私钥，ed25519 在 jsch 上会 Auth fail
export FILE_SFTP_KNOWN_HOSTS=/path/to/known_hosts  # ssh-keyscan -H <IP> > known_hosts；留空会有中间人 WARN
mvn spring-boot:run
```

没有那台 Ubuntu 时，切回本地目录即可（`file.upload.path`，默认 `../petmall/upload/`）：

```bash
FILE_STORAGE_TYPE=local mvn spring-boot:run
```

细节与验证记录见 `docs/upload-remote-storage-v1.md`。

### 3. 前端商城

```bash
cd frontend
npm install
npm run dev
# http://localhost:8081
```

### 4. 后台管理

```bash
cd admin
npm install
npm run dev
# http://localhost:8083
```

### 5. AI 导购服务

```bash
cd ai-service
pip install -r requirements.txt

# 创建 .env 文件
cat > .env << EOF
DEEPSEEK_API_KEY=your_key_here
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
EOF

# 启动
uvicorn main:app --host 127.0.0.1 --port 8082 --reload   # 本地开发仅监听本机，避免 AI 服务暴露到局域网 / 公网
# 或 Windows 用 run.bat
# http://localhost:8082
```

## 端口汇总

| 服务 | 端口 | URL |
|------|------|-----|
| 后端 API | 8080 | http://localhost:8080/api |
| 前端商城 | 8081 | http://localhost:8081 |
| AI 服务 | 8082 | http://localhost:8082 |
| 管理后台 | 8083 | http://localhost:8083 |

## 容器化部署（Ubuntu 虚拟机）

四端 + MySQL 用 Docker Compose 一次起在同一台 Ubuntu 机器上，端口与本地开发完全一致（8081/8083/8080/8082）。
同机部署时图片走共享目录 `/srv/petmall/upload`（不再需要 SFTP），老图与数据库里的相对路径都不用改。

```bash
cp .env.example .env      # 填 DEEPSEEK_API_KEY
docker compose up -d --build
```

完整步骤、验收清单与 22 条已预判的坑 → [docs/docker-deploy-ubuntu-v1.md](docs/docker-deploy-ubuntu-v1.md)

## 种子账号（示例，仅本地演示用）

| 用户名 | 密码 | 角色 | 用途 |
|--------|------|------|------|
| admin | admin123 | 管理员（2） | 后台管理 / 审核 |
| merchant1 | merchant123 | 商家（1） | 商家管理 / 发布商品 |
| user1~user5 | petmallDemo2026 | 普通用户（0） | C 端购物测试 |

> ⚠️ **安全提示**：以上为示例种子账号与弱密码，**仅用于本地开发 / 演示**。本仓库已设为公开，部署到任何可访问的环境前，请务必：
> 1. 修改所有默认密码（含数据库账号，见上方 `application.yml` 配置）；
> 2. 同步更新 `sql/petmall.sql` 种子数据中的密码，不要使用真实密码或弱密码；
> 3. 不要在文档 / 代码 / 配置中保留任何真实凭据。

## 关键业务流程

### 订单状态机

```
0 待支付 → 1 待发货 → 2 待收货 → 3 已完成
            ↘ 4 已取消（超时/用户取消）
            ↘ 5 退款中 → 6 已退款
```

### 充值审核流

```
用户提交充值 → 状态 0(待审核) → 管理员审核
                                    ├→ 状态 1(通过)：余额增加
                                    └→ 状态 2(拒绝)：余额不变
```

### 评价审核流

```
用户提交评价 → 状态 pending → 管理员审核
                                    ├→ 状态 passed：前端可见
                                    └→ 状态 rejected：前端不可见
```

### AI 推荐安全机制

AI 服务采用三重防编造机制：

1. **定稿锁单**：模型必须通过 `recommend_products` 工具勾选商品 ID，系统校验 ID 仅在真实查询结果中才出卡
2. **输出口兜底**：正则截断模型可能复读的内部附注
3. **ID 文本兜底**：从模型回答中提取商品 ID 时仅匹配当前轮次已查到的真实商品

## 稳定性优化（并发与安全）

针对真实缺陷做了修复，均已在真实后端验证：

| 缺陷 | 修复 |
|------|------|
| 并发下单超卖（读库存与扣减非原子） | `updateStock` 改为条件更新 `SET stock = stock + #{quantity} WHERE id = #{id} AND stock + #{quantity} >= 0`，受影响行数为 0 即库存不足，抛异常回滚 |
| 待支付订单永不超时取消，库存长期占用 | `@EnableScheduling` 每 60s 扫描创建超过 30 分钟（`order.pay.timeout-minutes`）的待支付订单，取消并回补库存；支付、取消也用条件更新判行数，避免与用户操作撞车导致重复回补 |
| 上传无类型校验，可传 `.html`/`.svg` 造成存储型 XSS | 后缀白名单（png/jpg/jpeg/gif/webp/bmp）+ 前 12 字节图片头校验（PNG/JPEG/GIF/WEBP/BMP） |
| 图片只存后端本机磁盘：多实例各存各的，后端要背图片带宽 | 抽出 `ImageStore`（`local` / `sftp` 两个实现）：后端 SFTP 写到 Ubuntu 文件服务器，图片流量由该机 nginx 直出 `/upload/`；URL 契约不变，数据库 0 迁移，8 处组件只把硬编码的后端域名改成相对路径 |
| 后台商品图预览一律 404（`admin/vue.config.js` 缺 `/upload` 代理） | 补 `/upload` 代理指向文件服务器；8 处 `getImageUrl` 原来拼死 `http://localhost:8080/api`，改为相对路径（sftp 模式下后端不再直出 `/upload`，拼后端域名必 404） |
| **上线回归**：商家端传图失败（返回 nginx 404 页） | `admin/vue.config.js` 的 `/upload` 代理写在了 `/api` 之前——本版 http-proxy-middleware 按路径**包含**匹配，`/api/file/upload` 含 `/upload`，被抢去转发给 nginx。`/api` 提前即可（`frontend/vue.config.js` 同样调整） |
| 单文件默认 1MB 限制与前端「不超过 2MB」提示不符，且超限返回 401/500 堆栈 | `max-file-size: 5MB` + `resolve-lazily: true`（否则异常转发到 `/error` 被登录拦截器盖成 401）+ `/error` 放行 + `@ExceptionHandler` 返回「图片过大，单张不要超过 5MB」 |

验证结果：8 个并发请求抢 1 件库存 → 成功 1 / 失败 7，库存未变负；回退订单创建时间 → 订单自动转为已取消且库存回补；`.txt` 与伪装成 `.png` 的文本被拒，真实图片正常上传；真图上传后本地目录不增、远端出现该文件、nginx 取回 md5 一致，6MB 图返回友好提示，远端不可达返回友好提示且不落本地；商家真实链路（带 token 经 `:8083` 代理 `POST /api/file/upload`）返回 200，新旧图片经 `:8081`/`:8083` 代理均为 200 `image/png`。**教训**：验证要照用户真实点击路径跑（上传接口走 dev 代理、图片走组件拼出来的 URL），只 curl 自己改的那条路会漏掉代理抢路和组件硬编码域名这两类回归。

回归测试：`backend/src/test/java/com/petmall/service/impl/OrderServiceImplTest.java`（3 个用例，纯 Mockito 不连库）与 `backend/src/test/java/com/petmall/storage/ImageStoreTest.java`（4 个用例），运行 `cd backend && mvn test`。

## 目录结构

```
├── backend/              # Spring Boot 后端
│   ├── src/main/java/com/petmall/
│   │   ├── controller/   # REST 控制器
│   │   ├── entity/       # 实体类
│   │   ├── mapper/       # MyBatis-Plus Mapper
│   │   ├── service/      # 业务逻辑
│   │   ├── common/       # 拦截器、JWT、Result 响应封装
│   │   └── config/       # Web 配置
│   ├── src/main/resources/
│   │   ├── mapper/       # MyBatis XML
│   │   └── application.yml
│   └── pom.xml
├── frontend/             # Vue 2 前端商城
│   └── src/
│       ├── api/          # Axios 封装 + 接口
│       ├── router/       # 路由
│       ├── store/        # Vuex 状态
│       └── views/        # 页面组件
├── admin/                # Vue 2 后台管理
│   └── src/
│       ├── api/
│       ├── router/
│       ├── store/
│       └── views/
├── ai-service/           # Python AI 导购
│   ├── agent.py          # Function Calling 推理引擎
│   ├── main.py           # FastAPI 入口 + SSE 流式
│   ├── mall_tools.py     # 商城工具定义 + Schema
│   └── config.py         # 环境变量
├── sql/                  # 数据库
│   └── petmall.sql       # 建表 + 种子数据
└── README.md
```

## AI 导购工具清单

| 工具 | 说明 | 调用方式 |
|------|------|----------|
| `search_products(keyword)` | 按关键词搜索商品 | HTTP 调用后端 |
| `get_product_detail(product_id)` | 查询商品详情 | HTTP 调用后端 |
| `recommend_hot_products(category_id?)` | 推荐热门商品 | 本地执行 |
| `recommend_products(product_ids)` | 定稿推荐商品 | 本地校验执行 |
| `get_my_orders(token, status?)` | 查询用户订单 | HTTP 调用后端 |
| `get_order_detail(token, order_id)` | 订单详情 | HTTP 调用后端 |
| `add_to_cart(product_id, quantity)` | 加入购物车 | HTTP 调用后端 |
| `get_cart()` | 查看购物车 | HTTP 调用后端 |
| `delete_cart_item(cart_id)` | 删除购物车项 | HTTP 调用后端 |
| `get_my_pet_profile(token)` | 获取宠物档案 | HTTP 调用后端 |
| `set_pet_profile(token, ...)` | 保存宠物档案 | HTTP 调用后端 |
| `delete_pet_profile(token, pet_id)` | 删除宠物档案 | HTTP 调用后端 |
| `save_memory(fact)` | 记住用户长期偏好/事实（跨会话） | 本地执行 |
| `forget_memory(keyword?, all?)` | 删除用户长期记忆 | 本地执行 |

### AI 导购记忆

AI 导购带两层持久记忆（设计文档：`docs/ai-memory-v1.md`）：

- **L1 会话记忆**：多轮对话落盘到 `ai-service/data/memory.json`（原子写），重启服务或刷新页面后上下文不丢；`GET /api/chat/history` 供前端回放（已过滤内部附注）。
- **L2 用户长期记忆**：模型在对话中主动调用 `save_memory` 记录跨会话事实（宠物名、过敏、品牌偏好等），每轮自动注入；每用户上限 20 条，支持去重、关键词删除和一键清空（`GET /api/memory/list`、`DELETE /api/memory/clear`，Bearer 商城 token 鉴权）。
- 宠物类型/品种/年龄/体重等结构化信息仍走 MySQL 宠物档案（L3），不与 L2 重复。
- 存储采用单文件 JSON（零新依赖），单进程有效；多实例部署时只需把 `memory.py` 存储后端换成 Redis/MySQL，上层接口不变。
- **会话管理**：一键新建会话、左侧可上下滑动的会话列表（标题自动从会话前三个问题摘要、附时间戳）、切回旧会话、物理删除（二次确认）。新建/切换只隔断 L1 会话上下文，L2/L3 保留。

