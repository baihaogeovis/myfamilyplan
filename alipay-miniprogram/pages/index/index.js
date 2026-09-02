const api = require('../../utils/api')
const util = require('../../utils/util')

const menus = [
  { icon: '收', color: '#f56c6c', name: '收入', path: '/pages/record/record?type=income' },
  { icon: '支', color: '#e6a23c', name: '支出', path: '/pages/record/record?type=expense' },
  { icon: '预', color: '#67c23a', name: '预算', path: '/pages/budget/budget' },
  { icon: '审', color: '#409eff', name: '审批', path: '/pages/approval/approval' },
  { icon: '成', color: '#7c4dff', name: '成员', path: '/pages/members/members' },
  { icon: '工', color: '#2f9e91', name: '工作台', path: '/pages/workbench/workbench' },
  { icon: '资', color: '#5b8ff9', name: '资金', path: '/pages/fixedExpense/fixedExpense' },
  { icon: '生', color: '#9254de', name: '生活', path: '/pages/lifePlan/lifePlan' },
  { icon: '长', color: '#f759ab', name: '成长', path: '/pages/growthPlan/growthPlan' },
  { icon: '奖', color: '#ff9f43', name: '奖励', path: '/pages/reward/reward' },
  { icon: '通', color: '#13c2c2', name: '通知', path: '/pages/notifications/notifications' }
]

Page({
  data: {
    menus,
    overview: {
      monthIncome: 0,
      monthExpense: 0,
      budgetRemaining: 0,
      pendingCount: 0,
      memberCount: 0
    },
    recent: [],
    currentMemberName: ''
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    this.setData({
      currentMemberName: getApp().globalData.currentMemberName || ''
    })
    this.loadData()
  },

  async loadData() {
    try {
      const [overview, recent] = await Promise.all([
        api.dashboard.overview(),
        api.dashboard.recent(5)
      ])
      const recentList = (recent || []).map((item) => ({
        ...item,
        amountText: `${item.type === 'EXPENSE' ? '-' : '+'}${util.formatMoney(item.amount)}`,
        dateText: util.formatDate(item.date)
      }))
      this.setData({ overview, recent: recentList })
    } catch (e) { /* 错误已提示 */ }
  },

  // tabBar 页面(switchTab 跳转, 不支持 query, 参数经 globalData 传递)
  TAB_PATHS: ['/pages/record/record', '/pages/workbench/workbench', '/pages/statistics/statistics', '/pages/profile/profile'],

  goMenu(e) {
    const path = e.currentTarget.dataset.path
    const app = getApp()
    if (!app.globalData.currentMemberId) {
      my.showModal({
        title: '提示',
        content: '请先在「我的」页面选择当前家庭成员',
        confirmText: '去选择',
        success: (res) => {
          if (res.confirm) my.switchTab({ url: '/pages/profile/profile' })
        }
      })
      return
    }
    const base = path.split('?')[0]
    if (this.TAB_PATHS.includes(base)) {
      const query = path.split('?')[1]
      if (query) {
        const params = {}
        query.split('&').forEach((kv) => {
          const idx = kv.indexOf('=')
          params[kv.slice(0, idx)] = kv.slice(idx + 1)
        })
        if (params.type) app.globalData.recordType = params.type
      }
      my.switchTab({ url: base })
      return
    }
    my.navigateTo({ url: path })
  },

  goRecord(e) {
    const type = e.currentTarget.dataset.type
    const app = getApp()
    if (!app.globalData.currentMemberId) {
      my.showToast({ type: 'none', content: '请先在「我的」选择成员' })
      return
    }
    app.globalData.recordType = type
    my.switchTab({ url: '/pages/record/record' })
  },

  categoryLabel(v) { return util.categoryLabel(v) }
})
