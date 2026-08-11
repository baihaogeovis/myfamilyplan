import request from '@/api/request'

export const login = (data: { username: string; password: string }) => {
  return request.post('/auth/login', data)
}

export const getUserInfo = () => {
  return request.get('/auth/info')
}

export const logout = () => {
  return request.post('/auth/logout')
}
