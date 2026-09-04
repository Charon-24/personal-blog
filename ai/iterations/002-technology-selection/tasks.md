# 002 Technology Selection 任务清单

> 文档状态：Verified  
> 更新日期：2026-09-05

## 状态说明

- `[ ]`：待办
- `[-]`：进行中
- `[x]`：已完成并验证

## 本迭代任务

- [x] 继承 `001-foundation` 的 V1.1 需求和模块化单体约束。
- [x] 确认 Nuxt 4、Vue 3、TypeScript、Nuxt UI 和 Pinia 前端技术路线。
- [x] 确认 Java 21、Spring Boot 4.1、Maven Wrapper 和 MyBatis-Plus 后端技术路线。
- [x] 确认 PostgreSQL、Flyway、Spring Session JDBC 和混合语言搜索方案。
- [x] 确认服务端 Session、CSRF、Argon2id 和同源部署安全边界。
- [x] 确认 Markdown、媒体本地持久卷和 SMTP 邮件方案。
- [x] 确认 Docker Compose、Caddy、GitHub Actions 和自动化测试方案。
- [x] 检查本机 Java、Maven、Node.js、pnpm、Docker 和 Git 版本。
- [x] 更新项目入口和前后端技术说明。
- [x] 校验迭代文档结构、链接、状态和关键选型覆盖。

## 验证记录

- Java：21.0.12，符合 Java 21 基线。
- 系统 Maven：3.6.3，满足 Spring Boot 4.1 最低要求；工程将使用 Maven Wrapper 3.9.x。
- Node.js：24.20.0，符合 Node.js 24 基线。
- pnpm：11.19.0，符合 pnpm 11 基线。
- Docker：29.1.3，可用于后续本地编排与镜像验证。
- Git：2.48.1；当前项目目录尚未初始化为 Git 仓库。
- 本迭代未生成应用代码、未安装项目依赖、未创建部署资源。

## 后续任务

- [ ] 创建下一迭代，设计核心数据模型、索引和 Flyway 迁移边界。
- [ ] 定义 `/api/v1` 业务契约、Problem Details 扩展和权限矩阵。
- [ ] 设计公开前台、用户中心和管理端的主要页面流程。
- [ ] 初始化 Git 仓库、Nuxt 工程、Spring Boot 工程和本地 Docker Compose。
- [ ] 建立 GitHub 远端后启用 Actions 和 Dependabot。

