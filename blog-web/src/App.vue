<script setup lang="ts">
import { computed, defineAsyncComponent, nextTick, onMounted, onUnmounted, reactive, ref, watch } from 'vue'

import {
  fetchCurrentUser,
  fetchPostDetail,
  fetchRelatedPosts,
  fetchSiteStats,
  viewPost,
  fetchPosts,
  fetchSiteInfo,
  fetchTags,
  likePost,
  login,
  logout,
  publishPost as createPost,
  setAuthToken,
} from './api/blog'
import type { BlogPost, SiteInfo } from './types/blog'
import { plainTextFromHtml, sanitizeRichHtml } from './utils/sanitizeHtml'

const RichTextEditor = defineAsyncComponent(() => import('./components/RichTextEditor.vue'))
const AppModal = defineAsyncComponent(() => import('./components/AppModal.vue'))

const route = ref(window.location.hash || '#/')
const loadingPosts = ref(false)
const detailLoading = ref(false)
const isPublishing = ref(false)
const isLoggingIn = ref(false)
const errorText = ref('')
const publishStatus = ref('')
const toastText = ref('')
const showBackToTop = ref(false)
const draftSavedAt = ref('')
const detailBodyRef = ref<HTMLElement | null>(null)
const currentTheme = ref<'dark' | 'light'>('dark')
const themeStorageKey = 'open-spec:theme:v1'

function applyTheme(theme: 'dark' | 'light') {
  currentTheme.value = theme
  document.documentElement.setAttribute('data-theme', theme)
  localStorage.setItem(themeStorageKey, theme)
}

function toggleTheme() {
  applyTheme(currentTheme.value === 'dark' ? 'light' : 'dark')
}

const siteInfo = ref<SiteInfo>({
  title: 'EDY.LOG',
  subtitle: 'PERSONAL BLOG',
  bio: '记录技术、设计与创作思考。',
})
const tags = ref<string[]>([])
const posts = ref<BlogPost[]>([])
const currentPost = ref<BlogPost | null>(null)
const relatedPosts = ref<BlogPost[]>([])
const siteStats = ref({ totalPosts: 0, totalViews: 0, totalLikes: 0 })
const detailToc = ref<TocItem[]>([])
const detailTocHtml = ref('')

const filters = reactive({
  search: '',
  tag: '',
  page: 1,
  pageSize: 6,
  total: 0,
  totalPages: 1,
})

const publishForm = reactive({
  title: '',
  excerpt: '',
  contentHtml: '<p>在这里开始创作...</p>',
  tags: '',
})

const isPostDetail = computed(() => route.value.startsWith('#/posts/'))
const currentSlug = computed(() => {
  const raw = route.value.replace('#/posts/', '')
  try {
    return decodeURIComponent(raw)
  } catch {
    return raw
  }
})
const featuredPosts = computed(() => posts.value.slice(0, 2))
const isPostsRoute = computed(() => route.value === '#/posts')
const isPublishRoute = computed(() => route.value === '#/publish')
const isAboutRoute = computed(() => route.value === '#/about')
const isLoginRoute = computed(() => route.value === '#/login')
const canPublish = computed(() => authState.isAuthenticated)
const displayUsername = computed(() => {
  if (!authState.username) {
    return '用户'
  }
  if (authState.username === 'goodeat') {
    return 'GoodEat 管理员'
  }
  return authState.username
})
const safeDetailHtml = computed(() => sanitizeRichHtml(currentPost.value?.contentHtml ?? ''))

let filterTimer: number | undefined
let toastTimer: number | undefined
let draftTimer: number | undefined
let likeTickTimer: number | undefined
const publishDraftKey = 'open-spec:publish-draft:v2'
const authStorageKey = 'open-spec:auth-token:v1'
const likeCooldownMs = 3000

const authState = reactive({
  token: '',
  username: '',
  isAuthenticated: false,
})

const loginForm = reactive({
  username: '',
  password: '',
})

const permissionModal = reactive({
  open: false,
  title: '需要登录',
  description: '该操作仅登录用户可用，请先完成登录。',
})

const logoutModal = reactive({
  open: false,
})
const likingMap = reactive<Record<string, boolean>>({})
const likeCooldownUntil = reactive<Record<string, number>>({})
const likePopMap = reactive<Record<string, boolean>>({})
const likeNow = ref(Date.now())
const likePopTimers = new Map<string, number>()

function onHashChange() {
  route.value = window.location.hash || '#/'
}

function navigate(path: string) {
  window.location.hash = path
}

function openPermissionModal() {
  permissionModal.open = true
}

function closePermissionModal() {
  permissionModal.open = false
}

function confirmPermissionModal() {
  closePermissionModal()
  navigate('#/login')
}

function saveAuthToken(token: string) {
  authState.token = token
  authState.isAuthenticated = true
  setAuthToken(token)
  localStorage.setItem(authStorageKey, token)
}

function clearAuth() {
  authState.token = ''
  authState.username = ''
  authState.isAuthenticated = false
  setAuthToken('')
  localStorage.removeItem(authStorageKey)
}

async function restoreAuth() {
  const storedToken = localStorage.getItem(authStorageKey)
  if (!storedToken) {
    return
  }
  setAuthToken(storedToken)
  try {
    const result = await fetchCurrentUser()
    authState.username = result.data.user.username
    saveAuthToken(storedToken)
  } catch {
    clearAuth()
  }
}

async function submitLogin() {
  if (isLoggingIn.value) {
    return
  }
  if (!loginForm.username.trim() || !loginForm.password.trim()) {
    showToast('请先填写账号和密码。')
    return
  }
  isLoggingIn.value = true
  try {
    const result = await login({
      username: loginForm.username.trim(),
      password: loginForm.password.trim(),
    })
    saveAuthToken(result.data.token)
    authState.username = result.data.user.username
    showToast('登录成功。')
    loginForm.password = ''
    navigate('#/')
  } catch {
    showToast('登录失败：账号或密码错误。')
  } finally {
    isLoggingIn.value = false
  }
}

