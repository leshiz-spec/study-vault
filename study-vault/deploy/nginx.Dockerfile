ARG NODE_IMAGE=node:20-alpine
ARG NGINX_IMAGE=nginx:1.27-alpine

FROM ${NODE_IMAGE} AS frontend-build
WORKDIR /app

COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund

COPY frontend/ ./
RUN npm run build

FROM ${NGINX_IMAGE}
COPY deploy/nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=frontend-build /app/dist /usr/share/nginx/html

EXPOSE 80
