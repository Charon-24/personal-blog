<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const items = ref<any[]>([])
async function load() { items.value = await api.request('/admin/config') }
async function save(item: any) {
  await api.request(`/admin/config/${encodeURIComponent(item.configKey)}`, {
    method: 'PUT',
    body: { value: item.configValue, description: item.description, publicValue: item.publicValue },
  })
}
onMounted(load)
</script>

<template>
  <h1 class="page-title">
    站点配置
  </h1><table class="table">
    <thead><tr><th>键</th><th>值</th><th>公开</th><th>操作</th></tr></thead><tbody>
      <tr
        v-for="item in items"
        :key="item.configKey"
      >
        <td>{{ item.configKey }}</td><td>
          <textarea
            v-model="item.configValue"
            rows="2"
          />
        </td><td>
          <input
            v-model="item.publicValue"
            type="checkbox"
          >
        </td><td>
          <button @click="save(item)">
            保存
          </button>
        </td>
      </tr>
    </tbody>
  </table>
</template>
