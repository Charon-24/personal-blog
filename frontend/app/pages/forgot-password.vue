<script setup lang="ts">
const email = ref('')
const done = ref(false)
const api = useApi()
async function submit() {
  await api.request('/auth/password/forgot', { method: 'POST', body: { email: email.value } })
  done.value = true
}
</script>

<template>
  <section class="panel form-stack">
    <h1 class="page-title">
      找回密码
    </h1>
    <template v-if="!done">
      <label>注册邮箱<input
        v-model="email"
        type="email"
      ></label>
      <button @click="submit">
        发送重置邮件
      </button>
    </template>
    <p v-else>
      如果该邮箱已注册，你会收到一封重置邮件。
    </p>
  </section>
</template>
