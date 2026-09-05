# 003 MVP Implementation 任务清单

> 文档状态：Approved  
> 更新日期：2026-09-05

## 设计与建项

- [x] 明确 MVP 范围、默认产品规则和本地联调拓扑。
- [x] 初始化 Spring Boot、Nuxt、Docker Compose、Caddy 和 CI 工程。
- [x] 建立 Flyway 数据模型和 OpenAPI 基线。

## 后端

- [x] 实现账号、资料、Session、CSRF 和密码流程。
- [x] 实现内容生命周期、四级权限、分类标签、归档和搜索。
- [x] 实现媒体、点赞、评论、回复和通知。
- [x] 实现站点配置、友情链接、统计、治理和审计。
- [x] 补充权限、迁移和核心 API 自动化测试。

## 前端

- [x] 实现公共页面与认证入口。
- [x] 实现用户中心、Markdown 编辑和权限管理。
- [x] 实现互动、媒体和管理页面。
- [x] 补充类型检查、组件测试和构建验证。

## 交付

- [x] 完成本地联调和 Docker 基础设施验证。
- [x] 更新 README、测试结果和遗留问题。
- [x] 提交并推送到 origin/dev。

## 验证记录

- `backend\\mvnw.cmd -B verify`：通过，5 个测试，0 失败、0 错误、0 跳过；Testcontainers 使用 PostgreSQL 18.6。
- `pnpm lint`、`pnpm typecheck`、`pnpm test`、`pnpm build`：全部通过；Markdown XSS 单元测试 1 个通过。
- `pnpm test:e2e`：通过；复用本机 Edge 执行公共页面导航冒烟测试 1 个。
- OpenAPI 3.1 文档生成及前端 TypeScript 类型生成：通过。
- `docker compose` 开发环境：PostgreSQL 18 和 Mailpit 健康检查通过；Spring Boot 启动、Flyway 迁移及 API 文档访问通过。

## 遗留事项

- 生产 VPS、域名、HTTPS 证书和真实 SMTP 服务商需要在获得部署环境后验证。
- 当前自动化覆盖关键权限路径；更完整的浏览器角色矩阵和后台操作回归将在后续迭代继续补充。
