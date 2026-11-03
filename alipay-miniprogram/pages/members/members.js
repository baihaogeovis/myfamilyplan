const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    list: [],
    currentId: null,
    showForm: false,
    formMode: 'add',
    form: {
      id: null,
      memberName: '',
      relation: 'CHILD',
      phone: '',
      remark: ''
    }
  },

  onShow() {
    this.setData({ currentId: getApp().globalData.currentMemberId })
    this.loadList()
  },

  async loadList() {
    try {
      const list = await api.member.list()
      this.setData({
        list: (list || []).map((m) => ({
          ...m,
          relationText: util.relationText(m.relation),
          roleText: m.relation === 'CHILD' ? '孩子' : (m.relation === 'OTHER' ? '家人' : '家长')
        }))
      })
    } catch (e) { /* 错误已提示 */ }
  },

  setCurrent(e) {
    const item = e.currentTarget.dataset.item
    getApp().setCurrentMember(item.id, item.memberName)
    this.setData({ currentId: item.id })
    my.showToast({ type: 'success', content: `现在是 ${item.memberName}` })
  },

  openAdd() {
    this.setData({
      showForm: true,
      formMode: 'add',
      form: { id: null, memberName: '', relation: 'CHILD', phone: '', remark: '' }
    })
  },

  openEdit(e) {
    const item = e.currentTarget.dataset.item
    this.setData({
      showForm: true,
      formMode: 'edit',
      form: {
        id: item.id,
        memberName: item.memberName,
        relation: item.relation || 'OTHER',
        phone: item.phone || '',
        remark: item.remark || ''
      }
    })
  },

  closeForm() { this.setData({ showForm: false }) },
  noop() {},
  onName(e) { this.setData({ 'form.memberName': e.detail.value }) },
  onPhone(e) { this.setData({ 'form.phone': e.detail.value }) },
  onRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  chooseRelation(e) { this.setData({ 'form.relation': e.currentTarget.dataset.value }) },

  async submitForm() {
    const { form, formMode } = this.data
    if (!form.memberName.trim()) {
      my.showToast({ type: 'none', content: '请输入成员姓名' })
      return
    }
    const payload = {
      memberName: form.memberName,
      relation: form.relation,
      phone: form.phone,
      remark: form.remark
    }
    try {
      if (formMode === 'add') {
        await api.member.add(payload)
      } else {
        await api.member.update({ ...payload, id: form.id })
      }
      this.setData({ showForm: false })
      my.showToast({ type: 'success', content: '保存成功' })
      this.loadList()
    } catch (e) { /* 错误已提示 */ }
  },

  removeItem(e) {
    const item = e.currentTarget.dataset.item
    my.confirm({
      title: '删除确认',
      content: `确定删除成员「${item.memberName}」吗？`,
      success: async (res) => {
        if (res.confirm) {
          try {
            await api.member.remove(item.id)
            my.showToast({ type: 'success', content: '已删除' })
            this.loadList()
          } catch (err) { /* 错误已提示 */ }
        }
      }
    })
  }
})
