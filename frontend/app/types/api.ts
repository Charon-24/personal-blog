export interface PageResult<T> {
  items: T[]
  total: number
  page: number
  size: number
}

export interface User {
  id: string
  username: string
  email?: string
  displayName: string
  bio?: string
  role?: 'USER' | 'ADMIN'
  status?: string
  avatarMediaId?: string
  publishedCount?: number
}

export interface Content {
  id: string
  authorId: string
  authorUsername: string
  authorDisplayName: string
  type: 'BLOG' | 'NOTE'
  title: string
  slug: string
  summary?: string
  bodyMarkdown: string
  coverMediaId?: string
  categoryId?: string
  categoryName?: string
  status: 'DRAFT' | 'PUBLISHED' | 'OFFLINE' | 'DELETED'
  visibility: 'PUBLIC' | 'AUTHENTICATED' | 'RESTRICTED' | 'PRIVATE'
  commentsEnabled: boolean
  pinned: boolean
  viewCount: number
  likeCount: number
  commentCount: number
  tags: string[]
  publishedAt?: string
  createdAt: string
  updatedAt: string
}

export interface Problem {
  title?: string
  status?: number
  detail?: string
}
