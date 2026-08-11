import request from './request'

export interface GrowthPlan {
  id?: number
  memberId: number
  title: string
  goalType: string
  category?: string
  description?: string
  targetDate?: string
  status?: string
  progress?: number
  remark?: string
}

export function getGrowthPlanList(memberId: number, goalType?: string, status?: string) {
  return request.get('/growth-plan/list', { params: { memberId, goalType, status } })
}

export function addGrowthPlan(data: GrowthPlan) {
  return request.post('/growth-plan', data)
}

export function updateGrowthPlan(data: GrowthPlan) {
  return request.put('/growth-plan', data)
}

export function updateGrowthProgress(id: number, progress: number) {
  return request.put(`/growth-plan/progress/${id}`, null, { params: { progress } })
}

export function changeGrowthStatus(id: number, status: string) {
  return request.put(`/growth-plan/status/${id}`, null, { params: { status } })
}

export function deleteGrowthPlan(id: number) {
  return request.delete(`/growth-plan/${id}`)
}
