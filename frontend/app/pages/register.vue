<script setup lang="ts">
const api = useApi()
const form = reactive({ username: '', email: '', password: '' })
const message = ref('')
const error = ref('')

async function submit() {
  error.value = ''
  try {
    await api.request('/auth/register', { method: 'POST', body: form })
    message.value = '注册成功，请到邮箱完成验证。本地开发可在 Mailpit 查看邮件。'
  }
  catch (reason: any) {
    error.value = reason.message
  }
}
useSeoMeta({ title: '注册' })
</script>

<template>
  <section class="panel form-stack">
    <div class="eyebrow">
      Join the writers
    </div>
    <h1 class="page-title">
      创建账号
    </h1>
    <label>用户名<input
      v-model="form.username"
      pattern="[A-Za-z0-9_]{3,40}"
      autocomplete="username"
    ></label>
    <label>邮箱<input
      v-model="form.email"
      type="email"
      autocomplete="email"
    ></label>
    <label>密码（至少 10 位）<input
      v-model="form.password"
      type="password"
      minlength="10"
      autocomplete="new-password"
    ></label>
    <p
      v-if="message"
      class="success"
    >
      {{ message }}
    </p>
    <p
      v-if="error"
      class="error"
    >
      {{ error }}
    </p>
    <button @click="submit">
      注册
    </button>
  </section>
</template>
