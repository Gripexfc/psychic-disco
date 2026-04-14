import type { ApiResult, BlogPost, ListPostsResponse, SiteInfo } from '../types/blog'

const apiBase = ((import.meta.env.VITE_API_BASE_URL as string | undefined) ?? '/api').replace(/\/$/, '')
const fallbackApiBase = '/api'
const requestTimeout = 8000
let authToken = ''

function getApiBases() {
  return apiBase === fallbackApiBase ? [apiBase] : [apiBase, fallbackApiBase]
}

export function setAuthToken(token: string) {
  authToken = token
}

async function fetchWithTimeout(input: string, init?: RequestInit): Promise<Response> {
  const controller = new AbortController()
  const timer = window.setTimeout(() => controller.abort(), requestTimeout)
  try {
    return await fetch(input, { ...init, signal: controller.signal })
  } finally {
    window.clearTimeout(timer)
  }
}

async function requestJson<T>(path: string, init?: RequestInit): Promise<ApiResult<T>> {
  let latestError: Error | undefined
  const candidates = getApiBases()
  for (const base of candidates) {
    try {
      const response = await fetchWithTimeout(`${base}${path}`, {
        headers: {
          'Content-Type': 'application/json',
          ...(authToken ? { Authorization: `Bearer ${authToken}` } : {}),
          ...(init?.headers ?? {}),
        },
        ...init,
      })

      const json = (await response.json()) as ApiResult<T>
      if (!response.ok || json.error) {
        throw new Error(json.error?.message ?? '请求失败。')
      }
      return json
    } catch (error) {
      latestError = error instanceof Error ? error : new Error('请求失败。')
    }
  }

  if (latestError?.name === 'AbortError') {
    throw new Error('接口超时，请检查后端服务是否启动。')
  }
  throw new Error(`${latestError?.message ?? '接口不可用。'}（已尝试 ${candidates.join('、')}）`)
}

export async function fetchSiteInfo() {
  return requestJson<SiteInfo>('/site')
}

export async function fetchTags() {
  return requestJson<string[]>('/tags')
}

export async function fetchPosts(params: {
  page: number
  pageSize: number
  search?: string
  tag?: string
}) {
  const query = new URLSearchParams({
    page: String(params.page),
    pageSize: String(params.pageSize),
  })
  if (params.search) {
    query.set('search', params.search)
  }
  if (params.tag) {
    query.set('tag', params.tag)
  }
  return requestJson<ListPostsResponse>(`/posts?${query.toString()}`)
}

export async function fetchPostDetail(slug: string) {
  return requestJson<BlogPost>(`/posts/${encodeURIComponent(slug)}`)
}

export async function likePost(idOrSlug: string | number) {
  return requestJson<BlogPost>(`/posts/${encodeURIComponent(String(idOrSlug))}/like`, {
    method: 'POST',
  })
}

export async function publishPost(payload: {
  title: string
  excerpt: string
  content: string
  contentHtml: string
  tags: string[]
}) {
  return requestJson<BlogPost>('/posts', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function login(payload: { username: string; password: string }) {
  return requestJson<{ token: string; user: { username: string } }>('/auth/login', {
    method: 'POST',
    body: JSON.stringify(payload),
  })
}

export async function fetchCurrentUser() {
  return requestJson<{ user: { username: string } }>('/auth/me')
}

export async function logout() {
  return requestJson<{ success: boolean }>('/auth/logout', {
    method: 'POST',
  })
}

