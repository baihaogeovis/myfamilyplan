const api = require('../../utils/api')
const util = require('../../utils/util')

const taskTypes = [
  { value: '', label: '全部' },
  { value: 'TODAY', label: '今日' },
  { value: 'PERIODIC', label: '周期' },
  { value: 'ONE_TIME', label: '一次性' }
]

Page({
  data: {
    taskTypes,
    taskType: '',
    date: '',
    tasks: [],
    stats: { total: 0, completed: 0 },
    showForm: false,
    formMode: 'add',
    form: {
      id: null,
      taskName: '',
      taskType: 'TODAY',
      priority: 'MEDIUM',
      remark: ''
    }
  },

  onLoad() {
    const today = util.today()
    this.setData({ date: today })
    wx.setNavigationBarTitle({ title: today })
  },

  onShow() {
    this.reload()
  },

  onPullDownRefresh() {
    this.reload().then(() => wx.stopPullDownRefresh())
  },

  onDateChange(e) {
    this.setData({ date: e.detail.value }, () => this.reload())
  },

  onTypeChange(e) {
    this.setData({ taskType: e.currentTarget.dataset.value }, () => this.reload())
  },

  reload() {
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      this.setData({ tasks: [], stats: { total: 0, completed: 0 } })
      return Promise.resolve()
    }
    return api.dailyTask.list({
      memberId,
      date: this.data.date
    }).then((tasks) => {
      const list = (tasks || []).filter((t) =>
        !this.data.taskType || t.taskType === this.data.taskType
      ).map((t) => ({
        ...t,
        priorityText: this.priorityText(t.priority),
        priorityCls: this.priorityCls(t.priority),
        typeText: this.typeText(t.taskType)
      }))
      const completed = list.filter((t) => t.completed).length
      this.setData({
        tasks: list,
        stats: { total: list.length, completed }
      })
    }).catch(() => {})
  },

  priorityText(p) {
    const map = { HIGH: '高', MEDIUM: '中', LOW: '低' }
    return map[p] || '中'
  },

  priorityCls(p) {
    const map = { HIGH: 'tag-red', MEDIUM: 'tag-orange', LOW: 'tag-gray' }
    return map[p] || 'tag-orange'
  },

  typeText(t) {
    const map = { TODAY: '今日', PERIODIC: '周期', ONE_TIME: '一次性' }
    return map[t] || t
  },

  toggleComplete(e) {
    const item = e.currentTarget.dataset.item
    api.dailyTask.complete(item.id, !item.completed).then(() => {
      this.reload()
    }).catch(() => {})
  },

  openAdd() {
    this.setData({
      showForm: true,
      formMode: 'add',
      form: { id: null, taskName: '', taskType: 'TODAY', priority: 'MEDIUM', remark: '' }
    })
  },

  openEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        taskName: item.taskName,
        taskType: item.taskType || 'TODAY',
        priority: item.priority || 'MEDIUM',
        remark: item.remark || ''
      }
    })
  },

  closeForm() { this.setData({ showForm: false }) },
  noop() {},
  onFormName(e) { this.setData({ 'form.taskName': e.detail.value }) },
  onFormRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  chooseType(e) { this.setData({ 'form.taskType': e.currentTarget.dataset.value }) },
  choosePriority(e) { this.setData({ 'form.priority': e.currentTarget.dataset.value }) },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.taskName.trim()) {
      wx.showToast({ title: '请输入任务名称', icon: 'none' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      wx.showToast({ title: '请先选择家庭成员', icon: 'none' })
      return
    }
    const payload = {
      memberId,
      taskName: form.taskName,
      taskType: form.taskType,
      priority: form.priority,
      taskDate: this.data.date,
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api.dailyTask.add(payload)
      } else {
        await api.dailyTask.update({ ...payload, id: form.id })
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
      content: `删除任务「${item.taskName}」？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.dailyTask.remove(item.id)
            wx.showToast({ title: '已删除', icon: 'success' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