function confirmLogout() {
  logoutModal.open = true
}

function cancelLogout() {
  logoutModal.open = false
}

async function doLogout() {
  try {
    await logout()
  } catch {
    // ignore network errors on logout
  }
  cancelLogout()
  clearAuth()
  showToast('已退出登录。')
  navigate('#/')
}

function showToast(message: string) {
  toastText.value = message
  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }
  toastTimer = window.setTimeout(() => {
    toastText.value = ''
  }, 2400)
}

function postLikeKey(post: BlogPost) {
  return post.slug || String(post.id)
}

function getPostLikes(post: BlogPost) {
  const likes = Number(post.likes)
  return Number.isFinite(likes) && likes >= 0 ? likes : 0
}

function getReadingTime(post: BlogPost): number {
  const content = post.content || ''
  // Average Chinese reading speed: ~400 chars/min, English: ~200 words/min
  const chineseChars = (content.match(/[\u4e00-\u9fa5]/g) || []).length
  const englishWords = (content.replace(/[\u4e00-\u9fa5]/g, ' ').match(/\S+/g) || []).length
  const totalMinutes = (chineseChars / 400) + (englishWords / 200)
  return Math.max(1, Math.ceil(totalMinutes))
}

interface TocItem {
  id: string
  text: string
  level: number
}

function extractToc(html: string | undefined): TocItem[] {
  const content = html ?? ''
  const toc: TocItem[] = []
  let counter = 0
  const regex = /<h([2-3])[^>]*>(.*?)<\/h[2-3]>/gi
  let match
  while ((match = regex.exec(content)) !== null) {
    const level = parseInt(match[1] ?? '2')
    const text = (match[2] ?? '').replace(/<[^>]+>/g, '')
    const id = `heading-${counter++}`
    toc.push({ id, text, level })
  }
  return toc
}

function getLikeCooldownLeft(post: BlogPost) {
  const key = postLikeKey(post)
  const remaining = (likeCooldownUntil[key] ?? 0) - likeNow.value
  if (remaining <= 0) {
    return 0
  }
  return Math.ceil(remaining / 1000)
}

function isLikeDisabled(post: BlogPost) {
  const key = postLikeKey(post)
  return Boolean(likingMap[key]) || getLikeCooldownLeft(post) > 0
}

function isLikePopping(post: BlogPost) {
  return Boolean(likePopMap[postLikeKey(post)])
}

function triggerLikePop(key: string) {
  likePopMap[key] = false
  window.requestAnimationFrame(() => {
    likePopMap[key] = true
    const oldTimer = likePopTimers.get(key)
    if (oldTimer) {
      window.clearTimeout(oldTimer)
    }
    const nextTimer = window.setTimeout(() => {
      likePopMap[key] = false
      likePopTimers.delete(key)
    }, 520)
    likePopTimers.set(key, nextTimer)
  })
}

function syncLikedPost(updated: BlogPost) {
  posts.value = posts.value.map((post) => (post.id === updated.id ? { ...post, ...updated } : post))
  if (currentPost.value?.id === updated.id) {
    currentPost.value = { ...currentPost.value, ...updated }
  }
}

async function submitLike(post: BlogPost) {
  const key = postLikeKey(post)
  if (isLikeDisabled(post)) {
    return
  }
  likingMap[key] = true
  try {
    const result = await likePost(post.slug || post.id)
    syncLikedPost(result.data)
    likeCooldownUntil[key] = Date.now() + likeCooldownMs
    triggerLikePop(key)
  } catch {
    showToast('点赞失败，请稍后重试。')
  } finally {
    likingMap[key] = false
  }
}

function saveDraft() {
  const payload = {
    title: publishForm.title,
    excerpt: publishForm.excerpt,
    contentHtml: publishForm.contentHtml,
    tags: publishForm.tags,
    updatedAt: new Date().toLocaleTimeString(),
  }
  localStorage.setItem(publishDraftKey, JSON.stringify(payload))
  draftSavedAt.value = payload.updatedAt
}

function restoreDraft() {
  const raw = localStorage.getItem(publishDraftKey)
  if (!raw) {
    return
  }
  try {
    const payload = JSON.parse(raw) as typeof publishForm & { updatedAt?: string }
    publishForm.title = payload.title ?? ''
    publishForm.excerpt = payload.excerpt ?? ''
    publishForm.contentHtml = payload.contentHtml ?? '<p>在这里开始创作...</p>'
    publishForm.tags = payload.tags ?? ''
    draftSavedAt.value = payload.updatedAt ?? ''
  } catch {
    localStorage.removeItem(publishDraftKey)
  }
}

function clearDraft() {
  localStorage.removeItem(publishDraftKey)
  draftSavedAt.value = ''
}

async function loadSite() {
  try {
    const result = await fetchSiteInfo()
    siteInfo.value = result.data
    const stats = await fetchSiteStats()
    siteStats.value = stats.data
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '加载站点信息失败。'
  }
}

async function loadTags() {
  try {
    const result = await fetchTags()
    tags.value = result.data
  } catch {
    tags.value = []
  }
}

async function loadPosts() {
  loadingPosts.value = true
  errorText.value = ''
  try {
    const result = await fetchPosts({
      page: filters.page,
      pageSize: filters.pageSize,
      search: filters.search.trim() || undefined,
      tag: filters.tag || undefined,
    })
    posts.value = result.data.items
    const pagination = (result.meta?.pagination as Record<string, number> | undefined) ?? {}
    filters.total = pagination.total ?? result.data.items.length
    filters.totalPages = pagination.totalPages ?? 1
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '加载文章失败。'
  } finally {
    loadingPosts.value = false
  }
}

