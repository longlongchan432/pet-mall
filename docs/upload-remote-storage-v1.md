# 商品图片存储迁移到 Ubuntu 文件服务器 设计

> 日期: 2026-10-16
> 状态: **已实施并实测通过**（方案 C；代码、Ubuntu 侧、55 张历史图迁移、§五 全部用例均已完成，见 §七）
> 设计原则: 继承现有红线——① 上传内容仍然只认图片（后缀白名单 + 文件头校验，先验后落盘，这条不能因为换了存储就放松）② 写路径与读路径解耦（后端只管写，图片流量由 nginx 吃）③ **URL 契约不变**（数据库继续存 `/upload/<uuid>.<ext>`，老图不裂、前端组件 0 改动）

---

## 一、现状与缺口

| 环节 | 现状 | 位置 |
|---|---|---|
| 写 | 落本地磁盘 `../petmall/upload/` | `backend/src/main/java/com/petmall/controller/FileController.java:24,55-58`；配置 `backend/src/main/resources/application.yml:48` |
| 读 | Spring MVC 静态映射 `/upload/**` → `file:` URI | `backend/src/main/java/com/petmall/config/WebConfig.java:19-20,44-46` |
| 前端读 | dev 代理 `/upload` → `localhost:8080`，`pathRewrite` 成 `/api/upload` | `frontend/vue.config.js` |
| 后台读 | **没有 `/upload` 代理规则** → 后台商品图预览实际是 404（现存缺陷，本次顺手修根因） | `admin/vue.config.js` |
| 数据 | 数据库存相对路径 `/upload/<uuid>.png` | `sql/petmall.sql:30,138,206,276`（`product.product_image` / `category.image` / `review.images`） |
| 存量 | `petmall/upload/` 55 个文件、25MB，**未被 git 跟踪**（`git ls-files petmall/upload` = 0，不会污染仓库） | — |

**缺口**：图片与后端同机 → 后端换机器/多实例部署时图片不共享；上传目录没法统一备份；后端进程顺手把图片带宽也扛了。

---

## 二、选型：企业里真实怎么做

| 方案 | 后端改动 | 新增依赖 | 图片读路径 | 企业常见度 | 结论 |
|---|---|---|---|---|---|
| **A. SMB/NFS 挂载** Ubuntu 共享目录成本地盘 | `application.yml` **一行** | 0 | 仍由后端 8080 静态服务 | 中小项目内网共享偶尔用；**把 SMB 当 Web 静态目录在企业里少见**（权限耦合、单点、SMB 并发性能差、无法扩多机） | 备选（最省，见 2.3） |
| **B. 后端 SFTP 写 + SFTP 读回** | 新增 JSch + 两端代理 | 1 | 每张图都从 Ubuntu 流回 8080 再吐给浏览器 | 少见（等于自建一个更慢的静态服务器） | **不做** |
| **C. 后端 SFTP 写 + Ubuntu nginx 直出** ✅ | 新增 JSch + `ImageStore` 抽象 | 1 | nginx 直接吃图片流量，后端不背带宽 | **主流**：业务系统「写走后端、读走静态服务器/CDN」是标准姿势 | **采用** |
| **D. 对象存储**（MinIO / 阿里云 OSS / S3） | 新增 SDK | 1 SDK + 1 中间件 | 对象存储/CDN 直出 | 云上最主流 | **升级路径**，不在本次范围（要给演示环境多装一个中间件） |

### 2.1 为什么推荐 C

- 它把**「谁写」和「谁读」拆开了**：写是低频、要校验、要鉴权 → 留在后端；读是高频、纯静态 → 交给 nginx。B 的问题正是把这两件事混在一起。
- URL 契约不变 → 数据库 0 迁移、前端 11 处 `<img>` 0 改动、老图 0 裂。
- 与 D 只差一层：以后换对象存储时，`ImageStore` 加一个 `S3ImageStore` 实现即可，前端与 URL 方案几乎不动。

### 2.2 C 与「企业级」的差距（诚实声明）

