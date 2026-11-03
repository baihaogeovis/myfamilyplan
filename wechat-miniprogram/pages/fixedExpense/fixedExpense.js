const modal = require('../../utils/modal')
const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    categories: util.fixedExpenseCategories(),
    days: Array.from({ length: 28 }, (_, i) => i + 1),
    list: [],
    overview: null,
    year: 0,
    month: 0,
    showForm: false,
    modalHold: false,
    formMode: 'add',
    form: {
      id: null,
      expenseName: '',
      category: '',
      amount: '',
      dueDay: 1,
      active: true,
      remark: ''
    }
  },

  onLoad() {
    this.setData({ year: new Date().getFullYear(), month: new Date().getMonth() + 1 })
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
      this.setData({ list: [], overview: null })
      return Promise.resolve()
    }
    return Promise.all([
      api.fixedExpense.list({ memberId }).then((list) => {
        this.setData({
          list: (list || []).map((item) => ({
            ...item,
            amountText: util.formatMoney(item.amount),
            activeText: item.active ? '启用' : '停用'
          }))
        })
      }),
      api.fixedExpense.overview({ memberId, year: this.data.year, month: this.data.month })
        .then((ov) => {
          this.setData({ overview: ov })
        })
    ]).catch(() => {})
  },

  openAdd() {
    if (!modal.canOpen(this)) return
    this.setData({
      showForm: true,
      formMode: 'add',
      form: {
        id: null,
        expenseName: '',
        category: '',
        amount: '',
        dueDay: 1,
        active: true,
        remark: ''
      }
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
        expenseName: item.expenseName,
        category: item.category || '',
        amount: String(item.amount || ''),
        dueDay: item.dueDay || 1,
        active: item.active !== false,
        remark: item.remark || ''
      }
    })
  },

  closeForm() { modal.closeModal(this, { showForm: false }) },
  noop() {},
  categoryLabel(v) { return util.categoryLabel(v) },
  onName(e) { this.setData({ 'form.expenseName': e.detail.value }) },
  onAmount(e) { this.setData({ 'form.amount': e.detail.value }) },
  onRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  onDueDay(e) { this.setData({ 'form.dueDay': Number(e.detail.value) }) },
  onActiveChange(e) { this.setData({ 'form.active': e.detail.value }) },
  chooseCategory(e) { this.setData({ 'form.category': e.currentTarget.dataset.cat }) },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.expenseName.trim()) {
      wx.showToast({ title: '请输入支出项目名称', icon: 'none' })
      return
    }
    if (!form.amount || Number(form.amount) <= 0) {
      wx.showToast({ title: '请输入有效金额', icon: 'none' })
      return
    }
    if (!form.category) {
      wx.showToast({ title: '请选择分类', icon: 'none' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      wx.showToast({ title: '请先选择家庭成员', icon: 'none' })
      return
    }
    const payload = {
      memberId,
      expenseName: form.expenseName,
      category: form.category,
      amount: Number(form.amount),
      dueDay: form.dueDay,
      active: form.active,
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api.fixedExpense.add(payload)
      } else {
        await api.fixedExpense.update({ ...payload, id: form.id })
      }
      modal.closeModal(this, { showForm: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  toggleActive(e) {
    const item = e.currentTarget.dataset.item
    api.fixedExpense.update({ ...item, active: !item.active }).then(() => {
      this.reload()
    }).catch(() => {})
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '删除确认',
      content: `确定删除固定支出「${item.expenseName}」吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.fixedExpense.remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
