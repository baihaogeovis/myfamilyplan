const api = require('../../utils/api')
const util = require('../../utils/util')

const groups = [
  {
    title: '今天',
    hint: '先安排，再记一笔',
    items: [
      { icon: '安', tone: 'sage', name: '今日安排', path: '/pages/workbench/workbench' },
      { icon: '收', tone: 'sage', name: '记收入', path: '/pages/record/record?type=income' },
      { icon: '支', tone: 'clay', name: '记支出', path: '/pages/record/record?type=expense' },
      { icon: '报', tone: 'ink', name: '月报', path: '/pages/statistics/statistics' }
    ]
  },
  {
    title: '成长',
    hint: '计划和可以兑换的奖励',
    items: [
      { icon: '长', tone: 'plum', name: '成长', path: '/pages/growthPlan/growthPlan' },
      { icon: '奖', tone: 'honey', name: '奖励', path: '/pages/reward/reward' },
      { icon: '活', tone: 'clay', name: '想做的事', path: '/pages/lifePlan/lifePlan' }
    ]
  },
  {
    title: '家里',
    hint: '账单、审批和家人',
    items: [
      { icon: '预', tone: 'sage', name: '预算', path: '/pages/budget/budget' },
      { icon: '账', tone: 'ink', name: '固定账单', path: '/pages/fixedExpense/fixedExpense' },
      { icon: '审', tone: 'clay', name: '审批', path: '/pages/approval/approval' },
      { icon: '人', tone: 'plum', name: '家人', path: '/pages/members/members' },
      { icon: '讯', tone: 'honey', name: '提醒', path: '/pages/notifications/notifications' }
    ]
  }
]

function hello(name) {
  const h = new Date().getHours()
  const word = h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
  return name ? `${word}，${name}` : word
}

function dateText() {
  const d = new Date()
  const w = ['日', '一', '二', '三', '四', '五', '六']
  return `${d.getMonth() + 1}月${d.getDate()}日 周${w[d.getDay()]}`
}

Page({
  data: {
    groups,
    overview: {
      monthIncome: 0,
      monthExpense: 0,
      budgetRemaining: 0,
      pendingCount: 0,
      memberCount: 0
    },
    recent: [],
    currentMemberName: '',
    greeting: '你好',
    dateText: ''
  },

  onShow() {
    this.refresh()
  },

  async onPullDownRefresh() {
    await this.loadData()
    wx.stopPullDownRefresh()
  },

  refresh() {
    const name = getApp().globalData.currentMemberName || ''
    this.setData({
      currentMemberName: name,
      greeting: hello(name),
      dateText: dateText()
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
        dateText: util.formatDate(item.date),
        categoryText: util.categoryLabel(item.category)
      }))
      this.setData({ overview, recent: recentList })
    } catch (e) { /* 错误已提示 */ }
  },

  TAB_PATHS: ['/pages/record/record', '/pages/workbench/workbench', '/pages/statistics/statistics', '/pages/profile/profile'],

  goMenu(e) {
    const path = e.currentTarget.dataset.path
    const app = getApp()
    if (!app.globalData.currentMemberId) {
      wx.showModal({
        title: '提示',
        content: '请先在「我的」页面选择当前家庭成员',
        confirmText: '去选择',
        success: (res) => {
          if (res.confirm) wx.switchTab({ url: '/pages/profile/profile' })
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
      wx.switchTab({ url: base })
      return
    }
    wx.navigateTo({ url: path })
  },

  goRecord(e) {
    const type = e.currentTarget.dataset.type
    const app = getApp()
    if (!app.globalData.currentMemberId) {
      wx.showToast({ title: '请先在「我的」选择成员', icon: 'none' })
      return
    }
    app.globalData.recordType = type
    wx.switchTab({ url: '/pages/record/record' })
  }
})
