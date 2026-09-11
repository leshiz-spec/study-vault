# StudyVault 项目结构与实现逻辑

## 1. 项目定位

StudyVault 是一个模块化单体（modular monolith）全栈应用：Vue 前端、Spring Boot 后端和 PostgreSQL 数据库共同组成一个系统。

当前实际实现包括：

- 用户注册、登录、退出和 JWT Cookie 认证
- Markdown 笔记创建、读取、修改和删除
- 回收站、恢复和永久删除
- 搜索、标签、收藏、排序和分页
- Markdown 导入、单篇导出和 ZIP 批量导出
- 笔记版本历史与恢复
- 笔记复习状态和学习任务
- AI 笔记摘要，以及没有 API Key 时的本地摘要 fallback

尚未完整实现的功能包括：

- AI 生成复习题
- AI 推荐相关笔记
- 正式的分类（category）系统
- 可用的笔记链接和反向链接功能。数据库迁移已经创建 `note_links` 表，但没有对应的 Java Entity、Repository、Service、Controller 和前端页面
- Dashboard 统计 API
- 前端自动化测试脚本

## 2. 顶层目录

```text
study-vault/
├── frontend/   Vue 3 单页应用
├── backend/    Spring Boot REST API
├── deploy/     Docker Compose、Nginx 和备份配置
├── .env.example
├── .env.production.example
└── README.md
```

### `frontend/`

负责浏览器中可以看见和操作的部分，包括页面、表单、路由、页面状态、Markdown 预览和 HTTP 请求。

主要文件：

- `src/main.ts`：创建 Vue 应用，安装 Pinia 和 Vue Router
- `src/App.vue`：应用外壳、导航栏、主题切换和 `<RouterView>`
- `src/router.ts`：URL 与页面的对应关系，以及登录保护
- `src/api.ts`：所有后端 API 请求的统一入口
- `src/stores/auth.ts`：当前登录用户和登录状态
- `src/stores/theme.ts`：浅色/深色主题状态
- `src/views/NotesView.vue`：笔记列表、搜索、标签筛选、收藏、导入导出
- `src/views/NoteEditorView.vue`：编辑器、Markdown 预览、图片、版本历史、复习状态和 AI 摘要
- `src/views/TrashView.vue`：回收站、恢复和永久删除
- `src/views/TagManagementView.vue`：标签管理
- `src/views/StudyTasksView.vue`：学习任务管理

当前没有独立的 `components/` 目录，因此主要界面和局部逻辑直接放在各个 View 中。

### `backend/`

负责可信的业务处理，包括身份验证、权限判断、输入验证、事务、数据库访问和 AI Provider 调用。

主要包：

- `controller`：接收 HTTP 请求，读取参数并返回 DTO
- `service`：业务规则、用户所有权检查和事务边界
- `repository`：通过 Spring Data JPA 查询数据库
- `entity`：Java 对象与数据库表的映射
- `dto`：API 请求和响应的数据格式
- `security`：JWT 创建、验证和当前用户恢复
- `exception`：稳定错误码和统一异常响应
- `config`：Spring Security、BCrypt 和请求日志配置
- `resources/db/migration`：Flyway 数据库迁移

### `deploy/`

负责把系统作为容器运行：

- `docker-compose.yml`：启动 PostgreSQL、backend 和 Nginx
- `nginx.conf`：提供 Vue 静态文件，并把 `/api` 转发给 Spring Boot
- `nginx.Dockerfile`：构建 Vue，再把 `dist/` 放入 Nginx 镜像
- `docker-compose.prod.yml`：生产环境重启策略和 Secure Cookie 配置
- `backup.sh`：使用 `pg_dump` 备份 PostgreSQL

## 3. 系统请求总链路

```text
User action
  -> Vue View
  -> frontend/src/api.ts
  -> /api HTTP request + JWT Cookie
  -> Vite proxy（开发）或 Nginx（部署）
  -> Spring Security JWT filter
  -> Controller
  -> Service
  -> Repository / JPA
  -> PostgreSQL
  -> DTO + ApiResponse
  -> frontend/src/api.ts
  -> Vue reactive state
  -> 页面重新渲染
```

例如用户保存笔记时：

