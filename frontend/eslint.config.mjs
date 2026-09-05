import withNuxt from './.nuxt/eslint.config.mjs'

export default withNuxt(
  {
    ignores: ['app/types/openapi.d.ts'],
  },
  {
    rules: {
      '@stylistic/max-statements-per-line': 'off',
      '@typescript-eslint/no-explicit-any': 'off',
      'vue/no-multiple-template-root': 'off',
      // Markdown is transformed by remark-rehype and rehype-sanitize before rendering.
      'vue/no-v-html': 'off',
    },
  },
)
