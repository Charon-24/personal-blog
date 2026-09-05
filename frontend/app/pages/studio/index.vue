<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'auth' })
const auth = useAuthStore()
const api = useApi()
const { data } = await useAsyncData('studio-summary', () => api.request<any>('/studio/contents?size=5'))
</script>

<template>
  <div class="eyebrow">
    Creator studio
  </div>
  <h1 class="page-title">
    你好，{{ auth.user?.displayName }}
  </h1>
  <div class="stats">
    <div class="panel stat">
      <strong>{{ data?.total || 0 }}</strong>内容
    </div>
    <div class="panel stat">
      <strong>{{ data?.items?.filter((x: any) => x.status === 'PUBLISHED').length || 0 }}</strong>最近发布
    </div>
  </div>
  <div class="toolbar">
    <h2>最近编辑</h2><NuxtLink
      class="button"
      to="/studio/contents/new"
    >写新内容</NuxtLink>
  </div>
  <ContentCard
    v-for="content in data?.items"
    :key="content.id"
    :content="content"
  />
</template>