1. `NoteEditorView.vue` 的 `save()` 读取标题和正文。
2. `api.ts` 的 `updateNote()` 发送 `PUT /api/notes/{id}`，JSON 为 `{ title, content }`。
3. 浏览器自动带上 HttpOnly JWT Cookie。
4. JWT Filter 验证 Cookie，并恢复当前 `User`。
5. `NoteController.update()` 接收请求。
6. `NoteService.update()` 使用 `findByIdAndUser(id, user)` 查询笔记，保证只能修改自己的笔记。
7. 如果内容变化，旧版本先写入 `note_revisions`，然后更新 `notes`。
8. Entity 转换成 `NoteResponse`，再包装成统一 JSON 返回。
9. `apiRequest()` 提取 `payload.data`，页面完成保存或显示错误。

## 4. Controller、Service、Repository、Entity、DTO

### Controller

Controller 是 HTTP 入口。例如 `NoteController` 把 `/api/notes` 下的 URL 映射到 Java 方法。它负责路径参数、请求体、认证对象、HTTP 响应和 DTO，不负责复杂业务。

### Service

Service 是业务核心。例如 `NoteService` 决定删除是软删除、修改是否需要产生历史版本，以及所有查询必须属于当前用户。多个相关数据库操作通过 `@Transactional` 放在同一个事务中。

### Repository

Repository 是数据库访问层。`NoteRepository.findByIdAndUser(id, user)` 会生成同时匹配笔记 ID 和用户的查询。搜索使用 `NoteSpecifications` 动态组合关键词、标签、收藏和状态条件。

### Entity

Entity 描述 Java 对象与数据库表的对应关系。例如 `Note` 对应 `notes`，`User` 对应 `users`，`@ManyToOne` 表示多篇笔记可以属于同一个用户。

### DTO

DTO 是 API 的输入输出格式。例如 `NoteCreateRequest` 只允许传标题和正文，不允许传 `userId`；`NoteResponse` 返回安全的笔记字段，但不会把完整 User Entity 或密码哈希序列化给浏览器。

所有普通 JSON 响应使用统一结构：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

失败时由 `GlobalExceptionHandler` 返回 `NOTE_NOT_FOUND`、`VALIDATION_ERROR`、`AI_TIMEOUT` 等稳定错误码。

## 5. Authentication 和 JWT

注册/登录流程：

```text
LoginView / RegisterView
  -> Pinia auth store
  -> POST /api/auth/login 或 /register
  -> AuthController
  -> AuthService
  -> UserRepository
  -> BCrypt 校验或哈希密码
  -> JwtService 签发 JWT
  -> Set-Cookie: STUDYVAULT_TOKEN=...
```

密码通过 BCrypt 哈希后保存到 `users.password_hash`，不会明文存储。

JWT 的 subject 是用户 ID，并包含 username、签发时间和过期时间。JWT 被放入 `STUDYVAULT_TOKEN` Cookie，Cookie 使用：

- `HttpOnly`：前端 JavaScript 不能读取 token
- `SameSite=Lax`：降低跨站请求风险
- `Path=/`：整个应用请求都能携带
- `Secure`：生产 HTTPS 环境应设置为 true

后续请求由 `JwtAuthenticationFilter` 读取 Cookie、验证签名和有效期、查询用户，并把 User 放进 Spring Security 的认证上下文。

Controller 和 Service 永远使用认证上下文里的用户，不信任前端传入的用户 ID。这是跨用户数据隔离的主要安全边界。

## 6. 笔记 CRUD 和扩展功能

- 创建：`POST /api/notes`
- 读取列表：`GET /api/notes`
- 读取单篇：`GET /api/notes/{id}`
- 修改：`PUT /api/notes/{id}`
- 移入回收站：`DELETE /api/notes/{id}`
- 查看回收站：`GET /api/notes/trash`
- 恢复：`POST /api/notes/{id}/restore`
- 永久删除：`DELETE /api/notes/{id}/permanent`
- 切换收藏：`POST /api/notes/{id}/favorite`

普通删除只把 `notes.status` 从 `active` 改成 `trash`。永久删除只允许删除已经在回收站中的笔记。

修改笔记时，如果标题或正文发生变化，Service 会在同一事务中把修改前的标题和正文保存到 `note_revisions`。恢复历史版本仍经过正常 update 流程，所以恢复前的当前版本也会被保留。

编辑器的 autosave 只是把草稿写入浏览器 `localStorage`，不是数据库自动保存。数据库写入仍由 `Save note` 按钮触发。

## 7. 搜索、标签、复习状态和学习任务

### 搜索

`NotesView.vue` 调用：

