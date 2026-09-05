import { expect, test } from '@playwright/test'

test('public shell exposes primary navigation', async ({ page }) => {
  await page.goto('/')
  await expect(page.getByText('PERSONAL / BLOG')).toBeVisible()
  await expect(page.getByRole('link', { name: '搜索' })).toBeVisible()
  await expect(page.getByRole('link', { name: '登录' })).toBeVisible()
})
