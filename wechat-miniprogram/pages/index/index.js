const api = require('../../utils/api')
const util = require('../../utils/util')

const groups = [
  {
    title: '计划',
    hint: '今天要做的，和生活里想攒的',
    items: [
      { icon: '安', tone: 'sage', name: '今日安排', path: '/pages/workbench/workbench' },
      { icon: '活', tone: 'clay', name: '生活', path: '/pages/lifePlan/lifePlan' },
      { icon: '长', tone: 'plum', name: '成长', path: '/pages/growthPlan/growthPlan' }
    ]
  },
  {
    title: '钱',
    hint: '记账、账单，和要过目的支出',
    items: [
      { icon: '支', tone: 'clay', name: '记支出', path: '/pages/record/record?type=expense' },
      { icon: '收', tone: 'sage', name: '记收入', path: '/pages/record/record?type=income' },
      { icon: '预', tone: 'sage', name: '预算', path: '/pages/budget/budget' },
      { icon: '账', tone: 'ink', name: '固定账单', path: '/pages/fixedExpense/fixedExpense' },
      { icon: '审', tone: 'clay', name: '审批', path: '/pages/approval/approval' },
      { icon: '报', tone: 'ink', name: '月报', path: '/pages/statistics/statistics' }
    ]
  },
  {
    title: '我的',
    hint: '通知',
    items: [
      { icon: '讯', tone: 'honey', name: '通知', path: '/pages/notifications/notifications' }
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

  goMenu(e) {
    const path = e.currentTarget.dataset.path
    const app = getApp()
    if (!app.globalData.currentMemberId) {
      wx.showModal({
        title: '提示',
        content: '请先在「我的」页面选择当前家庭成员',
        confirmText: '去选择',
        success: (res) => {
          if (res.confirm) wx.reLaunch({ url: '/pages/profile/profile' })
        }
      })
      return
    }
    const base = path.split('?')[0]
    const query = path.split('?')[1]
    if (query) {
      query.split('&').forEach((kv) => {
        const idx = kv.indexOf('=')
        const key = kv.slice(0, idx)
        if (key === 'type') app.globalData.recordType = kv.slice(idx + 1)
      })
    }
    const roots = ['/pages/index/index', '/pages/members/members', '/pages/lifePlan/lifePlan', '/pages/record/record', '/pages/reward/reward', '/pages/profile/profile']
    if (roots.includes(base)) {
      wx.reLaunch({ url: path })
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
    wx.reLaunch({ url: `/pages/record/record?type=${type}` })
  }
})