差的是**对象存储**那层（多副本、签名 URL、天然 CDN）。本项目是演示体量，多装一个 MinIO 是为用不到的水平扩展付中间件税。见 §六 升级路径。

### 2.3 为什么不选 A（尽管它几乎不用改代码）

| 问题 | 说明 |
|---|---|
| 权限耦合 | Ubuntu 的 samba 账号 ← Windows 挂载 ← 后端进程，三处权限任一改动都会让上传静默失败 |
| 单点与性能 | SMB 不适合高并发小文件读；nginx 挂在网络文件系统上，缓存与 `sendfile` 优势全废 |
| 无法扩多机 | 挂载点必须每台后端都配一遍，与「多实例共享图片」的初衷相悖 |
| 结论 | 若你只图省事、且这台 Ubuntu 就是唯一后端宿主机，A 可用；否则 C 更接近真实项目 |

---

## 三、目标架构

```
                    写（低频、带校验、带 JWT）
  admin 8083 ──POST /api/file/upload──► Spring Boot 8080 ──SFTP:22──► /srv/petmall/upload/
                                          (ImageStore: sftp)              │
                    读（高频、纯静态）                                      │
  浏览器 ──GET /upload/<uuid>.png──► nginx:80 ──alias───────────────────┘
     ▲                                     ▲
     └─ dev: frontend 8081 / admin 8083 代理 /upload → http://<UBUNTU_IP>:80
        prod: 前端部署的 nginx 反代 /upload/ → 文件服务器（同源相对路径不变）
```

---

## 四、实现

### 4.1 依赖（1 个，纯 Java，零传递依赖）

```xml
<dependency>
    <groupId>com.github.mwiede</groupId>
    <artifactId>jsch</artifactId>
    <version>0.2.25</version>
</dependency>
```

- mwiede/jsch 是 JSch 的维护分支（经典 `com.jcraft:jsch:0.1.55` 已停维护、有历史 CVE）。
- 已核对：`jsch-0.2.25.jar` 的 class 文件版本 **52 = Java 8**，本机 JDK 1.8.0_502 可直接跑；其 POM **无 `<dependencies>`**，不引入任何传递依赖。

### 4.2 配置

`backend/src/main/resources/application.yml`：

```yaml
spring:
  servlet:
    multipart:
      # 当前未配置 → Spring 默认单文件 1MB，而前端提示"不超过2MB"：1~2MB 的图现在就会被拒
      max-file-size: 5MB
      max-request-size: 10MB
      # 关键：懒解析。超限时异常才在 Controller 里抛出，被 @ExceptionHandler 接住返回友好提示；
      # 不懒解析时 Tomcat 在进 Controller 之前就抛，异常转发到 /error，会被登录拦截器盖成 401（实测踩过）
      resolve-lazily: true

# 文件上传配置
file:
  storage:
    type: ${FILE_STORAGE_TYPE:sftp}   # sftp | local（local = 无虚拟机也能起后端，见 4.3）
  upload:
    path: ../petmall/upload/          # 仅 local 模式使用，保留为离线回退
  sftp:
    host: ${FILE_SFTP_HOST:}
    port: ${FILE_SFTP_PORT:22}
    username: ${FILE_SFTP_USER:}
    private-key: ${FILE_SFTP_KEY:}            # 服务账号私钥路径；与 password 二选一，私钥优先
    password: ${FILE_SFTP_PASSWORD:}          # 只从环境变量注入，不入库、不进 git
    remote-dir: ${FILE_SFTP_DIR:/srv/petmall/upload}
    known-hosts: ${FILE_SFTP_KNOWN_HOSTS:}    # 留空=不校验主机指纹（仅开发），生产必须填
```

