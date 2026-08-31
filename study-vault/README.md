# StudyVault

StudyVault 是一个个人 Markdown 知识库的模块化单体项目骨架。

## 目录

- `frontend/`：Vue 3 + TypeScript + Vite + Pinia + Vue Router
- `backend/`：Java 17 + Spring Boot 3 + Maven，按 controller/service/repository/entity/dto/exception/security/config 分层
- `deploy/`：Docker Compose 与 Nginx 配置

## 本地开发

```bash
cd frontend && npm install && npm run dev
cd backend && mvn spring-boot:run
```

后端提供 `GET /api/health` 与 `GET /api/ready` 健康检查。

## 生产部署准备

在服务器上复制 `.env.production.example` 为 `.env.production`，填写强密码和至少 32 位的随机 `JWT_SECRET`，然后从 `study-vault/` 目录执行：

```bash
docker compose --env-file .env.production -f deploy/docker-compose.yml up -d --build
```

真实的 `.env.production` 已加入 Git 忽略规则，不应提交到仓库。
