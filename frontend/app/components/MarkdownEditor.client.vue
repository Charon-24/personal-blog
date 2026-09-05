<script setup lang="ts">
import { basicSetup } from 'codemirror'
import { markdown } from '@codemirror/lang-markdown'
import { EditorState } from '@codemirror/state'
import { EditorView, keymap } from '@codemirror/view'

const model = defineModel<string>({ default: '' })
const host = ref<HTMLElement>()
let view: EditorView | undefined

onMounted(() => {
  view = new EditorView({
    parent: host.value,
    state: EditorState.create({
      doc: model.value,
      extensions: [
        basicSetup,
        markdown(),
        keymap.of([]),
        EditorView.lineWrapping,
        EditorView.updateListener.of((update) => {
          if (update.docChanged) model.value = update.state.doc.toString()
        }),
      ],
    }),
  })
})

onBeforeUnmount(() => view?.destroy())
</script>

<template>
  <div
    ref="host"
    class="editor-host"
  />
</template>
