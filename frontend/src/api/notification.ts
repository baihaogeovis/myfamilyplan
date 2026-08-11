import request from './request'

export interface Notification {
  id?: number
  familyId?: number
  userId?: number
  title: string
  content?: string
  type?: string
  isRead?: number
  createTime?: string
}

export function getNotificationList(params: {
  page?: number
  size?: number
  isRead?: number
}) {
  return request.get('/notification/list', { params })
}

export function addNotification(data: Notification) {
  return request.post('/notification', data)
}

export function markRead(id: number) {
  return request.put(`/notification/${id}/read`)
}

export function markAllRead() {
  return request.post('/notification/read/all')
}

export function deleteNotification(id: number) {
  return request.delete(`/notification/${id}`)
}

export function getUnreadCount() {
  return request.get('/notification/unread/count')
}
