/**
 * 家庭成长系统 - 微信小程序入口
 */
const authApi = require('./utils/api').auth

App({
  globalData: {
    // 登录态与当前操作成员(同步存储于 Storage)
    token: '',
    familyInfo: null,
    currentMemberId: null,
    currentMemberName: ''
  },

  onLaunch() {
    // 恢复登录态
    const token = wx.getStorageSync('token')
    if (token) {
      this.globalData.token = token
    }
    const familyInfo = wx.getStorageSync('familyInfo')
    if (familyInfo) {
      this.globalData.familyInfo = familyInfo
    }
    const currentMemberId = wx.getStorageSync('currentMemberId')
    const currentMemberName = wx.getStorageSync('currentMemberName')
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
    wx.setStorageSync('token', data.token)
    wx.setStorageSync('familyInfo', this.globalData.familyInfo)
  },

  /**
   * 设置当前操作成员
   */
  setCurrentMember(memberId, memberName) {
    this.globalData.currentMemberId = memberId
    this.globalData.currentMemberName = memberName || ''
    wx.setStorageSync('currentMemberId', memberId)
    wx.setStorageSync('currentMemberName', this.globalData.currentMemberName)
  },

  /**
   * 退出登录
   */
  logout() {
    this.globalData.token = ''
    this.globalData.familyInfo = null
    this.globalData.currentMemberId = null
    this.globalData.currentMemberName = ''
    wx.removeStorageSync('token')
    wx.removeStorageSync('familyInfo')
    wx.removeStorageSync('currentMemberId')
    wx.removeStorageSync('currentMemberName')
    authApi.logout().catch(() => {})
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
