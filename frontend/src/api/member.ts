import request from './request'

export interface FamilyMember {
  id?: number
  familyId?: number
  userId?: number
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

export function getMemberList() {
  return request.get('/member/list')
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

/** HTTP 403 的服务端原因。业务码 403（HTTP 200）已由拦截器提示，这里不再重复。 */
export function memberForbiddenMessage(err: unknown): string | null {
  if (!err || typeof err !== 'object' || !('response' in err)) return null
  const response = (err as { response?: { status?: number; data?: unknown } }).response
  if (!response || response.status !== 403) return null
  const data = response.data
  if (typeof data === 'string') {
    try {
      const parsed = JSON.parse(data) as { message?: string }
      return parsed.message || data || '请求失败'
    } catch {
      return data || '请求失败'
    }
  }
  if (data && typeof data === 'object') {
    const message = (data as { message?: string }).message
    if (message) return message
  }
  return '请求失败'
}
