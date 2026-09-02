const api = require('../../utils/api')
const util = require('../../utils/util')

const tabs = [
  { key: 'spend', label: '花额度' },
  { key: 'earn', label: '赚额度' },
  { key: 'pending', label: '待确认' },
  { key: 'txn', label: '流水' },
  { key: 'setting', label: '设置' }
]

const directionCls = {
  GRANT: 'tag-blue',
  EARN: 'tag-green',
  SPEND: 'tag-red',
  EXPIRE: 'tag-gray',
  ADJUST: 'tag-orange'
}

const statusCls = {
  PENDING: 'tag-orange',
  CONFIRMED: 'tag-green',
  REJECTED: 'tag-gray'
}

function stepOf(item) {
  return item.unitMinutes && item.unitMinutes > 0 ? item.unitMinutes : 30
}

/**
 * 折算本次额度: 按时长整档向上取 × 每档额度, 与后端 RewardServiceImpl.calcPoints 保持一致
 */
function calcCost(item, minutes, quantity) {
  const points = item.points || 0
  if (item.pricingMode === 'DURATION') {
    return Math.ceil((minutes || 0) / stepOf(item)) * points
  }
  return (quantity || 1) * points
}

function decorateItem(item, balance) {
  const step = stepOf(item)
  const isDuration = item.pricingMode === 'DURATION'
  const minutes = item.minutes || step
  const quantity = item.quantity || 1
  const cost = calcCost(item, minutes, quantity)
  const color = item.color || '#2fae52'
  return Object.assign({}, item, {
    step: step,
    isDuration: isDuration,
    minutes: isDuration ? minutes : 0,
    quantity: quantity,
    cost: cost,
    steps: isDuration ? Math.ceil(minutes / step) : 0,
    initial: (item.itemName || '?').charAt(0),
    iconBg: color + '20',
    iconColor: color,
    priceText: isDuration
      ? `${item.points} 额度 / ${step} 分钟`
      : `${item.points} 额度 / 次`,
    categoryText: util.rewardCategoryText(item.category),
    lack: item.itemType === 'SPEND' && cost > (balance || 0)
  })
}

function decorateTxn(row) {
  const points = row.points || 0
  return Object.assign({}, row, {
    directionText: util.rewardDirectionText(row.direction),
    directionCls: directionCls[row.direction] || 'tag-gray',
    statusText: util.rewardStatusText(row.status),
    statusCls: statusCls[row.status] || 'tag-gray',
    pointsText: points > 0 ? `+${points}` : `${points}`,
    gainText: points > 0 ? 'gain' : 'cost',
    detailText: row.minutes ? `${row.minutes} 分钟` : (row.quantity > 1 ? `${row.quantity} 次` : ''),
    costAbs: Math.abs(points),
    timeText: util.formatDateTime(row.createTime),
    revocable: row.status === 'CONFIRMED' && row.direction === 'SPEND'
  })
}

function emptyItemForm() {
  return {
    id: null,
    itemName: '',
    itemType: 'SPEND',
    category: 'OTHER',
    pricingMode: 'TIMES',
    points: 10,
    unitMinutes: 30,
    sortOrder: 0,
    remark: ''
  }
}

