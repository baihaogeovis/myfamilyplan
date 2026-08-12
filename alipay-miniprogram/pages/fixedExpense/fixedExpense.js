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

  closeForm() { this.setData({ showForm: false }) },
  noop() {},
  categoryLabel(v) { return util.categoryLabel(v) },
  onName(e) { this.setData({ 'form.expenseName': e.detail.value }) },
  onAmount(e) { this.setData({ 'form.amount': e.detail.value }) },
  onRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  onDueDay(e) { this.setData({ 'form.dueDay': this.data.days[Number(e.detail.value)] }) },
  onActiveChange(e) { this.setData({ 'form.active': e.detail.value }) },
  chooseCategory(e) { this.setData({ 'form.category': e.currentTarget.dataset.cat }) },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.expenseName.trim()) {
      my.showToast({ type: 'none', content: '请输入支出项目名称' })
      return
    }
    if (!form.amount || Number(form.amount) <= 0) {
      my.showToast({ type: 'none', content: '请输入有效金额' })
      return
    }
    if (!form.category) {
      my.showToast({ type: 'none', content: '请选择分类' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      my.showToast({ type: 'none', content: '请先选择家庭成员' })
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
      this.setData({ showForm: false })
      my.showToast({ type: 'success', content: '保存成功' })
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
    my.confirm({
      title: '删除确认',
      content: `确定删除固定支出「${item.expenseName}」吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.fixedExpense.remove(item.id)
            my.showToast({ type: 'success', content: '已删除' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
