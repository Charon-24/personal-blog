# 002 Technology Selection 测试计划

> 文档状态：Verified  
> 更新日期：2026-09-05

## 1. 本迭代验证范围

本迭代只验证技术选型文档、项目说明和本机工具链，不验证尚未生成的应用、数据库、容器或流水线。

## 2. 结构检查

- `ai/iterations/002-technology-selection` 包含需求、架构、任务、测试和决策五份标准文档。
- `001-foundation` 内容保持不变。
- 根目录、后端和前端 README 指向当前迭代文档。
- `backend` 和 `frontend` 仍未出现构建文件、依赖目录或生成代码。

## 3. 一致性检查

- 技术选型继续采用一个 Nuxt 应用和一个 Spring Boot 模块化单体。
- 四级内容权限和用户数据隔离适用于 API、SSR、搜索、缓存及媒体访问。
- 前后端契约统一使用 OpenAPI，API 前缀统一为 `/api/v1`。
- 认证统一使用 Spring Security 服务端 Session 和 PostgreSQL Session 存储。
- 数据迁移统一使用 Flyway；数据访问统一使用 MyBatis-Plus 与显式 SQL。
- 部署统一使用单台 Linux VPS、Docker Compose 和 Caddy。
- Redis、MinIO、独立搜索引擎、消息队列、微服务及 Kubernetes 均不在首期基线中。

## 4. 工具链检查

- `java --version` 返回 Java 21。
- `mvn --version` 能以 Java 21 运行，版本不低于 Spring Boot 4.1 最低要求。
- `node --version` 返回 Node.js 24。
- `pnpm --version` 返回 pnpm 11。
- `docker --version` 和 `git --version` 可正常执行。

## 5. 后续工程测试基线

- 后端使用 Testcontainers PostgreSQL 验证 Flyway、Mapper、Session 和真实数据库查询。
- 权限测试覆盖跨账号资源编号篡改、四级可见性、权限收紧、搜索过滤和媒体越权。
- Markdown 测试覆盖脚本、事件属性、危险 URL、外部链接和代码块。
- 前端覆盖公共页面 SSR、用户中心客户端渲染、登录失效、CSRF 和禁止私人缓存。
- Playwright 覆盖注册验证、登录、创作、发布、授权、搜索和互动核心流程。
- CI 检查 OpenAPI 生成类型与后端契约同步，并执行前后端构建和部署冒烟测试。

## 6. 完成标准

- 结构、相对链接和关键选型检查全部通过。
- 本机工具链满足后续建项要求，已知的系统 Maven 版本差异由 Maven Wrapper 解决。
- 需求、架构、任务、测试和决策文档互相一致。
- 未修改历史迭代，未提前初始化应用或基础设施。
