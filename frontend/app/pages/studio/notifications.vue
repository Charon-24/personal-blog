<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'auth' })
const api = useApi()
const items = ref<any[]>([])
async function load() { items.value = await api.request('/notifications') }
async function read(id: string) { await api.request(`/notifications/${id}/read`, { method: 'POST' }); await load() }
async function readAll() { await api.request('/notifications/read-all', { method: 'POST' }); await load() }
onMounted(load)
</script>

<template>
  <div class="toolbar">
    <h1 class="page-title">
      通知
    </h1><button
      class="secondary"
      @click="readAll"
    >
      全部已读
    </button>
  </div><div
    v-for="item in items"
    :key="item.id"
    class="panel"
    :style="{ opacity: item.readAt ? .55 : 1 }"
    @click="read(item.id)"
  >
    <strong>{{ item.actorDisplayName || '系统' }}</strong> {{ item.message }}<p>{{ new Date(item.createdAt).toLocaleString() }}</p>
  </div>
</template>