- 凭据注入：IDEA Run Configuration 的环境变量，或本地 `.env`（根 `.gitignore` 目前是**空文件**，本次补上 `.env` / `application-local.yml` / `.ssh/`）。
- **私钥必须用 RSA**：实测 mwiede `jsch 0.2.25` 拿 OpenSSH ed25519 私钥做 SFTP 会 `Auth fail for methods 'publickey,password'`（同一把钥匙 `ssh -i` 能正常登录），换成 `ssh-keygen -t rsa -b 3072` 后立刻通过。换密钥后一定要实测一次上传，别只看 `ssh` 能登。
- `known-hosts` 填了 → `StrictHostKeyChecking=yes` + `UserKnownHostsFile`；留空 → `no`，并在启动时打一条 WARN 日志点名这是 MITM 风险（给旋钮 + 明说，不静默降级）。生成：`ssh-keyscan -H <IP> > .ssh/known_hosts`。

### 4.3 代码：最小改动（4 个文件）

| 文件 | 改动 |
|---|---|
| `backend/src/main/java/com/petmall/storage/ImageStore.java`（新增） | 接口：`String save(byte[] data, String extension)` → 返回 `/upload/<uuid>.<ext>` |
| `backend/src/main/java/com/petmall/storage/LocalImageStore.java`（新增） | 现逻辑原样搬（`Paths.get(uploadPath).toAbsolutePath()` + `Files.createDirectories` + `Files.write`），`@ConditionalOnProperty(name="file.storage.type", havingValue="local")` |
| `backend/src/main/java/com/petmall/storage/SftpImageStore.java`（新增） | JSch `Session` + `ChannelSftp.put(ByteArrayInputStream, remotePath, OVERWRITE)`，try-with-resources 关闭，失败抛 `IOException` 由 Controller 转 `Result.error`；`@ConditionalOnProperty(..., havingValue="sftp", matchIfMissing=true)` |
| `backend/src/main/java/com/petmall/controller/FileController.java`（改） | 注入 `ImageStore`；**后缀白名单 + 12 字节文件头校验保持不动**（先验后落盘）；第 55-58 行的本地 `transferTo` 换成 `save(file.getBytes(), extension)`；补一个 `@ExceptionHandler(MaxUploadSizeExceededException)` 返回友好提示（**必须配 `resolve-lazily: true` 才生效**，见 4.2） |
| `backend/src/main/java/com/petmall/config/WebConfig.java`（改） | `/upload/**` 静态映射**仅在 `type=local` 时注册**（远端模式交给 nginx，避免两处真相）；拦截器放行清单补 `/error`——异常转发到 `/error` 时会再过一遍登录校验，把真实错误盖成 401（实测踩过） |

**为什么要留 `local` 实现**：README 的「快速启动」要求没有 Ubuntu 也能把后端跑起来；它同时是离线演示与回滚路径。代价是「一个接口 + 两个真实实现」，这不是预留抽象——第二个实现现在就在用。

**不改的**：数据库表结构、`Result` 封装、JWT 拦截器、`/file/upload` 路径、上传返回体结构（`{url: "/upload/xxx.png"}`）。

### 4.4 Ubuntu 侧（一次性，需 root）——2026-10-16 已在本机 `<SERVER_IP>` 执行

```bash
# 1. 目录与权限：专用账号可写，nginx worker 可读
sudo adduser --system --group --home /srv/petmall --shell /bin/bash petmall
sudo mkdir -p /srv/petmall/upload /srv/petmall/.ssh
sudo chown -R petmall:petmall /srv/petmall
sudo chmod 700 /srv/petmall/.ssh && sudo chmod 600 /srv/petmall/.ssh/authorized_keys
sudo chmod 755 /srv/petmall          # 必须有 x 位，否则 www-data 穿不过这一层，图片全 404
sudo chmod 755 /srv/petmall/upload

# 2. nginx（只提供图片，不给可执行内容）
sudo apt install -y nginx             # 实测版本 1.28.3-2ubuntu1.11
sudo rm -f /etc/nginx/sites-enabled/default
sudo tee /etc/nginx/sites-available/petmall-upload >/dev/null <<'EOF'
server {
    listen 80 default_server;
    server_name _;
    root /srv/petmall;               # 见下方「为什么用 root 而不是 alias」
    location /upload/ {
        autoindex off;
        expires 7d;                  # 它已经会发 Cache-Control: max-age=604800，别再加 add_header 重复
    }
    # 双保险：静态目录绝不放行可执行/可渲染内容（后端上传已挡，这里是第二道）
    location ~* ^/upload/.*\.(php|jsp|asp|html|htm|js|svg)$ { deny all; }
}
EOF
sudo ln -sf /etc/nginx/sites-available/petmall-upload /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl enable --now nginx
```