async function loadPostDetail(slug: string) {
  detailLoading.value = true
  errorText.value = ''
  currentPost.value = null
  relatedPosts.value = []
  detailToc.value = []
  detailTocHtml.value = ''
  try {
    const result = await fetchPostDetail(slug)
    currentPost.value = result.data
    // Generate TOC
    if (result.data.contentHtml) {
      detailToc.value = extractToc(result.data.contentHtml)
      detailTocHtml.value = result.data.contentHtml ? addHeadingIds(result.data.contentHtml) : ''
    }
    // Increment view count silently
    viewPost(slug).catch(() => {})
    // Load related posts
    const related = await fetchRelatedPosts(slug, 3)
    relatedPosts.value = related.data
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '加载文章详情失败。'
  } finally {
    detailLoading.value = false
  }
}

function addHeadingIds(html: string): string {
  let counter = 0
  return html.replace(/<h([2-3])([^>]*)>(.*?)<\/h[2-3]>/gi, (_, level, attrs, content) => {
    return `<h${level}${attrs} id="heading-${counter++}">${content}</h${level}>`
  })
}

function scrollToHeading(id: string) {
  const el = document.getElementById(id)
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

function resetPublishForm() {
  publishForm.title = ''
  publishForm.excerpt = ''
  publishForm.contentHtml = '<p>在这里开始创作...</p>'
  publishForm.tags = ''
  clearDraft()
}

async function publishPost() {
  if (!canPublish.value) {
    openPermissionModal()
    showToast('请先登录后再发布文章。')
    return
  }
  if (isPublishing.value) {
    return
  }
  publishStatus.value = ''
  const cleanHtml = sanitizeRichHtml(publishForm.contentHtml)
  const plainContent = plainTextFromHtml(cleanHtml)
  const payload = {
    title: publishForm.title.trim(),
    excerpt: publishForm.excerpt.trim(),
    content: plainContent,
    contentHtml: cleanHtml,
    tags: publishForm.tags
      .split(',')
      .map((tag) => tag.trim())
      .filter(Boolean),
  }

  if (!payload.title || !payload.excerpt || !payload.content) {
    publishStatus.value = '发布失败：标题、摘要和正文不能为空。'
    showToast('请先完善标题、摘要和正文内容。')
    return
  }

  isPublishing.value = true
  try {
    const result = await createPost(payload)
    publishStatus.value = '发布成功，正在跳转到文章详情。'
    showToast('文章发布成功。')
    resetPublishForm()
    await Promise.all([loadPosts(), loadTags()])
    navigate(`#/posts/${result.data.slug}`)
  } catch (error) {
    publishStatus.value = error instanceof Error ? `发布失败：${error.message}` : '发布失败。'
    showToast('发布失败，请检查后重试。')
  } finally {
    isPublishing.value = false
  }
}

async function retryCurrentFlow() {
  errorText.value = ''
  if (route.value.startsWith('#/posts/')) {
    await loadPostDetail(currentSlug.value)
    return
  }
  if (route.value === '#/posts') {
    await loadPosts()
    return
  }
  await Promise.all([loadSite(), loadTags(), loadPosts()])
}

function prevPage() {
  if (filters.page > 1) {
    filters.page -= 1
  }
}

function nextPage() {
  if (filters.page < filters.totalPages) {
    filters.page += 1
  }
}

async function copyText(content: string, message = '复制成功') {
  try {
    await navigator.clipboard.writeText(content)
    showToast(message)
  } catch {
    showToast('复制失败，请手动复制。')
  }
}

function decorateCodeBlocks() {
  const root = detailBodyRef.value
  if (!root) {
    return
  }
  root.querySelectorAll('pre').forEach((pre) => {
    if (pre.querySelector('.copy-code-btn')) {
      return
    }
    const button = document.createElement('button')
    button.className = 'copy-code-btn'
    button.type = 'button'
    button.textContent = '复制代码'
    pre.appendChild(button)
  })
}

function onWindowScroll() {
  showBackToTop.value = window.scrollY > 360
}

function scrollToTop() {
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

function handleDetailAreaClick(event: MouseEvent) {
  const target = event.target as HTMLElement
  if (!target.classList.contains('copy-code-btn')) {
    return
  }
  const code = target.parentElement?.querySelector('code')?.textContent ?? ''
  void copyText(code, '代码已复制。')
}

watch(
  () => [filters.search, filters.tag] as const,
  () => {
    filters.page = 1
    if (filterTimer) {
      window.clearTimeout(filterTimer)
    }
    filterTimer = window.setTimeout(() => {
      if (route.value === '#/posts') {
        void loadPosts()
      }
    }, 250)
  },
)

watch(
  () => [publishForm.title, publishForm.excerpt, publishForm.contentHtml, publishForm.tags] as const,
  () => {
    if (!isPublishRoute.value) {
      return
    }
    if (draftTimer) {
      window.clearTimeout(draftTimer)
    }
    draftTimer = window.setTimeout(() => {
      saveDraft()
    }, 600)
  },
)

watch(
  () => filters.page,
  () => {
    if (route.value === '#/posts') {
      void loadPosts()
    }
  },
)

watch(
  () => route.value,
  async (value) => {
    if (value === '#/posts') {
      await loadPosts()
      return
    }
    if (value.startsWith('#/posts/')) {
      await loadPostDetail(currentSlug.value)
      await nextTick()
      decorateCodeBlocks()
      return
    }
    if (value === '#/publish') {
      restoreDraft()
    }
  },
  { immediate: true },
)

onMounted(async () => {
  // Restore theme from localStorage
  const savedTheme = localStorage.getItem(themeStorageKey) as 'dark' | 'light' | null
  if (savedTheme) {
    applyTheme(savedTheme)
  }
  
  if (!window.location.hash) {
    window.location.hash = '#/'
  }
  window.addEventListener('hashchange', onHashChange)
  window.addEventListener('scroll', onWindowScroll, { passive: true })
  likeTickTimer = window.setInterval(() => {
    likeNow.value = Date.now()
  }, 1000)
  restoreDraft()
  await restoreAuth()
  await Promise.all([loadSite(), loadTags(), loadPosts()])
})

onUnmounted(() => {
  if (filterTimer) {
    window.clearTimeout(filterTimer)
  }
  if (draftTimer) {
    window.clearTimeout(draftTimer)
  }
  if (toastTimer) {
    window.clearTimeout(toastTimer)
  }
  if (likeTickTimer) {
    window.clearInterval(likeTickTimer)
  }
  likePopTimers.forEach((timer) => {
    window.clearTimeout(timer)
  })
  likePopTimers.clear()
  window.removeEventListener('scroll', onWindowScroll)
  window.removeEventListener('hashchange', onHashChange)
})
</script>

<template>
  <div class="page-bg"></div>
  <div class="app-shell">
    <header class="top-nav glass">
      <div class="brand" @click="navigate('#/')">{{ siteInfo.title }}</div>
      <nav>
        <button class="nav-link" :class="{ active: route === '#/' }" @click="navigate('#/')">首页</button>
        <button class="nav-link" :class="{ active: isPostsRoute || isPostDetail }" @click="navigate('#/posts')">文章</button>
        <button class="nav-link" :class="{ active: isPublishRoute }" @click="navigate('#/publish')">发布</button>
        <button class="nav-link" :class="{ active: isAboutRoute }" @click="navigate('#/about')">关于</button>
        <button class="nav-link theme-toggle" @click="toggleTheme" :title="currentTheme === 'dark' ? '切换到浅色模式' : '切换到深色模式'">
          {{ currentTheme === 'dark' ? '🌙' : '☀️' }}
        </button>
        <button v-if="!authState.isAuthenticated" class="nav-link" :class="{ active: isLoginRoute }" @click="navigate('#/login')">
          登录
        </button>
        <div v-else class="auth-chip">
          <span class="auth-state">已登录</span>
          <span class="auth-name">{{ displayUsername }}</span>
          <button class="nav-link logout-btn" @click="confirmLogout">退出</button>
    </div>
      </nav>
  </header>

    <main class="content-wrap">
      <section v-if="route === '#/'" class="hero glass">
        <p class="eyebrow">{{ siteInfo.subtitle }}</p>
        <h1>你好，我是 {{ siteInfo.authorName || '朋友' }}，记录技术与创作灵感。</h1>
        <p class="hero-text">{{ siteInfo.bio }}</p>
        <div class="hero-actions">
          <button class="btn btn-main" @click="navigate('#/posts')">阅读文章</button>
          <button class="btn btn-ghost" @click="navigate('#/publish')">发布新文章</button>
        </div>
        <div class="site-stats">
          <div class="stat-item">
            <span class="stat-value">{{ siteStats.totalPosts }}</span>
            <span class="stat-label">文章</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ siteStats.totalViews }}</span>
            <span class="stat-label">阅读</span>
          </div>
          <div class="stat-divider"></div>
          <div class="stat-item">
            <span class="stat-value">{{ siteStats.totalLikes }}</span>
            <span class="stat-label">点赞</span>
          </div>
        </div>
      </section>

      <section v-if="route === '#/'" class="featured">
        <h2>精选文章</h2>
        <p v-if="loadingPosts">文章加载中...</p>
        <p v-if="!loadingPosts && featuredPosts.length === 0">暂无精选文章，稍后重试。</p>
        <div class="card-grid">
          <article v-for="post in featuredPosts" :key="post.id" class="post-card glass">
            <p class="post-date">{{ post.date }} <span class="view-count">· {{ getReadingTime(post) }} 分钟</span></p>
            <h3>{{ post.title }}</h3>
            <p>{{ post.excerpt }}</p>
            <button class="text-link" @click="navigate(`#/posts/${post.slug}`)">阅读全文</button>
          </article>
        </div>
      </section>

      <section v-if="isPostsRoute" class="post-list">
        <h2>所有文章</h2>
        <div class="filter-bar glass">
          <input v-model="filters.search" type="search" placeholder="搜索标题/摘要/正文..." />
          <select v-model="filters.tag">
            <option value="">全部标签</option>
            <option v-for="tag in tags" :key="tag" :value="tag">{{ tag }}</option>
          </select>
        </div>
        <p v-if="loadingPosts">文章加载中...</p>
        <p v-if="!loadingPosts && posts.length === 0">当前暂无文章。</p>
        <div class="card-grid">
          <article v-for="post in posts" :key="post.id" class="post-card glass">
            <p class="post-date">{{ post.date }} <span class="view-count">· {{ getReadingTime(post) }} 分钟</span></p>
            <h3>{{ post.title }}</h3>
            <p>{{ post.excerpt }}</p>
            <div class="tag-row">
              <span v-for="tag in post.tags" :key="tag" class="tag">{{ tag }}</span>
            </div>
            <div class="card-actions">
              <button class="text-link" @click="navigate(`#/posts/${post.slug}`)">查看详情</button>
              <button
                class="btn btn-ghost like-btn"
                :class="{ 'is-pop': isLikePopping(post), 'is-cooling': getLikeCooldownLeft(post) > 0 }"
                :disabled="isLikeDisabled(post)"
                @click="submitLike(post)"
              >
                <span class="like-core">
                  <span class="like-icon">♥</span>
                  <span class="like-count">点赞 {{ getPostLikes(post) }}</span>
                </span>
                <span v-if="isLikePopping(post)" class="like-plus-one">+1</span>
                <span v-if="getLikeCooldownLeft(post) > 0" class="cooldown-text">{{ getLikeCooldownLeft(post) }}s</span>
              </button>
            </div>
          </article>
        </div>
        <div class="pagination">
          <button class="btn btn-ghost" :disabled="filters.page <= 1 || loadingPosts" @click="prevPage">
            上一页
          </button>
          <span>第 {{ filters.page }} / {{ filters.totalPages }} 页（共 {{ filters.total }} 篇）</span>
          <button
            class="btn btn-ghost"
            :disabled="filters.page >= filters.totalPages || loadingPosts"
            @click="nextPage"
          >
            下一页
          </button>
        </div>
      </section>

      <section v-if="isPostDetail" class="post-detail glass">
        <template v-if="detailLoading">
          <h2>文章加载中...</h2>
        </template>
        <template v-else-if="currentPost">
          <p class="post-date">{{ currentPost.date }} <span class="view-count">· 阅读 {{ currentPost.views ?? 0 }}</span> <span class="view-count">· {{ getReadingTime(currentPost) }} 分钟阅读</span></p>
          <h2>{{ currentPost.title }}</h2>
          <div class="detail-layout">
            <div class="detail-main">
              <div
                v-if="currentPost.contentHtml"
                ref="detailBodyRef"
                class="detail-content rich-content"
                @click="handleDetailAreaClick"
                v-html="detailTocHtml || safeDetailHtml"
              ></div>
              <p v-else class="detail-content">{{ currentPost.content }}</p>
            </div>
            <aside v-if="detailToc.length > 0" class="detail-toc">
              <p class="toc-title">目录</p>
              <nav>
                <button
                  v-for="item in detailToc"
                  :key="item.id"
                  class="toc-item"
                  :class="{ 'toc-h3': item.level === 3 }"
                  @click="scrollToHeading(item.id)"
                >
                  {{ item.text }}
                </button>
              </nav>
            </aside>
          </div>
          <div class="tag-row detail-tag-row">
            <span v-for="tag in currentPost.tags" :key="tag" class="tag">{{ tag }}</span>
          </div>
          <div class="detail-actions">
            <button
              class="btn btn-ghost like-btn"
              :class="{ 'is-pop': isLikePopping(currentPost), 'is-cooling': getLikeCooldownLeft(currentPost) > 0 }"
              :disabled="isLikeDisabled(currentPost)"
              @click="submitLike(currentPost)"
            >
              <span class="like-core">
                <span class="like-icon">♥</span>
                <span class="like-count">点赞 {{ getPostLikes(currentPost) }}</span>
              </span>
              <span v-if="isLikePopping(currentPost)" class="like-plus-one">+1</span>
              <span v-if="getLikeCooldownLeft(currentPost) > 0" class="cooldown-text">
                {{ getLikeCooldownLeft(currentPost) }}s 后可再赞
              </span>
            </button>
            <button class="btn btn-main" @click="navigate('#/posts')">返回列表</button>
          </div>

          <div v-if="relatedPosts.length > 0" class="related-posts">
            <h3>相关文章</h3>
            <div class="related-list">
              <article v-for="post in relatedPosts" :key="post.id" class="related-card" @click="navigate(`/posts/${post.slug}`)">
                <p class="related-title">{{ post.title }}</p>
                <p class="related-meta">{{ post.date }} · {{ getReadingTime(post) }} 分钟阅读</p>
              </article>
            </div>
          </div>
        </template>
        <template v-else>
          <h2>文章不存在</h2>
          <p>你访问的文章可能已被删除或链接错误。</p>
          <button class="btn btn-main" @click="navigate('#/posts')">回到文章页</button>
        </template>
      </section>

      <section v-if="route === '#/publish'" class="publish glass">
        <h2>发布文章</h2>
        <p>升级为成熟编辑器：支持更稳定排版、撤销重做、链接管理与更一致的内容回显。</p>
        <p v-if="!canPublish" class="auth-tip">当前为游客模式，登录后可发布文章。</p>
        <p v-if="draftSavedAt" class="draft-hint">草稿已自动保存（{{ draftSavedAt }}）</p>
        <form class="publish-form" @submit.prevent="publishPost">
          <label>
            标题
            <input v-model="publishForm.title" type="text" placeholder="输入文章标题" :disabled="isPublishing" />
          </label>
          <label>
            摘要
            <input
              v-model="publishForm.excerpt"
              type="text"
              placeholder="一句话摘要"
              :disabled="isPublishing"
            />
          </label>
          <label>
            标签（逗号分隔）
            <input v-model="publishForm.tags" type="text" placeholder="Vue, API, Blog" :disabled="isPublishing" />
          </label>
          <label>
            正文
            <RichTextEditor v-model="publishForm.contentHtml" :disabled="isPublishing" @notify="showToast" />
          </label>
          <div class="publish-actions">
            <button class="btn btn-main" type="submit" :disabled="isPublishing">
              {{ isPublishing ? '发布中...' : '发布文章' }}
            </button>
            <button class="btn btn-ghost" type="button" :disabled="isPublishing" @click="clearDraft">清除草稿</button>
          </div>
        </form>
        <p v-if="publishStatus" class="status-text">{{ publishStatus }}</p>
      </section>

      <section v-if="isLoginRoute" class="login-card glass">
        <h2>账号登录</h2>
        <p class="login-desc">无注册流程，请使用管理员预置账号登录。</p>
        <form class="login-form" @submit.prevent="submitLogin">
          <label>
            账号
            <input
              id="login-username"
              v-model="loginForm.username"
              name="username"
              type="text"
              autocomplete="username"
              placeholder="请输入账号"
              :disabled="isLoggingIn"
            />
          </label>
          <label>
            密码
            <input
              id="login-password"
              v-model="loginForm.password"
              name="password"
              type="password"
              autocomplete="current-password"
              placeholder="请输入密码"
              :disabled="isLoggingIn"
            />
          </label>
          <button class="btn btn-main" type="submit" :disabled="isLoggingIn">
            {{ isLoggingIn ? '登录中...' : '登录' }}
          </button>
        </form>
      </section>

      <section v-if="route === '#/about'" class="about glass">
        <h2>关于我</h2>
        <p>
          我关注前端开发、用户体验和个人创造力表达。这个博客将持续更新：开发笔记、设计思考和项目复盘。
        </p>
        <p>目标是打造一个“第一眼就记住”的个人站点，同时保持稳定、清晰、可维护。</p>
        <div class="tag-row">
          <span v-for="tag in tags" :key="tag" class="tag">{{ tag }}</span>
        </div>
      </section>

      <section v-if="errorText" class="error-box glass">
        <strong>请求异常：</strong>{{ errorText }}
        <div class="error-actions">
          <button class="btn btn-main" @click="retryCurrentFlow">重试请求</button>
        </div>
      </section>
      <button v-if="showBackToTop" class="to-top-btn" @click="scrollToTop">回到顶部</button>
      <div v-if="toastText" class="toast">{{ toastText }}</div>
  </main>
    <AppModal
      :open="permissionModal.open"
      title="权限受限"
      :description="permissionModal.description"
      confirm-text="去登录"
      cancel-text="取消"
      @close="closePermissionModal"
      @cancel="closePermissionModal"
      @confirm="confirmPermissionModal"
    />
    <AppModal
      :open="logoutModal.open"
      title="退出登录"
      description="确认退出当前账号吗？退出后将无法继续发布文章。"
      confirm-text="确认退出"
      cancel-text="取消"
      danger
      @close="cancelLogout"
      @cancel="cancelLogout"
      @confirm="doLogout"
    />
  </div>
