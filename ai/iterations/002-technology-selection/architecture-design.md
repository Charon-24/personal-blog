# 002 Technology Selection 架构设计

> 文档状态：Approved  
> 对应需求：V1.1 与 `002-technology-selection/requirements.md`  
> 更新日期：2026-09-05

## 1. 总体架构

首期继续采用 `001-foundation` 确定的渐进式模块化单体。运行时由一个 Nuxt Web 应用、一个 Spring Boot API、一个 PostgreSQL 实例和 Caddy 入口组成；Mailpit 仅用于本地开发。

```text
浏览器
  |
  v
Caddy（HTTPS / 同源路由）
  |-- 页面与前端资源 --> Nuxt 4
  `-- /api/* --------> Spring Boot 4.1
                              |-- PostgreSQL 18
                              |-- 本地媒体持久卷
                              `-- SMTP
```

不引入独立网关、Redis、对象存储、搜索引擎、消息队列或微服务组件。

## 2. 前端设计

### 2.1 工程基线

| 范畴 | 选择 |
| --- | --- |
| 运行时 | Node.js 24 |
| 包管理 | pnpm 11，提交锁文件并固定 `packageManager` |
| 框架 | Nuxt 4、Vue 3、TypeScript strict |
| UI | Nuxt UI |
| 状态 | Pinia，仅保存跨页面客户端状态 |
| 请求 | `useFetch`、ofetch、OpenAPI 生成类型 |
| 校验 | Zod |
| 单元/组件测试 | Vitest、Vue Test Utils、Nuxt Test Utils |
| 端到端测试 | Playwright |

Vue Router、SSR 数据获取和页面元信息使用 Nuxt 内建能力，不重复引入同类框架。服务端数据优先由页面级 `useFetch` 获取，Pinia 不作为服务端数据缓存层。

### 2.2 渲染和缓存边界

- 首页、公开博客、公开用户主页、分类、标签、归档、搜索和公共信息页使用 SSR。
- `/studio/**` 用户中心和 `/admin/**` 管理端使用客户端渲染，并设置禁止共享缓存和禁止搜索引擎收录。
- 仅明确为“已发布且公开”的响应可以进入页面缓存；登录可见、指定用户可见、仅自己可见、草稿和已下线内容不得使用公共缓存。
- Nuxt SSR 请求公开 API 时使用容器内部地址；浏览器请求统一访问同源 `/api`。

### 2.3 Markdown

- 数据库存储原始 Markdown，不存储未经约束的用户 HTML。
- CodeMirror 6 提供编辑、快捷键和大文本体验。
- 预览和详情页共用 remark/rehype 渲染管线，启用 GFM 和 Shiki 代码高亮。
- 渲染管线不启用原始 HTML，并通过 `rehype-sanitize` 白名单限制链接、图片和代码相关属性。
- 外部链接补充安全的 `rel` 属性，图片只允许受支持协议和平台媒体地址。

## 3. 后端设计

### 3.1 工程基线

| 范畴 | 选择 |
| --- | --- |
| Java | Java 21 LTS |
| 框架 | Spring Boot 4.1.x、Spring MVC |
| 构建 | Maven Wrapper 3.9.x |
| 数据访问 | MyBatis-Plus Spring Boot 4 Starter |
| 数据迁移 | Flyway |
| 安全 | Spring Security、Spring Session JDBC、Argon2id |
| API 文档 | springdoc-openapi、OpenAPI 3.1 |
| 运维 | Spring Boot Actuator、结构化应用日志 |
| 邮件 | Spring Mail、SMTP |

后端保持单一 Maven 应用和单一部署单元，内部继续按照用户、内容、访问授权、互动、媒体、平台管理和公共基础模块组织。使用 ArchUnit 验证模块依赖方向。

### 3.2 数据访问规则

- MyBatis-Plus 的 `BaseMapper`、Wrapper 和分页插件只用于边界清晰的基础查询。
- 内容可见性、所有权、搜索、统计、批量操作和管理员处置使用明确的 Mapper 方法及 SQL。
- 不启用 MyBatis-Plus 多租户插件代替业务权限；平台公共数据、作者数据和授权数据不符合单一租户列模型。
- 所有写操作由应用服务根据认证上下文补充所有者，不接受客户端用户编号作为最终归属。
- 复杂查询必须把可见性条件落实到 SQL，不允许先查询越权数据再仅靠序列化层过滤。

## 4. 数据、搜索和会话

- PostgreSQL 18 是唯一业务数据库，使用 Flyway 按版本迁移。
- Spring Session JDBC 与业务表共用实例但使用独立表；将来多实例部署时无需更改浏览器认证协议。
- 英文搜索使用加权 `tsvector`/`tsquery`；标题、摘要和中文内容使用 `pg_trgm`、`ILIKE` 与相似度评分。
- 搜索应用服务封装查询和排序策略，以便后续替换为 Meilisearch；数据库仍是真实数据来源。
- 首期不增加 Redis。只有出现多实例共享缓存、数据库 Session 压力或可量化热点后才重新评估。

## 5. 认证与 API

- Caddy 对外提供单一站点来源，避免生产环境跨域认证。
- 登录成功后由 Spring Security 创建服务端 Session；浏览器只保存不可被脚本读取的会话 Cookie。
- 写请求必须携带 CSRF Token；Nuxt 客户端负责从允许读取的 CSRF Cookie 或响应中取值并放入请求头。
- Cookie 在生产环境启用 `Secure`、`HttpOnly`、`SameSite=Lax`，开发环境仅对 `Secure` 做环境化调整。
- REST API 固定 `/api/v1` 前缀；业务错误使用 Problem Details 扩展字段承载稳定错误码和请求追踪标识。
- OpenAPI 是前后端契约来源；生成的 TypeScript 类型进入前端源码，CI 检查重新生成后不存在差异。

## 6. 媒体和邮件

- 媒体模块定义保存、读取、删除和生成访问响应的存储端口，首期适配器写入 Docker 持久卷。
- 数据库记录所有者、用途、真实媒体类型、大小、校验值和关联内容；文件名和 URL 不参与授权判断。
- 私有或受限媒体经 Spring Boot 鉴权后响应；公开媒体仅在所属内容仍公开时允许缓存。
- 本地 SMTP 指向 Mailpit；生产环境通过密钥管理或服务器环境变量配置 SMTP，不提交凭据。

## 7. 部署与运维

- 本地开发通过 Docker Compose 启动 PostgreSQL 和 Mailpit，Nuxt 与 Spring Boot 可直接在宿主机运行以便调试。
- 生产环境通过 Docker Compose 启动 Caddy、Nuxt、Spring Boot 和 PostgreSQL，媒体与数据库使用独立持久卷。
- Caddy 自动管理 HTTPS，`/api/*` 路由到 Spring Boot，其余路由到 Nuxt。
- Spring Boot Actuator 仅公开必要的健康检查；管理端点不直接暴露到公网。
- PostgreSQL 使用定时 `pg_dump`，媒体卷独立备份；恢复流程必须在上线迭代中演练。

## 8. 持续集成与测试

- GitHub Actions 分为后端、前端、契约和容器冒烟检查，允许无依赖的任务并行执行。
- 后端流水线运行 Maven 测试和打包，使用 Testcontainers PostgreSQL 验证真实迁移与查询行为。
- 前端流水线运行 lint、TypeScript 检查、Vitest 和 Nuxt 生产构建；关键浏览器流程由 Playwright 执行。
- 契约检查从后端 OpenAPI 重新生成前端类型，并在生成结果与仓库内容不一致时失败。
- Docker Compose 冒烟检查只验证镜像启动、依赖健康状态和 Caddy 路由，不在 CI 中执行生产部署。
- Dependabot 管理 Maven、pnpm、GitHub Actions 和容器镜像依赖更新；流水线不得依赖生产密钥。

## 9. 依赖与版本策略

- 初始化时固定当时选定分支的最新稳定补丁版本，并提交 Maven Wrapper、`pnpm-lock.yaml` 和容器镜像版本。
- Java 固定 21；Spring Boot 固定 4.1.x；Nuxt 固定 4.x；PostgreSQL 固定 18.x。
- Dependabot 可提出补丁和同主版本更新，跨主版本升级必须新建决策记录并完成回归验证。
- 新依赖必须解决明确需求，不并行引入功能重复的请求、状态、校验或 UI 库。

## 10. 后续设计输入

下一迭代基于本设计完成核心数据模型、REST API 契约、错误码、权限判定矩阵和主要页面交互；这些内容确认后再初始化前后端工程与部署配置。
