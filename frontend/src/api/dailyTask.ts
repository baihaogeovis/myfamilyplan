import request from './request'

export interface DailyTask {
  id?: number
  memberId: number
  taskName: string
  taskType?: string
  taskDate: string
  completed?: number
  priority?: string
  remark?: string
}

export function getDailyTaskList(memberId: number, date: string) {
  return request.get('/daily-task/list', { params: { memberId, date } })
}

export function getDailyTaskPage(params: {
  memberId: number
  page?: number
  size?: number
  taskType?: string
  completed?: number
  startDate?: string
  endDate?: string
}) {
  return request.get('/daily-task/page', { params })
}

export function addDailyTask(data: DailyTask) {
  return request.post('/daily-task', data)
}

export function updateDailyTask(data: DailyTask) {
  return request.put('/daily-task', data)
}

export function completeDailyTask(id: number, completed: number) {
  return request.put(`/daily-task/complete/${id}`, null, { params: { completed } })
}

export function deleteDailyTask(id: number) {
  return request.delete(`/daily-task/${id}`)
}
