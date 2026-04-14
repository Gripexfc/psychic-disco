import crypto from 'node:crypto'

import cors from 'cors'
import express from 'express'

import { readPosts, readSiteInfo, writePosts } from './storage.js'

const app = express()
const port = Number(process.env.PORT ?? 3000)
const frontendOrigin = process.env.FRONTEND_ORIGIN ?? 'http://localhost:5173'
const allowedOrigins = Array.from(
  new Set([frontendOrigin, 'http://localhost:5173', 'http://127.0.0.1:5173', 'http://localhost:4173']),
)
const authUser = {
  username: 'goodeat',
  password: 'goodeat123456',
}
const authToken = 'goodeat-session-token'

app.use(express.json({ limit: '1mb' }))
app.use(
  cors({
    origin(origin, callback) {
      if (!origin || allowedOrigins.includes(origin)) {
        callback(null, true)
        return
      }
      callback(new Error(`Origin ${origin} is not allowed by CORS`))
    },
    credentials: false,
  }),
)

app.use((req, _res, next) => {
  const requestId = crypto.randomUUID()
  req.headers['x-request-id'] = requestId
  next()
})

function ok(data, meta = {}) {
  return { data, meta, error: null }
}

function fail(code, message, details) {
  return {
    data: null,
    meta: {},
    error: { code, message, details },
  }
}

