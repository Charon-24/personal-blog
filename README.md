# Personal Blog

Personal Blog 是一个面向多用户的博客与笔记平台，也是一个使用 AI 完成需求分析、设计、开发、测试和迭代管理的 Vibe Coding 练习项目。

## 当前状态

- 当前迭代：`003-mvp-implementation`
- 当前阶段：MVP 设计与工程实现
- 后端路线：Java 21 + Spring Boot 4.1 + MyBatis-Plus 模块化单体
- 前端路线：Node.js 24 + Nuxt 4 + Vue 3 + TypeScript + Nuxt UI
- 代码状态：MVP 已初始化并覆盖认证、创作、权限、互动、媒体与管理主流程

## 目录导航

- [backend](backend/README.md)：后端职责、模块边界和后续建项入口。
- [frontend](frontend/README.md)：前端职责、应用边界和后续建项入口。
- [ai](ai/README.md)：AI 协作规则、迭代文档和文档模板。
- [产品需求基线](ai/iterations/001-foundation/requirements.md)：V1.1 首期产品需求。
- [当前需求](ai/iterations/003-mvp-implementation/requirements.md)：MVP 范围、默认规则和验收标准。
- [架构设计](ai/iterations/003-mvp-implementation/architecture-design.md)：模块、数据、接口和联调设计。
- [任务清单](ai/iterations/003-mvp-implementation/tasks.md)：当前实现任务与验证记录。
- [测试计划](ai/iterations/003-mvp-implementation/test-plan.md)：功能、安全和工程测试基线。
- [决策记录](ai/iterations/003-mvp-implementation/decisions.md)：本迭代产品与实现决策。

## 工作方式

1. 开始开发前先阅读根目录的 [AGENTS.md](AGENTS.md)。
2. 以当前迭代的 `requirements.md` 为产品需求来源，以 `architecture-design.md` 为技术边界。
3. 需求发生变化时创建新迭代目录，不直接覆盖已结束迭代。
4. 每次实现同步更新任务、测试和决策文档。
5. 数据模型、API 契约和页面交互发生变化时，同步更新当前迭代文档。

## 本地联调

1. 从 `.env.example` 复制一份不纳入 Git 的 `.env`，至少填写 `POSTGRES_PASSWORD`。
2. 执行 `docker compose -f compose.dev.yaml up -d`，启动 PostgreSQL 18（本机端口 5433）与 Mailpit。
3. IDEA 打开 `backend/pom.xml`，在运行配置中设置与 `.env` 一致的 `DB_PASSWORD`，启动 `PersonalBlogApplication`（8080）。
4. VS Code 打开 `frontend`，执行 `corepack enable`、`pnpm install`、`pnpm dev`（3000）。
5. 浏览器访问 `http://localhost:3000`；邮件调试界面为 `http://localhost:8025`，OpenAPI 为 `http://localhost:8080/swagger-ui.html`。

完整容器运行使用 `docker compose up --build -d`，入口由 Caddy 暴露在 80/443。

## 常用检查

- 后端：`cd backend && ./mvnw verify`
- 前端：`cd frontend && pnpm lint && pnpm typecheck && pnpm test && pnpm build`
- Compose：设置 `POSTGRES_PASSWORD` 后执行 `docker compose config --quiet`

## 下一步

- 使用 IDEA 与 VS Code 完成注册邮件、发布、指定用户访问和互动的人工联调。
- 在真实内容规模出现搜索相关性瓶颈后，再评估 Meilisearch。
