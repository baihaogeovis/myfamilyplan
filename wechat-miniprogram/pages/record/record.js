const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    type: 'income', // income | expense
    categories: [],
    list: [],
    page: 1,
    size: 20,
    total: 0,
    hasMore: true,
    loading: false,
    // 弹窗表单
    showForm: false,
    formMode: 'add', // add | edit
    form: {
      id: null,
      amount: '',
      category: '',
      source: '',
      date: '',
      remark: ''
    }
  },

  onLoad(options) {
    // 优先使用全局传递的记账类型(首页快捷记账经 switchTab 进入)
    const globalType = getApp().globalData.recordType
    const type = (options && options.type) || globalType || 'income'
    const safeType = type === 'expense' ? 'expense' : 'income'
    getApp().globalData.recordType = ''
    this.setData({
      type: safeType,
      categories: safeType === 'income' ? util.incomeCategories() : util.expenseCategories()
    })
    wx.setNavigationBarTitle({
      title: safeType === 'income' ? '收入记录' : '支出记录'
    })
  },

  onShow() {
    const globalType = getApp().globalData.recordType
    if (globalType) {
      const safeType = globalType === 'expense' ? 'expense' : 'income'
      getApp().globalData.recordType = ''
      this.setData({
        type: safeType,
        categories: safeType === 'income' ? util.incomeCategories() : util.expenseCategories()
      })
      wx.setNavigationBarTitle({ title: safeType === 'income' ? '收入记录' : '支出记录' })
    }
    this.reload()
  },

  onPullDownRefresh() {
    this.reload().then(() => wx.stopPullDownRefresh())
  },

  switchType(e) {
    const type = e.currentTarget.dataset.type
    if (type === this.data.type) return
    this.setData({
      type,
      categories: type === 'income' ? util.incomeCategories() : util.expenseCategories()
    })
    wx.setNavigationBarTitle({ title: type === 'income' ? '收入记录' : '支出记录' })
    this.reload()
  },

  reload() {
    this.setData({ page: 1, list: [], hasMore: true })
    return this.loadList()
  },

  async loadList() {
    if (this.data.loading || !this.data.hasMore) return
    this.setData({ loading: true })
    const memberId = getApp().globalData.currentMemberId
    try {
      const res = await api[this.data.type].page({
        memberId,
        page: this.data.page,
        size: this.data.size
      })
      const records = (res.records || []).map((item) => ({
        ...item,
        amountText: util.formatMoney(item.amount),
        dateText: util.formatDate(item.date || item.incomeDate || item.expenseDate)
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

  onReachBottom() {
    this.loadList()
  },

  // ---------- 新增 / 编辑 ----------
  openAdd() {
    this.setData({
      showForm: true,
      formMode: 'add',
      form: {
        id: null,
        amount: '',
        category: '',
        source: '',
        date: util.today(),
        remark: ''
      }
    })
  },

  openEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        amount: String(item.amount),
        category: item.category || '',
        source: item.source || '',
        date: util.formatDate(item.date || item.incomeDate || item.expenseDate),
        remark: item.remark || ''
      }
    })
  },

  closeForm() {
    this.setData({ showForm: false })
  },

  noop() {},
  categoryLabel(v) { return util.categoryLabel(v) },

  onFormAmount(e) {
    this.setData({ 'form.amount': e.detail.value })
  },
  onFormSource(e) {
    this.setData({ 'form.source': e.detail.value })
  },
  onFormRemark(e) {
    this.setData({ 'form.remark': e.detail.value })
  },
  onFormDate(e) {
    this.setData({ 'form.date': e.detail.value })
  },
  chooseCategory(e) {
    this.setData({ 'form.category': e.currentTarget.dataset.cat })
  },

  async submitForm() {
    const { form, type, formMode } = this.data
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
      wx.showToast({ title: '请先在「我的」选择成员', icon: 'none' })
      return
    }
    const date = form.date || util.today()
    const payload = {
      memberId,
      amount: Number(form.amount),
      category: form.category,
      source: form.source,
      ...(type === 'income' ? { incomeDate: date } : { expenseDate: date }),
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api[type].add(payload)
      } else {
        await api[type].update({ ...payload, id: form.id })
      }
      this.setData({ showForm: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  async removeItem(e) {
    const item = e.currentTarget.dataset.item
    const { type } = this.data
    wx.showModal({
      title: '删除确认',
      content: `确定删除该${type === 'income' ? '收入' : '支出'}记录吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api[type].remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
