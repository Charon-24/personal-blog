<script setup lang="ts">
const route = useRoute()
const password = ref('')
const state = ref('')
const api = useApi()
async function submit() {
  try {
    await api.request('/auth/password/reset', {
      method: 'POST',
      body: { token: String(route.query.token || ''), password: password.value },
    })
    state.value = '密码已更新。'
  }
  catch (error: any) {
    state.value = error.message
  }
}
</script>

<template>
  <section class="panel form-stack">
    <h1 class="page-title">
      设置新密码
    </h1>
    <label>新密码<input
      v-model="password"
      type="password"
      minlength="10"
    ></label>
    <button @click="submit">
      保存新密码
    </button>
    <p>{{ state }}</p>
  </section>
</template>
