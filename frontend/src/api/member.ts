import request from './request'

export interface FamilyMember {
  id?: number
  userId: number
  memberName: string
  relation?: string
  gender?: string
  birthday?: string
  phone?: string
  idCard?: string
  role?: string
  avatar?: string
  remark?: string
}

export function getMemberList(userId: number) {
  return request.get('/member/list', { params: { userId } })
}

export function addMember(data: FamilyMember) {
  return request.post('/member', data)
}

export function updateMember(data: FamilyMember) {
  return request.put('/member', data)
}

export function deleteMember(id: number) {
  return request.delete(`/member/${id}`)
}
