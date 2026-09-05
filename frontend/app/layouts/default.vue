<script setup lang="ts">
const auth = useAuthStore()
onMounted(() => auth.load())
</script>

<template>
  <div>
    <header class="site-header">
      <NuxtLink
        to="/"
        class="brand"
      >PERSONAL / BLOG</NuxtLink>
      <nav>
        <NuxtLink to="/search">搜索</NuxtLink>
        <NuxtLink to="/archive">归档</NuxtLink>
        <NuxtLink to="/about">关于</NuxtLink>
        <NuxtLink to="/links">友链</NuxtLink>
        <NuxtLink
          v-if="auth.user"
          to="/studio"
        >创作中心</NuxtLink>
        <NuxtLink
          v-else
          to="/login"
        >登录</NuxtLink>
      </nav>
    </header>
    <main class="page-shell">
      <slot />
    </main>
    <footer class="site-footer">
      写作属于作者，访问服从边界。
    </footer>
  </div>
</template>
