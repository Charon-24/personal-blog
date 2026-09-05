<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'auth' })
const api = useApi()
const categories = ref<any[]>([])
const tags = ref<any[]>([])
const form = reactive({ name: '', description: '', sortOrder: 0, enabled: true })
async function load() {
  ;[categories.value, tags.value] = await Promise.all([
    api.request<any[]>('/studio/categories'), api.request<any[]>('/studio/tags'),
  ])
}
async function create() {
  await api.request('/studio/categories', { method: 'POST', body: form })
  Object.assign(form, { name: '', description: '', sortOrder: 0, enabled: true })
  await load()
}
async function remove(kind: 'categories' | 'tags', id: string) {
  await api.request(`/studio/${kind}/${id}`, { method: 'DELETE' })
  await load()
}
onMounted(load)
</script>

<template>
  <h1 class="page-title">
    分类与标签
  </h1>
  <section class="panel form-stack">
    <h2>新建分类</h2>
    <label>名称<input v-model="form.name"></label><label>说明<input v-model="form.description"></label>
    <button @click="create">
      创建
    </button>
  </section>
  <h2>分类</h2><div class="content-grid">
    <div
      v-for="item in categories"
      :key="item.id"
      class="panel"
    >
      <strong>{{ item.name }}</strong> · {{ item.contentCount }} 篇 <button
        class="secondary"
        @click="remove('categories', item.id)"
      >
        删除
      </button>
    </div>
  </div>
  <h2>标签</h2><div class="content-grid">
    <div
      v-for="item in tags"
      :key="item.id"
      class="panel"
    >
      # {{ item.name }} · {{ item.contentCount }} 篇 <button
        class="secondary"
        @click="remove('tags', item.id)"
      >
        删除
      </button>
    </div>
  </div>
</template>