**为什么用 `root` 而不是 `alias`**：第一版写成 `location /upload/ { alias /srv/petmall/upload/; …嵌套 deny 子 location… }`，文件存在、`www-data` 也读得到（`sudo -u www-data head -c 4 <file>` 返回 PNG 头），但请求一律 404，且 `error.log` 连一条 not-found 都没有。改成 `root /srv/petmall` + 把 deny 提到同级正则 location 后立刻 200。alias 那次没查到根因（怀疑嵌套 location 与 alias 叠加），**结论：用 root 版，别用 alias 版**；改完配置记得 `systemctl reload nginx` 后再验一次，否则会拿旧 worker 的 404 误判。

**实测**：`curl -I http://<SERVER_IP>/upload/<name>.png` → `200`，`Content-Type: image/png`，`Cache-Control: max-age=604800`；`.svg` → `403`；55 张历史图逐张 200。

### 4.5 前端（代理 2 条 + 组件 8 处改相对路径）

- `frontend/vue.config.js`：`/upload` 代理 target 由 `http://localhost:8080` 改为 `http://<UBUNTU_IP>`，并删掉 `pathRewrite`（不再经后端）。
- `admin/vue.config.js`：新增同样一条 `/upload` 代理（修掉后台预览 404 的根因）。
- **顺序有讲究**：`/api` 必须排在 `/upload` 之前。本版 `http-proxy-middleware`（2.0.9）是按路径**包含**匹配的，`/api/file/upload` 含 `/upload`，`/upload` 写在前面就会把上传接口抢去转发给 nginx（实测返回 nginx 404 页，商家端传图直接失败）。`/ai` 同理必须排在 `/api` 之前。
- **组件也得改**：8 处 `getImageUrl`（`admin` 3 处、`frontend` 5 处）原来把相对路径拼成 `http://localhost:8080/api/upload/...`，`local` 模式下这是后端直出、能用；`sftp` 模式下后端不接管 `/upload`，拼后端域名必 404，改成返回相对路径交给 dev 代理。
- 生产环境：由前端部署的 nginx 反代 `/upload/` 到文件服务器，前端代码同一套相对路径。以后要上域名/CDN，只改这一处。

### 4.6 历史 55 张迁移——已执行

```bash
scp -i .ssh/petmall_deploy petmall/upload/* petmall@<SERVER_IP>:/srv/petmall/upload/
ssh -i .ssh/petmall_deploy petmall@<SERVER_IP> "ls /srv/petmall/upload | wc -l"   # 实测 55，25M
```

- 抽 3 张 md5 比对：`local == remote` 全部一致；数据库 URL 不变 → 老商品图不裂（实测 39 个被库引用的路径 + 55 个文件名全部 HTTP 200）。
- 迁移后**本地目录保留不动**，作为回滚备份。

---

## 五、验证（每条都能跑）——已全部执行

> 为了不动你正在跑的 8080 后端，实现验证时另起了两个实例：`:8090`（正常 SFTP 指向 `<SERVER_IP>`，带 `known_hosts`）与 `:8091`（指向不存在的 `192.168.100.199`，测不可达）。`/file/upload` 本就在拦截器白名单里，所以 curl 不用带 JWT。