```text
GET /api/search?q=...&tag=...&favorite=...&status=active&page=...&size=...&sort=...
```

`SearchController` 解析分页和排序，`NoteService` 调用 `NoteSpecifications`。查询首先固定当前用户，再组合标题/正文模糊匹配、标签、收藏和状态条件。过滤、排序和分页在数据库中完成。

### 标签

标签由 `tags` 表保存，通过 `note_tags` 连接到笔记。一篇笔记可以有多个标签，一个标签也可以属于多篇笔记。

添加关系前，`NoteTagService` 分别检查笔记和标签都属于当前用户。同一用户不能创建同名标签，同一笔记也不能重复绑定相同标签。

### 复习状态

复习状态保存在 `notes.review_status`，可选值为：

- `not_started`
- `learning`
- `review`
- `mastered`

更新接口是 `PUT /api/notes/{id}/review-status`。

### 学习任务

学习任务通过 `/api/tasks` CRUD。任务包含标题、截止日期和 `todo`、`in_progress`、`done` 状态，可以通过 `study_task_notes` 关联多篇笔记。

`StudyTaskService` 会检查任务和所有关联笔记都属于当前用户。

### Markdown 和图片

前端使用 `marked` 把 Markdown 转成 HTML，并使用 DOMPurify 清理 HTML，降低 XSS 风险。

图片在浏览器中转成 Base64 Data URL，再作为 `<img>` 字符串嵌入笔记正文。缩放、改名和裁剪也在浏览器完成。当前没有独立图片表或对象存储。

## 8. 数据库主要表和关系

```text
users 1 ---- N notes
users 1 ---- N tags
users 1 ---- N study_tasks

notes N ---- M tags          通过 note_tags
notes 1 ---- N revisions     通过 note_revisions
study_tasks N ---- M notes   通过 study_task_notes
notes N ---- M notes         note_links 表已存在，但应用层未完成
```

主要表：

- `users`：用户、邮箱、BCrypt 密码哈希和时间戳
- `notes`：标题、Markdown 正文、AI 摘要、active/trash 状态、收藏、复习状态和所属用户
- `tags`：标签名、颜色和所属用户
- `note_tags`：笔记与标签的连接表
- `note_revisions`：笔记旧标题和旧正文快照
- `study_tasks`：学习任务及所属用户
- `study_task_notes`：学习任务和笔记的多对多连接表
- `note_links`：为笔记互链准备的表，目前未被应用代码使用

数据库变化由 `V1` 到 `V6` Flyway migration 管理。Spring Boot 启动时执行尚未应用的迁移。Hibernate 使用 `ddl-auto: validate`，只检查 Entity 和数据库结构是否匹配。

## 9. AI 摘要完整流程

```text
User clicks Generate Summary
  -> NoteEditorView.generateSummary()
  -> POST /api/notes/{id}/summarize
  -> JWT authentication
  -> NoteController.summarize()
  -> NoteSummaryService.summarize()
  -> NoteRepository.findByIdAndUser()
  -> AiSummaryProvider
  -> OpenAiSummaryProvider 或 LocalSummaryProvider
  -> AI response
  -> NoteSummaryResponse
  -> Vue 收到 summary
  -> PUT /api/notes/{id}/summary
  -> 保存到 notes.summary
  -> 页面显示只读摘要和 Summary saved
```

### 前端发生什么

`NoteEditorView.vue` 中的 `generateSummary()`：

1. 把按钮改成 `Generating...`。
2. 调用 `summarizeNote(id)`。
3. 收到摘要后写入 `summaryDraft`。
4. 立即调用 `saveNoteSummary(id, result.summary)`。
5. 保存成功后显示只读摘要和 `Summary saved`。

摘要按钮只出现在已经保存、拥有 ID 的笔记上。

### 后端如何取得正文

`NoteSummaryService` 使用：

```java
notes.findByIdAndUser(id, user)
```

只有确认笔记属于当前用户之后，才调用 Provider。因此别人的笔记内容不会被发送给 AI。

这里取的是数据库中已经保存的 `note.content`。如果用户在编辑器里改了内容但还没有点击 `Save note`，AI 仍会总结数据库里的旧版本。

### Prompt 在哪里构造

Prompt 在 `OpenAiSummaryProvider.java` 中构造。发送给 OpenAI-compatible API 的 JSON 核心结构是：

