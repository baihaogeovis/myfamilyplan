const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    list: [],
    page: 1,
    size: 20,
    total: 0,
    hasMore: true,
    loading: false,
    unreadCount: 0
  },

  onShow() {
    this.reload()
  },

  reload() {
    this.setData({ page: 1, list: [], hasMore: true })
    return Promise.all([this.loadUnread(), this.loadList()])
  },

  loadUnread() {
    return api.notification.unreadCount().then((data) => {
      this.setData({ unreadCount: data && data.count ? data.count : 0 })
    }).catch(() => {})
  },

  async loadList() {
    if (this.data.loading || !this.data.hasMore) return
    this.setData({ loading: true })
    try {
      const res = await api.notification.list({
        page: this.data.page,
        size: this.data.size
      })
      const records = (res.records || []).map((item) => ({
        ...item,
        dateText: util.formatDate(item.createTime),
        typeText: this.typeText(item.type)
      }))
      this.setData({
        list: [...this.data.list, ...records],
        total: res.total || 0,
        hasMore: this.data.list.length + records.length < (res.total || 0),
        page: this.data.page + 1
      })
    } catch (e) {
      // 错误已提示
    } finally {
      this.setData({ loading: false })
    }
  },

  typeText(type) {
    const map = { BILL: '缴费', APPROVAL: '审批', BUDGET: '预算', SYSTEM: '系统', TASK: '任务' }
    return map[type] || type || '系统'
  },

  onReachBottom() {
    this.loadList()
  },

  markRead(e) {
    const item = e.currentTarget.dataset.item
    if (item.isRead) return
    api.notification.read(item.id).then(() => {
      this.reload()
    }).catch(() => {})
  },

  markAllRead() {
    if (!this.data.unreadCount) {
      my.showToast({ type: 'none', content: '暂无未读通知' })
      return
    }
    my.confirm({
      title: '全部已读',
      content: '确定将所有通知标记为已读吗？',
      success: (res) => {
        if (res.confirm) {
          api.notification.readAll().then(() => {
            my.showToast({ type: 'success', content: '已全部标记已读' })
            this.reload()
          }).catch(() => {})
        }
      }
    })
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    my.confirm({
      title: '删除确认',
      content: '确定删除该通知吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.notification.remove(item.id)
            my.showToast({ type: 'success', content: '已删除' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