| # | 用例 | 命令 | 期望 | 实测结果 |
|---|---|---|---|---|
| 1 | 真图上传 + 远端落盘 + nginx 可读 | `curl -F file=@a.png http://localhost:8090/api/file/upload` | 返回 `data.url=/upload/<uuid>.png`；远端出现该文件；`curl http://<IP>/upload/<uuid>.png` → 200 且字节一致 | ✅ `{"code":200,"data":{"url":"/upload/5407410c-…-c2ec530fbe06.png"}}`；本地 `petmall/upload` 仍是 55（没落本地），远端 55→56；nginx 取回 716035 字节、md5 `c9a6ea66…` 与源图一致 |
| 2 | 非图片后缀 | 上传 `a.txt` | `msg=只允许上传图片（png/jpg/jpeg/gif/webp/bmp）` | ✅ 原文返回 |
| 3 | 改名伪装 | 文本改名 `a.png` | `msg=文件内容不是有效图片` | ✅ 原文返回 |
| 4 | 老图不裂 | 远端条数 = 55；抽样 md5；库引用路径 HTTP 200 | 通过 | ✅ 55/55 个文件 HTTP 200；库里 39 个去重图片路径（`product.image` / `review.images`）全部 200 |
| 5 | 大小上限 | 传 6MB 图（`max-file-size: 5MB`） | 友好提示而非 500 堆栈 | ✅ `msg=图片过大，单张不要超过 5MB`（第一次是 401，根因见 4.2/4.3 的两处修复） |
| 6 | 远端不可达 | 把 host 改成不存在的 IP 后上传 | 返回 `文件上传失败：…`，不抛堆栈，不落本地 | ✅ 8.1s 超时后 `msg=文件上传失败：连接文件服务器失败（petmall@192.168.100.199:22）：timeout: socket is not established`，本地计数仍 55 |
| 7 | 回归 | `cd backend && mvn test` | `OrderServiceImplTest` 3 用例仍绿 | ✅ `Tests run: 7, Failures: 0, Errors: 0`（OrderServiceImplTest 3 + ImageStoreTest 4），BUILD SUCCESS |

---

## 六、天花板与升级路径

| 天花板 | 影响 | 升级路径 |
|---|---|---|
| 单台文件服务器，无副本 | 挂了图片全 404 | 换对象存储（S3/MinIO）或 nginx 后挂多台 + 同步 |
| 每次上传新建一次 SFTP 连接 | 高频上传时握手开销明显（演示量级无感） | SSH 连接池；或改用对象存储 HTTP API（无连接握手） |
| 图片 URL 公开可枚举（UUID） | 知道 URL 即可访问 | 需要私有时改签名 URL（对象存储天然支持） |
| 无 CDN、无格式转换 | 首屏图片带宽靠 nginx 单机 | CDN + 前端 `webp` + nginx `gzip` |
| 后端整文件进内存（≤5MB） | 大文件占堆 | 直接流式转发到 SFTP，或改直传对象存储（前端拿签名 URL 直传） |
| nginx 里只 deny 可执行后缀 | 依赖配置正确性 | 目录挂载 `noexec` + 只给只读权限 |

---

## 七、实施结果与遗留（2026-10-16）

环境事实（都已落进配置，不再是 `<UBUNTU_IP>` 占位）：

| 项 | 值 |
|---|---|
| 文件服务器 | `<SERVER_IP>`（Ubuntu 26.04.1 LTS），nginx 1.28.3 监听 80，`root /srv/petmall` 直出 `/upload/` |
| 服务账号 | `petmall`（系统账号，home `/srv/petmall`，只能写 `/srv/petmall/upload`） |
| 密钥 | 本机 `.ssh/petmall_rsa`（RSA-3072，后端用）+ `.ssh/petmall_deploy`（ed25519，运维用）；`.ssh/` 已 gitignore。`known_hosts` 由 `ssh-keyscan -H <SERVER_IP>` 生成 |
| 环境变量 | `FILE_SFTP_KEY`、`FILE_SFTP_KNOWN_HOSTS`、`FILE_SFTP_USER`（host / port / remote-dir 已用默认值写进 `application.yml`） |

收尾已完成（2026-10-16 21:07，三个服务都已起来并实测）：

