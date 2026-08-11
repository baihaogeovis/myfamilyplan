import request from './request'

export interface BudgetPlan {
  id?: number
  memberId: number
  amount: number
  category: string
  periodType: string
  year: number
  month?: number
  remark?: string
}

export function getBudgetPage(params: {
  memberId: number
  page?: number
  size?: number
  year?: number
  month?: number
  periodType?: string
}) {
  return request.get('/budget/page', { params })
}

export function addBudget(data: BudgetPlan) {
  return request.post('/budget', data)
}

export function updateBudget(data: BudgetPlan) {
  return request.put('/budget', data)
}

export function deleteBudget(id: number) {
  return request.delete(`/budget/${id}`)
}
