# 四端容器化部署到 Ubuntu 虚拟机（Docker Compose）v1

> 状态：**待你执行**。本文档与你仓库里的 `docker-compose.yml` + 4 个 `Dockerfile` + 2 个 `nginx.conf` 是配套的一组，
> 按 §1→§6 顺序敲一遍即可在虚拟机上跑起完整四端；§7 是我预判你会撞上的坑（多数是我自己已经撞过并实测过的）。
>
> 这份文档里我**没有**替你部署：所有命令都是给你自己在 Ubuntu 虚拟机上执行的。
> 我在这台机器上做过的只是「写文件 + 只读校验」（见 §5.1，`docker compose config` 和两个 `nginx -t` 都已通过）。

## 0. 你会得到什么

跑完之后，虚拟机上是 5 个容器（四端 + 数据库），端口与你现在 Windows 上开发时一模一样，浏览器直接开 IP 即可：

| 容器 | 内容 | 宿主机端口 | 你会用它做什么 |
|---|---|---|---|
| `petmall-mysql` | MySQL 8 | 不发布（只在 compose 网络内） | 数据。想用 Navicat 连就取消 compose 里那两行注释 |
| `petmall-backend` | Spring Boot 2.7.18 / JDK8 | **8080** | `/api/...` 业务接口（context-path 就是 `/api`） |
| `petmall-ai` | FastAPI + SSE | **8082** | AI 导购（`/health`、`/api/chat/stream`） |
| `petmall-web` | Vue2 构建 → nginx | **8081** | 商城端；顺带发 `/api`、`/ai`、`/upload` |
| `petmall-admin` | Vue2 构建 → nginx | **8083** | 后台/商家端；顺带发 `/api`、`/upload` |

架构上唯一和开发形态不同的地方：**图片不再走 SFTP，改成共享目录**。

```
浏览器 ──:8081──▶ petmall-web(nginx) ──/api──▶ petmall-backend ──▶ petmall-mysql
                          │  ──/ai──▶ petmall-ai ──▶ (DeepSeek API)
                          └──/upload──▶ /srv/petmall/upload （容器内 bind mount，和宿主同一份文件）
```

为什么同机部署要把 SFTP 换成共享目录：SFTP 的价值在「文件服务器在另一台机器」。同一台机器上还 SSH 连自己，
只是多了一次网络往返、多一套密钥管理、多一个失败点。**跨机部署的写法保留在 §9**，那边继续用你已经调通的 SFTP 那套。

老图不会丢：`/srv/petmall/upload` 现在就在虚拟机上（63 个文件），容器直接挂它，数据库里 `/upload/<uuid>.png` 这类相对路径
一个都不用改。

---

## 1. 动手前的检查

### 1.1 虚拟机侧

```bash
# 进虚拟机（在 Windows 的 Git Bash 里执行；密钥是你之前那把部署密钥）
ssh -i ~/.ssh/petmall_deploy root@<SERVER_IP>
# 这台机器上密钥在项目 .ssh/ 下，所以也可以：
# ssh -i D:/Dev/Projects/pet-mall/.ssh/petmall_deploy root@<SERVER_IP>
```

进去之后逐条看：

```bash
docker --version                 # 期望：Docker version 29.x
docker compose version           # 期望：v2.x / v5.x（插件）。注意命令是 docker compose，不是 docker-compose
df -h /                          # 期望：还剩 ≥10G
free -h                          # 期望：Mem total ≥3G（这台是 3.3G，够跑，但构建 npm 时要小心，见 §7-5）
ss -ltnp | grep -E ':80|:8080|:8081|:8082|:8083|:3306'   # 期望：只有 80 被 native nginx 占用，其余空
docker ps -a --format '{{.Names}}\t{{.Status}}'          # 期望：全是 Exited，其中有一个叫 mysql（名字已被占！）
docker images | head -20         # mysql:8 和 nginx:1.20.2 已在本地 → 本次构建不用再拉它们
```

