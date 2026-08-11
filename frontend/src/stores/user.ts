import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * 兼容对象：供顶部栏等展示家庭名称/头像
 */
export interface UserInfo {
  userId?: number
  username: string
  nickname: string
  avatar: string
  role: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('token') || '')
  const familyId = ref<number | null>(Number(localStorage.getItem('familyId')) || null)
  const familyName = ref<string>(localStorage.getItem('familyName') || '')
  const username = ref<string>(localStorage.getItem('username') || '')
  const avatar = ref<string>(localStorage.getItem('avatar') || '')
  // 兼容旧页面：将家庭信息映射为用户信息展示
  const userInfo = ref<UserInfo | null>({
    username: username.value,
    nickname: familyName.value || '我的家庭',
    avatar: avatar.value,
    role: 'ADMIN'
  })

  const setToken = (newToken: string) => {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  const setLoginInfo = (data: {
    familyId: number
    familyName: string
    username: string
    avatar?: string
    currentMemberId?: number
  }) => {
    familyId.value = data.familyId
    familyName.value = data.familyName
    username.value = data.username
    avatar.value = data.avatar || ''
    localStorage.setItem('familyId', String(data.familyId))
    localStorage.setItem('familyName', data.familyName)
    localStorage.setItem('username', data.username)
    localStorage.setItem('avatar', data.avatar || '')
    userInfo.value = {
      username: data.username,
      nickname: data.familyName,
      avatar: data.avatar || '',
      role: 'ADMIN'
    }
  }

  const logout = () => {
    token.value = ''
    familyId.value = null
    familyName.value = ''
    username.value = ''
    avatar.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('userInfo')
    localStorage.removeItem('familyId')
    localStorage.removeItem('familyName')
    localStorage.removeItem('username')
    localStorage.removeItem('avatar')
    localStorage.removeItem('currentMemberId')
  }

  return {
    token,
    familyId,
    familyName,
    username,
    avatar,
    userInfo,
    setToken,
    setLoginInfo,
    logout
  }
})
