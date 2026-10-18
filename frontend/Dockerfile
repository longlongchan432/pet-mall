# 商城端镜像：node 构建 → nginx 托管
# vue-cli 5 / webpack 5，node 18 足够；webpack 4 时代 node 17+ 的 openssl 报错与这里无关
FROM node:18 AS build
WORKDIR /app
# 有 package-lock.json，用 npm ci 锁版本；万一报 lock 不匹配（Windows 生成的 lock 偶发），改成 npm install
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

# 运行阶段复用虚拟机上已有的 nginx:1.20.2（省一次拉取）
FROM nginx:1.20.2
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
