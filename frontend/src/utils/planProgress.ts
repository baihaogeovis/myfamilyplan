/**
 * 工作台「计划进度」：生活计划和成长计划合成一个百分比，并数还在进行的个数。
 * 已取消的不计入。已完成按 100。生活计划进行中按 50，其余未完成按 0。
 * 成长计划用自己的进度。
 */

export interface LifePlanScore {
  status?: string | null
}

export interface GrowthPlanScore {
  status?: string | null
  progress?: number | null
}

export interface PlanSummary {
  progress: number
  openCount: number
}

export function summarizePlans(life: LifePlanScore[] = [], growth: GrowthPlanScore[] = []): PlanSummary {
  const scored: number[] = []
  let openCount = 0
  life.forEach((plan) => {
    if (!plan || plan.status === 'CANCELLED') return
    scored.push(plan.status === 'COMPLETED' ? 100 : plan.status === 'IN_PROGRESS' ? 50 : 0)
    if (isOpen(plan.status)) openCount += 1
  })
  growth.forEach((plan) => {
    if (!plan || plan.status === 'CANCELLED') return
    scored.push(plan.status === 'COMPLETED' ? 100 : finiteProgress(plan.progress))
    if (isOpen(plan.status)) openCount += 1
  })
  const progress = scored.length
    ? Math.round(scored.reduce((sum, value) => sum + value, 0) / scored.length)
    : 0
  return { progress, openCount }
}

function isOpen(status?: string | null) {
  return status !== 'COMPLETED' && status !== 'CANCELLED'
}

function finiteProgress(value?: number | null) {
  const raw = Number(value || 0)
  return Number.isFinite(raw) ? raw : 0
}
