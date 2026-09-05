<template>
  <div class="workspace">
    <aside>
      <NuxtLink
        to="/"
        class="brand"
      >PERSONAL / BLOG</NuxtLink>
      <NuxtLink to="/studio">概览</NuxtLink>
      <NuxtLink to="/studio/contents">内容</NuxtLink>
      <NuxtLink to="/studio/categories">分类与标签</NuxtLink>
      <NuxtLink to="/studio/media">媒体库</NuxtLink>
      <NuxtLink to="/studio/notifications">通知</NuxtLink>
      <NuxtLink to="/studio/profile">个人资料</NuxtLink>
      <NuxtLink to="/admin">管理后台</NuxtLink>
    </aside>
    <main><slot /></main>
  </div>
</template>
