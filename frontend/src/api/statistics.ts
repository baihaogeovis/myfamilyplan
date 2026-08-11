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

export function getCategoryStat(month?: string) {
  return request.get('/statistics/category', { params: { month } })
}

export function getMonthlyStat(year?: number) {
  return request.get('/statistics/monthly', { params: { year } })
}
