const api = require('../../utils/api')
const util = require('../../utils/util')

function hello(name) {
  const h = new Date().getHours()
  const word = h < 11 ? '早上好' : h < 14 ? '中午好' : h < 18 ? '下午好' : '晚上好'
  return name ? `${word}，${name}` : word
}

function asList(value) {
  if (Array.isArray(value)) return value
  if (value && Array.isArray(value.records)) return value.records
  return []
}

function openStatus(status) {
  return status !== 'COMPLETED' && status !== 'CANCELLED'
}

function planProgressOf(life, growth) {
  const scored = []
  let open = 0
  life.forEach((plan) => {
    if (plan.status === 'CANCELLED') return
    scored.push(plan.status === 'COMPLETED' ? 100 : plan.status === 'IN_PROGRESS' ? 50 : 0)
    if (openStatus(plan.status)) open += 1
  })
  growth.forEach((plan) => {
    if (plan.status === 'CANCELLED') return
    scored.push(plan.status === 'COMPLETED' ? 100 : Number(plan.progress || 0))
    if (openStatus(plan.status)) open += 1
  })
  const progress = scored.length ? Math.round(scored.reduce((sum, n) => sum + n, 0) / scored.length) : 0
  return { progress, open }
}

function buildNext(approvals, notes, life, growth) {
  const items = []
  asList(approvals).forEach((row) => {
    items.push({
      key: `a${row.id}`,
      kind: '待审',
      title: row.purpose,
      meta: `¥${util.formatMoney(row.amount)}`,
      path: '/pages/approval/approval'
    })
  })
  asList(notes).forEach((row) => {
    items.push({
      key: `n${row.id}`,
      kind: '未读',
      title: row.title,
      meta: row.content || '通知',
      path: '/pages/notifications/notifications'
    })
  })
  life.filter((plan) => openStatus(plan.status)).slice(0, 3).forEach((plan) => {
    items.push({
      key: `l${plan.id}`,
      kind: '生活',
      title: plan.planName,
      meta: plan.planDate || '生活计划',
      path: '/pages/lifePlan/lifePlan'
    })
  })
  growth.filter((plan) => openStatus(plan.status)).slice(0, 3).forEach((plan) => {
    items.push({
      key: `g${plan.id}`,
      kind: '成长',
      title: plan.title,
      meta: `${plan.progress || 0}%`,
      path: '/pages/growthPlan/growthPlan'
    })
  })
  return items.slice(0, 6)
}

Page({
  data: {
    greeting: '你好',
    balanceText: '0',
    pendingCount: 0,
    planProgress: 0,
    planHint: '还没有进行中的计划',
    unread: 0,
    nextList: []
  },

  onShow() {
    this.refresh()
  },

  refresh() {
    const name = getApp().globalData.currentMemberName || ''
    this.setData({ greeting: hello(name) })
    this.loadData()
  },

  async loadData() {
    const memberId = getApp().globalData.currentMemberId
    try {
      const [overview, unreadRes, notes, approvals, life, growth] = await Promise.all([
        api.dashboard.overview(),
        api.notification.unreadCount().catch(() => ({ count: 0 })),
        api.notification.list({ page: 1, size: 4, isRead: 0 }).catch(() => ({ records: [] })),
        memberId
          ? api.approval.page({ approverId: memberId, status: 'PENDING', page: 1, size: 4 }).catch(() => ({ records: [] }))
          : { records: [] },
        memberId ? api.lifePlan.list({ memberId }).catch(() => []) : [],
        memberId ? api.growthPlan.list({ memberId }).catch(() => []) : []
      ])
      const lifeList = asList(life)
      const growthList = asList(growth)
      const progress = planProgressOf(lifeList, growthList)
      const income = Number(overview.monthIncome || 0)
      const expense = Number(overview.monthExpense || 0)
      this.setData({
        balanceText: util.formatMoney(income - expense),
        pendingCount: overview.pendingCount || 0,
        planProgress: progress.progress,
        planHint: progress.open > 0 ? `${progress.open} 个还在进行` : '还没有进行中的计划',
        unread: Number((unreadRes && unreadRes.count) || 0),
        nextList: buildNext(approvals, notes, lifeList, growthList)
      })
    } catch (e) { /* 错误已提示 */ }
  },

  goMenu(e) {
    const path = e.currentTarget.dataset.path
    const app = getApp()
    if (!app.globalData.currentMemberId) {
      my.showModal({
        title: '提示',
        content: '请先在「我的」页面选择当前家庭成员',
        confirmText: '去选择',
        success: (res) => {
          if (res.confirm) my.reLaunch({ url: '/pages/profile/profile' })
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
      my.reLaunch({ url: path })
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
    my.reLaunch({ url: `/pages/record/record?type=${type}` })
  }
})
