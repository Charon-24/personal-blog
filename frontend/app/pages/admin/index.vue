<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const { data } = await useAsyncData('admin-stats', () => api.request<Record<string, number>>('/admin/stats'))
</script>

<template>
  <div class="eyebrow">
    Administration
  </div><h1 class="page-title">
    管理后台
  </h1>
  <div
    class="toolbar"
    style="justify-content:flex-start"
  >
    <NuxtLink
      class="button secondary"
      to="/admin/users"
    >用户</NuxtLink><NuxtLink
      class="button secondary"
      to="/admin/contents"
    >内容</NuxtLink><NuxtLink
      class="button secondary"
      to="/admin/comments"
    >互动</NuxtLink><NuxtLink
      class="button secondary"
      to="/admin/config"
    >站点配置</NuxtLink><NuxtLink
      class="button secondary"
      to="/admin/links"
    >友链</NuxtLink><NuxtLink
      class="button secondary"
      to="/admin/audit"
    >审计</NuxtLink>
  </div>
  <div class="stats">
    <div
      v-for="(value, key) in data"
      :key="key"
      class="panel stat"
    >
      <strong>{{ value }}</strong>{{ key }}
    </div>
  </div>
</template>