</template>

<style scoped>
.page-bg {
  position: fixed;
  inset: 0;
  z-index: -1;
  background: var(--page-bg);
}

.app-shell {
  width: min(1100px, 92vw);
  margin: 2rem auto 4rem;
}

.glass {
  background: var(--bg-glass);
  border: 1px solid var(--border);
  border-radius: 1.1rem;
  backdrop-filter: blur(12px);
}

.top-nav {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0.9rem 1rem;
  background: var(--nav-bg);
}

.brand {
  font-size: 1.15rem;
  font-weight: 700;
  letter-spacing: 0.08em;
  cursor: pointer;
  color: var(--text-primary);
}

nav {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  flex-wrap: wrap;
}

.nav-link {
  border: 0;
  background: transparent;
  color: var(--text-primary);
  padding: 0.55rem 0.8rem;
  border-radius: 0.7rem;
  cursor: pointer;
  transition: background-color 0.22s ease;
}

.nav-link:hover {
  background: rgba(255, 255, 255, 0.1);
}

.nav-link.active {
  background: var(--accent-glow);
  box-shadow: inset 0 0 0 1px var(--accent);
}

.theme-toggle {
  font-size: 1.1rem;
  padding: 0.4rem 0.6rem;
}

.theme-toggle:hover {
  background: rgba(255, 255, 255, 0.1);
}

