<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'admin' })
const api = useApi()
const { data } = await useAsyncData('admin-audit', () => api.request<any>('/admin/audit?size=100'))
</script>

<template>
  <h1 class="page-title">
    审计日志
  </h1><table class="table">
    <thead><tr><th>时间</th><th>操作者</th><th>动作</th><th>对象</th><th>原因</th></tr></thead><tbody>
      <tr
        v-for="item in data?.items"
        :key="item.id"
      >
        <td>{{ new Date(item.createdAt).toLocaleString() }}</td><td>@{{ item.actorUsername }}</td><td>{{ item.action }}</td><td>{{ item.targetType }} / {{ item.targetId }}</td><td>{{ item.reason }}</td>
      </tr>
    </tbody>
  </table>
</template>