这台虚拟机当前的真实情况（我查过）：

- Docker 29.9.1；Compose 是**插件**，`docker-compose` 命令不存在。
- 磁盘 `/` 38G，已用 22G，**剩 14G**；内存 3.3G。
- 80 端口被**之前我给你装的 native nginx** 占着（只服务 `/upload`），其余 8080/8081/8082/8083/3306 全空闲。
- 已有镜像 `mysql:8`、`nginx:1.20.2`、`redis`、`app-tlias`、`milvus` 等 —— 我们正好复用前两个。
- 已有一个退出的容器叫 `mysql`（别的课程的 demo）→ 所以本项目一律命名 `petmall-*`，不跟你抢名字。

### 1.2 你自己机器侧

- 项目根目录：`D:\Dev\Projects\pet-mall`。
- 需要传的东西：源码 + `sql/` + 我新加的 `docker-compose.yml` / `Dockerfile` / `nginx.conf` / `.env.example`。
- **不要**传：`node_modules/`（Windows 版，几百 MB，且有平台绑定的二进制）、`backend/target/`（29MB，容器里会自己打）、
  `petmall/upload/`（55 张本地备份，虚拟机上已有 63 张）、`.ssh/`（密钥绝不能进虚拟机项目目录）。

---

## 2. 把项目放到虚拟机

在 Windows 的 Git Bash（项目根目录）执行：

```bash
cd /d/Dev/Projects/pet-mall

# 1) 打包，排除本机专用的东西。.env 也排除：密钥单独传，别混进项目目录
tar --exclude=node_modules --exclude=target --exclude=.git --exclude=.ssh \
    --exclude=petmall/upload --exclude=dist --exclude=.env \
    -czf /tmp/petmall-src.tgz .

# 2) 传到虚拟机
scp -i .ssh/petmall_deploy /tmp/petmall-src.tgz root@<SERVER_IP>:/tmp/

# 3) 在虚拟机上解开
ssh -i .ssh/petmall_deploy root@<SERVER_IP> \
  'mkdir -p /opt/pet-mall && tar -xzf /tmp/petmall-src.tgz -C /opt/pet-mall && ls /opt/pet-mall'
```

期望看到 `admin  ai-service  backend  docker-compose.yml  docs  frontend  petmall  README.md  sql`。

> 包大概 5～10MB。如果 `scp` 报权限/网络错误，先 `ping <SERVER_IP>` 和 `ssh` 手工试一次。
> 改完代码要重新上线时，重复这三步即可（tar 会覆盖同名文件，已存在的容器数据卷不受影响）。

---

## 3. 数据怎么进 MySQL（先做决定）

容器第一次启动时，如果数据卷是空的，MySQL 会自动执行挂进去的 `sql/petmall.sql`。所以你有两条路：

### 路线 A：全新导入项目自带的种子数据（推荐第一次上线走这条）

什么都不用做，`sql/` 已经在 compose 里挂好了。你得到的是 README 里写的那套种子账号
（admin/admin123、merchant1/merchant123、user1~user5/petmallDemo2026）和示例商品。

代价：**你最近在 Windows 上测试产生的数据（自己加的商品、订单、评价）会没有**。
图片文件不会丢 —— 它们一直在 `/srv/petmall/upload`。

### 路线 B：把你现在正在用的数据迁过去

