/**
 * 家庭成长系统 - 支付宝小程序入口
 */
const authApi = require('./utils/api').auth

App({
  globalData: {
    token: '',
    familyInfo: null,
    currentMemberId: null,
    currentMemberName: ''
  },

  onLaunch() {
    const token = my.getStorageSync({ key: 'token' }).data
    if (token) {
      this.globalData.token = token
    }
    const familyInfo = my.getStorageSync({ key: 'familyInfo' }).data
    if (familyInfo) {
      this.globalData.familyInfo = familyInfo
    }
    const currentMemberId = my.getStorageSync({ key: 'currentMemberId' }).data
    const currentMemberName = my.getStorageSync({ key: 'currentMemberName' }).data
    if (currentMemberId) {
      this.globalData.currentMemberId = currentMemberId
      this.globalData.currentMemberName = currentMemberName || ''
    }
  },

  /**
   * 保存登录态(登录/注册成功后调用)
   */
  setLoginState(data) {
    this.globalData.token = data.token
    this.globalData.familyInfo = {
      familyId: data.familyId,
      familyName: data.familyName,
      username: data.username,
      avatar: data.avatar || ''
    }
    if (data.currentMemberId) {
      this.setCurrentMember(data.currentMemberId, data.currentMemberName || '')
    }
    my.setStorageSync({ key: 'token', data: data.token })
    my.setStorageSync({ key: 'familyInfo', data: this.globalData.familyInfo })
  },

  /**
   * 设置当前操作成员
   */
  setCurrentMember(memberId, memberName) {
    this.globalData.currentMemberId = memberId
    this.globalData.currentMemberName = memberName || ''
    my.setStorageSync({ key: 'currentMemberId', data: memberId })
    my.setStorageSync({ key: 'currentMemberName', data: this.globalData.currentMemberName })
  },

  /**
   * 退出登录
   */
  logout() {
    this.globalData.token = ''
    this.globalData.familyInfo = null
    this.globalData.currentMemberId = null
    this.globalData.currentMemberName = ''
    my.removeStorageSync({ key: 'token' })
    my.removeStorageSync({ key: 'familyInfo' })
    my.removeStorageSync({ key: 'currentMemberId' })
    my.removeStorageSync({ key: 'currentMemberName' })
    authApi.logout().catch(() => {})
    my.reLaunch({ url: '/pages/login/login' })
  }
})
