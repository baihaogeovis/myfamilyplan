const api = require('../../utils/api')

Page({
  data: {
    mode: 'login', // login | register
    username: '',
    password: '',
    familyName: '',
    ownerName: ''
  },

  onLoad() {
    const token = my.getStorageSync({ key: 'token' }).data
    if (token) {
      my.reLaunch({ url: '/pages/index/index' })
    }
  },

  switchMode() {
    this.setData({ mode: this.data.mode === 'login' ? 'register' : 'login' })
  },

  onUsername(e) {
    this.setData({ username: e.detail.value })
  },
  onPassword(e) {
    this.setData({ password: e.detail.value })
  },
  onFamilyName(e) {
    this.setData({ familyName: e.detail.value })
  },
  onOwnerName(e) {
    this.setData({ ownerName: e.detail.value })
  },

  async submit() {
    const { mode, username, password, familyName, ownerName } = this.data
    if (!username.trim() || !password) {
      my.showToast({ type: 'none', content: '请输入账号和密码' })
      return
    }
    if (mode === 'register') {
      if (!familyName.trim()) {
        my.showToast({ type: 'none', content: '请输入家庭名称' })
        return
      }
      try {
        const data = await api.auth.register({ familyName, username, password, ownerName })
        getApp().setLoginState(data)
        this.enterHome()
      } catch (e) { /* 错误已由 request 层提示 */ }
      return
    }
    try {
      const data = await api.auth.login({ username, password })
      getApp().setLoginState(data)
      this.enterHome()
    } catch (e) { /* 错误已由 request 层提示 */ }
  },

  enterHome() {
    my.showToast({ type: 'success', content: '登录成功' })
    setTimeout(() => {
      my.reLaunch({ url: '/pages/index/index' })
    }, 600)
  }
})
