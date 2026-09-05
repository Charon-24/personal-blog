# Backend

本目录用于存放 Personal Blog 的 Spring Boot 后端服务。

## 当前状态

Spring Boot MVP 已初始化。IDEA 可直接导入本目录的 `pom.xml`，项目统一使用 Maven Wrapper 3.9.11。

## 技术基线

- Java 21、Spring Boot 4.1.x、Spring MVC 和 Maven Wrapper 3.9.x。
- MyBatis-Plus Spring Boot 4 Starter、PostgreSQL 18 和 Flyway。
- Spring Security、Spring Session JDBC、Bean Validation、Actuator 和 Spring Mail。
- springdoc-openapi 生成 OpenAPI 3.1，REST API 使用 `/api/v1` 前缀。
- JUnit Jupiter、MockMvc、Testcontainers PostgreSQL 和 ArchUnit。

## 架构方向

首期采用单部署单元的模块化单体，预定业务边界如下：

- 用户模块：账号、认证、账号状态和个人资料。
- 内容模块：博客、笔记、草稿、分类、标签和归档。
- 访问授权模块：四级内容权限和指定用户授权名单。
- 互动模块：点赞、评论、回复和互动通知。
- 媒体模块：头像、封面和正文图片。
- 平台管理模块：用户治理、内容治理、站点配置和审计。
- 公共基础模块：统一异常、校验、日志和共享基础能力。

模块之间应通过明确的应用服务边界协作，不得直接跨模块修改内部数据。

## 本地启动

先在仓库根目录启动 `compose.dev.yaml`。随后设置环境变量：

- `DB_PASSWORD`：与根目录 `.env` 一致，必填。
- `APP_COOKIE_SECURE=false`：仅本地 HTTP 联调使用。
- 可选 `BOOTSTRAP_ADMIN_USERNAME`、`BOOTSTRAP_ADMIN_EMAIL`、`BOOTSTRAP_ADMIN_PASSWORD`：仅在尚无管理员时初始化一个管理员。

在 IDEA 运行 `com.charon.personalblog.PersonalBlogApplication`，或执行 `.\\mvnw.cmd spring-boot:run`。

Swagger UI：`http://localhost:8080/swagger-ui.html`；健康检查：`http://localhost:8080/actuator/health`。

## 设计输入

- [产品需求](../ai/iterations/001-foundation/requirements.md)
- [当前架构设计](../ai/iterations/003-mvp-implementation/architecture-design.md)
- [当前任务](../ai/iterations/003-mvp-implementation/tasks.md)