```json
{
  "model": "配置的模型名称",
  "temperature": 0.2,
  "messages": [
    {
      "role": "system",
      "content": "Summarize the user's note clearly and briefly. Return only the summary."
    },
    {
      "role": "user",
      "content": "数据库中保存的完整笔记正文"
    }
  ]
}
```

不会发送用户密码、JWT、用户 ID、标签或 API Key 给模型正文；API Key 只放在 HTTP Authorization header 中。

### OpenAI / DeepSeek 调用

Provider 使用 Java 标准 `HttpClient` 请求 `AI_API_URL`，并设置：

```text
Authorization: Bearer <AI_API_KEY>
Content-Type: application/json
```

默认配置是 OpenAI Chat Completions。DeepSeek 提供兼容接口，因此设置：

```text
AI_API_URL=https://api.deepseek.com/v1/chat/completions
AI_MODEL=deepseek-chat
```

即可使用 DeepSeek。项目没有单独引入 DeepSeek SDK。

### AI 返回数据如何处理

后端预期 AI 返回 OpenAI-compatible JSON，并读取：

```text
choices[0].message.content
```

提取后会 trim 空白并包装成：

```json
{
  "success": true,
  "data": {
    "noteId": 123,
    "summary": "生成的摘要"
  },
  "error": null
}
```

超时、HTTP 429、无效密钥、无效 URL、空响应或无法解析的 JSON 会分别转换成 `AI_TIMEOUT`、`AI_RATE_LIMITED`、`AI_PROVIDER_ERROR` 等稳定错误。

### 没有 API Key 时

如果 `AI_API_KEY` 为空且 `AI_LOCAL_FALLBACK=true`，系统不会访问远程模型，而是使用 `LocalSummaryProvider`：清理部分 Markdown/HTML，提取前三句话，最多返回 500 个字符。

如果关闭 fallback 又没有 Key，则返回 `AI_NOT_CONFIGURED`。

### API Key 放在哪里

Key 应放在后端运行环境的 `AI_API_KEY`，例如未提交到 Git 的 `.env` 或服务器 Secret。Docker Compose 只把它传入 backend 容器。

不能把 Key 放在 Vue 前端，因为 Vue 最终会编译成浏览器可下载的 JavaScript。放在前端意味着任何访问者都能从开发者工具或构建文件中取得 Key，继而冒用额度和权限。

### AI 和普通 CRUD 的架构区别

普通 CRUD 基本是：

```text
Controller -> Service -> Repository -> PostgreSQL
```

AI 摘要则是：

```text
Controller -> NoteSummaryService -> Repository（读取笔记）
                                 -> AiSummaryProvider -> 外部 AI API
```

AI 调用比 CRUD 多了外部依赖、网络延迟、费用、不稳定输出、限流和超时。因此项目增加了 Provider 接口隔离供应商，并为 AI 故障设计专门错误码和本地 fallback。

当前 AI 功能还有一个实现细节：生成结果会被前端立即保存到 `notes.summary`，用户不能先编辑再确认。这与“生成内容先作为可编辑草稿、用户确认后保存”的理想流程有所不同。

## 10. Docker、Nginx、PostgreSQL、Spring Boot、Vue 的位置

- **Vue**：运行在浏览器中，负责界面和用户交互。
- **Nginx**：系统公开入口，提供 Vue 构建后的静态文件，并把 `/api` 请求代理给 Spring Boot。
- **Spring Boot**：运行后端 REST API、Spring Security、业务服务、JPA 和 AI Provider。
- **PostgreSQL**：保存用户、笔记、标签、版本和学习任务，只处于 Docker 内部数据库网络，不公开生产端口。
- **Docker Compose**：同时启动并连接 PostgreSQL、backend 和 Nginx。

网络关系：

```text
Internet / Browser
       |
       v
Nginx :80/:443
   |          \
   |           -> Vue static files
   v
Spring Boot :8080
   |
   v
PostgreSQL :5432（内部网络）
```

只有 Nginx 映射公共端口。PostgreSQL 数据放在 `postgres_data` volume 中，因此重新创建容器不会自动删除数据库文件。

## 11. 当前验证状态

整理本文档时实际执行了：

- `cd backend && mvn test`：93 个测试通过
- `cd frontend && npm run build`：生产构建通过
- `docker compose -f deploy/docker-compose.yml config --quiet`：Compose 配置校验通过

后端 Repository 测试使用 H2；本次没有执行真实 PostgreSQL 集成测试、远程 OpenAI/DeepSeek 调用或完整部署后的浏览器烟雾测试。
