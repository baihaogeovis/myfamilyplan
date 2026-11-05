/**
 * 仪表盘、月报、计划列表这类汇总请求的短缓存。
 * 同一键在途只发一次；写入成功或回到前台时整表丢掉，避免看到旧数字。
 */

const SUMMARY_PREFIXES = [
  '/dashboard',
  '/statistics',
  '/fixed-expense/overview',
  '/life-plan',
  '/growth-plan',
  '/approval',
  '/notification'
]

export const SUMMARY_INVALIDATED_EVENT = 'familyplan-summary-invalidated'

interface CacheEntry {
  at: number
  value: unknown
}

export class SummaryMemory {
  private entries = new Map<string, CacheEntry>()
  private inflight = new Map<string, Promise<unknown>>()
  private generation = 0
  private ttlMs: number
  private now: () => number

  constructor(ttlMs: number, now: () => number = Date.now) {
    this.ttlMs = ttlMs
    this.now = now
  }

  get<T>(key: string, loader: () => Promise<T>): Promise<T> {
    const hit = this.entries.get(key)
    if (hit && this.now() - hit.at < this.ttlMs) {
      return Promise.resolve(hit.value as T)
    }
    const pending = this.inflight.get(key)
    if (pending) {
      return pending as Promise<T>
    }
    const generation = this.generation
    const promise = loader()
      .then((value) => {
        if (generation === this.generation) {
          this.entries.set(key, { at: this.now(), value })
        }
        this.inflight.delete(key)
        return value
      })
      .catch((error) => {
        this.inflight.delete(key)
        throw error
      })
    this.inflight.set(key, promise)
    return promise
  }

  clear() {
    this.generation += 1
    this.entries.clear()
    this.inflight.clear()
  }
}

export const summaryMemory = new SummaryMemory(20_000)

export function clearSummaryCache() {
  summaryMemory.clear()
}

export function isCachedSummaryGet(url: string): boolean {
  const path = normalizePath(url)
  return SUMMARY_PREFIXES.some((prefix) => path === prefix || path.startsWith(`${prefix}/`) || path.startsWith(`${prefix}?`))
}

export function summaryCacheKey(input: {
  token?: string | null
  memberId?: string | null
  url: string
  params?: unknown
}): string {
  return [
    input.token || '',
    input.memberId || '',
    normalizePath(input.url),
    stableStringify(input.params)
  ].join('|')
}

export function onSummaryInvalidated(handler: () => void): () => void {
  const listener = () => handler()
  window.addEventListener(SUMMARY_INVALIDATED_EVENT, listener)
  return () => window.removeEventListener(SUMMARY_INVALIDATED_EVENT, listener)
}

let refreshInstalled = false

export function installSummaryRefresh() {
  if (refreshInstalled || typeof window === 'undefined' || typeof document === 'undefined') {
    return
  }
  refreshInstalled = true
  document.addEventListener('visibilitychange', () => {
    if (document.visibilityState !== 'visible') return
    clearSummaryCache()
    window.dispatchEvent(new Event(SUMMARY_INVALIDATED_EVENT))
  })
}

function normalizePath(url: string): string {
  const path = String(url || '').split('?')[0]
  if (!path) return '/'
  return path.startsWith('/') ? path : `/${path}`
}

function stableStringify(value: unknown): string {
  if (value == null) return ''
  if (typeof value !== 'object') return String(value)
  if (Array.isArray(value)) {
    return `[${value.map((item) => stableStringify(item)).join(',')}]`
  }
  const record = value as Record<string, unknown>
  return `{${Object.keys(record)
    .sort()
    .map((key) => `${key}:${stableStringify(record[key])}`)
    .join(',')}}`
}