Page({
  data: {
    tabs: tabs,
    tab: 'spend',
    children: [],
    childId: null,
    summary: null,
    spendItems: [],
    earnItems: [],
    pendingList: [],
    txnList: [],
    txnPage: 1,
    txnHasMore: false,
    setting: { weeklyQuota: 0, periodType: 'WEEK', carryOver: 1 },
    periodTypes: util.rewardPeriodTypes(),
    itemTypes: util.rewardItemTypes(),
    pricingModes: util.rewardPricingModes(),
    categories: util.rewardCategories(),
    adjust: { minus: false, points: 10, remark: '' },
    showItemForm: false,
    itemFormMode: 'add',
    itemForm: emptyItemForm()
  },

  onShow() {
    this.boot()
  },


  /** 孩子列表: 优先展示子女, 兜底展示全部成员 */
  boot() {
    return api.member.list().then((list) => {
      const members = list || []
      const kids = members.filter((m) => m.relation === 'CHILD')
      const pool = kids.length > 0 ? kids : members
      const children = pool.map((m) => ({
        id: m.id,
        name: m.memberName,
        initial: (m.memberName || '?').charAt(0),
        relationText: util.relationText(m.relation)
      }))
      const stored = getApp().globalData.currentMemberId
      const keep = children.filter((c) => c.id === this.data.childId)[0] ||
        children.filter((c) => c.id === stored)[0] || children[0]
      this.setData({ children: children, childId: keep ? keep.id : null }, () => this.reloadAll())
    }).catch(() => {})
  },

  reloadAll() {
    if (!this.data.childId) return Promise.resolve()
    return this.loadSummary().then(() => Promise.all([
      this.loadItems(),
      this.loadPending(),
      this.loadTxn(1)
    ])).catch(() => {})
  },

  loadSummary() {
    return api.reward.summary(this.data.childId).then((summary) => {
      this.setData({
        summary: summary,
        'setting.weeklyQuota': summary.weeklyQuota || 0,
        'setting.periodType': summary.periodType || 'WEEK',
        'setting.carryOver': summary.carryOver === 0 ? 0 : 1
      })
    })
  },

  loadItems() {
    return api.reward.itemList().then((list) => {
      const balance = this.data.summary ? this.data.summary.balance : 0
      const previous = {}
      this.data.spendItems.concat(this.data.earnItems).forEach((item) => {
        previous[item.id] = item
      })
      const kept = (list || []).map((raw) => {
        const draft = previous[raw.id]
        // 只回填步进器输入，避免用旧的额度/价格覆盖刚改过的方案
        const item = draft
          ? Object.assign({}, raw, { minutes: draft.minutes, quantity: draft.quantity })
          : raw
        return decorateItem(item, balance)
      })
      this.setData({
        spendItems: kept.filter((i) => i.itemType === 'SPEND'),
        earnItems: kept.filter((i) => i.itemType === 'EARN')
      })
    })
  },

  loadPending() {
    return api.reward.txnPage({ memberId: this.data.childId, status: 'PENDING', page: 1, size: 50 })
      .then((res) => this.setData({ pendingList: (res.records || []).map(decorateTxn) }))
  },

  loadTxn(page) {
    const target = page || 1
    return api.reward.txnPage({ memberId: this.data.childId, page: target, size: 10 }).then((res) => {
      const records = (res.records || []).map(decorateTxn)
      const merged = target === 1 ? records : this.data.txnList.concat(records)
      this.setData({
        txnList: merged,
        txnPage: target,
        txnHasMore: target * (res.size || 10) < (res.total || 0)
      })
    })
  },

  switchChild(e) {
    const id = Number(e.currentTarget.dataset.id)
    const child = this.data.children.filter((c) => c.id === id)[0]
    if (!child || id === this.data.childId) return
    getApp().setCurrentMember(child.id, child.name)
    this.setData({ childId: id, txnList: [], txnPage: 1 }, () => this.reloadAll())
  },

  onTab(e) {
    this.setData({ tab: e.currentTarget.dataset.key })
  },

  /** 次数/时长步进: group=spend|earn, field=quantity|minutes */
  onStep(e) {
    const ds = e.currentTarget.dataset
    const group = ds.group === 'earn' ? 'earnItems' : 'spendItems'
    const list = this.data[group]
    const index = Number(ds.index)
    const item = list[index]
    if (!item) return
    const next = Object.assign({}, item)
    if (ds.field === 'minutes') {
      next.minutes = Math.max(ds.delta > 0 ? item.step : 0, (item.minutes || 0) + Number(ds.delta) * item.step)
    } else {
      next.quantity = Math.max(1, (item.quantity || 1) + Number(ds.delta))
    }
    next.cost = calcCost(next, next.minutes, next.quantity)
    next.steps = next.isDuration ? Math.ceil(next.minutes / next.step) : 0
    next.lack = group === 'spendItems' && next.cost > (this.data.summary ? this.data.summary.balance : 0)
    this.setData({ [`${group}[${index}]`]: next })
  },

  doSpend(e) {
    const item = this.data.spendItems[Number(e.currentTarget.dataset.index)]
    if (!item) return
    api.reward.spend({
      memberId: this.data.childId,
      itemId: item.id,
      quantity: item.quantity,
      minutes: item.isDuration ? item.minutes : undefined
    }).then(() => {
      my.showToast({ type: 'none', content: '已提交，请在待确认中通过' })
      this.setData({ tab: 'pending' })
      return this.reloadAll()
    }).catch(() => {})
  },

  doEarn(e) {
    const item = this.data.earnItems[Number(e.currentTarget.dataset.index)]
    if (!item) return
    api.reward.earn({ memberId: this.data.childId, itemId: item.id, quantity: item.quantity })
      .then(() => {
        my.showToast({ type: 'success', content: `已奖励 ${item.cost} 额度` })
        return this.reloadAll()
      }).catch(() => {})
  },

  doGrant() {
    api.reward.grant(this.data.childId).then(() => {
      my.showToast({ type: 'success', content: '发放成功' })
      return this.reloadAll()
    }).catch(() => {})
  },

  doConfirm(e) {
    const row = this.data.pendingList[Number(e.currentTarget.dataset.index)]
    my.confirm({
      title: '确认消费',
      content: `通过「${row.itemName}」将扣减 ${row.costAbs} 个额度`,
      success: (res) => {
        if (!res.confirm) return
        api.reward.confirmTxn(row.id).then(() => {
          my.showToast({ type: 'success', content: '已通过' })
          return this.reloadAll()
        }).catch(() => {})
      }
    })
  },

  doReject(e) {
    const row = this.data.pendingList[Number(e.currentTarget.dataset.index)]
    my.confirm({
      title: '驳回消费',
      content: `驳回「${row.itemName}」？不会扣减额度`,
      success: (res) => {
        if (!res.confirm) return
        api.reward.rejectTxn(row.id).then(() => {
          my.showToast({ type: 'success', content: '已驳回' })
          return this.reloadAll()
        }).catch(() => {})
      }
    })
  },

  /** 已生效的消费按撤销处理并回补额度 */
  doDeleteTxn(e) {
    const row = this.data.txnList[Number(e.currentTarget.dataset.index)]
    my.confirm({
      title: row.revocable ? '撤销消费' : '删除记录',
      content: row.revocable
        ? `撤销后「${row.itemName}」的 ${row.costAbs} 个额度将退回孩子账户`
        : '确定删除这条额度记录？',
      success: (res) => {
        if (!res.confirm) return
        api.reward.removeTxn(row.id).then(() => {
          my.showToast({ type: 'success', content: row.revocable ? '已撤销' : '已删除' })
          return this.reloadAll()
        }).catch(() => {})
      }
    })
  },

  loadMoreTxn() {
    if (this.data.txnHasMore) this.loadTxn(this.data.txnPage + 1)
  },

  onSettingInput(e) {
    this.setData({ 'setting.weeklyQuota': Number(e.detail.value) || 0 })
  },

  choosePeriodType(e) {
    this.setData({ 'setting.periodType': e.currentTarget.dataset.value })
  },

  toggleCarry() {
    this.setData({ 'setting.carryOver': this.data.setting.carryOver ? 0 : 1 })
  },

  saveSetting() {
    api.reward.saveAccount({
      memberId: this.data.childId,
      weeklyQuota: this.data.setting.weeklyQuota,
      periodType: this.data.setting.periodType,
      carryOver: this.data.setting.carryOver
    }).then(() => {
      my.showToast({ type: 'success', content: '设置已保存' })
      return this.reloadAll()
    }).catch(() => {})
  },

  onAdjustField(e) {
    const field = e.currentTarget.dataset.field
    const value = field === 'remark' ? e.detail.value : Math.abs(Number(e.detail.value) || 0)
    this.setData({ [`adjust.${field}`]: value })
  },

  onAdjustStep(e) {
    const delta = Number(e.currentTarget.dataset.delta)
    this.setData({ 'adjust.points': Math.max(0, (this.data.adjust.points || 0) + delta) })
  },

  toggleAdjustSign() {
    this.setData({ 'adjust.minus': !this.data.adjust.minus })
  },

  doAdjust() {
    const points = this.data.adjust.minus ? -this.data.adjust.points : this.data.adjust.points
    if (!points) {
      my.showToast({ type: 'none', content: '调整额度不能为 0' })
      return
    }
    api.reward.adjust(this.data.childId, points, this.data.adjust.remark).then(() => {
      my.showToast({ type: 'success', content: '调整成功' })
      this.setData({ 'adjust.remark': '' })
      return this.reloadAll()
    }).catch(() => {})
  },

  /** 活动方案维护 */
  openAddItem() {
    this.setData({ showItemForm: true, itemFormMode: 'add', itemForm: emptyItemForm() })
  },

  openEditItem(e) {
    const source = e.currentTarget.dataset.group === 'earn' ? this.data.earnItems : this.data.spendItems
    const item = source[Number(e.currentTarget.dataset.index)]
    this.setData({
      showItemForm: true,
      itemFormMode: 'edit',
      itemForm: {
        id: item.id,
        itemName: item.itemName,
        itemType: item.itemType,
        category: item.category || 'OTHER',
        pricingMode: item.pricingMode || 'TIMES',
        points: item.points || 0,
        unitMinutes: item.unitMinutes || 30,
        sortOrder: item.sortOrder || 0,
        remark: item.remark || ''
      }
    })
  },

  closeItemForm() {
    this.setData({ showItemForm: false })
  },

  noop() {},

  onItemInput(e) {
    const field = e.currentTarget.dataset.field
    const value = ['itemName', 'remark'].indexOf(field) >= 0
      ? e.detail.value
      : Number(e.detail.value) || 0
    this.setData({ [`itemForm.${field}`]: value })
  },

  chooseItemField(e) {
    this.setData({ [`itemForm.${e.currentTarget.dataset.field}`]: e.currentTarget.dataset.value })
  },

  submitItem() {
    const form = this.data.itemForm
    if (!form.itemName.trim()) {
      my.showToast({ type: 'none', content: '请输入活动名称' })
      return
    }
    if (!form.points || form.points <= 0) {
      my.showToast({ type: 'none', content: '请填写每次/每档额度' })
      return
    }
    const payload = {
      id: form.id,
      itemName: form.itemName,
      itemType: form.itemType,
      category: form.category,
      pricingMode: form.pricingMode,
      points: form.points,
      unitMinutes: form.pricingMode === 'DURATION' ? (form.unitMinutes || 30) : undefined,
      sortOrder: form.sortOrder,
      active: 1
    }
    const action = this.data.itemFormMode === 'add'
      ? api.reward.addItem(payload)
      : api.reward.updateItem(payload)
    action.then(() => {
      this.setData({ showItemForm: false })
      my.showToast({ type: 'success', content: '保存成功' })
      return this.reloadAll()
    }).catch(() => {})
  },

  removeItem(e) {
    const ds = e.currentTarget.dataset
    const source = ds.group === 'earn' ? this.data.earnItems : this.data.spendItems
    const item = source[Number(ds.index)]
    my.confirm({
      title: '删除方案',
      content: `删除「${item.itemName}」？历史流水不受影响`,
      success: (res) => {
        if (!res.confirm) return
        api.reward.removeItem(item.id).then(() => {
          my.showToast({ type: 'success', content: '已删除' })
          return this.reloadAll()
        }).catch(() => {})
      }
    })
  }
})
