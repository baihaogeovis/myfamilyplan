const api = require('../../utils/api')

Page({
  data: {
    family: null,
    members: [],
    currentMemberId: null,
    currentMemberName: '',
    showPwd: false,
    pwdForm: { oldPassword: '', newPassword: '' }
  },

  onShow() {
    this.loadData()
  },

  async loadData() {
    const app = getApp()
    this.setData({
      currentMemberId: app.globalData.currentMemberId,
      currentMemberName: app.globalData.currentMemberName || ''
    })
    try {
      const [family, members] = await Promise.all([
        api.family.me().catch(() => null),
        api.member.list().catch(() => [])
      ])
      this.setData({ family, members: members || [] })
    } catch (e) { /* 错误已提示 */ }
  },

  chooseMember(e) {
    const id = e.currentTarget.dataset.id
    const name = e.currentTarget.dataset.name
    const app = getApp()
    app.setCurrentMember(id, name)
    this.setData({ currentMemberId: id, currentMemberName: name })
    my.showToast({ type: 'success', content: `已切换为 ${name}` })
    this.loadData()
  },

  goMembers() {
    my.reLaunch({ url: '/pages/members/members' })
  },

  goNotes() {
    my.navigateTo({ url: '/pages/notifications/notifications' })
  },

  openPwd() {
    this.setData({ showPwd: true, pwdForm: { oldPassword: '', newPassword: '' } })
  },

  closePwd() { this.setData({ showPwd: false }) },
  noop() {},
  onOldPwd(e) { this.setData({ 'pwdForm.oldPassword': e.detail.value }) },
  onNewPwd(e) { this.setData({ 'pwdForm.newPassword': e.detail.value }) },

  async submitPwd() {
    const { oldPassword, newPassword } = this.data.pwdForm
    if (!oldPassword || !newPassword) {
      my.showToast({ type: 'none', content: '请输入新旧密码' })
      return
    }
    try {
      await api.family.changePassword({ oldPassword, newPassword })
      this.setData({ showPwd: false })
      my.showToast({ type: 'success', content: '密码修改成功' })
    } catch (e) { /* 错误已提示 */ }
  },

  logout() {
    my.confirm({
      title: '退出登录',
      content: '确定退出当前账号吗？',
      success: (res) => {
        if (res.confirm) getApp().logout()
      }
    })
  }
})
