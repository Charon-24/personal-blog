<script setup lang="ts">
import type { Content, PageResult } from '~/types/api'

const route = useRoute()
const router = useRouter()
const api = useApi()
const query = ref(String(route.query.q || ''))
const results = ref<PageResult<Content>>()

async function search() {
  if (!query.value.trim()) return
  await router.replace({ query: { q: query.value } })
  results.value = await api.request<PageResult<Content>>(`/search?q=${encodeURIComponent(query.value)}`)
}
if (query.value) await search()
useSeoMeta({ title: '搜索' })
</script>

<template>
  <h1 class="page-title">
    搜索
  </h1>
  <div class="toolbar">
    <input
      v-model="query"
      placeholder="搜索标题、摘要和正文"
      @keyup.enter="search"
    >
    <button @click="search">
      搜索
    </button>
  </div>
  <p v-if="results">
    {{ results.total }} 条结果
  </p>
  <div class="content-grid">
    <ContentCard
      v-for="content in results?.items"
      :key="content.id"
      :content="content"
    />
  </div>
</template>
