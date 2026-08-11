import request from './request'

export interface ApprovalRecord {
  id?: number
  applicantId: number
  approverId?: number
  amount: number
  purpose: string
  status?: string
  createTime?: string
  approveTime?: string
  remark?: string
}

export function getApprovalPage(params: {
  applicantId?: number
  approverId?: number
  status?: string
  page?: number
  size?: number
}) {
  return request.get('/approval/page', { params })
}

export function applyApproval(data: ApprovalRecord) {
  return request.post('/approval', data)
}

export function approveApproval(id: number, approverId: number, remark?: string) {
  return request.post(`/approval/${id}/approve`, null, { params: { approverId, remark } })
}

export function rejectApproval(id: number, approverId: number, remark?: string) {
  return request.post(`/approval/${id}/reject`, null, { params: { approverId, remark } })
}

export function getPendingCount(approverId?: number) {
  return request.get('/approval/pending/count', { params: { approverId } })
}

export function deleteApproval(id: number) {
  return request.delete(`/approval/${id}`)
}
