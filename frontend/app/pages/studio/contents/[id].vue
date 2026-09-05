<script setup lang="ts">
definePageMeta({ layout: 'studio', middleware: 'auth' })
const route = useRoute()
</script>

<template>
  <ContentForm :id="String(route.params.id)" />
</template>
