<script setup lang="ts">
import type { Content, PageResult } from '~/types/api'

const api = useApi()
const { data } = await useAsyncData('home-contents', () =>
  api.request<PageResult<Content>>('/contents?size=12&publicOnly=true'),
)
useSeoMeta({ title: '首页' })
</script>

<template>
  <section class="hero">
    <div>
      <div class="eyebrow">
        Multi-author publishing
      </div>
      <h1>把想法写成<br>可以抵达的文字。</h1>
    </div>
    <p>面向多位作者的独立博客空间。公开分享，也尊重登录可见、指定用户和私人笔记的边界。</p>
  </section>
  <section>
    <div class="toolbar">
      <h2>最新发布</h2>
      <NuxtLink
        class="button secondary"
        to="/search"
      >探索全部</NuxtLink>
    </div>
    <div class="content-grid">
      <ContentCard
        v-for="content in data?.items"
        :key="content.id"
        :content="content"
      />
    </div>
    <p v-if="!data?.items.length">
      还没有公开内容，登录后去创作中心发布第一篇吧。
    </p>
  </section>
</template>
