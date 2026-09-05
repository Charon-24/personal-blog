<script setup lang="ts">
const route = useRoute()
const api = useApi()
const state = ref('正在验证…')

onMounted(async () => {
  try {
    await api.request('/auth/verify-email', { method: 'POST', body: { token: String(route.query.token || '') } })
    state.value = '邮箱验证成功，现在可以登录。'
  }
  catch (error: any) {
    state.value = error.message
  }
})
</script>

<template>
  <section class="panel">
    <h1 class="page-title">
      邮箱验证
    </h1><p>{{ state }}</p><NuxtLink to="/login">前往登录</NuxtLink>
  </section>
</template>
