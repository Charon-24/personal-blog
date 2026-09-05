<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const data = ref<any>()
async function load() { data.value = await api.request('/admin/comments?size=100') }
async function moderate(item: any, status: string) {
  await api.request(`/admin/comments/${item.id}`, { method: 'PATCH', body: { status, reason: '后台评论治理' } })
  await load()
}
onMounted(load)
</script>

<template>
  <h1 class="page-title">
    互动治理
  </h1><table class="table">
    <thead><tr><th>用户</th><th>内容</th><th>评论</th><th>状态</th><th>操作</th></tr></thead><tbody>
      <tr
        v-for="item in data?.items"
        :key="item.id"
      >
        <td>@{{ item.username }}</td><td>{{ item.contentTitle }}</td><td>{{ item.body || '已删除' }}</td><td>{{ item.status }}</td><td>
          <button @click="moderate(item, 'VISIBLE')">
            显示
          </button> <button
            class="secondary"
            @click="moderate(item, 'HIDDEN')"
          >
            隐藏
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