function toSlug(input) {
  const normalized = input
    .toLowerCase()
    .normalize('NFKD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim()
    .replace(/[^\p{Letter}\p{Number}\s-]/gu, '')
    .replace(/\s+/g, '-')
    .replace(/-+/g, '-')
    .replace(/^-+|-+$/g, '')
  return normalized
}

function toPositiveInt(value, fallback) {
  const num = Number(value)
  return Number.isInteger(num) && num > 0 ? num : fallback
}

function sanitizeHtml(input) {
  if (typeof input !== 'string') {
    return ''
  }
  return input
    .replace(/<script[\s\S]*?>[\s\S]*?<\/script>/gi, '')
    .replace(/\son\w+="[^"]*"/gi, '')
    .replace(/\sjavascript:/gi, '')
}

function normalizePost(post) {
  const likes = Number(post?.likes)
  return {
    ...post,
    likes: Number.isFinite(likes) && likes >= 0 ? likes : 0,
  }
}

function normalizePosts(posts) {
  return posts.map((item) => normalizePost(item))
}

function getBearerToken(req) {
  const authorization = req.headers.authorization
  if (!authorization || !authorization.startsWith('Bearer ')) {
    return ''
  }
  return authorization.slice(7).trim()
}

function requireAuth(req, res) {
  const token = getBearerToken(req)
  if (token !== authToken) {
    res.status(401).json(fail('UNAUTHORIZED', '请先登录后再执行该操作。'))
    return false
  }
  return true
}

app.get('/health', (_req, res) => {
  res.json(ok({ status: 'ok' }))
})

app.get('/api/health', (_req, res) => {
  res.json(ok({ status: 'ok' }))
})

app.post('/api/auth/login', (req, res) => {
  const { username, password } = req.body ?? {}
  if (username !== authUser.username || password !== authUser.password) {
    res.status(401).json(fail('INVALID_CREDENTIALS', '账号或密码错误。'))
    return
  }
  res.json(
    ok({
      token: authToken,
      user: {
        username: authUser.username,
      },
    }),
  )
})

app.get('/api/auth/me', (req, res) => {
  const token = getBearerToken(req)
  if (token !== authToken) {
    res.status(401).json(fail('UNAUTHORIZED', '登录态无效，请重新登录。'))
    return
  }
  res.json(
    ok({
      user: {
        username: authUser.username,
      },
    }),
  )
})

app.post('/api/auth/logout', (_req, res) => {
  res.json(ok({ success: true }))
})

app.get('/api/site', async (_req, res) => {
  try {
    const site = await readSiteInfo()
    res.json(ok(site))
  } catch (error) {
    res.status(500).json(fail('SITE_READ_FAILED', 'Failed to read site information.', error))
  }
})

app.get('/api/tags', async (_req, res) => {
  try {
    const posts = normalizePosts(await readPosts())
    const tags = Array.from(new Set(posts.flatMap((post) => post.tags))).sort((a, b) => a.localeCompare(b))
    res.json(ok(tags))
  } catch (error) {
    res.status(500).json(fail('TAGS_READ_FAILED', 'Failed to load tags.', error))
  }
})

app.get('/api/posts', async (req, res) => {
  try {
    const page = toPositiveInt(req.query.page, 1)
    const pageSize = toPositiveInt(req.query.pageSize, 6)
    const tag = req.query.tag?.trim()
    const search = req.query.search?.trim().toLowerCase()
    const includeDraft = req.query.includeDraft === 'true'

    const posts = normalizePosts(await readPosts())
    let filtered = includeDraft ? posts : posts.filter((post) => post.published)

    if (tag) {
      filtered = filtered.filter((post) => post.tags.includes(tag))
    }

    if (search) {
      filtered = filtered.filter((post) => {
        return (
          post.title.toLowerCase().includes(search) ||
          post.excerpt.toLowerCase().includes(search) ||
          post.content.toLowerCase().includes(search)
        )
      })
    }

    const sorted = [...filtered].sort((a, b) => (a.date < b.date ? 1 : -1))
    const total = sorted.length
    const start = (page - 1) * pageSize
    const items = sorted.slice(start, start + pageSize)

    res.json(
      ok(
        { items },
        {
          pagination: {
            page,
            pageSize,
            total,
            totalPages: Math.max(1, Math.ceil(total / pageSize)),
          },
        },
      ),
    )
  } catch (error) {
    res.status(500).json(fail('POSTS_READ_FAILED', 'Failed to load posts.', error))
  }
})

app.get('/api/posts/:idOrSlug', async (req, res) => {
  try {
    const idOrSlug = req.params.idOrSlug
    const posts = normalizePosts(await readPosts())
    const byId = Number(idOrSlug)
    const post = Number.isInteger(byId)
      ? posts.find((item) => item.id === byId)
      : posts.find((item) => item.slug === idOrSlug)

    if (!post) {
      res.status(404).json(fail('POST_NOT_FOUND', `Post "${idOrSlug}" was not found.`))
      return
    }

    res.json(ok(post))
  } catch (error) {
    res.status(500).json(fail('POST_READ_FAILED', 'Failed to load post detail.', error))
  }
})

app.post('/api/posts/:idOrSlug/like', async (req, res) => {
  try {
    const idOrSlug = req.params.idOrSlug
    const byId = Number(idOrSlug)
    const posts = normalizePosts(await readPosts())
    const postIndex = Number.isInteger(byId)
      ? posts.findIndex((item) => item.id === byId)
      : posts.findIndex((item) => item.slug === idOrSlug)

    if (postIndex < 0) {
      res.status(404).json(fail('POST_NOT_FOUND', `Post "${idOrSlug}" was not found.`))
      return
    }

    const updatedPost = {
      ...posts[postIndex],
      likes: posts[postIndex].likes + 1,
    }
    const nextPosts = [...posts]
    nextPosts[postIndex] = updatedPost
    await writePosts(nextPosts)

    res.json(ok(updatedPost))
  } catch (error) {
    res.status(500).json(fail('POST_LIKE_FAILED', 'Failed to like post.', error))
  }
})

app.post('/api/posts', async (req, res) => {
  try {
    if (!requireAuth(req, res)) {
      return
    }
    const { title, excerpt, content, contentHtml, tags } = req.body
    const normalizedHtml = sanitizeHtml(contentHtml)
    const normalizedText = typeof content === 'string' ? content.trim() : ''
    if (!title || !excerpt || (!normalizedText && !normalizedHtml)) {
      res
        .status(400)
        .json(fail('INVALID_POST_PAYLOAD', 'title, excerpt and one of content/contentHtml are required.'))
      return
    }

    const posts = normalizePosts(await readPosts())
    const newId = posts.reduce((max, item) => Math.max(max, item.id), 0) + 1
    const slugBase = toSlug(title) || `post-${newId}`
    let slug = slugBase
    let suffix = 1
    while (posts.some((post) => post.slug === slug)) {
      slug = `${slugBase}-${suffix}`
      suffix += 1
    }

    const newPost = {
      id: newId,
      slug,
      title,
      excerpt,
      content: normalizedText || normalizedHtml.replace(/<[^>]+>/g, '').trim(),
      contentHtml: normalizedHtml || undefined,
      date: new Date().toISOString().slice(0, 10),
      tags: Array.isArray(tags) ? tags.map((tag) => String(tag).trim()).filter(Boolean) : [],
      published: true,
      likes: 0,
    }

    const nextPosts = [newPost, ...posts]
    await writePosts(nextPosts)

    res.status(201).json(ok(newPost))
  } catch (error) {
    res.status(500).json(fail('POST_CREATE_FAILED', 'Failed to publish post.', error))
  }
})

app.use((_req, res) => {
  res.status(404).json(fail('ROUTE_NOT_FOUND', 'The requested API route does not exist.'))
})

app.listen(port, () => {
  console.log(`Backend API ready at http://localhost:${port}`)
})

