import assert from 'node:assert/strict'
import { test } from 'node:test'
import { summarizePlans } from './planProgress.ts'

test('averages open life and growth plans and ignores cancelled ones', () => {
  const summary = summarizePlans(
    [
      { status: 'COMPLETED' },
      { status: 'IN_PROGRESS' },
      { status: 'PLANNED' },
      { status: 'CANCELLED' }
    ],
    [
      { status: 'IN_PROGRESS', progress: 80 },
      { status: 'COMPLETED', progress: 10 },
      { status: 'CANCELLED', progress: 90 }
    ]
  )
  // 100, 50, 0, 80, 100 → 330 / 5 = 66
  assert.deepEqual(summary, { progress: 66, openCount: 3 })
})

test('empty plans and bad progress stay at zero', () => {
  assert.deepEqual(summarizePlans(), { progress: 0, openCount: 0 })
  const summary = summarizePlans([], [{ status: 'IN_PROGRESS', progress: Number.NaN }])
  assert.deepEqual(summary, { progress: 0, openCount: 1 })
})
