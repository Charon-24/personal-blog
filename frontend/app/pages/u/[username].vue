<script setup lang="ts">
import type { Content, PageResult, User } from '~/types/api'

const route = useRoute()
const api = useApi()
const username = String(route.params.username)
const [{ data: profile }, { data: contents }] = await Promise.all([
  useAsyncData(`profile-${username}`, () => api.request<User>(`/users/${username}`)),
  useAsyncData(`profile-contents-${username}`, () =>
    api.request<PageResult<Content>>(`/contents?author=${encodeURIComponent(username)}&publicOnly=true`)),
])
useSeoMeta({ title: () => profile.value?.displayName || username })
</script>

<template>
  <section
    v-if="profile"
    class="hero"
  >
    <div>
      <div class="eyebrow">
        @{{ profile.username }}
      </div><h1>{{ profile.displayName }}</h1>
    </div>
    <p>{{ profile.bio || '这位作者还没有写简介。' }}<br>已发布 {{ profile.publishedCount }} 篇公开内容。</p>
  </section>
  <div class="content-grid">
    <ContentCard
      v-for="content in contents?.items"
      :key="content.id"
      :content="content"
    />
  </div>
</template>
