<script setup lang="ts">
const props = defineProps<{ source: string }>()
const html = ref('')

watchEffect(async () => {
  html.value = await renderMarkdown(props.source || '')
})
</script>

<template>
  <article
    class="markdown-body"
    v-html="html"
  />
</template>
