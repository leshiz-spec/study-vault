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

后端提供 `GET /api/health` 与 `GET /api/ready` 健康检查。生产环境请复制 `.env.example` 为 `.env`，再执行 `docker compose -f deploy/docker-compose.yml up -d --build`。
