import request from './request'

export interface RewardAccount {
  id?: number
  familyId?: number
  memberId: number
  balance?: number
  weeklyQuota?: number
  periodType?: string
  carryOver?: number
  remark?: string
  status?: number
}

export interface RewardItem {
  id?: number
  familyId?: number
  itemName: string
  itemType: string
  category?: string
  pricingMode?: string
  points: number
  unitMinutes?: number
  icon?: string
  color?: string
  sortOrder?: number
  active?: number
  remark?: string
}

export interface RewardTxn {
  id?: number
  familyId?: number
  memberId?: number
  itemId?: number
  itemName?: string
  category?: string
  direction?: string
  status?: string
  points?: number
  quantity?: number
  minutes?: number
  periodKey?: string
  operatorMemberId?: number
  confirmTime?: string
  remark?: string
  createTime?: string
}

export interface RewardCategoryItem {
  category: string
  points: number
}

export interface RewardSummary {
  memberId: number
  memberName: string
  balance: number
  weeklyQuota: number
  periodType: string
  carryOver: number
  periodKey: string
  granted: number
  earned: number
  spent: number
  expired: number
  adjusted: number
  pendingCount: number
  spendByCategory: RewardCategoryItem[]
}

export interface RewardTxnPage {
  records: RewardTxn[]
  total: number
  size: number
  current: number
}

export interface RewardTxnQuery {
  memberId?: number
  direction?: string
  status?: string
  startDate?: string
  endDate?: string
  page?: number
  size?: number
}

export function getRewardAccount(memberId: number) {
  return request.get<RewardAccount>('/reward/account', { params: { memberId } })
}

export function saveRewardAccount(data: RewardAccount) {
  return request.put<void>('/reward/account', data)
}

export function grantRewardQuota(memberId: number, points?: number) {
  return request.post<RewardTxn>('/reward/account/grant', null, { params: { memberId, points } })
}

export function getRewardSummary(memberId: number, periodKey?: string) {
  return request.get<RewardSummary>('/reward/summary', { params: { memberId, periodKey } })
}

export function spendReward(data: { memberId: number; itemId: number; quantity?: number; minutes?: number; remark?: string }) {
  return request.post<RewardTxn>('/reward/spend', data)
}

export function earnReward(data: { memberId: number; itemId: number; quantity?: number; remark?: string }) {
  return request.post<RewardTxn>('/reward/earn', data)
}

export function adjustReward(memberId: number, points: number, remark?: string) {
  return request.post<RewardTxn>('/reward/adjust', null, { params: { memberId, points, remark } })
}

export function getRewardTxnPage(params: RewardTxnQuery) {
  return request.get<RewardTxnPage>('/reward/txn/page', { params })
}

export function confirmRewardTxn(id: number) {
  return request.put<void>(`/reward/txn/${id}/confirm`)
}

export function rejectRewardTxn(id: number, remark?: string) {
  return request.put<void>(`/reward/txn/${id}/reject`, null, { params: { remark } })
}

export function deleteRewardTxn(id: number) {
  return request.delete<void>(`/reward/txn/${id}`)
}

export function getRewardItemList(itemType?: string) {
  return request.get<RewardItem[]>('/reward/item/list', { params: { itemType } })
}

export function addRewardItem(data: RewardItem) {
  return request.post<void>('/reward/item', data)
}

export function updateRewardItem(data: RewardItem) {
  return request.put<void>('/reward/item', data)
}

export function deleteRewardItem(id: number) {
  return request.delete<void>(`/reward/item/${id}`)
}

/** 按时长方案折算本次消耗额度: 整档向上取 × 每档额度, 与后端 RewardServiceImpl.calcPoints 保持一致 */
export function calcSpendPoints(item: RewardItem, minutes: number): number {
  const unit = item.points || 0
  if (item.pricingMode !== 'DURATION') {
    return unit
  }
  const step = item.unitMinutes && item.unitMinutes > 0 ? item.unitMinutes : 30
  if (!minutes || minutes <= 0) {
    return 0
  }
  return Math.ceil(minutes / step) * unit
}
