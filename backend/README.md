# Backend API

File-based backend service for the blog frontend.

## Quick Start

1. Install dependencies:

```sh
npm install
```

2. Copy env template and run:

```sh
cp env.example .env
npm run dev
```

Server default: `http://localhost:3000`

## Main Endpoints

- `GET /health`
- `GET /api/health`
- `GET /api/site`
- `GET /api/tags`
- `GET /api/posts?page=1&pageSize=6`
- `GET /api/posts/:idOrSlug`
- `POST /api/posts/:idOrSlug/like`
- `POST /api/posts`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/logout`

Full API examples: see `docs/api.md`.

