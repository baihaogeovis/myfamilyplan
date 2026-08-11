import request from './request'

export interface DashboardOverview {
  monthIncome: number
  monthExpense: number
  budgetRemaining: number
  pendingCount: number
  memberCount: number
}

export interface TrendPoint {
  date: string
  income: number
  expense: number
}

export interface RecentTransaction {
  id: number
  type: string
  category: string
  amount: number
  date: string
  remark?: string
}

export function getDashboardOverview() {
  return request.get('/dashboard/overview')
}

export function getDashboardTrend(month?: string) {
  return request.get('/dashboard/trend', { params: { month } })
}

export function getDashboardRecent(limit = 10) {
  return request.get('/dashboard/recent', { params: { limit } })
}
