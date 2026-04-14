import { mkdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'

const dataDir = path.resolve(process.cwd(), 'data')
const postsPath = path.join(dataDir, 'posts.json')
const sitePath = path.join(dataDir, 'site.json')

async function ensureDataDir() {
  await mkdir(dataDir, { recursive: true })
}

export async function readPosts() {
  await ensureDataDir()
  const raw = await readFile(postsPath, 'utf8')
  return JSON.parse(raw)
}

export async function writePosts(posts) {
  await ensureDataDir()
  const json = JSON.stringify(posts, null, 2)
  await writeFile(postsPath, json, 'utf8')
}

export async function readSiteInfo() {
  await ensureDataDir()
  const raw = await readFile(sitePath, 'utf8')
  return JSON.parse(raw)
}