```bash
# ① Windows 上导出（路径按你的 MySQL 安装位置；这里是本机实测可用的那条）
MY="/d/Dev/MySQL/MySQL Server 8.0/bin/mysqldump"
"$MY" -h127.0.0.1 -uroot -pyour_mysql_password --default-character-set=utf8mb4 \
      --databases pet_mall --single-transaction > /tmp/pet_mall-dump.sql

# ② 传到虚拟机
scp -i .ssh/petmall_deploy /tmp/pet_mall-dump.sql root@<SERVER_IP>:/tmp/

# ③ 先起 MySQL（只起它），等健康检查通过再导入
#    注意：如果 compose 已经把 sql/ 初始化过一遍，这里导入的 dump 里带 DROP TABLE，会覆盖掉种子数据
ssh -i .ssh/petmall_deploy root@<SERVER_IP> 'cd /opt/pet-mall && docker compose up -d petmall-mysql'
# 等 20 秒左右，然后导入：
ssh -i .ssh/petmall_deploy root@<SERVER_IP> \
  'cd /opt/pet-mall && docker compose exec -T petmall-mysql mysql -uroot -pyour_mysql_password pet_mall < /tmp/pet_mall-dump.sql'
```

> `--databases` 会带上 `CREATE DATABASE` + `USE`，所以不会和容器自动建库打架。
> 导入后一定验一次（§5.3），否则可能整库中文乱码你还不知道。

**两条路都别忘了**：容器里的 MySQL 时区钉死成 `+08:00`（compose 里那行 `--default-time-zone=+08:00`），
和 JDBC 连接串里的 `serverTimezone=Asia/Shanghai` 对齐 —— 不对齐的话所有订单时间会差 8 小时。

---

## 4. 密钥：根目录的 `.env`

compose 会自动读项目根目录的 `.env`。它已被 `.gitignore` 忽略，不会被提交。

```bash
# 方式一：直接把本机 ai-service/.env 拷过去当根 .env（最省事，内容通常就是 DEEPSEEK_API_KEY=...）
scp -i .ssh/petmall_deploy ai-service/.env root@<SERVER_IP>:/opt/pet-mall/.env

# 然后进去确认一眼（别把内容贴到任何聊天/截图里）
ssh -i .ssh/petmall_deploy root@<SERVER_IP> 'grep -c "^DEEPSEEK_API_KEY=" /opt/pet-mall/.env'
# 期望输出：1

# 方式二：进去手填
ssh -i .ssh/petmall_deploy root@<SERVER_IP>
cd /opt/pet-mall && cp .env.example .env && vi .env     # 填上你的 sk-...
chmod 600 .env
```

少了这个文件，`docker compose up` 会直接报
`required variable DEEPSEEK_API_KEY is missing`（这是我故意设的：宁可启动前失败，也别让 AI 导购跑到线上才发现没 key）。

---

## 5. 起容器与验收

### 5.1 先校验配置（不用起容器，10 秒）

```bash
ssh -i .ssh/petmall_deploy root@<SERVER_IP>
cd /opt/pet-mall
docker compose config >/dev/null && echo "compose 语法 OK"
```

> 我在这台虚拟机上已经用你仓库里这两份 `nginx.conf` 跑过 `nginx -t`，两个都 `syntax is ok`；
> `docker compose config` 也 exit 0。所以这一步只要不是你改坏了，就该过。

### 5.2 构建 + 启动

```bash
docker compose up -d --build
```

首次会：拉/复用基础镜像 → Maven 打后端 → npm 装依赖 + 构建两个前端 → pip 装 AI 依赖。
网络好的话 5～15 分钟；这台机器只有 3.3G 内存，**如果卡在 npm 构建或容器被 OOM kill，看 §7-5**。

```bash
docker compose ps                # 5 个服务都应是 Up / healthy
docker compose logs --tail=30 petmall-backend
```

期望在 backend 日志里看到 `Tomcat started on port(s): 8080` 和 `Started PetTravelStationApplication`。

### 5.3 逐条验收（复制粘贴即可）

