export interface Option {
  value: string
  label: string
}

/** 收入分类 */
export const INCOME_CATEGORIES: Option[] = [
  { value: 'SALARY', label: '工资' },
  { value: 'BONUS', label: '奖金' },
  { value: 'INVESTMENT', label: '投资' },
  { value: 'PART_TIME', label: '兼职' },
  { value: 'GIFT', label: '红包' },
  { value: 'OTHER', label: '其他' }
]

/** 支出分类 */
export const EXPENSE_CATEGORIES: Option[] = [
  { value: 'FOOD', label: '餐饮' },
  { value: 'TRANSPORT', label: '交通' },
  { value: 'SHOPPING', label: '购物' },
  { value: 'HOUSING', label: '居住' },
  { value: 'EDUCATION', label: '教育' },
  { value: 'MEDICAL', label: '医疗' },
  { value: 'ENTERTAINMENT', label: '娱乐' },
  { value: 'SAVINGS', label: '储蓄' },
  { value: 'OTHER', label: '其他' }
]

/** 预算周期/分类 */
export const BUDGET_CATEGORIES: Option[] = [
  ...EXPENSE_CATEGORIES,
  { value: 'TOTAL', label: '总预算' }
]

/** 固定支出分类 */
export const FIXED_EXPENSE_CATEGORIES: Option[] = [
  { value: 'PROPERTY', label: '物业费' },
  { value: 'PARKING', label: '车位费' },
  { value: 'PARKING_MANAGE', label: '车位管理费' },
  { value: 'PHONE', label: '话费' },
  { value: 'INTERNET', label: '网费' },
  { value: 'UTILITY', label: '水电气' },
  { value: 'OTHER', label: '其他' }
]

/** 儿童成长目标分类 */
export const GROWTH_CATEGORIES: Option[] = [
  { value: 'STUDY', label: '学习' },
  { value: 'SPORTS', label: '运动' },
  { value: 'ART', label: '艺术' },
  { value: 'HABIT', label: '习惯' },
  { value: 'SOCIAL', label: '社交' },
  { value: 'SELF', label: '自理' },
  { value: 'OTHER', label: '其他' }
]

/** 支出类型 */
export const EXPENSE_TYPES: Option[] = [
  { value: 'NORMAL', label: '普通' },
  { value: 'BILL', label: '账单' },
  { value: 'SAVING', label: '储蓄' }
]

/** 仅用于图表/下拉展示的完整分类中文映射 */
export const CATEGORY_LABEL_MAP: Record<string, string> = Object.fromEntries(
  [...INCOME_CATEGORIES, ...EXPENSE_CATEGORIES, ...BUDGET_CATEGORIES, ...FIXED_EXPENSE_CATEGORIES, ...GROWTH_CATEGORIES]
    .map((item) => [item.value, item.label])
)

export const categoryLabel = (value?: string): string =>
  value ? CATEGORY_LABEL_MAP[value] || value : ''

/** 奖励方案类型 */
export const REWARD_ITEM_TYPES: Option[] = [
  { value: 'SPEND', label: '花额度' },
  { value: 'EARN', label: '赚额度' }
]

/** 奖励方案计价方式 */
export const REWARD_PRICING_MODES: Option[] = [
  { value: 'TIMES', label: '按次' },
  { value: 'DURATION', label: '按时长' }
]

/** 奖励方案分类 */
export const REWARD_CATEGORIES: Option[] = [
  { value: 'STUDY', label: '学习' },
  { value: 'SPORTS', label: '运动' },
  { value: 'HABIT', label: '习惯' },
  { value: 'HOUSEWORK', label: '家务' },
  { value: 'SCREEN', label: '屏幕' },
  { value: 'TOY', label: '玩具' },
  { value: 'SNACK', label: '零食' },
  { value: 'ACTIVITY', label: '活动' },
  { value: 'OTHER', label: '其他' }
]

/** 额度流水方向 */
export const REWARD_DIRECTIONS: Option[] = [
  { value: 'GRANT', label: '周期发放' },
  { value: 'EARN', label: '行为赚取' },
  { value: 'SPEND', label: '消费扣减' },
  { value: 'EXPIRE', label: '周期清零' },
  { value: 'ADJUST', label: '手动调整' }
]

/** 额度流水状态 */
export const REWARD_TXN_STATUS: Option[] = [
  { value: 'PENDING', label: '待确认' },
  { value: 'CONFIRMED', label: '已生效' },
  { value: 'REJECTED', label: '已驳回' }
]

/** 额度发放周期 */
export const REWARD_PERIOD_TYPES: Option[] = [
  { value: 'WEEK', label: '按周发放' },
  { value: 'MONTH', label: '按月发放' }
]

const toLabelMap = (options: Option[]): Record<string, string> =>
  Object.fromEntries(options.map((item) => [item.value, item.label]))

const REWARD_CATEGORY_MAP = toLabelMap(REWARD_CATEGORIES)
const REWARD_DIRECTION_MAP = toLabelMap(REWARD_DIRECTIONS)
const REWARD_STATUS_MAP = toLabelMap(REWARD_TXN_STATUS)
const REWARD_ITEM_TYPE_MAP = toLabelMap(REWARD_ITEM_TYPES)
const REWARD_PRICING_MAP = toLabelMap(REWARD_PRICING_MODES)

export const rewardCategoryLabel = (value?: string): string =>
  value ? REWARD_CATEGORY_MAP[value] || value : '其他'

export const rewardDirectionLabel = (value?: string): string =>
  value ? REWARD_DIRECTION_MAP[value] || value : ''

export const rewardStatusLabel = (value?: string): string =>
  value ? REWARD_STATUS_MAP[value] || value : ''

export const rewardItemTypeLabel = (value?: string): string =>
  value ? REWARD_ITEM_TYPE_MAP[value] || value : ''

export const rewardPricingLabel = (value?: string): string =>
  value ? REWARD_PRICING_MAP[value] || value : ''
