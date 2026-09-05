<script setup lang="ts">
const api = useApi()
const { data } = await useAsyncData('friend-links', () => api.request<Array<{ id: string, name: string, description?: string, url: string }>>('/site/links'))
useSeoMeta({ title: '友链' })
</script>

<template>
  <h1 class="page-title">
    友情链接
  </h1>
  <div class="content-grid">
    <a
      v-for="link in data"
      :key="link.id"
      class="panel"
      :href="link.url"
      target="_blank"
      rel="noopener noreferrer"
    >
      <h2>{{ link.name }}</h2><p>{{ link.description }}</p>
    </a>
  </div>
</template>
