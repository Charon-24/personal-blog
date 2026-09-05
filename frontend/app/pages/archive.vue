<script setup lang="ts">
const api = useApi()
const { data } = await useAsyncData('archive', () => api.request<Array<{ year: number, month: number, count: number }>>('/archive'))
useSeoMeta({ title: '归档' })
</script>

<template>
  <h1 class="page-title">
    时间归档
  </h1>
  <div class="content-grid">
    <div
      v-for="item in data"
      :key="`${item.year}-${item.month}`"
      class="panel"
    >
      <div class="eyebrow">
        {{ item.year }}
      </div><h2>{{ item.month }} 月</h2><p>{{ item.count }} 篇内容</p>
    </div>
  </div>
</template>
