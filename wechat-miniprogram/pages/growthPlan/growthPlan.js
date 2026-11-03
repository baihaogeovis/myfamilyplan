const modal = require('../../utils/modal')
const api = require('../../utils/api')
const util = require('../../utils/util')

const typeTabs = [
  { value: '', label: '全部' },
  { value: 'SHORT', label: '短期' },
  { value: 'LONG', label: '长期' }
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
    goalType: '',
    list: [],
    showForm: false,
    modalHold: false,
    formMode: 'add',
    form: {
      id: null,
      title: '',
      goalType: 'SHORT',
      category: '',
      description: '',
      targetDate: '',
      progress: 0,
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
    return api.growthPlan.list({ memberId, goalType: this.data.goalType }).then((list) => {
      this.setData({
        list: (list || []).map((item) => {
          const st = statusMap[item.status] || { text: item.status, cls: 'tag-gray' }
          return {
            ...item,
            typeText: util.growthPlanTypeText(item.goalType),
            statusText: st.text,
            statusCls: st.cls,
            progress: Number(item.progress || 0),
            dateText: util.formatDate(item.targetDate)
          }
        })
      })
    }).catch(() => {})
  },

  onTypeChange(e) {
    this.setData({ goalType: e.currentTarget.dataset.value }, () => this.reload())
  },

  openAdd() {
    if (!modal.canOpen(this)) return
    this.setData({
      showForm: true,
      formMode: 'add',
      form: {
        id: null,
        title: '',
        goalType: 'SHORT',
        category: '',
        description: '',
        targetDate: util.today(),
        progress: 0,
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
        title: item.title,
        goalType: item.goalType || 'SHORT',
        category: item.category || '',
        description: item.description || '',
        targetDate: util.formatDate(item.targetDate),
        progress: Number(item.progress || 0),
        remark: item.remark || ''
      }
    })
  },

  closeForm() { modal.closeModal(this, { showForm: false }) },
  noop() {},
  onTitle(e) { this.setData({ 'form.title': e.detail.value }) },
  onCategory(e) { this.setData({ 'form.category': e.detail.value }) },
  onDesc(e) { this.setData({ 'form.description': e.detail.value }) },
  onDate(e) { this.setData({ 'form.targetDate': e.detail.value }) },
  onRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  onProgress(e) { this.setData({ 'form.progress': Number(e.detail.value) }) },
  chooseType(e) { this.setData({ 'form.goalType': e.currentTarget.dataset.value }) },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.title.trim()) {
      wx.showToast({ title: '请输入目标名称', icon: 'none' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      wx.showToast({ title: '请先选择家庭成员', icon: 'none' })
      return
    }
    const payload = {
      memberId,
      title: form.title,
      goalType: form.goalType,
      category: form.category,
      description: form.description,
      targetDate: form.targetDate,
      progress: form.progress,
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api.growthPlan.add(payload)
      } else {
        await api.growthPlan.update({ ...payload, id: form.id })
      }
      modal.closeModal(this, { showForm: false })
      wx.showToast({ title: '保存成功', icon: 'success' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  changeStatus(e) {
    const item = e.currentTarget.dataset.item
    const status = e.currentTarget.dataset.status
    api.growthPlan.updateStatus(item.id, status).then(() => {
      wx.showToast({ title: '状态已更新', icon: 'success' })
      this.reload()
    }).catch(() => {})
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    wx.showModal({
      title: '删除确认',
      content: `删除目标「${item.title}」？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.growthPlan.remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  },

  goLife() {
    wx.switchTab({ url: '/pages/lifePlan/lifePlan' })
  }
})
