# Personal Blog

Personal Blog 是一个面向多用户的博客与笔记平台，也是一个使用 AI 完成需求分析、设计、开发、测试和迭代管理的 Vibe Coding 练习项目。

## 当前状态

- 当前迭代：`002-technology-selection`
- 当前阶段：技术栈基线已确认，等待数据模型、API 和页面交互设计
- 后端路线：Java 21 + Spring Boot 4.1 + MyBatis-Plus 模块化单体
- 前端路线：Node.js 24 + Nuxt 4 + Vue 3 + TypeScript + Nuxt UI
- 代码状态：尚未生成前后端应用代码

## 目录导航

- [backend](backend/README.md)：后端职责、模块边界和后续建项入口。
- [frontend](frontend/README.md)：前端职责、应用边界和后续建项入口。
- [ai](ai/README.md)：AI 协作规则、迭代文档和文档模板。
- [产品需求基线](ai/iterations/001-foundation/requirements.md)：V1.1 首期产品需求。
- [当前需求](ai/iterations/002-technology-selection/requirements.md)：技术选型范围和验收标准。
- [架构设计](ai/iterations/002-technology-selection/architecture-design.md)：技术架构、运行拓扑和工程约束。
- [任务清单](ai/iterations/002-technology-selection/tasks.md)：当前迭代任务与验证记录。
- [测试计划](ai/iterations/002-technology-selection/test-plan.md)：选型检查和后续测试基线。
- [决策记录](ai/iterations/002-technology-selection/decisions.md)：具体技术决策及复查条件。

## 工作方式

1. 开始开发前先阅读根目录的 [AGENTS.md](AGENTS.md)。
2. 以当前迭代的 `requirements.md` 为产品需求来源，以 `architecture-design.md` 为技术边界。
3. 需求发生变化时创建新迭代目录，不直接覆盖已结束迭代。
4. 每次实现同步更新任务、测试和决策文档。
5. 数据模型、API 契约和页面交互确认后，再分别初始化 `backend` 和 `frontend` 工程。

## 下一步

- 设计核心数据模型、索引、迁移边界和权限矩阵。
- 定义 `/api/v1` 契约、Problem Details 扩展和 OpenAPI 生成流程。
- 完成公共前台、用户中心和管理端的主要页面交互设计。
- 初始化 Git、Nuxt、Spring Boot、本地 Docker Compose 和 CI 工程基线。
