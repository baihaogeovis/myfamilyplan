import request from './request'

export interface IncomeRecord {
  id?: number
  memberId: number
  amount: number
  category: string
  source?: string
  incomeDate: string
  remark?: string
}

export function getIncomePage(params: {
  memberId: number
  page?: number
  size?: number
  category?: string
  startDate?: string
  endDate?: string
}) {
  return request.get('/income/page', { params })
}

export function addIncome(data: IncomeRecord) {
  return request.post('/income', data)
}

export function updateIncome(data: IncomeRecord) {
  return request.put('/income', data)
}

export function deleteIncome(id: number) {
  return request.delete(`/income/${id}`)
}
