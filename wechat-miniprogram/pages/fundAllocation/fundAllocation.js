const modal = require('../../utils/modal')
const api = require('../../utils/api')
const util = require('../../utils/util')

const COLORS = ['#E85D4C', '#2A9D8F', '#f7b733', '#3f7a56', '#8b6be0', '#35a7d7', '#c46a45', '#6d9a78']

function yearsAround() {
  const y = new Date().getFullYear()
  return [y - 1, y, y + 1]
}

Page({
  data: {
    period: 'MONTH',
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    years: yearsAround(),
    months: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
    categories: util.expenseCategories(),
    items: [],
    bar: [],
    totalText: '0',
    remainText: '100',
    complete: false,
    showForm: false,
    modalHold: false,
    formMode: 'add',
    form: { id: null, category: '', percentage: '' }
  },

  onShow() {
    this.reload()
  },

  onPullDownRefresh() {
    this.reload().then(() => wx.stopPullDownRefresh())
  },

  reload() {
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      this.setData({ items: [], bar: [], totalText: '0', remainText: '100', complete: false })
      return Promise.resolve()
    }
    const { period, year, month } = this.data
    const params = { memberId, period, year }
    if (period === 'MONTH') params.month = month
    return api.fundAllocation.list(params).then((res) => {
      const total = Number(res.totalPercentage || 0)
      const remaining = Number(res.remainingPercentage || 0)
      const scale = total > 100 ? 100 / total : 1
      const items = (res.items || []).map((item, index) => ({
        ...item,
        label: util.categoryLabel(item.category),
        percentText: this.formatPercent(item.percentage),
        color: COLORS[index % COLORS.length],
        width: Math.max(0, Number(item.percentage || 0)) + '%'
      }))
      const bar = items.map((item) => ({
        key: item.id,
        color: item.color,
        width: (Number(item.percentage || 0) * scale) + '%'
      }))
      this.setData({
        items,
        bar,
        totalText: this.formatPercent(total),
        remainText: this.formatPercent(remaining),
        complete: !!res.complete
      })
    }).catch(() => {})
  },

  formatPercent(value) {
    const num = Number(value || 0)
    return Number.isInteger(num) ? String(num) : num.toFixed(2).replace(/0+$/, '').replace(/\.$/, '')
  },

  switchPeriod(e) {
    const period = e.currentTarget.dataset.period
    if (period === this.data.period) return
    this.setData({ period }, () => this.reload())
  },

  onYearChange(e) {
    const year = this.data.years[Number(e.detail.value)]
    this.setData({ year }, () => this.reload())
  },

  onMonthChange(e) {
    const month = this.data.months[Number(e.detail.value)]
    this.setData({ month }, () => this.reload())
  },

  openAdd() {
    if (!modal.canOpen(this)) return
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      wx.showToast({ title: '请先在「我的」选择成员', icon: 'none' })
      return
    }
    this.setData({
      showForm: true,
      formMode: 'add',
      form: { id: null, category: '', percentage: '' }
    })
  },

  openEdit(e) {
    if (!modal.canOpen(this)) return
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        category: item.category || '',
        percentage: String(item.percentage)
      }
    })
  },

  closeForm() { modal.closeModal(this, { showForm: false }) },
  noop() {},
  chooseCategory(e) { this.setData({ 'form.category': e.currentTarget.dataset.cat }) },
  onPercent(e) { this.setData({ 'form.percentage': e.detail.value }) },

  projected(percentage, id) {
    const others = this.data.items
      .filter((item) => item.id !== id)
      .reduce((sum, item) => sum + Number(item.percentage || 0), 0)
    return Math.round((others + Number(percentage || 0)) * 100) / 100
  },

  async submitForm() {
    const { form, formMode, period, year, month } = this.data
    const percentage = Number(form.percentage)
    if (!form.category) {
      wx.showToast({ title: '请选择分类', icon: 'none' })
      return
    }
    if (!percentage || percentage <= 0 || percentage > 100) {
      wx.showToast({ title: '比例需大于 0 且不超过 100', icon: 'none' })
      return
    }
    if (this.projected(percentage, form.id) > 100) {
      wx.showToast({ title: '这个周期合计不能超过 100%', icon: 'none' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    const payload = {
      memberId,
      category: form.category,
      percentage,
      period,
      year,
      month: period === 'MONTH' ? month : null
    }
    try {
      if (formMode === 'add') {
        await api.fundAllocation.add(payload)
      } else {
        await api.fundAllocation.update({ ...payload, id: form.id })
      }
      modal.closeModal(this, { showForm: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '删除确认',
      content: '确定删除这条分配吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.fundAllocation.remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
