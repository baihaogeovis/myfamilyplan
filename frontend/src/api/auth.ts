import request from '@/api/request'

export interface LoginParams {
  username: string
  password: string
  /** 当前操作成员ID(可选: 指定后后端校验该成员属于该家庭) */
  memberId?: number
}

export const login = (data: LoginParams) => {
  return request.post('/auth/login', data)
}

export const getUserInfo = () => {
  return request.get('/auth/info')
}

export const logout = () => {
  return request.post('/auth/logout')
}
