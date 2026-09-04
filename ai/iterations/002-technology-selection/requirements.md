# 002 Technology Selection 需求说明

> 文档状态：Approved  
> 创建日期：2026-09-05  
> 继承需求：V1.1（`001-foundation`）  
> 上一迭代：`001-foundation`

## 1. 目标

在不改变 V1.1 产品范围的前提下，确定前端、后端、数据、认证、搜索、媒体、部署和测试技术基线，为后续数据模型、API 契约、页面交互设计及工程初始化提供稳定输入。

## 2. 本期范围

### 2.1 包含

- 确定 Nuxt、Spring Boot、PostgreSQL 和 Docker Compose 为首期运行基础。
- 确定服务端 Session、同源 Cookie、CSRF 和统一权限判断方案。
- 确定 MyBatis-Plus、Flyway、OpenAPI 和前端类型生成方案。
- 确定 Markdown、媒体存储、邮件、搜索和自动化测试技术路线。
- 确定单台 Linux VPS 的部署拓扑和 GitHub Actions 持续集成基线。

### 2.2 不包含

- 不生成 Nuxt 或 Spring Boot 工程，不安装项目依赖。
- 不定义业务数据库表、字段、索引或 Flyway 脚本。
- 不定义具体业务 API、请求响应模型或错误码清单。
- 不创建 Docker Compose、Caddy、GitHub Actions 或 Dependabot 配置文件。
- 不购买 VPS、域名、对象存储或邮件服务。

## 3. 技术基线要求

### 3.1 前端

- 使用 Node.js 24、pnpm 11、Nuxt 4、Vue 3 和 TypeScript 严格模式。
- 使用 Nuxt UI 构建公共博客前台、用户中心和平台管理端，使用 Pinia 管理需要跨页面保存的客户端状态。
- 数据请求优先使用 Nuxt `useFetch` 和 ofetch，不引入 Axios。
- 表单和运行时数据使用 Zod 校验。
- 公共内容页面支持 SSR 和按路由缓存；用户中心及管理端采用客户端渲染，不进入共享页面缓存。
- Markdown 编辑使用 CodeMirror 6，支持源码编辑和实时预览。
- Markdown 渲染使用 remark/rehype、GFM、Shiki 和严格 HTML 白名单；默认不支持用户提交原始 HTML。

### 3.2 后端

- 使用 Java 21、Spring Boot 4.1.x、Spring MVC 和 Maven Wrapper 3.9.x。
- 使用 Spring Security、Spring Session JDBC、Bean Validation、Actuator 和 Spring Mail。
- 使用 MyBatis-Plus 的 Spring Boot 4 Starter；简单 CRUD 和分页可使用 MyBatis-Plus，权限、搜索和统计查询使用显式 SQL。
- 使用 springdoc-openapi 生成 OpenAPI 3.1 文档。
- 使用 Flyway 管理全部数据库结构变化，禁止应用启动时自动修改业务表结构。

### 3.3 数据与搜索

- 使用 PostgreSQL 18 保存业务数据、审计信息和服务端 Session。
- 英文正文使用 PostgreSQL 全文检索能力；中文、标题和摘要使用 `pg_trgm`、`ILIKE` 和相似度查询补充。
- 搜索查询必须在数据库查询阶段执行内容状态、所有者和四级可见性过滤。
- 首期不引入 Redis、Meilisearch 或 Elasticsearch；搜索实现必须置于可替换的应用服务边界后。

### 3.4 认证与接口

- REST JSON API 使用 `/api/v1` 前缀，错误响应采用统一 Problem Details 结构。
- 登录采用服务端 Session；Session 保存于 PostgreSQL。
- 生产 Cookie 使用 `HttpOnly`、`Secure` 和 `SameSite=Lax`，修改类请求启用 CSRF 防护。
- 密码使用 Argon2id 摘要；用户身份只从服务端认证上下文获取。
- 前端从后端 OpenAPI 文档生成 TypeScript 类型，使用轻量类型化客户端，不生成完整业务 SDK。

### 3.5 媒体与邮件

- 首期媒体文件保存到 VPS 本地持久卷，元数据保存在 PostgreSQL。
- 媒体访问统一经过后端的所有权和内容可见性判断，不依赖不可猜测 URL 作为授权手段。
- 存储能力通过应用接口隔离，未来可替换为 S3 兼容对象存储。
- 邮件通过标准 SMTP 发送；本地开发使用 Mailpit，生产服务商通过环境变量配置。

### 3.6 部署与质量

- 生产环境使用 Docker Compose 运行 Caddy、Nuxt、Spring Boot 和 PostgreSQL。
- Caddy 负责 HTTPS 和同源路由：`/api/*` 转发到 Spring Boot，其余请求转发到 Nuxt。
- GitHub Actions 执行后端测试与打包、前端 lint/typecheck/测试/build、OpenAPI 类型同步检查和部署编排冒烟检查。
- Dependabot 仅在所选主版本范围内提出常规更新；Spring Boot、Nuxt 等跨主版本升级需单独决策。
- 后端测试使用 JUnit Jupiter、Spring Boot Test、MockMvc、Testcontainers PostgreSQL 和 ArchUnit。
- 前端测试使用 Vitest、Vue Test Utils、Nuxt Test Utils 和 Playwright。

## 4. 验收标准

- 五份标准迭代文档齐全，状态和内容一致。
- 根目录及前后端 README 明确当前技术基线和仍未初始化代码的状态。
- 所有选型均符合 `001-foundation` 的模块化单体、权限和数据隔离约束。
- 文档明确公开页面与受限页面的渲染及缓存边界。
- 文档明确首期不使用的基础设施，避免后续实现自行扩张范围。
- 本机 Java、Maven、Node.js、pnpm、Docker 和 Git 环境检查结果有记录。

