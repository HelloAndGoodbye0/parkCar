# ============================================================
# 前端多阶段构建：Node 构建 → Nginx 运行
# ============================================================
FROM node:20-alpine AS builder
WORKDIR /build

# 使用国内镜像源加速 npm 下载
RUN npm config set registry https://registry.npmmirror.com

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
RUN npm run build

# ---- 运行阶段 ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=builder /build/dist /usr/share/nginx/html
EXPOSE 80
