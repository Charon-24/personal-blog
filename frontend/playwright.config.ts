import { defineConfig, devices } from '@playwright/test'

const browserChannel = process.env.PLAYWRIGHT_CHANNEL as 'chrome' | 'msedge' | undefined

export default defineConfig({
  testDir: './tests/e2e',
  use: {
    baseURL: 'http://localhost:3000',
    trace: 'on-first-retry',
  },
  webServer: {
    command: 'pnpm dev',
    url: 'http://localhost:3000',
    reuseExistingServer: true,
  },
  projects: [{
    name: 'chromium',
    use: {
      ...devices['Desktop Chrome'],
      ...(browserChannel ? { channel: browserChannel } : {}),
    },
  }],
})
