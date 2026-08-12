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
    return [
      { value: 'SALARY', label: '工资' },
      { value: 'BONUS', label: '奖金' },
      { value: 'SIDEJOB', label: '兼职' },
      { value: 'INVESTMENT', label: '理财' },
      { value: 'INVESTRETURN', label: '投资收益' },
      { value: 'GIFT', label: '礼金' },
      { value: 'REIMBURSE', label: '报销' },
      { value: 'OTHER', label: '其他' }
    ]
  },

  /**
   * 支出分类
   */
  expenseCategories() {
    return [
      { value: 'FOOD', label: '餐饮' },
      { value: 'SHOPPING', label: '购物' },
      { value: 'TRANSPORT', label: '交通' },
      { value: 'HOUSING', label: '居住' },
      { value: 'UTILITY', label: '水电燃气' },
      { value: 'MEDICAL', label: '医疗' },
      { value: 'EDUCATION', label: '教育' },
      { value: 'ENTERTAINMENT', label: '娱乐' },
      { value: 'SOCIAL', label: '人情' },
      { value: 'CHILDCARE', label: '育儿' },
      { value: 'OTHER', label: '其他' }
    ]
  },

  /**
   * 固定支出分类
   */
  fixedExpenseCategories() {
    return [
      { value: 'PROPERTY', label: '物业费' },
      { value: 'PARKING', label: '车位费' },
      { value: 'PARKMGMT', label: '车位管理费' },
      { value: 'PHONE', label: '话费' },
      { value: 'INTERNET', label: '网费' },
      { value: 'UTILITY', label: '水电气' },
      { value: 'OTHER', label: '其他' }
    ]
  },

  /**
   * 分类中文翻译
   */
  categoryLabel(value) {
    if (!value) return ''
    const map = {
      SALARY: '工资', BONUS: '奖金', SIDEJOB: '兼职', INVESTMENT: '理财',
      INVESTRETURN: '投资收益', GIFT: '礼金', REIMBURSE: '报销',
      FOOD: '餐饮', SHOPPING: '购物', TRANSPORT: '交通', HOUSING: '居住',
      UTILITY: '水电燃气', MEDICAL: '医疗', EDUCATION: '教育',
      ENTERTAINMENT: '娱乐', SOCIAL: '人情', CHILDCARE: '育儿',
      PROPERTY: '物业费', PARKING: '车位费', PARKMGMT: '车位管理费',
      PHONE: '话费', INTERNET: '网费',
      OTHER: '其他'
    }
    return map[value] || value
  },

  /**
   * 支出类型
   */
  expenseTypes() {
    return [
      { value: 'NORMAL', label: '普通' },
      { value: 'BILL', label: '必需' },
      { value: 'SAVING', label: '储蓄' }
    ]
  },

  expenseTypeLabel(value) {
    const map = { NORMAL: '普通', BILL: '必需', SAVING: '储蓄' }
    return map[value] || value
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
