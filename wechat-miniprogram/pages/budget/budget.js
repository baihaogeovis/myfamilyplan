const api = require('../../utils/api')
const util = require('../../utils/util')

const periodTypes = [
  { value: 'MONTHLY', label: '月度' },
  { value: 'YEARLY', label: '年度' },
  { value: 'CUSTOM', label: '自定义' }
]

Page({
  data: {
    periodTypes,
    periodType: 'MONTHLY',
    year: new Date().getFullYear(),
    month: new Date().getMonth() + 1,
    list: [],
    categories: util.expenseCategories(),
    years: (() => {
      const y = new Date().getFullYear()
      return [y - 1, y, y + 1]
    })(),
    months: [0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12],
    periodTypeIndex: 0,
    showForm: false,
    formMode: 'add',
    form: {
      id: null,
      amount: '',
      category: '',
      periodType: 'MONTHLY',
      year: 0,
      month: 0,
      remark: ''
    }
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
      this.setData({ list: [] })
      return Promise.resolve()
    }
    return api.budget.page({
      memberId,
      page: 1,
      size: 100,
      year: this.data.year,
      month: this.data.month,
      periodType: this.data.periodType
    }).then((res) => {
      const list = (res.records || []).map((item) => ({
        ...item,
        amountText: util.formatMoney(item.amount),
        periodLabel: this.periodLabel(item.periodType)
      }))
      this.setData({ list })
    }).catch(() => {})
  },

  periodLabel(type) {
    const found = periodTypes.find((p) => p.value === type)
    return found ? found.label : type
  },

  onPeriodChange(e) {
    this.setData({ periodType: e.detail.value }, () => this.reload())
  },

  openAdd() {
    const now = new Date()
    const periodType = this.data.periodType
    this.setData({
      showForm: true,
      formMode: 'add',
      periodTypeIndex: this.indexByPeriod(periodType),
      form: {
        id: null,
        amount: '',
        category: '',
        periodType,
        year: now.getFullYear(),
        month: now.getMonth() + 1,
        remark: ''
      }
    })
  },

  openEdit(e) {
    const item = e.currentTarget.dataset.item
    const periodType = item.periodType || 'MONTHLY'
    this.setData({
      showForm: true,
      formMode: 'edit',
      periodTypeIndex: this.indexByPeriod(periodType),
      form: {
        id: item.id,
        amount: String(item.amount),
        category: item.category || '',
        periodType,
        year: item.year || new Date().getFullYear(),
        month: item.month || new Date().getMonth() + 1,
        remark: item.remark || ''
      }
    })
  },

  indexByPeriod(type) {
    const idx = periodTypes.findIndex((p) => p.value === type)
    return idx === -1 ? 0 : idx
  },

  closeForm() { this.setData({ showForm: false }) },
  noop() {},

  onFormAmount(e) { this.setData({ 'form.amount': e.detail.value }) },
  onFormRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  chooseCategory(e) { this.setData({ 'form.category': e.currentTarget.dataset.cat }) },
  onPeriodTypeChange(e) {
    const idx = Number(e.detail.value)
    this.setData({
      'form.periodType': periodTypes[idx].value,
      periodTypeIndex: idx
    })
  },
  onYearChange(e) {
    const index = Number(e.detail.value)
    this.setData({ 'form.year': this.data.years[index] })
  },
  onMonthChange(e) {
    const index = Number(e.detail.value)
    this.setData({ 'form.month': this.data.months[index] })
  },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.amount || Number(form.amount) <= 0) {
      wx.showToast({ title: '请输入有效预算金额', icon: 'none' })
      return
    }
    if (!form.category) {
      wx.showToast({ title: '请选择预算分类', icon: 'none' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      wx.showToast({ title: '请先选择家庭成员', icon: 'none' })
      return
    }
    const payload = {
      memberId,
      amount: Number(form.amount),
      category: form.category,
      periodType: form.periodType,
      year: form.year,
      month: form.month,
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api.budget.add(payload)
      } else {
        await api.budget.update({ ...payload, id: form.id })
      }
      this.setData({ showForm: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '删除确认',
      content: '确定删除该预算吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.budget.remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