```bash
# ① 后端活着 + 能读库（这条同时验证了数据库连接和中文编码）
curl -s http://<SERVER_IP>:8080/api/category/public/list | head -c 200; echo

# ② AI 服务活着
curl -s http://<SERVER_IP>:8082/health; echo

# ③ 上传图还能直出（拿一张真实存在的旧图）
curl -s -o /dev/null -w '%{http_code} %{content_type}\n' \
  http://<SERVER_IP>:8081/upload/01d035f7-03d9-4a25-9138-187397189bd1.png
# 期望：200 image/png

# ④ 后台端也能发图（这条是之前修过的「后台预览 404」）
curl -s -o /dev/null -w '%{http_code}\n' \
  http://<SERVER_IP>:8083/upload/01d035f7-03d9-4a25-9138-187397189bd1.png
# 期望：200

# ⑤ 容器里看到的图片目录 = 宿主目录（同一份文件）
docker compose exec petmall-backend sh -c 'ls /srv/petmall/upload | wc -l'
ls /srv/petmall/upload | wc -l
# 两个数字应相同（现在约 63）

# ⑥ 中文没乱码
docker compose exec -T petmall-mysql mysql -uroot -pyour_mysql_password pet_mall \
  -e "select id,name from category limit 3;" 2>/dev/null
# 期望：中文正常显示（不是 ??? 或一串问号方块）
```

### 5.4 浏览器验收（照你真实的点击路径跑，别只 curl）

1. `http://<SERVER_IP>:8081` 商城首页 —— 商品图正常显示（不是裂图）。
2. 登录 `user1/petmallDemo2026` → 商品详情 → 加入购物车 → 下单（订单列表里时间应是本地时间，不是差 8 小时）。
3. 打开右下角 AI 导购，随便问一句 —— 应**逐字吐出来**（流式）。如果卡很久然后一次性全出来，是 §7-10。
4. `http://<SERVER_IP>:8083` 后台 → `merchant1/merchant123` 登录 → 商品管理 → **上传一张新商品图** →
   保存成功后回到列表，图应能显示。
5. 上传完成后去虚拟机上确认文件真的落地了：
   ```bash
   ls -lt /srv/petmall/upload | head -3
   ```
   最新那个 uuid 文件名就是刚传的。

这 5 步全过，就是真的上线成功了。第 4 步是你上次踩坑的地方（`.vue.config.js` 代理顺序 + 组件硬编码域名），
容器版已经用容器内 nginx 代理规避掉了，但**验收仍然要照浏览器真实点击路径跑一遍**。

---

## 6. 改完代码怎么重新上线

```bash
cd /opt/pet-mall
# 只改了某一个端（比如后台前端）
docker compose up -d --build petmall-admin

# 改了后端
docker compose up -d --build petmall-backend

# 只改了 nginx.conf（前端容器）—— 也必须重新 build 镜像，restart 不够
docker compose up -d --build petmall-web

# 只改了 ai-service 的 python 代码
docker compose up -d --build petmall-ai
```

`git pull` 之类在这里不适用（虚拟机上的副本不带 `.git`）—— 按 §2 重新 tar+scp 覆盖，然后 `up -d --build`。

---

## 7. 我预判你会遇到的麻烦（症状 → 原因 → 处理）

