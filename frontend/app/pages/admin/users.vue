<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const data = ref<any>()
const query = ref('')
async function load() { data.value = await api.request(`/admin/users?size=100&q=${encodeURIComponent(query.value)}`) }
async function save(item: any) {
  await api.request(`/admin/users/${item.id}`, { method: 'PATCH', body: { role: item.role, status: item.status, reason: '管理员调整' } })
}
onMounted(load)
</script>

<template>
  <div class="toolbar">
    <h1 class="page-title">
      用户治理
    </h1><div style="display:flex">
      <input
        v-model="query"
        placeholder="搜索用户"
      ><button @click="load">
        搜索
      </button>
    </div>
  </div>
  <table class="table">
    <thead><tr><th>用户</th><th>邮箱</th><th>角色</th><th>状态</th><th>操作</th></tr></thead><tbody>
      <tr
        v-for="item in data?.items"
        :key="item.id"
      >
        <td>{{ item.displayName }}<br>@{{ item.username }}</td><td>{{ item.email }}</td><td>
          <select v-model="item.role">
            <option>USER</option><option>ADMIN</option>
          </select>
        </td><td>
          <select v-model="item.status">
            <option>PENDING</option><option>ACTIVE</option><option>DISABLED</option>
          </select>
        </td><td>
          <button @click="save(item)">
            保存
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
