/**
 * 流式 JSON 解析状态机：通过 fetch 接收外部 SSE，使用 partial-json 思路实时提取 content 与 branches。
 * 供 useStoryEngine 等调用，不包含 UI。
 */

export interface StreamParserCallbacks {
  onContentUpdate: (chunk: string) => void
  onBranchesReady: (branches: string[]) => void
  onDone?: () => void
  onError?: (err: Error) => void
}

const DATA_PREFIX = 'data: '
const DONE_MARKER = '[DONE]'

/**
 * 从可能残缺的 JSON 字符串中提取 "content" 字段的当前值（支持流式截断）。
 */
function extractContentFromPartialJson(raw: string): string | null {
  const contentKey = '"content"'
  const idx = raw.indexOf(contentKey)
  if (idx === -1) return null
  const afterKey = raw.slice(idx + contentKey.length)
  const colon = afterKey.indexOf(':')
  if (colon === -1) return null
  const rest = afterKey.slice(colon + 1).replace(/^\s+/, '')
  if (!rest.startsWith('"')) return null
  let i = 1
  let value = ''
  while (i < rest.length) {
    const c = rest[i]
    if (c === '\\') {
      if (i + 1 < rest.length) {
        value += rest[i + 1] === '"' ? '"' : rest[i + 1]
        i += 2
        continue
      }
      break
    }
    if (c === '"') break
    value += c
    i++
  }
  return value
}

/**
 * 从可能残缺的 JSON 中尝试解析 "branches" 数组。
 */
function extractBranchesFromPartialJson(raw: string): string[] | null {
  try {
    const parsed = JSON.parse(raw)
    if (parsed && Array.isArray(parsed.branches)) return parsed.branches
  } catch {
    // partial, ignore
  }
  return null
}

/**
 * 解析单条 data 负载：优先完整 JSON，否则用 partial 提取 content。
 */
function parseDataPayload(
  payload: string,
  callbacks: StreamParserCallbacks
): void {
  const trimmed = payload.trim()
  if (trimmed === DONE_MARKER) {
    callbacks.onDone?.()
    return
  }
  try {
    const obj = JSON.parse(trimmed)
    if (obj && typeof obj.content === 'string') {
      callbacks.onContentUpdate(obj.content)
      return
    }
    if (obj && Array.isArray(obj.branches)) {
      callbacks.onBranchesReady(obj.branches)
      return
    }
  } catch {
    // Partial JSON: try to extract content for typing effect
    const content = extractContentFromPartialJson(trimmed)
    if (content !== null) callbacks.onContentUpdate(content)
    const branches = extractBranchesFromPartialJson(trimmed)
    if (branches !== null) callbacks.onBranchesReady(branches)
  }
}

/**
 * 使用 fetch 请求 SSE 流，按行解析并回调 onContentUpdate / onBranchesReady。
 */
export async function streamSSE(
  url: string,
  options: RequestInit,
  callbacks: StreamParserCallbacks
): Promise<void> {
  const res = await fetch(url, {
    ...options,
    headers: {
      'Accept': 'text/event-stream',
      'Content-Type': 'application/json',
      ...options.headers,
    },
  })
  if (!res.ok) {
    const err = new Error(`SSE request failed: ${res.status}`)
    callbacks.onError?.(err)
    throw err
  }
  const reader = res.body?.getReader()
  if (!reader) {
    const err = new Error('No response body')
    callbacks.onError?.(err)
    throw err
  }
  const dec = new TextDecoder()
  let buffer = ''
  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break
      buffer += dec.decode(value, { stream: true })
      const lines = buffer.split(/\n/)
      buffer = lines.pop() ?? ''
      for (const line of lines) {
        if (line.startsWith(DATA_PREFIX)) {
          const payload = line.slice(DATA_PREFIX.length)
          parseDataPayload(payload, callbacks)
        }
      }
    }
    if (buffer.trim().length > 0 && buffer.startsWith(DATA_PREFIX)) {
      parseDataPayload(buffer.slice(DATA_PREFIX.length), callbacks)
    }
    callbacks.onDone?.()
  } catch (e) {
    callbacks.onError?.(e instanceof Error ? e : new Error(String(e)))
    throw e
  }
}