| # | 症状 | 原因 | 处理 |
|---|---|---|---|
| 1 | `docker-compose: command not found` | 这台机器只有 Compose 插件，没有独立的 `docker-compose` 可执行文件 | 一律用 `docker compose ...`（中间是空格） |
| 2 | `docker compose config` 报 `required variable DEEPSEEK_API_KEY is missing` | 根目录没有 `.env`，或没写 `DEEPSEEK_API_KEY=` | 回到 §4 |
| 3 | 启动报 `port is already allocated` | 80 被 native nginx 占（本项目没用到 80）；或你之前有残留容器 | `docker ps -a` 看谁占着；本项目映射的是 8081/8083/8080/8082/3306，正常不冲突 |
| 4 | 报容器名冲突（`mysql` 已存在） | 虚拟机里有别的课程留下的同名容器 | 本项目全部叫 `petmall-*`，compose 里写死了 `container_name`，正常不会撞。真撞了就 `docker rm 那个名字`（先确认里面没有你要的数据） |
| 5 | 构建中途容器被 kill / `npm run build` 卡死 | 这台机器只有 **3.3G 内存**，npm 构建峰值 1～1.5G，多容器同时构建就爆 | ①加 2G swap：`fallocate -l 2G /swapfile && chmod 600 /swapfile && mkswap /swapfile && swapon /swapfile`（重启后失效，要长期就写进 `/etc/fstab`）；②逐个构建：`docker compose build petmall-web`、再 `petmall-admin`、再 `petmall-backend`，最后 `docker compose up -d` |
| 6 | 磁盘写满 / 构建报 `no space left on device` | `/` 只剩 14G，构建缓存 + 镜像很吃空间 | 先看占用 `docker system df`；回收不用的：`docker system prune -a`（会删掉 tlias/milvus 那些不再用的镜像和构建缓存，**执行前看一眼 `docker images` 确认没有你要留的**） |
| 7 | `npm ci` 报 lock 文件不匹配 | 锁文件偶尔和 package.json 有细微差异 | 把该端 `Dockerfile` 里 `npm ci` 改成 `npm install`，重新 build |
| 8 | 传图返回 **413** | nginx 默认 `client_max_body_size 1m` | 已在前端 `nginx.conf` 里配了 `10m`。若你改过它，重建 `petmall-web`/`petmall-admin` 镜像 |
| 9 | 传图返回 **401 未登录**（而不是「图片过大」） | 老问题：Tomcat 在进 Controller 前抛 `MaxUploadSizeExceededException`，异常转发 `/error` 又被登录拦截器拦成 401 | 已修：`application.yml` 里 `servlet.multipart.resolve-lazily: true` + `WebConfig` 放行 `/error`。**别把这两处删了**，容器版同样依赖它 |
| 10 | AI 对话「卡住很久，然后一次性吐出来」 | SSE 被 nginx 缓冲 | 已在前端 `nginx.conf` 的 `/ai/` 里配了 `proxy_buffering off` + `proxy_read_timeout 300s`。改了别漏 |
| 11 | 图片 404 | 三种：①容器没挂 `/srv/petmall/upload`；②目录/文件权限让容器内 nginx 用户读不到；③nginx 用了 `alias` | ①检查 compose 里 web/admin 的 volumes；②`curl` 时看权限：目录要 `755`、文件至少 `644`（现在是对的，别 `chown` 成 700）；③**用 `root /srv/petmall` 不用 `alias`** —— 这台 nginx 上 alias 版实测对 `/upload/` 一律 404 |
| 12 | 刷新 `/product/1` 这类子路由变 404 | Vue Router 是 history 模式，磁盘上没有该文件 | 已在两个 frontend 的 `nginx.conf` 里配 `try_files $uri $uri/ /index.html` |
| 13 | 订单/评价时间差 8 小时 | 虚拟机系统时区是 UTC，而 JDBC 写死了 `serverTimezone=Asia/Shanghai` | 已在 compose 给 MySQL 钉了 `--default-time-zone=+08:00`。若你改了 MySQL 时区，两边必须一致 |
| 14 | 中文变成 `???` | 建库/导入时用了非 utf8mb4 字符集 | compose 已配 `--character-set-server=utf8mb4`；手工导入时加 `--default-character-set=utf8mb4`；已乱码就重新导入一次 |
| 15 | 改了 `sql/petmall.sql` 但重启动没生效 | 数据卷已存在，`docker-entrypoint-initdb.d` 只在**首次空卷**时执行 | 要么手动导：`docker compose exec -T petmall-mysql mysql -uroot -pyour_mysql_password pet_mall < xxx.sql`；要么清库重来 `docker compose down -v`（**会删掉数据库数据**，先备份！） |
| 16 | 后端日志报 `Communications link failure` / `Access denied` | 容器里的 `localhost` 是容器自己，不是数据库 | compose 已把 `SPRING_DATASOURCE_URL` 指向 `petmall-mysql:3306`；你手工加环境变量时也要用服务名 |
| 17 | 刚启动访问前端有短暂 **502** | `depends_on` 只保证启动顺序，不保证后端已经能服务 | 等 20 秒再刷；后端有 `condition: service_healthy`，MySQL 是稳的 |
| 18 | 改了 `nginx.conf` 却还是旧行为 | 前端容器的 nginx 配置是**打进镜像**的 | `docker compose up -d --build petmall-web`，`restart` 没用 |
| 19 | 从 Windows 打不开 `<SERVER_IP>:8081` | 网络/防火墙 | 先 `ping`；虚拟机目前没开 ufw，真开了就 `ufw allow 8081/tcp` 等 |
| 20 | `docker compose logs` 里看不到想要的日志 | 日志只保留在容器生命周期内 | `docker compose logs -f --tail=100 petmall-backend`；已重启过的老日志就没了（要看历史就加日志卷，非本次目标） |
| 21 | 构建时 pip/npm 下载极慢甚至超时 | 网络 | AI 的 `Dockerfile` 已默认用清华 PyPI 镜像；npm 慢就在 `frontend/`、`admin/` 的 Dockerfile 里给 `npm ci` 加 `--registry=https://registry.npmmirror.com`；Maven 慢就清掉 `dependency:resolve` 那行、并把 `backend/Dockerfile` 的 `maven` 基础镜像换成国内源 |
| 22 | 想改端口（比如 80 冲突） | 端口在 compose 的 `ports:` 里 | 只改 `ports:` 左侧（如 `"9081:80"`），不用动镜像 |