1. **8080 后端**：停掉旧进程 → `mvn package -DskipTests`（`target/pet-travel-station-1.0.0.jar` 被运行中的进程锁着，`repackage` 会报 `Unable to rename ... jar`，必须先停）→ 带环境变量 `nohup java -jar target/pet-travel-station-1.0.0.jar`，日志 `/tmp/petmall-backend.log`。真图上传实测：`{"code":200,"data":{"url":"/upload/0b34f767-…png"}}`，本地 `petmall/upload` 仍 55（没落本地），`http://<SERVER_IP>/upload/0b34f767-…png` → 200 且 md5 一致。
2. **8081 / 8083 前端**：`nohup npm run serve` 重启（日志 `/tmp/petmall-frontend.log`、`/tmp/petmall-admin.log`），走前端 dev server 的 `/upload` 代理实测均为 `200 image/png`——8083 这条就是之前后台预览 404 的修复。
3. 本机 `petmall/upload/` 的 55 张图**保留不动**，作为回滚备份；确认一段时间没问题后再删。
5. 重启命令（含环境变量）已写进 `README.md` 的「快速启动 · 后端 API」，别只记在这里。

### 7.1 上线后发现的回归与修复（2026-10-16 21:40）

用户报「商家端图片上传失败」——因为这次改动是我做的，先怀疑自己。后端日志里**没有任何 ERROR / Exception，也没有一条 `file/upload` 请求**，说明请求根本没到后端；复现后拿到决定性证据：经 `:8083` 代理 `POST /api/file/upload` 返回的是 **nginx 的 404 页**（`Server: nginx/1.28.3`），直连 `:8080` 则是 `{"code":200,...}`。两个回归：

1. **上传接口被 `/upload` 代理抢走**。`admin/vue.config.js` 里我把 `/upload` 写在 `/api` 前面；本版 http-proxy-middleware 2.0.9 按路径**包含**匹配，`/api/file/upload` 含 `/upload` → 转发给 Ubuntu nginx → 404。探针证实这个匹配规则：`:8083/api/file/upload`、`/api/upload`、`/api/user/upload` 都 404（nginx 页），`/api/xupload`、`/api/category/public/list` 正常。修法：`/api` 排到 `/upload` 之前，`frontend/vue.config.js` 同样调整。
2. **8 处组件硬编码后端域名的图片 URL 全部 404**。`getImageUrl` 拼的是 `http://localhost:8080/api/upload/...`（`frontend/src/views/Index.vue:162` 拼的是 `/api/upload/...`）：`local` 模式下后端直出 `/upload` 所以能用，`sftp` 模式下后端已不接管，直接 404。修法：8 处一律返回相对路径（`imagePath`），由 dev 代理 `/upload` 转发到 nginx。改动文件：`admin/src/views/admin/ProductList.vue`、`admin/src/views/merchant/ProductList.vue`、`admin/src/views/merchant/ProductForm.vue`、`frontend/src/views/Index.vue`、`Cart.vue`、`OrderList.vue`、`ProductDetail.vue`、`Search.vue`。

**教训**：之前 §五 的验证只 curl 了「代理路径 `/upload/<file>`」和「nginx 直出 URL」，没走组件实际用的 URL，也没走商家真实上传链路（带 token 的 `POST /api/file/upload` 经 dev 代理），所以两个回归都没被那张验证表盖住——验证要按**用户真实点击路径**跑，不是按我改的那条路跑。

修复后实测：经 `:8083` 代理带 merchant1 token 上传 → `{"code":200,"data":{"url":"/upload/cc4ce438-…png"}}`；`:8081/upload/<old>.png`、`:8083/upload/<old>.png` → 200 `image/png`；`/api/category/public/list` 仍 200（没被代理规则误伤）。

未做的事（有意）：`local` 模式只由 `ImageStoreTest` 的单元用例覆盖，没做端到端（默认就是 `sftp`）；`ufw` 不用开（80 已在监听并已收到请求）。日志放在 `/tmp`，机器重启后清空——要长期留存就把它改成 `>> 项目内路径/app.log`。