.auth-chip {
  display: flex;
  align-items: center;
  gap: 0.45rem;
  border-radius: 999px;
  background: rgba(148, 163, 184, 0.18);
  border: 1px solid rgba(148, 163, 184, 0.35);
  padding: 0.24rem 0.45rem 0.24rem 0.62rem;
}

.auth-state {
  font-size: 0.72rem;
  color: #a5b4fc;
}

.auth-name {
  font-size: 0.82rem;
  font-weight: 600;
  color: #bfdbfe;
}

.logout-btn {
  padding: 0.35rem 0.65rem;
}

.content-wrap {
  margin-top: 1.2rem;
  display: grid;
  gap: 1.2rem;
}

.hero,
.post-detail,
.about,
.publish,
.error-box {
  padding: 1.5rem;
}

.eyebrow {
  color: var(--accent);
  font-size: 0.8rem;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

h1 {
  margin-top: 0.6rem;
  font-size: clamp(1.6rem, 3.5vw, 2.5rem);
  line-height: 1.18;
  color: var(--text-primary);
}

h2 {
  font-size: clamp(1.25rem, 2.2vw, 1.8rem);
  margin-bottom: 0.9rem;
  color: var(--text-primary);
}

h3 {
  margin: 0.35rem 0 0.5rem;
  font-size: 1.1rem;
  color: var(--text-primary);
}

.hero-text {
  margin-top: 0.8rem;
  max-width: 70ch;
  color: var(--text-secondary);
}

.hero-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.7rem;
  flex-wrap: wrap;
}

