/**
 * 工具函数
 */
module.exports = {
  /**
   * 金额格式化: 1234.5 -> 1,234.50
   */
  formatMoney(value) {
    const num = Number(value || 0)
    return num.toLocaleString('zh-CN', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    })
  },

  /**
   * 日期格式化
   */
  formatDate(date) {
    if (!date) return ''
    const d = new Date(date)
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${d.getFullYear()}-${m}-${day}`
  },

  /**
   * 获取当前日期字符串 yyyy-MM-dd
   */
  today() {
    return this.formatDate(new Date())
  },

  /**
   * 获取当前月份字符串 yyyy-MM
   */
  currentMonth() {
    const d = new Date()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    return `${d.getFullYear()}-${m}`
  },

  /**
   * 关系文案
   */
  relationText(relation) {
    const map = {
      OWNER: '户主',
      SPOUSE: '配偶',
      CHILD: '子女',
      PARENT: '父母',
      OTHER: '其他'
    }
    return relation ? map[relation] || relation : '其他'
  },

  /**
   * 收入分类
   */
  incomeCategories() {
    return ['工资', '奖金', '兼职', '理财', '投资收益', '礼金', '报销', '其他']
  },

  /**
   * 支出分类
   */
  expenseCategories() {
    return ['餐饮', '购物', '交通', '居住', '水电燃气', '医疗', '教育', '娱乐', '人情', '育儿', '其他']
  },

  /**
   * 固定支出分类
   */
  fixedExpenseCategories() {
    return ['物业费', '车位费', '车位管理费', '话费', '网费', '水电气', '其他']
  },

  /**
   * 生活计划类型
   */
  lifePlanTypes() {
    return ['APPLIANCE', 'TRAVEL']
  },

  lifePlanTypeText(type) {
    return type === 'APPLIANCE' ? '家电计划' : '旅游计划'
  },

  /**
   * 成长计划类型
   */
  growthPlanTypes() {
    return ['SHORT', 'LONG']
  },

  growthPlanTypeText(type) {
    return type === 'SHORT' ? '短期目标' : '长期目标'
  }
}
