<script setup lang="ts">
import type { Content } from '~/types/api'

const props = defineProps<{ id?: string }>()
const api = useApi()
const router = useRouter()
const categories = ref<Array<{ id: string, name: string }>>([])
const message = ref('')
const error = ref('')
const form = reactive({
  type: 'BLOG',
  title: '',
  slug: '',
  summary: '',
  bodyMarkdown: '# 开始写作\n\n',
  coverMediaId: undefined as string | undefined,
  categoryId: undefined as string | undefined,
  visibility: 'PUBLIC',
  commentsEnabled: true,
  pinned: false,
  tagsText: '',
  grantsText: '',
})

onMounted(async () => {
  categories.value = await api.request('/studio/categories')
  if (props.id) {
    const [content, grants] = await Promise.all([
      api.request<Content>(`/studio/contents/${props.id}`),
      api.request<string[]>(`/studio/contents/${props.id}/grants`),
    ])
    Object.assign(form, content, {
      tagsText: content.tags.join(', '),
      grantsText: grants.join(', '),
    })
  }
})

function payload() {
  return {
    ...form,
    tags: form.tagsText.split(',').map(value => value.trim()).filter(Boolean),
    grantedUsernames: form.grantsText.split(',').map(value => value.trim()).filter(Boolean),
    categoryId: form.categoryId || null,
    coverMediaId: form.coverMediaId || null,
  }
}

async function save(publish = false) {
  error.value = ''
  try {
    const content = props.id
      ? await api.request<Content>(`/studio/contents/${props.id}`, { method: 'PUT', body: payload() })
      : await api.request<Content>('/studio/contents', { method: 'POST', body: payload() })
    if (publish) await api.request(`/studio/contents/${content.id}/publish`, { method: 'POST' })
    message.value = publish ? '已发布' : '草稿已保存'
    if (!props.id) await router.replace(`/studio/contents/${content.id}`)
  }
  catch (reason: any) {
    error.value = reason.message
  }
}
</script>

<template>
  <div
    class="form-stack"
    style="max-width:none"
  >
    <div class="toolbar">
      <h1 class="page-title">
        {{ id ? '编辑内容' : '新建内容' }}
      </h1>
      <div style="display:flex;gap:.5rem">
        <button
          class="secondary"
          @click="save(false)"
        >
          保存草稿
        </button><button @click="save(true)">
          发布
        </button>
      </div>
    </div>
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
    <label>标题<input
      v-model="form.title"
      maxlength="200"
    ></label>
    <div style="display:grid;grid-template-columns:repeat(4,1fr);gap:1rem">
      <label>类型<select v-model="form.type"><option value="BLOG">博客</option><option value="NOTE">随笔</option></select></label>
      <label>可见性<select v-model="form.visibility"><option value="PUBLIC">公开</option><option value="AUTHENTICATED">登录可见</option><option value="RESTRICTED">指定用户</option><option value="PRIVATE">仅自己</option></select></label>
      <label>分类<select v-model="form.categoryId"><option :value="undefined">未分类</option><option
        v-for="item in categories"
        :key="item.id"
        :value="item.id"
      >{{ item.name }}</option></select></label>
      <label>自定义地址<input
        v-model="form.slug"
        placeholder="留空自动生成"
      ></label>
    </div>
    <label>摘要<textarea
      v-model="form.summary"
      rows="2"
      maxlength="500"
    /></label>
    <label>标签（逗号分隔）<input v-model="form.tagsText"></label>
    <label v-if="form.visibility === 'RESTRICTED'">允许访问的用户名（逗号分隔）<input v-model="form.grantsText"></label>
    <div class="editor-grid">
      <MarkdownEditor v-model="form.bodyMarkdown" />
      <div class="editor-preview">
        <MarkdownView :source="form.bodyMarkdown" />
      </div>
    </div>
    <div style="display:flex;gap:1rem">
      <label><input
        v-model="form.commentsEnabled"
        type="checkbox"
        style="width:auto"
      > 允许评论</label><label><input
        v-model="form.pinned"
        type="checkbox"
        style="width:auto"
      > 置顶</label>
    </div>
  </div>
</template>
