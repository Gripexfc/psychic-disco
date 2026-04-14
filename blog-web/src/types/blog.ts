export type BlogPost = {
  id: number
  slug: string
  title: string
  excerpt: string
  content: string
  contentHtml?: string
  date: string
  tags: string[]
  published: boolean
  likes?: number
  views?: number
}

export type SiteInfo = {
  title: string
  subtitle: string
  bio: string
  authorName?: string
}

export type SiteStats = {
  totalPosts: number
  totalViews: number
  totalLikes: number
}

export type ApiError = {
  code: string
  message: string
}

export type ApiResult<T> = {
  data: T
  meta?: Record<string, unknown>
  error: ApiError | null
}

export type ListPostsResponse = {
  items: BlogPost[]
}