.site-stats {
  display: flex;
  align-items: center;
  gap: 1.2rem;
  margin-top: 1.5rem;
  padding: 0.8rem 1.2rem;
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 0.9rem;
  width: fit-content;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 0.15rem;
}

.stat-value {
  font-size: 1.3rem;
  font-weight: 700;
  color: var(--accent);
}

.stat-label {
  font-size: 0.72rem;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
}

.stat-divider {
  width: 1px;
  height: 28px;
  background: var(--border);
}

.btn {
  border: 0;
  border-radius: 0.75rem;
  padding: 0.66rem 1rem;
  cursor: pointer;
  font-weight: 600;
  transition: transform 0.2s ease;
}

.btn:hover {
  transform: translateY(-1px);
}

.btn-main {
  background: linear-gradient(135deg, var(--accent), #818cf8);
  color: var(--bg-primary);
}

.btn-ghost {
  background: var(--card-bg);
  color: var(--text-primary);
  border: 1px solid var(--border);
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(12, minmax(0, 1fr));
  gap: 0.95rem;
}

.post-card {
  grid-column: span 12;
  padding: 1rem;
  background: var(--card-bg);
  border: 1px solid var(--border);
  transition:
    transform 0.25s ease,
    border-color 0.25s ease,
    box-shadow 0.25s ease;
  will-change: transform;
}

.post-card:hover {
  transform: translateY(-4px);
  border-color: var(--accent);
  box-shadow: var(--shadow);
}

.post-date {
  color: var(--text-secondary);
  font-size: 0.82rem;
}

.view-count {
  color: #94a3b8;
  font-size: 0.78rem;
}

.text-link {
  margin-top: 0.7rem;
  border: 0;
  background: transparent;
  color: #67e8f9;
  cursor: pointer;
  padding: 0;
}

.card-actions {
  margin-top: 0.75rem;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 0.6rem;
  flex-wrap: wrap;
}

.tag-row {
  margin-top: 0.75rem;
  display: flex;
  gap: 0.45rem;
  flex-wrap: wrap;
}

.detail-tag-row {
  margin-top: 1rem;
}

.tag {
  font-size: 0.76rem;
  color: var(--tag-text);
  background: var(--tag-bg);
  border: 1px solid var(--border);
  border-radius: 999px;
  padding: 0.2rem 0.55rem;
}

.detail-content {
  margin: 0.8rem 0 0.4rem;
  color: var(--text-secondary);
  line-height: 1.8;
  font-size: 0.97rem;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 180px;
  gap: 1.5rem;
  align-items: start;
}

@media (max-width: 768px) {
  .detail-layout {
    grid-template-columns: 1fr;
  }
  .detail-toc {
    display: none;
  }
}

.detail-main {
  min-width: 0;
}

.detail-toc {
  position: sticky;
  top: 1rem;
  padding: 0.8rem;
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 0.8rem;
  max-height: 70vh;
  overflow-y: auto;
}

.toc-title {
  font-size: 0.8rem;
  font-weight: 600;
  color: var(--text-muted);
  text-transform: uppercase;
  letter-spacing: 0.08em;
  margin-bottom: 0.6rem;
}

.toc-item {
  display: block;
  width: 100%;
  text-align: left;
  background: none;
  border: 0;
  color: var(--text-secondary);
  font-size: 0.82rem;
  padding: 0.3rem 0.4rem;
  cursor: pointer;
  border-radius: 0.4rem;
  transition: color 0.2s ease, background 0.2s ease;
}

.toc-item:hover {
  color: var(--accent);
  background: var(--accent-glow);
}

.toc-h3 {
  padding-left: 1rem;
  font-size: 0.78rem;
}

.detail-actions {
  margin-top: 1rem;
  display: flex;
  gap: 0.7rem;
  flex-wrap: wrap;
}

.like-btn {
  position: relative;
  overflow: hidden;
  isolation: isolate;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 0.45rem;
  min-width: 126px;
  padding: 0.52rem 0.85rem;
  border: 1px solid rgba(56, 189, 248, 0.5);
  background:
    radial-gradient(circle at 15% 20%, rgba(34, 211, 238, 0.35), transparent 48%),
    linear-gradient(130deg, rgba(8, 47, 73, 0.7), rgba(29, 78, 216, 0.32), rgba(91, 33, 182, 0.28));
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.35),
    0 8px 24px rgba(8, 47, 73, 0.35);
  transition:
    transform 0.22s ease,
    border-color 0.22s ease,
    box-shadow 0.22s ease;
}

