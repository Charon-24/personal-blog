import type { User } from '~/types/api'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const loaded = ref(false)
  const api = useApi()

  async function load() {
    if (loaded.value) return user.value
    try {
      user.value = await api.request<User>('/auth/me')
    }
    catch {
      user.value = null
    }
    finally {
      loaded.value = true
    }
    return user.value
  }

  async function login(identity: string, password: string) {
    user.value = await api.request<User>('/auth/login', { method: 'POST', body: { identity, password } })
    loaded.value = true
  }

  async function logout() {
    await api.request('/auth/logout', { method: 'POST' })
    user.value = null
  }

  return { user, loaded, load, login, logout }
})
