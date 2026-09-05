<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const items = ref<any[]>([])
const form = reactive({ name: '', description: '', url: '', iconUrl: '', sortOrder: 0, enabled: true })
async function load() { items.value = await api.request('/admin/links') }
async function create() { await api.request('/admin/links', { method: 'POST', body: form }); Object.assign(form, { name: '', description: '', url: '', iconUrl: '', sortOrder: 0, enabled: true }); await load() }
async function remove(id: string) { await api.request(`/admin/links/${id}`, { method: 'DELETE' }); await load() }
onMounted(load)
</script>

<template>
  <h1 class="page-title">
    友情链接
  </h1><section class="panel form-stack">
    <label>名称<input v-model="form.name"></label><label>地址<input v-model="form.url"></label><label>说明<input v-model="form.description"></label><button @click="create">
      添加
    </button>
  </section><table class="table">
    <tbody>
      <tr
        v-for="item in items"
        :key="item.id"
      >
        <td>{{ item.name }}</td><td>{{ item.url }}</td><td>{{ item.enabled ? '启用' : '停用' }}</td><td>
          <button
            class="secondary"
            @click="remove(item.id)"
          >
            删除
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
