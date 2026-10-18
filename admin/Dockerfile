# 后台端镜像：与商城端同一套（node 构建 → nginx 托管），只是 nginx.conf 少一段 /ai
FROM node:18 AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.20.2
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
