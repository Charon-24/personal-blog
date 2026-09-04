# 002 Technology Selection 决策记录

> 文档状态：Approved  
> 更新日期：2026-09-05

## DEC-006：使用单个 Nuxt 4 应用

- 状态：Accepted
- 决策：前端使用 Node.js 24、pnpm 11、Nuxt 4、Vue 3、TypeScript、Nuxt UI 和 Pinia；公共前台、用户中心和管理端位于同一工程。
- 原因：Nuxt 混合渲染同时满足公开内容 SEO 和后台交互需求，单工程减少共享组件、登录状态和部署配置重复。
- 未采用方案：纯 Vue SPA；公开站与后台拆成两个工程。
- 复查条件：前后台形成独立团队、发布节奏或部署边界。

## DEC-007：采用混合渲染和严格缓存边界

- 状态：Accepted
- 决策：公开内容使用 SSR 和受控缓存，用户中心与管理端使用客户端渲染；非公开内容禁止共享缓存和搜索引擎收录。
- 原因：保证公开阅读体验和 SEO，同时降低 Session 参与 SSR 时的数据泄露风险。
- 复查条件：出现必须服务端渲染的登录态页面，并有完整的按身份缓存设计。

## DEC-008：采用 Java 21 与 Spring Boot 4.1

- 状态：Accepted
- 决策：后端使用 Java 21、Spring Boot 4.1.x、Spring MVC 和 Maven Wrapper 3.9.x。
- 原因：Java 21 是 LTS，本机环境已具备；Spring Boot 4.1 支持 Java 21，适合绿地项目使用当前主线。
- 未采用方案：Spring Boot 3.5；WebFlux。
- 补充：系统 Maven 3.6.3 达到最低要求，但工程通过 Wrapper 固定较新的 3.9.x。

## DEC-009：使用 MyBatis-Plus 并保留显式 SQL

- 状态：Accepted
- 决策：使用 MyBatis-Plus Spring Boot 4 Starter；基础 CRUD 使用其通用能力，复杂权限、搜索和统计查询编写显式 SQL。
- 原因：兼顾开发效率和权限查询的可审查性。
- 未采用方案：Spring Data JPA；jOOQ；使用多租户插件代替业务权限模型。

## DEC-010：PostgreSQL 同时承载业务、Session 和首期搜索

- 状态：Accepted
- 决策：使用 PostgreSQL 18、Flyway、Spring Session JDBC、全文检索和 `pg_trgm`。
- 原因：首期单机低流量场景下减少基础设施，同时满足事务、审计、会话和中英混合基础搜索需求。
- 未采用方案：Redis Session；Meilisearch；Elasticsearch/OpenSearch；自编译中文分词扩展。
- 复查条件：出现可量化的搜索相关性、性能或多实例会话瓶颈。

## DEC-011：使用服务端 Session 和同源 Cookie

- 状态：Accepted
- 决策：Spring Security 管理认证，Session 存入 PostgreSQL；Cookie 使用 `HttpOnly`、`Secure`、`SameSite=Lax`，写请求启用 CSRF，密码使用 Argon2id。
- 原因：系统首期面向浏览器和 Nuxt SSR，同源服务端会话便于即时撤销并减少刷新令牌复杂度。
- 未采用方案：JWT 双令牌；独立身份平台。

## DEC-012：OpenAPI 作为前后端契约来源

- 状态：Accepted
- 决策：API 使用 `/api/v1`、JSON 和统一 Problem Details；后端生成 OpenAPI 3.1，前端生成 TypeScript 类型并使用轻量客户端。
- 原因：避免手写类型漂移，同时不引入大量生成业务代码。
- 未采用方案：前端手写重复类型；生成完整 SDK。

## DEC-013：Markdown 源码编辑与安全渲染

- 状态：Accepted
- 决策：使用 CodeMirror 6、remark/rehype、GFM、Shiki 和严格 HTML 白名单，默认禁用原始 HTML。
- 原因：满足 Markdown、代码块和实时预览需求，并将 XSS 防护作为默认行为。
- 未采用方案：所见即所得编辑器；允许任意 HTML。

## DEC-014：媒体使用本地持久卷，邮件使用 SMTP

- 状态：Accepted
- 决策：媒体经抽象存储接口写入 VPS 持久卷并由后端鉴权访问；邮件通过 SMTP 发送，本地使用 Mailpit。
- 原因：首期部署简单且不绑定供应商，同时保留迁移到 S3 的边界。
- 未采用方案：同机 MinIO；开发期即绑定云对象存储。
- 复查条件：媒体容量、流量、可靠性或多实例部署超出单机能力。

## DEC-015：使用 Docker Compose、Caddy 和 GitHub Actions

- 状态：Accepted
- 决策：生产使用 Docker Compose 和 Caddy；CI 使用 GitHub Actions，依赖更新使用 Dependabot。
- 原因：适合单台 Linux VPS，Caddy 简化 HTTPS，GitHub Actions 能统一验证前后端、契约和容器构建。
- 未采用方案：Kubernetes；云厂商专用网关；首期自行维护 Jenkins。

## DEC-016：核心链路采用分层自动化测试

- 状态：Accepted
- 决策：后端使用 JUnit Jupiter、MockMvc、Testcontainers 和 ArchUnit；前端使用 Vitest、Nuxt Test Utils 与 Playwright。
- 原因：真实 PostgreSQL 集成测试能覆盖权限和 SQL 行为，端到端测试覆盖浏览器会话与关键用户流程。
- 复查条件：测试耗时影响反馈周期时，优先优化分层和并行执行，不直接删除权限测试。

