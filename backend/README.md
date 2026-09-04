# Backend

本目录用于存放 Personal Blog 的 Spring Boot 后端服务。

## 当前状态

技术栈已经在 `002-technology-selection` 确认，但尚未初始化 Spring Boot 工程、构建文件、数据库或第三方依赖。

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

## 后续建项输入

- [产品需求](../ai/iterations/001-foundation/requirements.md)
- [当前架构设计](../ai/iterations/002-technology-selection/architecture-design.md)
- [技术决策](../ai/iterations/002-technology-selection/decisions.md)
