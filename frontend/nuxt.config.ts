export default defineNuxtConfig({
  modules: ['@nuxt/ui', '@pinia/nuxt', '@nuxt/eslint'],
  devtools: { enabled: true },
  app: {
    head: {
      titleTemplate: '%s · Personal Blog',
      meta: [{ name: 'description', content: '一个支持多用户创作、权限控制和互动的博客平台' }],
    },
  },
  css: ['~/assets/css/main.css'],
  runtimeConfig: {
    apiInternalBase: process.env.NUXT_API_INTERNAL_BASE || 'http://localhost:8080/api/v1',
    public: { apiBase: '/api/v1' },
  },
  routeRules: {
    '/': { swr: 60 },
    '/u/**': { swr: 60 },
    '/studio/**': { ssr: false, headers: { 'cache-control': 'private, no-store' } },
    '/admin/**': { ssr: false, headers: { 'cache-control': 'private, no-store' } },
  },
  compatibilityDate: '2026-09-01',
  nitro: {
    devProxy: {
      '/api': { target: 'http://localhost:8080', changeOrigin: true },
    },
  },
  typescript: { strict: true, typeCheck: true },
  eslint: { config: { stylistic: true } },
  fonts: {
    providers: {
      google: false,
      googleicons: false,
    },
  },
})
