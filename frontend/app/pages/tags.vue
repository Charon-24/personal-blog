<script setup lang="ts">
const api = useApi()
const { data } = await useAsyncData('tags', () => api.request<Array<{ id: string, name: string, contentCount: number }>>('/tags'))
</script>

<template>
  <h1 class="page-title">
    标签
  </h1><div
    class="toolbar"
    style="justify-content:flex-start;flex-wrap:wrap"
  >
    <span
      v-for="item in data"
      :key="item.id"
      class="panel"
    ># {{ item.name }} · {{ item.contentCount }}</span>
  </div>
</template>
