<script setup lang="ts">
import type { Content, PageResult } from '~/types/api'

definePageMeta({ layout: 'studio', middleware: 'auth' })
const api = useApi()
const status = ref('')
const data = ref<PageResult<Content>>()
async function load() {
  data.value = await api.request(`/studio/contents?size=100${status.value ? `&status=${status.value}` : ''}`)
}
async function action(id: string, name: string) {
  await api.request(`/studio/contents/${id}/${name}`, { method: 'POST' })
  await load()
}
onMounted(load)
</script>

<template>
  <div class="toolbar">
    <h1 class="page-title">
      我的内容
    </h1><NuxtLink
      class="button"
      to="/studio/contents/new"
    >新建</NuxtLink>
  </div>
  <label style="max-width:220px">状态<select
    v-model="status"
    @change="load"
  ><option value="">全部</option><option value="DRAFT">草稿</option><option value="PUBLISHED">已发布</option><option value="OFFLINE">已下线</option></select></label>
  <table class="table">
    <thead><tr><th>标题</th><th>类型</th><th>状态</th><th>可见性</th><th>操作</th></tr></thead>
    <tbody>
      <tr
        v-for="item in data?.items"
        :key="item.id"
      >
        <td><NuxtLink :to="`/studio/contents/${item.id}`">{{ item.title }}</NuxtLink></td><td>{{ item.type }}</td><td>{{ item.status }}</td><td>{{ item.visibility }}</td><td>
          <button
            v-if="item.status !== 'PUBLISHED'"
            @click="action(item.id, 'publish')"
          >
            发布
          </button> <button
            v-else
            class="secondary"
            @click="action(item.id, 'offline')"
          >
            下线
          </button> <button
            class="secondary"
            @click="action(item.id, 'delete')"
          >
            删除
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
