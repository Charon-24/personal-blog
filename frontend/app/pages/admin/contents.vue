<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const data = ref<any>()
async function load() { data.value = await api.request('/admin/contents?size=100') }
async function moderate(item: any, status: string) {
  await api.request(`/admin/contents/${item.id}`, { method: 'PATCH', body: { status, reason: '后台内容治理' } })
  await load()
}
onMounted(load)
</script>

<template>
  <h1 class="page-title">
    内容治理
  </h1><table class="table">
    <thead><tr><th>内容</th><th>作者</th><th>状态</th><th>可见性</th><th>操作</th></tr></thead><tbody>
      <tr
        v-for="item in data?.items"
        :key="item.id"
      >
        <td>{{ item.title }}</td><td>@{{ item.authorUsername }}</td><td>{{ item.status }}</td><td>{{ item.visibility }}</td><td>
          <button @click="moderate(item, 'PUBLISHED')">
            恢复
          </button> <button
            class="secondary"
            @click="moderate(item, 'OFFLINE')"
          >
            下线
          </button> <button
            class="secondary"
            @click="moderate(item, 'DELETED')"
          >
            删除
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
