# API Contract

Base URL: `http://localhost:3000`

## Response Shape

Success:

```json
{
  "data": {},
  "meta": {},
  "error": null
}
```

Failure:

```json
{
  "data": null,
  "meta": {},
  "error": {
    "code": "SOME_ERROR",
    "message": "Readable message",
    "details": {}
  }
}
```

## Endpoints

### GET /health

Quick server health check.

### GET /api/health

Health check endpoint under API prefix for proxy-friendly checks.

### GET /api/site

Returns blog site metadata.

### GET /api/tags

Returns all tags extracted from post data.

### GET /api/posts

Query:
- `page` (default `1`)
- `pageSize` (default `6`)
- `tag` (optional)
- `search` (optional)
- `includeDraft` (optional, `true` includes unpublished)

### GET /api/posts/:idOrSlug

- Example: `/api/posts/cyber-style-homepage`
- Example: `/api/posts/2`

### POST /api/posts/:idOrSlug/like

- Example: `/api/posts/cyber-style-homepage/like`
- Example: `/api/posts/2/like`
- No login required
- Each successful request increments `likes` by 1

### POST /api/posts

Body:

```json
{
  "title": "My first post",
  "excerpt": "A short summary",
  "content": "Long article content",
  "contentHtml": "<h2>Heading</h2><p>Rich text body</p>",
  "tags": ["Vue", "Diary"]
}
```

Requires header:

```txt
Authorization: Bearer goodeat-session-token
```

### POST /api/auth/login

Body:

```json
{
  "username": "goodeat",
  "password": "goodeat123456"
}
```

### GET /api/auth/me

Read current login state with bearer token.

### POST /api/auth/logout

Frontend can call it before local token cleanup.

