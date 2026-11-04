import assert from 'node:assert/strict'
import { test } from 'node:test'
import { SummaryMemory, isCachedSummaryGet, summaryCacheKey } from './summaryCache.ts'

test('summary gets cover dashboard statistics and plans but not ordinary lists', () => {
  assert.equal(isCachedSummaryGet('/dashboard/overview'), true)
  assert.equal(isCachedSummaryGet('statistics/monthly'), true)
  assert.equal(isCachedSummaryGet('/fixed-expense/overview'), true)
  assert.equal(isCachedSummaryGet('/life-plan/list'), true)
  assert.equal(isCachedSummaryGet('/approval/page'), true)
  assert.equal(isCachedSummaryGet('/notification/unread'), true)
  assert.equal(isCachedSummaryGet('/fixed-expense/list'), false)
  assert.equal(isCachedSummaryGet('/income/page'), false)
  assert.equal(isCachedSummaryGet('/export/income'), false)
})

test('cache key changes with token member and params', () => {
  const base = {
    token: 'a',
    memberId: '3',
    url: '/statistics/category',
    params: { month: '2026-11', year: 2026 }
  }
  assert.equal(summaryCacheKey(base), summaryCacheKey({ ...base, params: { year: 2026, month: '2026-11' } }))
  assert.notEqual(summaryCacheKey(base), summaryCacheKey({ ...base, memberId: '4' }))
  assert.notEqual(summaryCacheKey(base), summaryCacheKey({ ...base, token: 'b' }))
  assert.notEqual(summaryCacheKey(base), summaryCacheKey({ ...base, params: { month: '2026-12' } }))
})

test('reuses a fresh value and shares one in-flight load', async () => {
  let now = 1_000
  const cache = new SummaryMemory(20, () => now)
  let loads = 0
  const first = await cache.get('overview', async () => {
    loads += 1
    return 11
  })
  const second = await cache.get('overview', async () => {
    loads += 1
    return 22
  })
  assert.equal(first, 11)
  assert.equal(second, 11)
  assert.equal(loads, 1)

  let release: (value: number) => void = () => {}
  const pendingA = cache.get('trend', () => {
    loads += 1
    return new Promise<number>((resolve) => {
      release = resolve
    })
  })
  const pendingB = cache.get('trend', async () => {
    loads += 1
    return 99
  })
  release(7)
  assert.equal(await pendingA, 7)
  assert.equal(await pendingB, 7)
  assert.equal(loads, 2)

  now = 1_020
  const third = await cache.get('overview', async () => {
    loads += 1
    return 33
  })
  assert.equal(third, 33)
  assert.equal(loads, 3)
})

test('failures and cleared in-flight loads are not stored', async () => {
  const cache = new SummaryMemory(1_000, () => 0)
  await assert.rejects(() => cache.get('bad', async () => {
    throw new Error('down')
  }))
  assert.equal(await cache.get('bad', async () => 'ok'), 'ok')

  let release: (value: string) => void = () => {}
  const stale = cache.get('race', () => new Promise<string>((resolve) => {
    release = resolve
  }))
  cache.clear()
  release('stale')
  assert.equal(await stale, 'stale')
  let loads = 0
  assert.equal(await cache.get('race', async () => {
    loads += 1
    return 'fresh'
  }), 'fresh')
  assert.equal(loads, 1)
})
