<script setup lang="ts">
const api = useApi()
const { data } = await useAsyncData('site-config', () => api.request<Record<string, string>>('/site'))
useSeoMeta({ title: '关于' })
</script>

<template>
  <section class="hero">
    <h1>关于</h1><p>{{ data?.['site.about'] }}</p>
  </section>
</template>
