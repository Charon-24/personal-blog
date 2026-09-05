<script setup lang="ts">
import type { Content } from '~/types/api'

defineProps<{ content: Content }>()
</script>

<template>
  <article class="content-card">
    <div class="eyebrow">
      {{ content.type === 'BLOG' ? '博客' : '随笔' }}
      <span v-if="content.categoryName">· {{ content.categoryName }}</span>
    </div>
    <h2>
      <NuxtLink :to="`/content/${content.authorUsername}/${content.slug}`">
        {{ content.title }}
      </NuxtLink>
    </h2>
    <p>{{ content.summary || '作者还没有填写摘要。' }}</p>
    <div class="card-meta">
      <NuxtLink :to="`/u/${content.authorUsername}`">{{ content.authorDisplayName }}</NuxtLink>
      <span>{{ new Date(content.publishedAt || content.createdAt).toLocaleDateString() }}</span>
      <span>♡ {{ content.likeCount }}</span>
      <span>评论 {{ content.commentCount }}</span>
    </div>
  </article>
</template>
