import request from './request'

export interface FamilyInfo {
  familyId: number
  familyName: string
  username: string
  avatar?: string
  remark?: string
}

/** 注册新家庭(新增家庭账号) */
export interface RegisterParams {
  familyName: string
  username: string
  password: string
  /** 户主姓名(可选, 默认取家庭名称) */
  ownerName?: string
}

export const registerFamily = (data: RegisterParams) => {
  return request.post('/auth/register', data)
}

export const getFamilyInfo = () => {
  return request.get('/family/me')
}

export const updateFamilyInfo = (data: { familyName?: string; avatar?: string; remark?: string }) => {
  return request.put('/family/me', data)
}

export const changePassword = (data: { oldPassword: string; newPassword: string }) => {
  return request.put('/family/password', data)
}
