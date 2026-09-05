<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'auth' })
const api = useApi()
const items = ref<any[]>([])
async function load() { items.value = await api.request('/media') }
async function upload(event: Event) {
  const input = event.target as HTMLInputElement
  if (!input.files?.[0]) return
  const body = new FormData()
  body.append('file', input.files[0])
  await api.request('/media?purpose=CONTENT', { method: 'POST', body })
  await load()
}
async function remove(id: string) { await api.request(`/media/${id}`, { method: 'DELETE' }); await load() }
onMounted(load)
</script>

<template>
  <div class="toolbar">
    <h1 class="page-title">
      媒体库
    </h1><label class="button">上传图片<input
      type="file"
      accept="image/png,image/jpeg,image/webp,image/gif"
      hidden
      @change="upload"
    ></label>
  </div>
  <div class="content-grid">
    <div
      v-for="item in items"
      :key="item.id"
      class="panel"
    >
      <img
        :src="`/api/v1/media/${item.id}`"
        :alt="item.originalName"
        style="width:100%;height:180px;object-fit:cover"
      ><p>{{ item.originalName }} · {{ Math.ceil(item.sizeBytes / 1024) }}KB</p><button
        class="secondary"
        @click="remove(item.id)"
      >
        删除
      </button>
    </div>
  </div>
</template>
