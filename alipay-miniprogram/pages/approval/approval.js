const api = require('../../utils/api')
const util = require('../../utils/util')

const statuses = [
  { value: '', label: '全部' },
  { value: 'PENDING', label: '待审批' },
  { value: 'APPROVED', label: '已通过' },
  { value: 'REJECTED', label: '已拒绝' }
]

const statusMap = {
  PENDING: { text: '待审批', cls: 'tag-orange' },
  APPROVED: { text: '已通过', cls: 'tag-green' },
  REJECTED: { text: '已拒绝', cls: 'tag-red' }
}

Page({
  data: {
    statuses,
    status: '',
    members: [],
    list: [],
    page: 1,
    size: 20,
    total: 0,
    hasMore: true,
    loading: false,
    showForm: false,
    form: {
      amount: '',
      purpose: '',
      remark: ''
    }
  },

  onLoad() {
    this.loadMembers()
  },

  onShow() {
    this.reload()
  },

  async loadMembers() {
    try {
      const list = await api.member.list()
      this.setData({ members: list || [] })
    } catch (e) { /* 错误已提示 */ }
  },

  onStatusChange(e) {
    this.setData({ status: e.currentTarget.dataset.value }, () => this.reload())
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
      const res = await api.approval.page({
        page: this.data.page,
        size: this.data.size,
        status: this.data.status
      })
      const memberMap = {}
      this.data.members.forEach((m) => { memberMap[m.id] = m.memberName })

      const records = (res.records || []).map((item) => {
        const st = statusMap[item.status] || { text: item.status, cls: 'tag-gray' }
        return {
          ...item,
          amountText: util.formatMoney(item.amount),
          dateText: util.formatDate(item.createTime),
          statusText: st.text,
          statusCls: st.cls,
          applicantName: memberMap[item.applicantId] || `成员#${item.applicantId}`,
          isApplicant: item.applicantId === memberId,
          isPending: item.status === 'PENDING'
        }
      })
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

  openAdd() {
    this.setData({ showForm: true, form: { amount: '', purpose: '', remark: '' } })
  },

  closeForm() { this.setData({ showForm: false }) },
  noop() {},
  onFormAmount(e) { this.setData({ 'form.amount': e.detail.value }) },
  onFormPurpose(e) { this.setData({ 'form.purpose': e.detail.value }) },
  onFormRemark(e) { this.setData({ 'form.remark': e.detail.value }) },

  async submitForm() {
    const { form } = this.data
    if (!form.amount || Number(form.amount) <= 0) {
      my.showToast({ type: 'none', content: '请输入有效金额' })
      return
    }
    if (!form.purpose.trim()) {
      my.showToast({ type: 'none', content: '请填写申请事由' })
      return
    }
    const memberId = getApp().globalData.currentMemberId
    if (!memberId) {
      my.showToast({ type: 'none', content: '请先选择家庭成员' })
      return
    }
    try {
      await api.approval.add({
        applicantId: memberId,
        amount: Number(form.amount),
        purpose: form.purpose,
        remark: form.remark
      })
      this.setData({ showForm: false })
      my.showToast({ type: 'success', content: '提交成功' })
      this.reload()
    } catch (e) { /* 错误已提示 */ }
  },

  approve(e) {
    this.handleAudit(e, 'approve')
  },

  reject(e) {
    this.handleAudit(e, 'reject')
  },

  handleAudit(e, action) {
    const item = e.currentTarget.dataset.item
    const isApprove = action === 'approve'
    const memberId = getApp().globalData.currentMemberId
    my.confirm({
      title: isApprove ? '审批通过' : '审批拒绝',
      content: `确定${isApprove ? '通过' : '拒绝'}该笔${util.formatMoney(item.amount)}元的申请吗？`,
      success: async (res) => {
        if (!res.confirm) return
        try {
          if (isApprove) {
            await api.approval.approve(item.id, { approverId: memberId, remark: '同意' })
          } else {
            await api.approval.reject(item.id, { approverId: memberId, remark: '不同意' })
          }
          my.showToast({ type: 'success', content: '操作成功' })
          this.reload()
        } catch (err) { /* 错误已提示 */ }
      }
    })
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    my.confirm({
      title: '删除确认',
      content: '确定删除该申请吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.approval.remove(item.id)
            my.showToast({ type: 'success', content: '已删除' })
            this.reload()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
