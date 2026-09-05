<script setup lang="ts">
const api = useApi()
const { data } = await useAsyncData('categories', () => api.request<Array<{ id: string, name: string, description?: string, contentCount: number }>>('/categories'))
</script>

<template>
  <h1 class="page-title">
    分类
  </h1><div class="content-grid">
    <div
      v-for="item in data"
      :key="item.id"
      class="panel"
    >
      <h2>{{ item.name }}</h2><p>{{ item.description }} · {{ item.contentCount }} 篇</p>
    </div>
  </div>
</template>
