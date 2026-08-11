import request from './request'

export interface ExpenseRecord {
  id?: number
  memberId: number
  amount: number
  category: string
  type?: string
  expenseDate: string
  remark?: string
}

export function getExpensePage(params: {
  memberId: number
  page?: number
  size?: number
  category?: string
  type?: string
  startDate?: string
  endDate?: string
}) {
  return request.get('/expense/page', { params })
}

export function addExpense(data: ExpenseRecord) {
  return request.post('/expense', data)
}

export function updateExpense(data: ExpenseRecord) {
  return request.put('/expense', data)
}

export function deleteExpense(id: number) {
  return request.delete(`/expense/${id}`)
}
