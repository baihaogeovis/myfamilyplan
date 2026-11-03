const modal = require('../../utils/modal')
const api = require('../../utils/api')
const util = require('../../utils/util')

const MEMBER_EDIT_DENIED = '只有家长可以添加或编辑家人'

function canEdit(list, currentId) {
  const people = list || []
  const current = people.find((m) => String(m.id) === String(currentId))
  if (!current) return people.length === 0
  return current.relation === 'OWNER' || current.relation === 'SPOUSE' || current.relation === 'PARENT'
}

Page({
  data: {
    list: [],
    currentId: null,
    loaded: false,
    canManage: false,
    showForm: false,
    modalHold: false,
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
    const currentId = getApp().globalData.currentMemberId
    try {
      const list = await api.member.list()
      const people = list || []
      this.setData({
        currentId,
        loaded: true,
        canManage: canEdit(people, currentId),
        list: people.map((m) => ({
          ...m,
          relationText: util.relationText(m.relation),
          roleText: m.relation === 'CHILD' ? '孩子' : (m.relation === 'OTHER' ? '家人' : '家长')
        }))
      })
    } catch (e) {
      this.setData({
        currentId,
        loaded: true,
        canManage: canEdit(this.data.list, currentId)
      })
    }
  },

  ensureCanManage() {
    if (!this.data.loaded) return false
    if (this.data.canManage) return true
    my.showToast({ type: 'none', content: MEMBER_EDIT_DENIED, duration: 2500 })
    return false
  },

  setCurrent(e) {
    const item = e.currentTarget.dataset.item
    getApp().setCurrentMember(item.id, item.memberName)
    this.setData({
      currentId: item.id,
      canManage: canEdit(this.data.list, item.id)
    })
    my.showToast({ type: 'success', content: `现在是 ${item.memberName}` })
  },

  openAdd() {
    if (!this.ensureCanManage()) return
    if (!modal.canOpen(this)) return
    this.setData({
      showForm: true,
      formMode: 'add',
      form: { id: null, memberName: '', relation: 'CHILD', phone: '', remark: '' }
    })
  },

  openEdit(e) {
    if (!this.ensureCanManage()) return
    if (!modal.canOpen(this)) return
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

  closeForm() { modal.closeModal(this, { showForm: false }) },
  noop() {},
  onName(e) { this.setData({ 'form.memberName': e.detail.value }) },
  onPhone(e) { this.setData({ 'form.phone': e.detail.value }) },
  onRemark(e) { this.setData({ 'form.remark': e.detail.value }) },
  chooseRelation(e) { this.setData({ 'form.relation': e.currentTarget.dataset.value }) },

  async submitForm() {
    if (!this.ensureCanManage()) return
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
      modal.closeModal(this, { showForm: false })
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
