import request from './request'

export interface CategoryItem {
  category: string
  amount: number
}

export interface CategoryStatResponse {
  income: CategoryItem[]
  expense: CategoryItem[]
}

export interface MonthlyStat {
  month: number
  income: number
  expense: number
}

export function getCategoryStat(userId: number, month?: string) {
  return request.get('/statistics/category', { params: { userId, month } })
}

export function getMonthlyStat(userId: number, year?: number) {
  return request.get('/statistics/monthly', { params: { userId, year } })
}
