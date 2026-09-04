# Frontend

本目录用于存放 Personal Blog 的 Vue Web 应用。

## 当前状态

技术栈已经在 `002-technology-selection` 确认，但尚未初始化 Nuxt 工程、构建文件或项目依赖。

## 技术基线

- Node.js 24、pnpm 11、Nuxt 4、Vue 3 和 TypeScript 严格模式。
- Nuxt UI、Pinia、Zod、`useFetch`/ofetch 和 OpenAPI 生成类型。
- CodeMirror 6、remark/rehype、GFM、Shiki 和安全 HTML 白名单。
- 公共页面使用 SSR，用户中心和管理端使用客户端渲染。
- Vitest、Vue Test Utils、Nuxt Test Utils 和 Playwright。

## 应用范围

前端预计覆盖三个用户界面区域：

- 公共博客前台：首页、用户主页、博客详情、分类、标签、归档和搜索。
- 用户中心：个人资料、博客与笔记创作、权限设置、媒体库和互动管理。
- 平台管理端：用户管理、内容治理、互动治理、站点设置和审计查询。

所有页面都必须正确处理未登录、无权限、空数据、加载中、失败和内容已失效等状态。前端展示不能替代后端权限校验。

## 后续建项输入

- [产品需求](../ai/iterations/001-foundation/requirements.md)
- [当前架构设计](../ai/iterations/002-technology-selection/architecture-design.md)
- [测试计划](../ai/iterations/002-technology-selection/test-plan.md)
