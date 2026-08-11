const api = require('../../utils/api')
const util = require('../../utils/util')

Page({
  data: {
    month: '',
    year: 0,
    years: [],
    incomeCategory: [],
    expenseCategory: [],
    monthly: [],
    incomeTotal: 0,
    expenseTotal: 0,
    incomeGradient: '',
    expenseGradient: '',
    bars: []
  },

  onLoad() {
    const y = new Date().getFullYear()
    this.setData({
      month: util.currentMonth(),
      year: y,
      years: [y - 1, y, y + 1]
    })
  },

  onShow() {
    this.loadData()
  },

  async loadData() {
    try {
      const [category, monthly] = await Promise.all([
        api.statistics.category(this.data.month),
        api.statistics.monthly(this.data.year)
      ])

      const incomeTotal = this.total(category.income)
      const expenseTotal = this.total(category.expense)
      const monthlyList = monthly || []

      this.setData({
        incomeTotal,
        expenseTotal,
        incomeCategory: this.decorate(category.income, incomeTotal),
        expenseCategory: this.decorate(category.expense, expenseTotal),
        incomeGradient: this.buildGradient(category.income),
        expenseGradient: this.buildGradient(category.expense),
        bars: this.buildBars(monthlyList)
      })
    } catch (e) { /* 错误已提示 */ }
  },

  total(list) {
    return (list || []).reduce((sum, item) => sum + Number(item.amount || 0), 0)
  },

  decorate(list, total) {
    const colors = ['#67c23a', '#e6a23c', '#409eff', '#f56c6c', '#7c4dff', '#13c2c2', '#9254de', '#5b8ff9']
    return (list || []).map((item, index) => {
      const amount = Number(item.amount || 0)
      return {
        ...item,
        color: colors[index % colors.length],
        percent: total ? ((amount / total) * 100).toFixed(1) : '0.0'
      }
    })
  },

  buildGradient(list) {
    const total = this.total(list)
    if (!total) return ''
    const colors = ['#67c23a', '#e6a23c', '#409eff', '#f56c6c', '#7c4dff', '#13c2c2', '#9254de', '#5b8ff9']
    let cursor = 0
    const parts = (list || []).map((item, index) => {
      const pct = (Number(item.amount || 0) / total) * 100
      const from = cursor
      cursor += pct
      return `${colors[index % colors.length]} ${from.toFixed(2)}% ${cursor.toFixed(2)}%`
    })
    return `conic-gradient(${parts.join(',')})`
  },

  buildBars(monthly) {
    const max = Math.max(1, ...monthly.map((m) => Math.max(Number(m.income || 0), Number(m.expense || 0))))
    return monthly.map((m) => ({
      month: m.month,
      monthLabel: `${m.month}月`,
      incomeHeight: Math.round((Number(m.income || 0) / max) * 100),
      expenseHeight: Math.round((Number(m.expense || 0) / max) * 100)
    }))
  },

  onMonthChange(e) {
    this.setData({ month: e.detail.value }, () => this.loadData())
  },

  onYearChange(e) {
    this.setData({ year: this.data.years[Number(e.detail.value)] }, () => this.loadData())
  }
})
