const modal = require('../../utils/modal')
const api = require('../../utils/api')
const util = require('../../utils/util')

const typeTabs = [
  { value: '', label: '全部' },
  { value: 'APPLIANCE', label: '家电' },
  { value: 'TRAVEL', label: '旅游' }
]

const statusMap = {
  PLANNING: { text: '规划中', cls: 'tag-blue' },
  IN_PROGRESS: { text: '进行中', cls: 'tag-orange' },
  COMPLETED: { text: '已完成', cls: 'tag-green' },
  CANCELLED: { text: '已取消', cls: 'tag-gray' }
}

Page({
  data: {
    typeTabs,
    planType: '',
    statuses: ['PLANNING', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'],
    list: [],
    showForm: false,
    modalHold: false,
    formMode: 'add',
    form: {
      id: null,
      planName: '',
      planType: 'APPLIANCE',
      category: '',
      budget: '',
      planDate: '',
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
    return api.lifePlan.list({ memberId, planType: this.data.planType }).then((list) => {
      this.setData({
        list: (list || []).map((item) => {
          const st = statusMap[item.status] || { text: item.status, cls: 'tag-gray' }
          return {
            ...item,
            typeText: util.lifePlanTypeText(item.planType),
            statusText: st.text,
            statusCls: st.cls,
            budgetText: util.formatMoney(item.budget),
            dateText: util.formatDate(item.planDate)
          }
        })
      })
    }).catch(() => {})
  },

  onTypeChange(e) {
    this.setData({ planType: e.currentTarget.dataset.value }, () => this.reload())
  },

  openAdd() {
    if (!modal.canOpen(this)) return
    this.setData({
      showForm: true,
      formMode: 'add',
      form: {
        id: null,
        planName: '',
        planType: 'APPLIANCE',
        category: '',
        budget: '',
        planDate: util.today(),
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
        planName: item.planName,
        planType: item.planType || 'APPLIANCE',
        category: item.category || '',
        budget: String(item.budget || ''),
        planDate: util.formatDate(item.planDate),
        remark: item.remark || ''
      }
    })
  },

  closeForm() { modal.closeModal(this, { showForm: false }) },
  noop() {},
  onName(e) { this.setData({ 'form.planName': e.detail.value }) },
  onBudget(e) { this.setData({ 'form.budget': e.detail.value }) },
  onCategory(e) { this.setData({ 'form.category': e.detail.value }) },
  onDate(e) { this.setData({ 'form.planDate': e.detail.value }) },
  onRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  chooseType(e) { this.setData({ 'form.planType': e.currentTarget.dataset.value }) },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.planName.trim()) {
      wx.showToast({ title: '请输入计划名称', icon: 'none' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      wx.showToast({ title: '请先选择家庭成员', icon: 'none' })
      return
    }
    const payload = {
      memberId,
      planName: form.planName,
      planType: form.planType,
      category: form.category,
      budget: Number(form.budget || 0),
      planDate: form.planDate,
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api.lifePlan.add(payload)
      } else {
        await api.lifePlan.update({ ...payload, id: form.id })
      }
      modal.closeModal(this, { showForm: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  changeStatus(e) {
    const item = e.currentTarget.dataset.item
    const status = e.currentTarget.dataset.status
    api.lifePlan.updateStatus(item.id, status).then(() => {
      wx.showToast({ title: '状态已更新', icon: 'success' })
      this.reload()
    }).catch(() => {})
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '删除确认',
      content: `删除计划「${item.planName}」？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.lifePlan.remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  },

  goGrowth() {
    wx.navigateTo({ url: '/pages/growthPlan/growthPlan' })
  }
})