---

## 8. 日常运维

```bash
cd /opt/pet-mall

# 看状态 / 日志
docker compose ps
docker compose logs -f --tail=100 petmall-backend
docker compose logs --tail=50 petmall-mysql

# 重启单个服务 / 全部
docker compose restart petmall-backend
docker compose restart

# 停掉（保留数据）与彻底清掉（连数据库一起删！）
docker compose down
docker compose down -v          # ⚠️ 删掉 petmall-mysql-data 卷，数据库数据全没

# 数据库备份（在虚拟机上存一份，重要操作前必做）
docker compose exec -T petmall-mysql mysqldump -uroot -pyour_mysql_password --default-character-set=utf8mb4 \
  pet_mall > /root/pet_mall-$(date +%F).sql
# 恢复
docker compose exec -T petmall-mysql mysql -uroot -pyour_mysql_password pet_mall < /root/pet_mall-2026-10-17.sql

# 图片目录备份（它就是宿主目录，直接 tar 即可）
tar -czf /root/upload-$(date +%F).tgz -C /srv/petmall upload
```

`/srv/petmall/upload` 的所有权是 `petmall:petmall`，容器里后端以 root 写入 → 新文件属主会变成 `root:root`。
读没问题（别人有读权限），但如果你手工做了严格的 `chown/chmod`，注意别把「其他用户可读」抹掉，否则容器里的 nginx 读不到图。

---

## 9. 三个可选升级方向（先跑通主线再动）

### 9.1 文件服务器在另一台机器（跨机，继续用你调通的 SFTP）

把 backend 的这两个环境变量改掉即可，代码不用动：

```yaml
      FILE_STORAGE_TYPE: sftp
      FILE_SFTP_HOST: <SERVER_IP>
      FILE_SFTP_PORT: "22"
      FILE_SFTP_USER: petmall
      FILE_SFTP_KEY: /run/secrets/petmall_rsa        # 密钥用 compose 的 secrets 挂进容器，别打进镜像
      FILE_SFTP_KNOWN_HOSTS: /run/secrets/known_hosts
      FILE_SFTP_DIR: /srv/petmall/upload
```

同时把 web/admin 容器的 `volumes` 去掉（它们改成 `proxy_pass` 到那台文件服务器的 nginx，或者让浏览器直接访问那台机器）。
注意两点：**密钥必须是 RSA**（JSch 不认 ed25519，这是我实测踩过的坑）；**known_hosts 一定要给**（不给就是关闭主机校验 + 中间人风险）。

