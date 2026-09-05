import { defineVitestConfig } from '@nuxt/test-utils/config'

export default defineVitestConfig({
  test: {
    environment: 'nuxt',
    include: ['tests/unit/**/*.test.ts'],
    testTimeout: 20_000,
    coverage: { reporter: ['text', 'html'] },
  },
})
