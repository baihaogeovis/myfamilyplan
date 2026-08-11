import request from './request'

export interface LifePlan {
  id?: number
  memberId: number
  planType: string
  planName: string
  category?: string
  budget?: number
  planDate?: string
  status?: string
  priority?: string
  remark?: string
}

export function getLifePlanList(memberId: number, planType?: string, status?: string) {
  return request.get('/life-plan/list', { params: { memberId, planType, status } })
}

export function addLifePlan(data: LifePlan) {
  return request.post('/life-plan', data)
}

export function updateLifePlan(data: LifePlan) {
  return request.put('/life-plan', data)
}

export function changeLifePlanStatus(id: number, status: string) {
  return request.put(`/life-plan/status/${id}`, null, { params: { status } })
}

export function deleteLifePlan(id: number) {
  return request.delete(`/life-plan/${id}`)
}
