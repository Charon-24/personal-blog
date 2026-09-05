<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'auth' })
const auth = useAuthStore()
const api = useApi()
const form = reactive({ displayName: '', bio: '', avatarMediaId: undefined as string | undefined })
const password = reactive({ oldPassword: '', newPassword: '' })
const message = ref('')
onMounted(async () => {
  await auth.load()
  Object.assign(form, { displayName: auth.user?.displayName || '', bio: auth.user?.bio || '', avatarMediaId: auth.user?.avatarMediaId })
})
async function save() {
  auth.user = await api.request('/me/profile', { method: 'PATCH', body: form })
  message.value = '资料已保存'
}
async function changePassword() {
  await api.request('/me/password', { method: 'POST', body: password })
  password.oldPassword = ''; password.newPassword = ''; message.value = '密码已更新'
}
</script>

<template>
  <h1 class="page-title">
    个人资料
  </h1><p class="success">
    {{ message }}
  </p>
  <section class="panel form-stack">
    <h2>公开资料</h2><label>显示名称<input v-model="form.displayName"></label><label>简介<textarea
      v-model="form.bio"
      rows="5"
    /></label><label>头像媒体 ID<input v-model="form.avatarMediaId"></label><button @click="save">
      保存
    </button>
  </section>
  <section class="panel form-stack">
    <h2>修改密码</h2><label>当前密码<input
      v-model="password.oldPassword"
      type="password"
    ></label><label>新密码<input
      v-model="password.newPassword"
      type="password"
      minlength="10"
    ></label><button @click="changePassword">
      更新密码
    </button>
  </section>
</template>