.like-btn::before {
  content: '';
  position: absolute;
  inset: -1px;
  z-index: -1;
  background: linear-gradient(110deg, transparent 20%, rgba(255, 255, 255, 0.45) 45%, transparent 65%);
  transform: translateX(-130%);
}

.like-btn:hover {
  transform: translateY(-1px) scale(1.01);
  border-color: rgba(34, 211, 238, 0.95);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.4),
    0 12px 28px rgba(8, 47, 73, 0.46);
}

.like-btn:hover::before {
  animation: likeShimmer 0.85s ease;
}

.like-core {
  display: inline-flex;
  align-items: center;
  gap: 0.35rem;
}

.like-icon {
  color: #fb7185;
  font-size: 0.96rem;
  text-shadow:
    0 0 8px rgba(251, 113, 133, 0.55),
    0 0 14px rgba(244, 63, 94, 0.35);
}

.like-count {
  font-weight: 600;
  letter-spacing: 0.02em;
}

.like-btn.is-pop::after {
  content: '';
  position: absolute;
  left: 50%;
  top: 50%;
  width: 8px;
  height: 8px;
  border-radius: 999px;
  border: 2px solid rgba(103, 232, 249, 0.8);
  transform: translate(-50%, -50%);
  animation: likeRipple 0.55s ease-out forwards;
}

.like-btn.is-pop .like-icon {
  animation: heartBeat 0.5s cubic-bezier(0.2, 1, 0.2, 1);
}

.like-plus-one {
  position: absolute;
  top: -0.2rem;
  right: 0.45rem;
  font-size: 0.74rem;
  color: #f0abfc;
  text-shadow: 0 0 12px rgba(240, 171, 252, 0.62);
  animation: likePlusFloat 0.6s ease-out forwards;
}

.like-btn.is-cooling {
  border-color: rgba(148, 163, 184, 0.45);
  box-shadow: none;
  background: linear-gradient(130deg, rgba(15, 23, 42, 0.55), rgba(51, 65, 85, 0.35));
}

.like-btn.is-cooling .like-icon {
  color: #fda4af;
  text-shadow: none;
}

.like-btn:disabled {
  cursor: not-allowed;
  opacity: 0.78;
}

.cooldown-text {
  font-size: 0.74rem;
  color: #bfdbfe;
}

.related-posts {
  margin-top: 2rem;
  padding-top: 1.2rem;
  border-top: 1px solid var(--border);
}

.related-posts h3 {
  font-size: 1rem;
  margin-bottom: 0.8rem;
  color: var(--text-secondary);
}

.related-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 0.7rem;
}

.related-card {
  padding: 0.8rem;
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 0.8rem;
  cursor: pointer;
  transition: border-color 0.2s ease, transform 0.2s ease;
}

.related-card:hover {
  border-color: var(--accent);
  transform: translateY(-2px);
}

.related-title {
  font-size: 0.88rem;
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 0.3rem;
}

.related-meta {
  font-size: 0.76rem;
  color: var(--text-muted);
}

@keyframes likeShimmer {
  from {
    transform: translateX(-130%);
  }
  to {
    transform: translateX(140%);
  }
}

