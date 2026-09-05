export default defineNuxtRouteMiddleware(async () => {
  const auth = useAuthStore()
  await auth.load()
  if (!auth.user) return navigateTo('/login')
})
