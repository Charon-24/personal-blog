<script setup lang="ts">
import type { Content } from '~/types/api'

interface Comment {
  id: string
  username: string
  displayName: string
  rootId?: string
  parentId?: string
  body?: string
  status: string
  createdAt: string
}
const route = useRoute()
const api = useApi()
const { data: content } = await useAsyncData(`content-${route.params.username}-${route.params.slug}`, () =>
  api.request<Content>(`/contents/${route.params.username}/${route.params.slug}`),
)
const comments = ref<Comment[]>([])
const body = ref('')
const error = ref('')

async function loadComments() {
  if (content.value) comments.value = await api.request<Comment[]>(`/comments?contentId=${content.value.id}`)
}
async function comment(parentId?: string) {
  if (!content.value || !body.value.trim()) return
  try {
    await api.request('/comments', { method: 'POST', body: { contentId: content.value.id, parentId, body: body.value } })
    body.value = ''
    await loadComments()
  }
  catch (reason: any) {
    error.value = reason.message
  }
}
async function like() {
  if (content.value) {
    const result = await api.request<{ likeCount: number }>(`/contents/${content.value.id}/like`, { method: 'POST' })
    content.value.likeCount = result.likeCount
  }
}
onMounted(loadComments)
useSeoMeta({ title: () => content.value?.title || '内容' })
</script>

<template>
  <article v-if="content">
    <header class="hero">
      <div>
        <div class="eyebrow">
          {{ content.type }} · {{ content.visibility }}
        </div>
        <h1>{{ content.title }}</h1>
      </div>
      <p>{{ content.summary }}<br>作者 <NuxtLink :to="`/u/${content.authorUsername}`">{{ content.authorDisplayName }}</NuxtLink></p>
    </header>
    <MarkdownView :source="content.bodyMarkdown" />
    <div class="toolbar">
      <button @click="like">
        ♡ {{ content.likeCount }}
      </button><span>阅读 {{ content.viewCount }}</span>
    </div>
    <section
      v-if="content.commentsEnabled"
      class="panel"
    >
      <h2>评论</h2>
      <div class="form-stack">
        <textarea
          v-model="body"
          rows="3"
          placeholder="写下你的回应…"
        />
        <button @click="comment()">
          发表评论
        </button>
        <p
          v-if="error"
          class="error"
        >
          {{ error }}
        </p>
      </div>
      <article
        v-for="item in comments"
        :key="item.id"
      >
        <p><strong>{{ item.displayName }}</strong> · {{ new Date(item.createdAt).toLocaleString() }}</p>
        <p>{{ item.status === 'DELETED' ? '该评论已删除' : item.body }}</p>
      </article>
    </section>
  </article>
</template>
