# 003 MVP Implementation 架构设计

> 文档状态：Approved  
> 对应需求：V1.1 与 003-mvp-implementation/requirements.md  
> 更新日期：2026-09-05

## 1. 本地联调拓扑

    浏览器 :3000 -> Nuxt 开发服务器 -> /api 代理 -> Spring Boot :8080
                                                       |-> PostgreSQL 18 :5433
                                                       |-> Mailpit SMTP :1025
                                                       `-> 本地媒体卷

IDEA 启动后端，VS Code 启动前端，Docker Compose 只承载开发基础设施。生产镜像与 Caddy 配置作为工程基线提供，但不在本迭代部署。

## 2. 后端模块

- common：统一响应错误、分页、审计、认证上下文和基础配置。
- user：注册验证、登录身份、密码、资料和用户状态。
- content：博客/笔记、生命周期、分类、标签、归档和搜索。
- authorization：内容四级权限和指定用户授权判断。
- media：上传校验、元数据、本地文件适配器和鉴权读取。
- interaction：点赞、评论、回复和通知。
- administration：站点配置、友情链接、平台统计、用户和内容治理。

Controller 只处理 HTTP 转换，Application Service 组织用例，Mapper 负责持久化。跨模块通过公开服务协作，不直接调用其他模块 Mapper。

## 3. 数据模型

核心表为用户、邮箱令牌、内容、内容授权、分类、标签、内容标签、媒体、点赞、评论、通知、友情链接、站点配置和审计日志。所有用户资源包含不可伪造的所有者字段；内容使用状态和删除时间实现逻辑删除。

Flyway 创建业务表、Spring Session 表和 pg_trgm 扩展。数据库生成 UUID，时间统一存储为带时区时间。

## 4. 接口与安全

- API 固定 /api/v1，公开、登录用户、创作中心和管理端路径分区。
- 使用 JSON 登录处理器和服务端 Session；CSRF Token 通过可读取 Cookie 提供，Session Cookie 保持 HttpOnly。
- 无权访问内容统一返回 404，避免区分不存在与无权限。
- 列表和搜索 SQL 在查询阶段加入状态、所有者和可见性条件。
- OpenAPI 描述 DTO；前端生成类型，不把数据库实体直接作为接口模型。

## 5. 前端结构

- 公共区：首页、用户主页、内容详情、分类、标签、归档、搜索、关于、友链、登录注册和错误页。
- 用户区：资料、控制台、内容列表、编辑器、分类标签、互动、授权和媒体库。
- 管理区：控制台、用户、内容、互动、公共内容、站点设置和审计。
- useApi 统一处理 API 基址、Cookie、CSRF 和 Problem Details；Pinia 只保存认证和编辑草稿状态。
- 公共页面 SSR；/studio/** 与 /admin/** 关闭 SSR 并通过中间件校验身份。

## 6. 验证策略

- 后端使用 PostgreSQL Testcontainers 执行迁移和权限集成测试。
- 前端执行 ESLint、TypeScript、Vitest 和 Nuxt production build。
- Docker Compose 通过健康检查验证 PostgreSQL 和 Mailpit。
- GitHub Actions 在 Pull Request 和推送到 dev/main 时执行检查。

