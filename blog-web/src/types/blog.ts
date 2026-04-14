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
}

export type SiteInfo = {
  title: string
  subtitle: string
  bio: string
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