### 9.2 真正的「一个域名两个端」

再加一个 edge nginx 容器占 80，按域名分流：`mall.xxx` → `petmall-web:80`、`admin.xxx` → `petmall-admin:80`。
需要额外改：admin 的 `publicPath`、路由 base、以及两个前端的接口相对路径前缀 —— 属于前端改造，不是本次范围。
（现在 80 上的 native nginx 只服务 `/upload`，留着当「直接看图的入口」很方便，不用拆。）

### 9.3 换成对象存储（MinIO / OSS）

代码里已经有 `ImageStore` 这道缝（`LocalImageStore` / `SftpImageStore` 两个实现），加一个 `S3ImageStore` 就够了。
`cgr.dev/chainguard/minio:latest` 这台虚拟机上已经有镜像，起个容器就是 S3 兼容服务。
详见 `docs/upload-remote-storage-v1.md` §六。

---

## 10. 回滚：退回现在的形态

容器化不影响你现有的开发形态，两套可以并存（Windows 上 8080/8081/8083 那三个进程和虚拟机的容器不冲突）。

- 只想停掉容器、回到从前：`docker compose down`（不删卷，随时再 `up -d` 回来）。
- 想彻底不留痕迹：`docker compose down -v` + `docker rmi` 本项目构建出的镜像。
- 图片目录 `/srv/petmall/upload` 是宿主的，容器删了它还在 —— 老图永远不会因为这次部署丢。
- 数据库：容器里的库是独立的一份；你 Windows 上的 MySQL 一点没动。真出问题，Windows 那套照样跑。

---

## 附录 A：这次一起加进仓库的文件

| 文件 | 作用 | 你可能需要改它的场景 |
|---|---|---|
| `docker-compose.yml` | 编排五个服务、端口、卷、环境变量、健康检查 | 改端口、加/减服务、开 3306 |
| `backend/Dockerfile` | Maven 构建 → JRE8 运行 | Maven 太慢/要换基础镜像 |
| `backend/.dockerignore` | 不把本机 `target/` 传进构建上下文 | 一般不用动 |
| `frontend/Dockerfile`、`admin/Dockerfile` | node 构建 → nginx 托管 dist | 换 node 版本、npm 走国内源 |
| `frontend/nginx.conf`、`admin/nginx.conf` | 静态资源 + `/upload` 直出 + `/api`(+/ai) 转发 | 改上传大小限制、加新的后端路径 |
| `frontend/.dockerignore`、`admin/.dockerignore` | 滤掉 `node_modules/`、`dist/`（**关键**） | 一般不用动 |
| `ai-service/Dockerfile` | python 依赖 + uvicorn 启动 | pip 换源 |
| `ai-service/.dockerignore` | 滤掉 `.env`（密钥不进镜像）、`data/` | 一般不用动 |
| `.env.example` | `.env` 的模板（compose 读根目录 `.env`） | 加新的密钥 |
| `docs/docker-deploy-ubuntu-v1.md` | 本文档 | 部署完把「状态」改成「已上线」并记下实际踩到的坑 |

## 附录 B：地址速查

| 用途 | 地址 |
|---|---|
| 商城端 | `http://<SERVER_IP>:8081` |
| 后台/商家端 | `http://<SERVER_IP>:8083` |
| 后端 API | `http://<SERVER_IP>:8080/api/...` |
| AI 导购健康检查 | `http://<SERVER_IP>:8082/health` |
| 直接看某张图（native nginx） | `http://<SERVER_IP>/upload/<uuid>.png` |
| 直接看某张图（容器） | `http://<SERVER_IP>:8081/upload/<uuid>.png` |
| 种子账号 | admin/admin123（管理员）、merchant1/merchant123（商家）、user1~user5/petmallDemo2026 |
