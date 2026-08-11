import request from './request'

export interface FixedExpense {
  id?: number
  memberId: number
  expenseName: string
  category: string
  amount: number
  dueDay?: number
  active?: number
  autoRecord?: number
  remark?: string
}

export interface FinanceOverview {
  totalIncome: number
  totalExpense: number
  totalBalance: number
  monthIncome: number
  monthExpense: number
  monthBalance: number
  monthFixedTotal: number
  fixedExpenses: FixedExpense[]
}

export function getFixedExpenseList(memberId: number) {
  return request.get('/fixed-expense/list', { params: { memberId } })
}

export function getFinanceOverview(memberId: number, year: number, month: number) {
  return request.get('/fixed-expense/overview', { params: { memberId, year, month } })
}

export function addFixedExpense(data: FixedExpense) {
  return request.post('/fixed-expense', data)
}

export function updateFixedExpense(data: FixedExpense) {
  return request.put('/fixed-expense', data)
}

export function deleteFixedExpense(id: number) {
  return request.delete(`/fixed-expense/${id}`)
}