@keyframes heartBeat {
  0% {
    transform: scale(1);
  }
  38% {
    transform: scale(1.35);
  }
  65% {
    transform: scale(0.92);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes likePlusFloat {
  0% {
    opacity: 0;
    transform: translateY(0);
  }
  15% {
    opacity: 1;
  }
  100% {
    opacity: 0;
    transform: translateY(-16px);
  }
}

@keyframes likeRipple {
  0% {
    opacity: 0.75;
    width: 8px;
    height: 8px;
  }
  100% {
    opacity: 0;
    width: 90px;
    height: 90px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .like-btn,
  .like-btn::before,
  .like-btn::after,
  .like-btn.is-pop .like-icon,
  .like-plus-one {
    animation: none !important;
    transition: none !important;
  }
}

.rich-content :deep(h1),
.rich-content :deep(h2),
.rich-content :deep(h3),
.rich-content :deep(h4) {
  color: var(--text-primary);
  font-weight: 700;
  line-height: 1.3;
  margin: 1.5rem 0 0.8rem;
}

.rich-content :deep(h2) {
  font-size: 1.45rem;
  padding-bottom: 0.4rem;
  border-bottom: 1px solid var(--border);
}

.rich-content :deep(h3) {
  font-size: 1.2rem;
}

.rich-content :deep(h4) {
  font-size: 1.05rem;
}

.rich-content :deep(p) {
  color: var(--text-secondary);
  line-height: 1.8;
  margin: 0.9rem 0;
  font-size: 0.97rem;
}

.rich-content :deep(a) {
  color: var(--accent);
  text-decoration: underline;
  text-underline-offset: 2px;
}

.rich-content :deep(a:hover) {
  color: var(--accent-hover);
}

.rich-content :deep(ul),
.rich-content :deep(ol) {
  color: var(--text-secondary);
  margin: 0.8rem 0;
  padding-left: 1.5rem;
}

.rich-content :deep(li) {
  margin: 0.4rem 0;
  line-height: 1.7;
}

.rich-content :deep(blockquote) {
  border-left: 4px solid var(--accent);
  margin: 1rem 0;
  padding: 0.5rem 1rem;
  background: var(--card-bg);
  border-radius: 0 0.5rem 0.5rem 0;
  color: var(--text-secondary);
}

.rich-content :deep(code) {
  font-family: 'SF Mono', 'Fira Code', 'Consolas', monospace;
  background: rgba(103, 232, 249, 0.08);
  color: var(--accent);
  padding: 0.15rem 0.4rem;
  border-radius: 0.3rem;
  font-size: 0.88em;
}

.rich-content :deep(pre) {
  background: var(--card-bg);
  border: 1px solid var(--border);
  border-radius: 0.8rem;
  padding: 1rem 1.2rem;
  overflow-x: auto;
  margin: 1rem 0;
}

.rich-content :deep(pre code) {
  background: none;
  color: var(--text-secondary);
  padding: 0;
  font-size: 0.88rem;
  line-height: 1.6;
}

.rich-content :deep(img) {
  max-width: 100%;
  border-radius: 0.7rem;
  margin: 1rem 0;
}

.rich-content :deep(hr) {
  border: none;
  border-top: 1px solid var(--border);
  margin: 1.5rem 0;
}

.rich-content :deep(.copy-code-btn) {
  position: absolute;
  right: 0.6rem;
  top: 0.6rem;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 0.5rem;
  background: rgba(15, 23, 42, 0.75);
  color: #e2e8f0;
  padding: 0.2rem 0.45rem;
  cursor: pointer;
  font-size: 0.74rem;
}

.rich-content :deep(.copy-code-btn) {
  position: absolute;
  right: 0.6rem;
  top: 0.6rem;
  border: 1px solid rgba(255, 255, 255, 0.35);
  border-radius: 0.5rem;
  background: rgba(15, 23, 42, 0.75);
  color: #e2e8f0;
  padding: 0.2rem 0.45rem;
  cursor: pointer;
  font-size: 0.74rem;
}

.filter-bar {
  margin-bottom: 0.9rem;
  display: grid;
  gap: 0.6rem;
  padding: 0.9rem;
}

.filter-bar input,
.filter-bar select {
  border: 1px solid var(--input-border);
  background: var(--input-bg);
  color: var(--text-primary);
  border-radius: 0.7rem;
  padding: 0.6rem 0.72rem;
}

.pagination {
  margin-top: 1rem;
    display: flex;
  align-items: center;
  gap: 0.75rem;
  flex-wrap: wrap;
  color: var(--text-secondary);
}

.publish-form {
  margin-top: 1rem;
  display: grid;
  gap: 0.8rem;
}

.publish-form label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.92rem;
  color: var(--text-primary);
}

.login-card {
  padding: 1.5rem;
}

.login-desc {
  color: var(--text-muted);
}

.login-form {
  margin-top: 0.9rem;
  display: grid;
  gap: 0.75rem;
}

.login-form label {
  display: grid;
  gap: 0.35rem;
  font-size: 0.92rem;
}

.publish-form input,
.publish-form textarea,
.login-form input {
  border: 1px solid rgba(255, 255, 255, 0.2);
  background: rgba(255, 255, 255, 0.06);
  color: #eef2ff;
  border-radius: 0.7rem;
  padding: 0.65rem 0.75rem;
}

.auth-tip {
  color: #fda4af;
  margin-top: 0.4rem;
}

.status-text {
  margin-top: 0.8rem;
  color: #93c5fd;
}

.draft-hint {
  color: #c4b5fd;
  font-size: 0.86rem;
}

.publish-actions {
    display: flex;
  gap: 0.7rem;
    flex-wrap: wrap;
}

.error-actions {
  margin-top: 0.9rem;
}

.to-top-btn {
  position: fixed;
  right: 1.2rem;
  bottom: 1.2rem;
  border: 0;
  border-radius: 999px;
  background: linear-gradient(135deg, #22d3ee, #818cf8);
  color: #04111f;
  padding: 0.5rem 0.85rem;
  cursor: pointer;
  box-shadow: 0 8px 20px rgba(15, 23, 42, 0.35);
}

.toast {
  position: fixed;
  left: 50%;
  bottom: 1.3rem;
  transform: translateX(-50%);
  background: rgba(2, 6, 23, 0.86);
  color: #e2e8f0;
  border: 1px solid rgba(148, 163, 184, 0.35);
  border-radius: 0.6rem;
  padding: 0.45rem 0.7rem;
}

@media (min-width: 700px) {
  .post-card {
    grid-column: span 6;
  }

  .filter-bar {
    grid-template-columns: 1fr 180px;
  }
}

@media (max-width: 640px) {
  .top-nav {
    align-items: flex-start;
    flex-direction: column;
    gap: 0.65rem;
  }
}
</style>
