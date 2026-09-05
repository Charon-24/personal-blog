<script setup lang="ts">
const auth = useAuthStore()
const identity = ref('')
const password = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await auth.login(identity.value, password.value)
    await navigateTo('/studio')
  }
  catch (reason: any) {
    error.value = reason.message
  }
}
useSeoMeta({ title: '登录' })
</script>

<template>
  <section class="panel form-stack">
    <div class="eyebrow">
      Welcome back
    </div>
    <h1 class="page-title">
      登录
    </h1>
    <label>用户名或邮箱<input
      v-model="identity"
      autocomplete="username"
    ></label>
    <label>密码<input
      v-model="password"
      type="password"
      autocomplete="current-password"
    ></label>
    <p
      v-if="error"
      class="error"
    >
      {{ error }}
    </p>
    <button @click="submit">
      进入创作空间
    </button>
    <p><NuxtLink to="/forgot-password">忘记密码？</NuxtLink> · <NuxtLink to="/register">创建账号</NuxtLink></p>
  </section>
</template>
