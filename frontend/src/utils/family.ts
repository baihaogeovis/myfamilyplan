/** 家人色与称呼。色板按成员 id 稳定循环，和 Beacon 的 per-member color 同一思路。 */

export interface MemberTone {
  bg: string
  ink: string
  dot: string
}

export const MEMBER_TONES: MemberTone[] = [
  { bg: '#e7f2ea', ink: '#1f6b45', dot: '#2f6b4f' },
  { bg: '#f8e6dc', ink: '#9a4a2c', dot: '#c46a45' },
  { bg: '#e5f3f8', ink: '#0b7285', dot: '#2f8eaa' },
  { bg: '#fff1d4', ink: '#8a5a08', dot: '#c4922a' },
  { bg: '#efe8fb', ink: '#5b3fb0', dot: '#7c5cbf' },
  { bg: '#fde8e6', ink: '#9b3328', dot: '#c45c4a' }
]

export function memberTone(id?: number | null): MemberTone {
  const n = Math.abs(Number(id) || 0)
  return MEMBER_TONES[n % MEMBER_TONES.length]
}

export function relationLabel(relation?: string): string {
  const map: Record<string, string> = {
    OWNER: '户主',
    SPOUSE: '配偶',
    CHILD: '子女',
    PARENT: '父母',
    OTHER: '其他'
  }
  return relation ? map[relation] || relation : '家人'
}

/** 家长 / 孩子，用来在界面上区分谁能审批、谁在攒奖励。 */
export function householdRole(relation?: string): '家长' | '孩子' | '家人' {
  if (relation === 'CHILD') return '孩子'
  if (relation === 'OWNER' || relation === 'SPOUSE' || relation === 'PARENT') return '家长'
  return '家人'
}

export function formatLocalDate(date = new Date()): string {
  const y = date.getFullYear()
  const m = String(date.getMonth() + 1).padStart(2, '0')
  const d = String(date.getDate()).padStart(2, '0')
  return `${y}-${m}-${d}`
}

const WEEKDAY = ['日', '一', '二', '三', '四', '五', '六']

export function formatFriendlyDate(date = new Date()): string {
  return `${date.getMonth() + 1}月${date.getDate()}日 周${WEEKDAY[date.getDay()]}`
}

export function greetingForHour(hour = new Date().getHours()): string {
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
}

export interface WeekDay {
  iso: string
  label: string
  dateNum: number
  isToday: boolean
}

/** 以周一为起点的一周，供首页和今日安排共用。 */
export function weekOf(anchorIso: string): WeekDay[] {
  const [y, m, d] = anchorIso.split('-').map(Number)
  const anchor = new Date(y, (m || 1) - 1, d || 1)
  const weekday = anchor.getDay()
  const mondayOffset = weekday === 0 ? -6 : 1 - weekday
  const start = new Date(anchor)
  start.setDate(anchor.getDate() + mondayOffset)
  const today = formatLocalDate()
  const labels = ['一', '二', '三', '四', '五', '六', '日']
  return labels.map((label, index) => {
    const day = new Date(start)
    day.setDate(start.getDate() + index)
    const iso = formatLocalDate(day)
    return { iso, label, dateNum: day.getDate(), isToday: iso === today }
  })
}
