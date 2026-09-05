import { describe, expect, it } from 'vitest'
import { renderMarkdown } from '../../app/utils/markdown'

describe('renderMarkdown', () => {
  it('renders GFM while removing executable HTML', async () => {
    const html = await renderMarkdown('# Safe\n\n<script>alert(1)</script>\n\n- [x] done')
    expect(html).toContain('<h1>Safe</h1>')
    expect(html).toContain('type="checkbox"')
    expect(html).not.toContain('<script>')
    expect(html).not.toContain('alert(1)')
  })
})
